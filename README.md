
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
