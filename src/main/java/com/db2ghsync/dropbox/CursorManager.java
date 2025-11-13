package com.db2ghsync.dropbox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.db2ghsync.common.ConfigKey;
import com.db2ghsync.common.ConfigManager;
import com.db2ghsync.exception.DropboxSyncException;

/**
 * Dropboxカーソル情報の管理クラス
 * <p>
 * カーソル情報のファイル保存・取得を担当します。
 */
public class CursorManager {

    private static final Logger logger = LoggerFactory.getLogger(CursorManager.class);

    /**
     * カーソル情報をファイルから読み込む
     * <p>
     * 設定ファイルで指定されたパスからカーソル情報を取得します。
     * ファイルが存在しない場合や読み込み失敗時は空文字を返します。
     *
     * @return ファイルの内容（空文字の場合は未取得）
     */
    public static String readCursor(String branchName) {

        logger.debug("Reading cursor for branch: {}", branchName);
        String cursorFilePath = ConfigManager.getProperty(ConfigKey.CURSOR_FILE_PATH);
        String cursor = readCursorFile(cursorFilePath, branchName);
        if (cursor.isEmpty()) {
            logger.info("No cursor file found for branch: {}", branchName);
        } else {
            logger.debug("Cursor read successfully for branch: {}", branchName);
        }
        return cursor;
    }

    /**
     * 指定パスのファイルからカーソル情報を取得します。
     * <p>
     * ファイルが存在しない場合や読み込み失敗時は空文字を返します。
     *
     * @param filePath カーソル情報ファイルのパス
     * @return ファイルの内容（空文字の場合は未取得）
     */
    private static String readCursorFile(String filePath, String branchName) {

        try {
            return Files.readString(Paths.get(filePath, branchName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // 初回更新でファイルがないケースとみなし、空文字を返却
            return "";
        }
    }

    /**
     * カーソル情報を一時Cursorファイルに保存します。
     * <p>
     * 設定ファイルで指定されたパスにカーソル情報を保存します。
     *
     * @param branchName    ブランチ名
     * @param currentCursor 保存するカーソル情報
     * @throws DropboxSyncException 書き込み失敗時
     */
    public static void writeTmpCursor(String branchName, String currentCursor) throws DropboxSyncException {

        logger.debug("Writing temporary cursor for branch: {}", branchName);
        String cursorFilePath = ConfigManager.getProperty(ConfigKey.CURSOR_FILE_PATH);
        writeCursorFile(cursorFilePath, branchName + ".tmp", currentCursor);
        logger.debug("Temporary cursor written successfully for branch: {}", branchName);
    }

    /**
     * カーソル情報をファイルに保存します。
     * <p>
     * 設定ファイルで指定されたパスに一時ファイルからカーソル情報を取得し、
     * カーソル情報を保存します。
     * 完了後、不要になった一時ファイルを削除します。
     *
     * @param branchName ブランチ名
     * @throws DropboxSyncException 書き込み・読み込み失敗時
     */
    public static void writeCursor(String branchName) throws DropboxSyncException {

        logger.debug("Writing cursor for branch: {}", branchName);
        String cursorFilePath = ConfigManager.getProperty(ConfigKey.CURSOR_FILE_PATH);
        writeCursorFile(cursorFilePath, branchName, readTmpCursorFile(cursorFilePath, branchName));
        deleteTmpCursor(cursorFilePath, branchName);
        logger.info("Cursor written successfully for branch: {}", branchName);
    }

    /**
     * 指定パスの一時ファイルからカーソル情報を取得します。
     * <p>
     * 完了時のCursorファイル更新用のメソッド。
     * ファイルが存在しない、または読み込みに失敗した場合はDropboxSyncExceptionをスローします。
     *
     * @param filePath   カーソル情報ファイルのパス
     * @param branchName ブランチ名
     * @return ファイルの内容
     * @throws DropboxSyncException 読み込み失敗時
     */
    private static String readTmpCursorFile(String filePath, String branchName) throws DropboxSyncException {

        try {
            return Files.readString(Paths.get(filePath, branchName + ".tmp"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DropboxSyncException("Can not read tmp file.", e);
        }

    }

    /**
     * 指定パスのファイルにカーソル情報を書き込みます。
     * <p>
     * ファイル書き込み失敗時はDropboxSyncExceptionをスローします。
     *
     * @param cursorFilePath ファイルパス
     * @param branchName     ブランチ名
     * @param currentCursor  保存するカーソル情報
     * @throws DropboxSyncException 書き込み失敗時
     */
    private static void writeCursorFile(String cursorFilePath, String branchName, String currentCursor)
            throws DropboxSyncException {
        try {
            Files.writeString(Paths.get(cursorFilePath, branchName), currentCursor, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DropboxSyncException("Can not write new cursor", e);
        }
    }

    private static void deleteTmpCursor(String cursorFilePath, String branchName) {

        try {
            Files.delete(Paths.get(cursorFilePath, branchName + ".tmp"));
            logger.debug("Temporary cursor file deleted for branch: {}", branchName);
        } catch (IOException e) {
            // 削除に失敗しても影響はないため、ログ出力のみで処理なし
            logger.warn("Failed to delete temporary cursor file for branch: {}", branchName, e);
        }
    }
}
