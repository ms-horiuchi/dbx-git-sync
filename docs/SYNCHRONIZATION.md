# 同期機能の詳細仕様

## 概要

このドキュメントでは、Dropbox-Git Syncアプリケーションの双方向同期機能について、詳細な仕様と動作フローを説明します。

## 同期方向

アプリケーションは2つの同期方向をサポートしています：

1. **Dropbox → Git** (`--direction dbx-to-git`)
2. **Git → Dropbox** (`--direction git-to-dbx`)

各方向で異なる差分検出メカニズムと処理フローを使用します。

---

## Dropbox → Git 同期

### 概要

Dropbox上の変更を検出し、GitHubリポジトリに反映します。カーソルベースの差分検出により、効率的に変更のみを同期します。

### 処理フロー

```
1. 対象ディレクトリ一覧を取得
   ↓
2. 各ディレクトリに対して:
   a. カーソルを読み込み
   b. Dropbox APIで差分を取得
   c. 対象拡張子のファイルをフィルタリング
   ↓
3. ディレクトリ名をブランチ名として使用
   a. ブランチをチェックアウト（存在しない場合は作成）
   ↓
4. 変更ファイルをダウンロード
   a. CREATE_OR_UPDATE: ファイルをダウンロード
   b. DELETE: ローカルファイルを削除
   ↓
5. Gitにコミット＆プッシュ
   ↓
6. カーソルを更新（次回の差分検出用）
```

### 差分検出メカニズム

#### カーソルベースの差分検出

Dropbox APIの`list_folder/continue`エンドポイントを使用：

1. **初回実行**: カーソルが存在しない場合、全ファイルを取得
2. **2回目以降**: 前回のカーソルから現在までの差分のみを取得

#### カーソルの管理

- **保存場所**: `cursor.file.path`で指定したディレクトリ
- **ファイル形式**: `<ブランチ名>`
- **更新タイミング**: 同期が正常に完了した後

### 設定項目

```properties
# 同期対象のDropboxディレクトリ（カンマ区切り）
# 各ディレクトリがGitブランチ名になる
target.directories=/branch1,/branch2,/branch3

# 同期対象のファイル拡張子（カンマ区切り）
target.file.extensions=.txt,.md,.java,.py

# カーソル保存先ディレクトリ
cursor.file.path=C:/path/to/cursor
```

### 動作例

```
Dropbox構造:
/branch1/
  ├─ file1.txt (新規作成)
  ├─ file2.md  (更新)
  └─ file3.java (削除)

↓ 同期実行

Git構造:
branch1ブランチ:
  ├─ file1.txt (追加)
  ├─ file2.md  (更新)
  └─ file3.java (削除)
```

---

## Git → Dropbox 同期

### 概要

GitHubリポジトリの変更を検出し、Dropboxに反映します。`git pull`による差分検出を使用し、指定ディレクトリ配下のファイルのみを同期します。

### 処理フロー

```
1. ローカルリポジトリを開く
   ↓
2. 全ブランチ一覧を取得
   ↓
3. 各ブランチに対して:
   a. ブランチをチェックアウト
   b. git pullを実行して差分を検出
   c. 変更ファイルリストを取得
   ↓
4. sync.target.dir配下のファイルのみをフィルタリング
   ↓
5. 各ファイルをDropboxにアップロード
   - アップロード先: /<ブランチ名>/<相対パス>
```

### 差分検出メカニズム

#### git pullによる差分検出

1. **HEADの記録**: `git pull`実行前のHEADコミットIDを記録
2. **git pull実行**: リモートから最新の変更を取得
3. **差分計算**: oldHead と newHead の間の差分を`git diff`で計算
4. **ファイルリスト取得**: 変更されたファイルのパスを抽出

#### checkoutBranch()とpullLatestChanges()の役割分担

**重要**: 2つのメソッドが明確に役割を分担しています。

##### checkoutBranch() - ブランチ切り替えのみ

```java
public void checkoutBranch(String branchName) throws GithubSyncException {
    // 役割: ブランチの切り替えのみを行う
    // pullはpullLatestChanges()で行うため、ここでは実行しない
    
    if (branchExists) {
        git.checkout().setName(branchName).call();
    } else {
        git.checkout().setName(branchName).setCreateBranch(true).call();
    }
}
```

##### pullLatestChanges() - 差分検出と取得

```java
public Set<String> pullLatestChanges() throws GithubSyncException {
    // 1. 現在のHEADを記録
    ObjectId oldHead = git.getRepository().resolve("HEAD");
    
    // 2. git pullを実行
    PullResult pullResult = git.pull()
        .setCredentialsProvider(credentialsProvider)
        .call();
    
    // 3. 新しいHEADを取得
    ObjectId newHead = git.getRepository().resolve("HEAD");
    
    // 4. HEADが変わっていない場合は空のSetを返す
    if (oldHead.equals(newHead)) {
        return new HashSet<>();
    }
    
    // 5. oldHeadとnewHeadの間の差分ファイルを取得
    return getChangedFilesBetweenCommits(git, oldHead, newHead);
}
```

