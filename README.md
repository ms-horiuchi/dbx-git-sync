
# Dropbox-Git Sync

## 概要
DropboxとGitHubリポジトリ間のファイル同期を自動化するバッチアプリケーションです。
指定したDropboxディレクトリとGitリポジトリ間で、ファイルの差分を検出し、双方向で同期を行います。

## 主な機能
- DropboxとGitHubリポジトリ間のファイル同期（差分検出・反映）
- Dropbox API・GitHub APIを利用した認証・操作
- 対象ディレクトリ・拡張子のフィルタリング
- 同期カーソルによる差分管理
- 設定ファイルによる柔軟なカスタマイズ

## 使い方

### GitHub Actionsによる自動実行（推奨）
1日1回自動的にDropboxとGitHubの同期を実行します。

詳細なセットアップ手順は [GitHub Actions セットアップガイド](docs/GITHUB_ACTIONS_SETUP.md) を参照してください。

#### セットアップ手順（概要）
1. GitHub リポジトリの Settings → Secrets and variables → Actions から、以下のシークレットを設定します：

   **必須のシークレット:**
   - `DROPBOX_REFRESH_TOKEN`: Dropbox APIのリフレッシュトークン
   - `DROPBOX_CLIENT_ID`: Dropbox APIのクライアントID
   - `DROPBOX_CLIENT_SECRET`: Dropbox APIのクライアントシークレット
   - または `DROPBOX_ACCESS_TOKEN`: Dropbox APIのアクセストークン
   - `GH_PAT`: GitHub Personal Access Token（リポジトリへの書き込み権限が必要）

   **オプションのシークレット:**
   - `TARGET_FILE_EXTENSIONS`: 対象ファイル拡張子（デフォルト: `.zip,.java,.xlsx,.xlsm,.png,.txt`）
   - `TARGET_DIRECTORIES`: 管理対象ディレクトリ（カンマ区切り）

2. ワークフローは毎日00:00 UTC（日本時間09:00）に自動実行されます。

3. 手動で実行する場合は、GitHub リポジトリの Actions タブから "Dropbox-GitHub Daily Sync" を選択し、"Run workflow" をクリックします。

#### 手動実行時のオプション
- **target_directories**: 同期対象ディレクトリをカンマ区切りで指定（オプション）
- **sync_target_dir**: 同期先ディレクトリ（デフォルト: review）

### ローカル環境での実行
1. `src/main/resources/config.properties.template` をコピーし、必要な値を設定して `config.properties` を作成します。
2. 必要な認証情報（Dropbox・GitHubのトークン等）を設定します。
3. Gradleでビルドします。
	```cmd
	gradlew.bat build
	```
4. アプリケーションを実行します。
	```cmd
	java -cp build/libs/dbx-git-sync-<バージョン>.jar com.db2ghsync.DropboxGitSyncApplication <configファイルのフルパス>
	```
	例：
	```cmd
	java -cp build/libs/dbx-git-sync-1.0.0.jar com.db2ghsync.DropboxGitSyncApplication C:/tmp/config/config.properties
	```

## 設定ファイル例
`config.properties.template` を参照してください。

---
This application synchronizes files between Dropbox and GitHub repositories. See above for usage in Japanese.
