package com.db2ghsync.common;

import lombok.Getter;

/**
 * 設定ファイル(config.properties)で利用するキー定数を定義する列挙型。
 * 各種設定値の取得に利用される。
 */
@Getter
public enum ConfigKey {

    /** Dropbox APIアクセストークン */
    DROPBOX_ACCESS_TOKEN("dropbox.access.token"),
    /** GitHubのPersonal Access Token */
    GITHUB_PAT("github.pat"),
    /** GitHubユーザー名 */
    GITHUB_USERNAME("github.username"),
    /** GitHubリモートリポジトリURL */
    GITHUB_REMOTE_URL("github.remote.url"),
    /** ローカルGitリポジトリのパス */
    LOCAL_REPO_PATH("local.repo.path"),
    /** Dropboxカーソルファイルのパス */
    CURSOR_FILE_PATH("cursor.file.path"),
    /** 同期対象ファイル拡張子(カンマ区切り) */
    TARGET_FILE_EXTENSIONS("target.file.extensions"),
    /** 同期対象ディレクトリ一覧(カンマ区切り) */
    TARGET_DIRECTORIES("target.directories");

    private String key;

    ConfigKey(String key) {
        this.key = key;
    }
}