**この設計により**:
- checkoutBranch()でpullすると、pullLatestChanges()で差分が検出できない問題を回避
- oldHeadとnewHeadの比較により、正確に変更ファイルを検出

### ファイルフィルタリング

#### sync.target.dirによるフィルタリング

```java
// 設定例: sync.target.dir=review

// ファイルパスの正規化
String normalized = normalizeRelativePath(updatedFile);
// 例: "review/file1.txt" → "review/file1.txt"
// 例: "/review/file2.txt" → "review/file2.txt"

// targetDirにスラッシュを追加
String targetDir = ensureTrailingSlash(normalized);
// 例: "review/" 

// フィルタリング判定
if (!normalized.startsWith(targetDir)) {
    // このファイルは除外
    continue;
}
```

#### パス正規化の詳細

1. **バックスラッシュをスラッシュに変換**: Windowsパス対応
2. **連続スラッシュを削除**: `//` → `/`
3. **先頭スラッシュを削除**: `/review` → `review`

### Dropboxへのアップロード

#### アップロードパスの構築

```java
// ブランチ名: feature/new-feature
// ローカルファイル: C:/repo/review/file.txt
// 相対パス: review/file.txt

// Dropboxパス: /feature/new-feature/review/file.txt
String dropboxPath = "/" + branchName + "/" + relativePath;
```

#### アップロード処理

- **モード**: `WriteMode.OVERWRITE` - 既存ファイルを上書き
- **タイムスタンプ**: ローカルファイルの最終更新日時を保持
- **リフレッシュトークン対応**: 認証エラー時に自動でトークンを更新

### 設定項目

```properties
# 同期対象ディレクトリ（リポジトリ内の相対パス）
# 先頭のスラッシュは不要（自動で削除される）
sync.target.dir=review
```

### 動作例

```
Git構造（feature/new-featureブランチ）:
review/
  ├─ file1.txt (新規作成)
  ├─ file2.md  (更新)
docs/
  └─ README.md (変更されたが、sync.target.dir外なので除外)

↓ 同期実行

Dropbox構造:
/feature/new-feature/
  └─ review/
      ├─ file1.txt (追加)
      └─ file2.md  (更新)
```

---

## 設定ファイル

### 全設定項目の説明

```properties
# ========================================
# GitHub設定
# ========================================

# GitHubパーソナルアクセストークン（必須）
# スコープ: repo
github.pat=ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# GitHubユーザー名（必須）
github.username=your_username

# GitHubリモートリポジトリURL（必須）
github.remote.url=https://github.com/your_username/your_repo.git

# ローカルリポジトリパス（必須）
# Windowsの場合は / を使用（例: C:/path/to/repo）
local.repo.path=C:/path/to/local/repo

# ========================================
# Dropbox設定
# ========================================

# Dropboxアクセストークン（refresh_token使用時は空でも可）
dropbox.access.token=

# Dropboxリフレッシュトークン（推奨）
dropbox.refresh.token=your_refresh_token

# DropboxアプリのクライアントID（refresh_token使用時は必須）
dropbox.client.id=your_client_id

# Dropboxアプリのクライアントシークレット（refresh_token使用時は必須）
dropbox.client.secret=your_client_secret

# ========================================
# Dropbox→Git同期用設定
# ========================================

# 同期対象のDropboxディレクトリ（カンマ区切り）
# 各ディレクトリがGitブランチ名になる
# 先頭のスラッシュは必須
target.directories=/dir1,/dir2,/dir3

# 同期対象のファイル拡張子（カンマ区切り）
# ドットを含める
target.file.extensions=.txt,.md,.java,.py

# カーソル保存先ディレクトリ（必須）
cursor.file.path=C:/path/to/cursor

# ========================================
# Git→Dropbox同期用設定
# ========================================

# 同期対象ディレクトリ（リポジトリ内の相対パス）
# 先頭のスラッシュは不要
sync.target.dir=review
```

---

## トラブルシューティング

### 問題: 「No changes detected」と表示され、変更が同期されない

#### 原因

Git→Dropbox同期は**差分同期のみ**を行います。以下の場合、変更は検出されません：

1. **ローカルリポジトリが既に最新**
   - 別のPCからプッシュした後、このPCで実行する前に、別の操作で既に`git pull`されている
   - ログに`Old HEAD: xxx`と`New HEAD: xxx`が同じ値として表示される

2. **sync.target.dir外の変更**
   - 変更されたファイルが`sync.target.dir`配下にない
   - ログに`Filtered out N files`として表示される

#### 解決方法

##### 方法1: 新しいコミットをプッシュしてから実行

別のPCで新しい変更を加えてプッシュ後、このアプリケーションを実行：

```bash
# 別のPC
echo "test" > review/newfile.txt
git add review/newfile.txt
git commit -m "Add new file"
git push origin your-branch
```

その後、アプリケーションを実行すると、この新しいコミットの差分がDropboxに同期されます。

