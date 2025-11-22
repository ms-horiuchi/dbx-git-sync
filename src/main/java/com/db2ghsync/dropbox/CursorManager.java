package com.db2ghsync.dropbox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.db2ghsync.common.AppConfig;
import com.db2ghsync.exception.DropboxSyncException;

/**
 * Dropboxカーソル情報の管理クラス
 * <p>
 * カーソル情報のファイル保存・取得を担当します。
 */
public class CursorManager implements CursorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CursorManager.class);

    private final String cursorFilePath;

    /**
     * コンストラクタ。依存関係を注入してCursorManagerを初期化する。
     * 
     * @param config アプリケーション設定
     */
    public CursorManager(AppConfig config) {
        Objects.requireNonNull(config, "AppConfig must not be null");
        this.cursorFilePath = config.getCursorFilePath();
    }

    /**
     * カーソル情報をファイルから読み込む
     * <p>
     * 設定ファイルで指定されたパスからカーソル情報を取得します。
     * ファイルが存在しない場合や読み込み失敗時は空文字を返します。
     *
     * @param branchName ブランチ名
     * @return ファイルの内容（空文字の場合は未取得）
     */
    @Override
    public String readCursor(String branchName) {

        LOGGER.debug("Reading cursor for branch: {}", branchName);
        String cursor = readCursorFile(cursorFilePath, branchName);
        if (cursor.isEmpty()) {
            LOGGER.info("No cursor file found for branch: {}", branchName);
        } else {
            LOGGER.debug("Cursor read successfully for branch: {}", branchName);
        }
        return cursor;
    }

    /**
     * 指定パスのファイルからカーソル情報を取得します。
     * <p>
     * ファイルが存在しない場合や読み込み失敗時は空文字を返します。
     *
     * @param filePath カーソル情報ファイルのパス
     * @param branchName ブランチ名
     * @return ファイルの内容（空文字の場合は未取得）
     */
    private String readCursorFile(String filePath, String branchName) {

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
    @Override
    public void writeTmpCursor(String branchName, String currentCursor) throws DropboxSyncException {

        LOGGER.debug("Writing temporary cursor for branch: {}", branchName);
        writeCursorFile(cursorFilePath, branchName + ".tmp", currentCursor);
        LOGGER.debug("Temporary cursor written successfully for branch: {}", branchName);
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
    @Override
    public void writeCursor(String branchName) throws DropboxSyncException {

        LOGGER.debug("Writing cursor for branch: {}", branchName);
        writeCursorFile(cursorFilePath, branchName, readTmpCursorFile(cursorFilePath, branchName));
        deleteTmpCursor(cursorFilePath, branchName);
        LOGGER.info("Cursor written successfully for branch: {}", branchName);
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
    private String readTmpCursorFile(String filePath, String branchName) throws DropboxSyncException {

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
    private void writeCursorFile(String cursorFilePath, String branchName, String currentCursor)
            throws DropboxSyncException {
        try {
            Files.writeString(Paths.get(cursorFilePath, branchName), currentCursor, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DropboxSyncException("Can not write new cursor", e);
        }
    }

    private void deleteTmpCursor(String cursorFilePath, String branchName) {

        try {
            Files.delete(Paths.get(cursorFilePath, branchName + ".tmp"));
            LOGGER.debug("Temporary cursor file deleted for branch: {}", branchName);
        } catch (IOException e) {
            // 削除に失敗しても影響はないため、ログ出力のみで処理なし
            LOGGER.warn("Failed to delete temporary cursor file for branch: {}", branchName, e);
        }
    }
}
