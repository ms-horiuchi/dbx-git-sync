# GitHub Actions セットアップガイド

このガイドでは、GitHub Actionsを使用してDropboxとGitHubの同期を自動化する方法を説明します。

## 概要

GitHub Actionsワークフローは以下の処理を1日1回（00:00 UTC / 09:00 JST）自動実行します：

1. リポジトリのチェックアウト
2. Java 17のセットアップ
3. アプリケーションのビルド
4. 設定ファイルの作成
5. Dropbox-GitHub同期の実行
6. 実行ログのアーカイブ

## 必須シークレットの設定

GitHub リポジトリの **Settings → Secrets and variables → Actions → New repository secret** から以下のシークレットを設定してください。

### Dropbox認証（いずれかの方法を選択）

#### 方法1: リフレッシュトークン認証（推奨）
- `DROPBOX_REFRESH_TOKEN`: Dropbox APIのリフレッシュトークン
- `DROPBOX_CLIENT_ID`: Dropbox APIのクライアントID  
- `DROPBOX_CLIENT_SECRET`: Dropbox APIのクライアントシークレット

#### 方法2: アクセストークン認証
- `DROPBOX_ACCESS_TOKEN`: Dropbox APIのアクセストークン

### GitHub認証
- `GH_PAT`: GitHub Personal Access Token
  - 必要な権限: `repo` (フルアクセス)
  - 作成方法: Settings → Developer settings → Personal access tokens → Generate new token

## オプションシークレットの設定

以下のシークレットは省略可能です。設定しない場合はデフォルト値が使用されます。

- `TARGET_FILE_EXTENSIONS`: 同期対象のファイル拡張子（カンマ区切り）
  - デフォルト: `.zip,.java,.xlsx,.xlsm,.png,.txt`
  - 例: `.txt,.md,.pdf`

- `TARGET_DIRECTORIES`: 同期対象のDropboxディレクトリ（カンマ区切り）
  - デフォルト: 空（すべてのディレクトリ）
  - 例: `/Documents,/Projects`

## Dropbox APIトークンの取得方法

### リフレッシュトークンの取得（推奨）

1. [Dropbox App Console](https://www.dropbox.com/developers/apps)にアクセス
2. 「Create app」をクリック
3. 以下を選択：
   - API: Scoped access
   - Type of access: Full Dropbox または App folder
   - Name: 任意のアプリ名
4. 作成後、「Permissions」タブで必要な権限を設定：
   - `files.metadata.read`
   - `files.content.read`
   - `files.content.write`
5. 「Settings」タブで以下を確認：
   - App key → `DROPBOX_CLIENT_ID`
   - App secret → `DROPBOX_CLIENT_SECRET`
6. OAuth 2フローでリフレッシュトークンを取得
   - 詳細は [Dropbox OAuth Guide](https://developers.dropbox.com/ja-jp/oauth-guide)を参照

### アクセストークンの取得（簡易版）

1. 上記1-5の手順を実施
2. 「Settings」タブの「Generated access token」で「Generate」をクリック
3. 生成されたトークンを `DROPBOX_ACCESS_TOKEN` として設定

**注意**: アクセストークンは有効期限があるため、リフレッシュトークンの使用を推奨します。

## GitHub Personal Access Tokenの取得方法

1. GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
2. 「Generate new token (classic)」をクリック
3. 以下を設定：
   - Note: dbx-git-sync（任意の名前）
   - Expiration: 有効期限を設定（90 days推奨）
   - Select scopes: `repo` にチェック
4. 「Generate token」をクリックし、表示されたトークンを `GH_PAT` として設定

**注意**: トークンは一度しか表示されないため、必ず保存してください。

## ワークフローの実行

### 自動実行
- 毎日00:00 UTC（日本時間09:00）に自動実行されます
- 実行状況は「Actions」タブで確認できます

### 手動実行
1. GitHub リポジトリの「Actions」タブを開く
2. 「Dropbox-GitHub Daily Sync」ワークフローを選択
3. 「Run workflow」ボタンをクリック
4. オプションで以下を設定可能：
   - **target_directories**: 同期対象ディレクトリ（カンマ区切り）
   - **sync_target_dir**: 同期先ディレクトリ（デフォルト: review）
5. 「Run workflow」を実行

## トラブルシューティング

### ワークフローが失敗する場合

1. **Actions」タブで失敗したワークフローを開く
2. 各ステップのログを確認
3. 「Artifacts」セクションから `sync-logs-{run-id}` をダウンロードして詳細を確認

### よくあるエラー

#### 認証エラー
- Dropbox/GitHubのトークンが正しく設定されているか確認
- トークンの有効期限が切れていないか確認

#### ビルドエラー
- Java 17が正しくセットアップされているか確認
- Gradle依存関係の問題がないか確認

#### 同期エラー
- 対象ディレクトリが存在するか確認
- ファイル拡張子の設定が正しいか確認

## セキュリティに関する注意事項

- すべての認証情報はGitHub Secretsに保存し、コードに直接記載しないでください
- Personal Access Tokenは定期的に更新してください
- 不要になったトークンは削除してください
- トークンの権限は必要最小限に設定してください

## ログの確認

同期処理のログは以下の方法で確認できます：

1. GitHub Actions実行ログ（リアルタイム）
2. アーティファクトとして保存された設定ファイルとカーソル情報（7日間保持）

ログには機密情報が含まれる可能性があるため、取り扱いに注意してください。
