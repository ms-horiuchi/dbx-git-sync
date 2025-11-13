package com.db2ghsync.dropbox;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.db2ghsync.common.ConfigKey;
import com.db2ghsync.common.ConfigManager;
import com.db2ghsync.common.SyncAction;
import com.db2ghsync.entity.SyncEntry;
import com.db2ghsync.exception.DropboxSyncException;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

/**
 * Dropbox APIと連携し、ファイル・フォルダの変更検知や情報取得を行うクライアントクラス。
 * <p>
 * 設定値（アクセストークン、拡張子、ディレクトリ）はConfigManagerから取得。
 * ファイル拡張子やディレクトリのフィルタリング、メタデータ変換も担当。
 */
public class DropboxClient {

    private static final Logger logger = LoggerFactory.getLogger(DropboxClient.class);

    // DropBox公式のクライアント
    private DbxClientV2 client;

    // 更新対象とする拡張子一覧
    private List<String> extensions;

    // 対象とするディレクトリ
    private List<String> directories;

    /**
     * コンストラクタ。設定値を取得し、Dropbox APIクライアントを初期化する。
     */
    public DropboxClient() {

        logger.info("Initializing Dropbox client");
        DbxRequestConfig config = DbxRequestConfig.newBuilder("db2ghsync-app").build();
        this.client = new DbxClientV2(config, ConfigManager.getProperty(ConfigKey.DROPBOX_ACCESS_TOKEN));
        this.extensions = Arrays.asList(ConfigManager.getProperty(ConfigKey.TARGET_FILE_EXTENSIONS).split(","));
        this.directories = Arrays.asList(ConfigManager.getProperty(ConfigKey.TARGET_DIRECTORIES).split(","));
        logger.info("Dropbox client initialized. Target extensions: {}, Target directories: {}", extensions,
                directories);
    }

    /**
     * ルートディレクトリ配下のディレクトリ一覧から、対象ディレクトリに一致するものを返却する。
     *
     * @return 拡張子一致ファイルのSyncEntryリスト
     * @throws DropboxSyncException ディレクトリ取得失敗時
     */
    public List<String> getTargetDirectories() throws DropboxSyncException {

        try {
            logger.debug("Fetching target directories from Dropbox");
            // 管理下にあるディレクトリ一覧を取得
            ListFolderResult result = client.files().listFolder("");

            // ディレクトリの中で対象のディレクトリを取得
            List<String> targetDirs = new ArrayList<String>();
            for (Metadata metadata : result.getEntries()) {
                if (directories.contains(metadata.getPathDisplay())) {
                    targetDirs.add(metadata.getPathDisplay());
                }
            }

            logger.info("Found {} target directories", targetDirs.size());
            // 現在閲覧可能な存在するディレクトリのみを対象とする
            return targetDirs;

        } catch (DbxException e) {
            throw new DropboxSyncException("Getting targetting directories, Error happened. ", e);
        }
    }

    /**
     * 差分取得用カーソルを使って、Dropbox上の変更（新規・更新・削除）を検知し、SyncEntryリストとして返す。
     *
     * @param targetDir 対象ディレクトリ
     * @param cursor    Dropbox APIの差分取得用カーソル
     * @return 変更されたSyncEntryのリスト
     * @throws DropboxSyncException 変更取得失敗時
     */
    public List<SyncEntry> getChangesWithCursor(String targetDir, String cursor)
            throws DropboxSyncException {

        try {
            logger.debug("Fetching changes with cursor for directory: {}", targetDir);
            String branchName = targetDir.startsWith("/") ? targetDir.substring(1) : targetDir;

            List<SyncEntry> changedEntries = new ArrayList<SyncEntry>();
            ListFolderResult result = client.files().listFolderContinue(cursor);

            while (true) {

                // 一覧取得したデータから対象の拡張子ファイルのデータを保持
                for (Metadata metadata : result.getEntries()) {
                    for (String extension : extensions) {
                        if (metadata.getName().endsWith(extension)) {
                            changedEntries.add(SyncEntryFactory.convertMetadataToSyncEntry(metadata));
                        }
                    }
                }

                // 最後まで到達した場合は終了
                if (!result.getHasMore()) {

                    break;
                }

                // 未読込のデータがある場合、再読み込みしてループ
                result = client.files().listFolderContinue(result.getCursor());

            }

            logger.info("Found {} changed entries for directory: {}", changedEntries.size(), targetDir);

            // 一時カーソルファイルのデータを更新
            try {
                CursorManager.writeTmpCursor(branchName, result.getCursor());
            } catch (DropboxSyncException e) {
                logger.error("Failed to write temporary cursor for branch: {}", branchName, e);
                throw e;
            }

            return changedEntries;
        } catch (DbxException e) {
            throw new DropboxSyncException("Getting changed files, Error happened. ", e);
        }
    }

