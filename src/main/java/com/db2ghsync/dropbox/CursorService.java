package com.db2ghsync.dropbox;

import com.db2ghsync.exception.DropboxSyncException;

/**
 * Dropboxカーソル情報の管理を行うサービスのインターフェース。
 * カーソル情報のファイル保存・取得を提供する。
 */
public interface CursorService {

    /**
     * カーソル情報をファイルから読み込む。
     * ファイルが存在しない場合や読み込み失敗時は空文字を返します。
     *
     * @param branchName ブランチ名
     * @return ファイルの内容（空文字の場合は未取得）
     */
    String readCursor(String branchName);

    /**
     * カーソル情報を一時Cursorファイルに保存します。
     *
     * @param branchName    ブランチ名
     * @param currentCursor 保存するカーソル情報
     * @throws DropboxSyncException 書き込み失敗時
     */
    void writeTmpCursor(String branchName, String currentCursor) throws DropboxSyncException;

    /**
     * カーソル情報をファイルに保存します。
     * 一時ファイルからカーソル情報を取得し、カーソル情報を保存します。
     * 完了後、不要になった一時ファイルを削除します。
     *
     * @param branchName ブランチ名
     * @throws DropboxSyncException 書き込み・読み込み失敗時
     */
    void writeCursor(String branchName) throws DropboxSyncException;
}