##### 方法2: ローカルリポジトリをリセット

ローカルリポジトリを過去の状態に戻してから実行：

```bash
cd C:\path\to\local\repo
git checkout your-branch
git reset --hard HEAD~5  # 5つ前のコミットに戻る
```

その後、アプリケーションを実行すると、5つのコミットの差分がDropboxに同期されます。

**注意**: この方法は既にローカルで作業している場合は推奨されません。

##### 方法3: sync.target.dirの確認

設定ファイルで正しいディレクトリが指定されているか確認：

```properties
# 正しい例
sync.target.dir=review

# 間違った例（先頭のスラッシュは不要）
sync.target.dir=/review
```

### 問題: 認証エラーが発生する

#### 原因

- GitHubパーソナルアクセストークンが無効または期限切れ
- Dropboxリフレッシュトークンが無効
- 権限スコープが不足

#### 解決方法

1. **GitHubトークンの確認**
   - GitHub > Settings > Developer settings > Personal access tokens
   - `repo`スコープが付与されているか確認
   - 有効期限を確認

2. **Dropboxトークンの確認**
   - リフレッシュトークンを使用している場合、client_idとclient_secretが正しいか確認
   - 必要に応じて新しいトークンを生成

### 問題: ファイルパスが文字化けする

#### 原因

Windowsコンソールのエンコーディング設定（Shift-JIS）とJavaのエンコーディング（UTF-8）が異なる。

#### 解決方法

実行時に以下のオプションを追加：

```cmd
java -Dfile.encoding=UTF-8 -jar build/libs/db2ghsync.jar --config config.properties --direction git-to-dbx
```

または、PowerShellを使用：

```powershell
$env:JAVA_OPTS="-Dfile.encoding=UTF-8"
java -jar build/libs/db2ghsync.jar --config config.properties --direction git-to-dbx
```

---

## 設計上の注意事項

### 差分同期のみ対応

現在の実装は**差分同期のみ**を行います。全ファイルを毎回同期する「フル同期モード」は実装されていません。

### 初回実行時の挙動

- **Dropbox→Git**: カーソルが存在しない場合、全ファイルを同期
- **Git→Dropbox**: ローカルリポジトリが既に最新の場合、何も同期されない

### ブランチ管理

- **Dropbox→Git**: Dropboxディレクトリ名がそのままGitブランチ名になる
- **Git→Dropbox**: 全てのローカルブランチが処理対象

### ファイル削除の扱い

- **Dropbox→Git**: Dropboxで削除されたファイルは、Gitでも削除される
- **Git→Dropbox**: Gitで削除されたファイルの扱いは、差分検出の仕組み上、変更ファイルとして検出されない可能性がある

---

## ログの見方

### Dropbox→Git同期のログ

```
INFO  DropboxGitSyncApplication - Selected sync direction: DBX_TO_GIT
INFO  DropboxClient - Found 2 target directories
INFO  SyncProcessor - Processing directory: /branch1
INFO  DropboxClient - Found 5 changed entries for directory: /branch1
INFO  GitRepositoryManager - Checked out branch: branch1
INFO  SyncProcessor - Downloaded 5 files
INFO  GitRepositoryManager - Files added and committed successfully
INFO  GitRepositoryManager - Changes pushed successfully to remote repository
```

### Git→Dropbox同期のログ

```
INFO  DropboxGitSyncApplication - Selected sync direction: GIT_TO_DBX
INFO  GitRepositoryManager - Git repository already exists
INFO  GitToDropboxProcessor - Processing 3 branches for git-to-dbx sync
INFO  GitToDropboxProcessor - Processing branch: main
INFO  GitRepositoryManager - Checked out branch: main
INFO  GitRepositoryManager - Old HEAD: abc123...
INFO  GitRepositoryManager - New HEAD: def456...
INFO  GitRepositoryManager - Detected 3 changed files: [review/file1.txt, review/file2.md, docs/README.md]
INFO  GitToDropboxProcessor - Branch main: pullLatestChanges returned 3 files
INFO  GitToDropboxProcessor - Target directory filter: review/
INFO  DropboxClient - Uploaded review/file1.txt to /main/review/file1.txt
INFO  DropboxClient - Uploaded review/file2.md to /main/review/file2.md
INFO  GitToDropboxProcessor - Branch main summary: Uploaded 2 files, Filtered out 1 files, Skipped 0 non-files.
```

### 変更が検出されない場合のログ

```
INFO  GitRepositoryManager - Old HEAD: abc123...
INFO  GitRepositoryManager - New HEAD: abc123...
INFO  GitRepositoryManager - HEAD unchanged - no new commits detected
INFO  GitToDropboxProcessor - Branch main: pullLatestChanges returned 0 files
INFO  GitToDropboxProcessor - No changes detected for branch main
```

→ **oldHeadとnewHeadが同じ**: ローカルが既に最新

---

**最終更新日**: 2025-11-23
**作成者**: 開発チーム

