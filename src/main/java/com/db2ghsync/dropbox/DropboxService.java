package com.db2ghsync.dropbox;

import java.nio.file.Path;
import java.util.List;

import com.db2ghsync.entity.SyncEntry;
import com.db2ghsync.exception.DropboxSyncException;

/**
 * Dropbox APIとの連携を行うサービスのインターフェース。
 * ファイル・フォルダの変更検知や情報取得、およびアップロードを提供する。
 */
public interface DropboxService {

    /**
     * ルートディレクトリ配下のディレクトリ一覧から、対象ディレクトリに一致するものを返却する。
     *
     * @return 対象ディレクトリのリスト
     * @throws DropboxSyncException ディレクトリ取得失敗時
     */
    List<String> getTargetDirectories() throws DropboxSyncException;

    /**
     * 差分取得用カーソルを使って、Dropbox上の変更（新規・更新・削除）を検知し、SyncEntryリストとして返す。
     *
     * @param targetDir 対象ディレクトリ
     * @param cursor    Dropbox APIの差分取得用カーソル
     * @return 変更されたSyncEntryのリスト
     * @throws DropboxSyncException 変更取得失敗時
     */
    List<SyncEntry> getChangesWithCursor(String targetDir, String cursor) throws DropboxSyncException;

    /**
     * 管理対象ディレクトリ配下の全ファイルのうち、指定拡張子に一致するものをSyncEntryリストとして返す。
     *
     * @param targetDir 対象ディレクトリ
     * @return 拡張子一致ファイルのSyncEntryリスト
     * @throws DropboxSyncException ファイル一覧取得失敗時
     */
    List<SyncEntry> getTargetFiles(String targetDir) throws DropboxSyncException;

    /**
     * 対象のファイルをダウンロードし、上書きするメソッド
     *
     * @param syncEntries ダウンロード対象のSyncEntryリスト
     * @throws DropboxSyncException ダウンロード・削除失敗時
     */
    void downloadFiles(List<SyncEntry> syncEntries) throws DropboxSyncException;

    /**
     * ローカルファイルをDropboxへアップロードする。
     *
     * @param localFilePath ローカルファイルパス
     * @param dropboxPath   アップロード先Dropboxパス
     * @throws DropboxSyncException アップロード失敗時
     */
    void uploadFile(Path localFilePath, String dropboxPath) throws DropboxSyncException;
}
