
# Dropbox-Git Sync

## 概要
DropboxとGitHubリポジトリ間の双方向ファイル同期を自動化するバッチアプリケーションです。
Dropbox→GitまたはGit→Dropboxの同期方向を選択でき、差分のみを効率的に反映します。

## 主な機能
- **双方向同期**: Dropbox↔GitHub間の双方向ファイル同期
  - Dropbox→Git: カーソルベースの差分検出
  - Git→Dropbox: git pullによる差分検出
- Dropbox API・GitHub APIを利用した認証・操作
- 対象ディレクトリ・拡張子のフィルタリング
- 差分のみを同期する効率的な処理
- 設定ファイルによる柔軟なカスタマイズ

## 使い方
1. `src/main/resources/config.properties.template` をコピーし、必要な値を設定して `config.properties` を作成します。
2. 必要な認証情報（Dropbox・GitHubのトークン等）を設定します。
3. Gradleでビルドします。
	```cmd
	gradlew.bat build
	```
4. アプリケーションを実行します（同期方向は `--direction` で指定）。
	```cmd
	java -jar build/libs/dbx-git-sync-<version>.jar --config C:\path\to\config.properties --direction dbx-to-git
	```
	- `--direction dbx-to-git` : Dropbox -> Git 同期（既存処理と同等）
	- `--direction git-to-dbx` : Git -> Dropbox 同期（旧git-dbx-syncの機能）

	例（Git->Dropbox）：
	```cmd
	java -jar build/libs/dbx-git-sync-1.0.0.jar --config C:\tmp\config\config.properties --direction git-to-dbx
	```

## 同期方向

### Dropbox → Git 同期 (`--direction dbx-to-git`)

Dropbox上の変更をGitHubリポジトリに反映します。

**処理フロー**:
1. Dropbox APIのカーソルを使って差分を検出
2. 指定した拡張子（`target.file.extensions`）のファイルのみをフィルタリング
3. 対象ディレクトリ（`target.directories`）ごとにブランチを作成/更新
4. 変更ファイルをダウンロードしてローカルリポジトリに反映
5. GitHubにコミット＆プッシュ

**設定項目**:
- `target.directories`: 同期対象のDropboxディレクトリ（各ディレクトリがブランチ名になる）
- `target.file.extensions`: 同期対象のファイル拡張子（カンマ区切り）

### Git → Dropbox 同期 (`--direction git-to-dbx`)

GitHubリポジトリの変更をDropboxに反映します。

**処理フロー**:
1. ローカルリポジトリの全ブランチを処理
2. 各ブランチで`git pull`を実行して差分を検出
3. 指定したディレクトリ（`sync.target.dir`）配下の変更ファイルのみを抽出
4. Dropbox APIで該当ファイルをアップロード

**設定項目**:
- `sync.target.dir`: リポジトリ内の同期対象ディレクトリ（例: `review`）

**注意事項**:
- **差分同期のみ**: 新しいコミットがある場合のみ同期されます
- **初回実行**: ローカルリポジトリが既に最新の場合、変更は検出されません
- **全ファイル同期**: 現在の実装では対応していません

## 設定ファイル例
`config.properties.template` を参照してください。

```properties
# GitHub設定
github.pat=your_personal_access_token
github.username=your_username
github.remote.url=https://github.com/your_username/your_repo.git
local.repo.path=C:/path/to/local/repo

# Dropbox設定
dropbox.access.token=your_access_token
dropbox.refresh.token=your_refresh_token
dropbox.client.id=your_client_id
dropbox.client.secret=your_client_secret

# Dropbox→Git同期用
target.directories=/dir1,/dir2
target.file.extensions=.txt,.md,.java

# Git→Dropbox同期用
sync.target.dir=review

# カーソル管理
cursor.file.path=C:/path/to/cursor
```

---
This application synchronizes files between Dropbox and GitHub repositories. Use `--direction dbx-to-git|git-to-dbx` to choose the flow.