    /**
     * 管理対象ディレクトリ配下の全ファイルのうち、指定拡張子に一致するものをSyncEntryリストとして返す。
     *
     * @param targetDir 対象ディレクトリ
     * @return 拡張子一致ファイルのSyncEntryリスト
     * @throws DropboxSyncException ファイル一覧取得失敗時
     */
    public List<SyncEntry> getTargetFiles(String targetDir) throws DropboxSyncException {

        try {
            logger.debug("Fetching all target files for directory: {}", targetDir);
            String branchName = targetDir.startsWith("/") ? targetDir.substring(1) : targetDir;

            // 対象ディレクトリごとに全ファイルを取得

            ListFolderResult result = client.files().listFolderBuilder(targetDir).withRecursive(true).start();

            List<SyncEntry> changedEntries = new ArrayList<SyncEntry>();
            while (true) {
                for (Metadata metadata : result.getEntries()) {
                    for (String extension : extensions) {
                        if (metadata.getName().endsWith(extension)) {
                            changedEntries.add(SyncEntryFactory.convertMetadataToSyncEntry(metadata));
                        }
                    }
                }

                if (!result.getHasMore()) {
                    break;
                }
                result = client.files().listFolderContinue(result.getCursor());
            }

            logger.info("Found {} target files for directory: {}", changedEntries.size(), targetDir);

            // 一時カーソルファイルのデータを更新
            // 対象ディレクトリのプッシュ完了後に本ファイルに反映

            CursorManager.writeTmpCursor(branchName, result.getCursor());
            return changedEntries;

        } catch (DbxException e) {
            throw new DropboxSyncException("Getting targeting-file-list, Error happened. ", e);
        }
    }

    /**
     * 対象のファイルをダウンロードし、上書きするメソッド
     *
     * @param syncEntries ダウンロード対象のSyncEntryリスト
     * @throws DropboxSyncException ダウンロード・削除失敗時
     */
    public void downloadFiles(List<SyncEntry> syncEntries) throws DropboxSyncException {

        logger.info("Downloading {} files from Dropbox", syncEntries.size());
        String gitPath = ConfigManager.getProperty(ConfigKey.LOCAL_REPO_PATH);

        for (SyncEntry entry : syncEntries) {
            if (entry.getAction().equals(SyncAction.CREATE_OR_UPDATE)) {
                downloadFile(entry.getDropboxPath(), gitPath);
            } else {
                deleteFile(entry.getDropboxPath(), gitPath);
            }
        }
        logger.info("Download completed for {} files", syncEntries.size());
    }

    /**
     * 指定したDropboxパスのファイルをローカルにダウンロードして保存する
     *
     * @param dropboxPath Dropbox上のファイルパス（例: /dir1/file.txt）
     * @param gitPath     ローカルリポジトリパス（例: C:/work/yourrepo）
     * @throws DropboxSyncException ダウンロード失敗時
     */
    private void downloadFile(String dropboxPath, String gitPath)
            throws DropboxSyncException {

        logger.debug("Downloading file: {}", dropboxPath);

        // Windows環境で動作不良を起こす可能性があるため、"/"をtrim
        String relativePath = dropboxPath.startsWith("/") ? dropboxPath.substring(1) : dropboxPath;
        int firstSlash = relativePath.indexOf("/");
        if (firstSlash != -1) {
            relativePath = relativePath.substring(firstSlash + 1);
        }

        Path path = Paths.get(gitPath, relativePath);

        // 親ディレクトリを作成
        if (Objects.nonNull(path.getParent())) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new DropboxSyncException("Creating directory failed.", e);
            }
        }

        // コピー処理を実行
        try (OutputStream out = Files.newOutputStream(path)) {

            client.files().download(dropboxPath).download(out);

        } catch (IOException | DbxException e) {
            throw new DropboxSyncException("Downloading file failed.", e);
        }

        logger.debug("Downloaded file: {} to {}", dropboxPath, path);
    }

    private void deleteFile(String dropboxPath, String gitPath)
            throws DropboxSyncException {

        logger.debug("Deleting file: {}", dropboxPath);

        // Windows環境で動作不良を起こす可能性があるため、"/"をtrim
        String relativePath = dropboxPath.startsWith("/") ? dropboxPath.substring(1) : dropboxPath;
        int firstSlash = relativePath.indexOf("/");
        if (firstSlash != -1) {
            relativePath = relativePath.substring(firstSlash + 1);
        }

        Path path = Paths.get(gitPath, relativePath);

        try {
            Files.delete(path);
            logger.debug("Deleted file: {}", path);

        } catch (IOException e) {
            throw new DropboxSyncException("Deleting file failed.", e);
        }
    }
}
