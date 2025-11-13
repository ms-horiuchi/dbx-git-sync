package com.db2ghsync.common;

/**
 * DropBox上のファイルステータス.
 * 
 * DropBox上のファイル更新をGitHubに反映させるためのステータス
 */
public enum SyncAction {

    CREATE_OR_UPDATE,
    DELETE;
}
