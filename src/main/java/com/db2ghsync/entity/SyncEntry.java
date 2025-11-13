package com.db2ghsync.entity;

import com.db2ghsync.common.SyncAction;

import lombok.Data;

/**
 * Dropboxファイルの同期情報を保持するエンティティクラス。
 * ファイルパス・ディレクトリ・ステータス等を管理する。
 */
@Data
public class SyncEntry {

    /**
     * DropBox上の絶対パス
     */
    /** Dropbox上の絶対パス */
    private final String dropboxPath;

    /** ファイル名 */
    private final String name;

    /**
     * ディレクトリ名.
     * 
     * DropBox上のルートディレクトリ配下のディレクトリ名がデフォルト
     */
    /** ディレクトリ名（Dropboxルート配下のディレクトリ） */
    private final String subDirectoryKey;

    /**
     * DropBox上のステータス情報
     * 
     */
    /** Dropbox上のステータス情報（SyncAction列挙型） */
    private final SyncAction action;
}
