package com.db2ghsync.dropbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.db2ghsync.common.AppConfig;
import com.db2ghsync.common.SyncAction;
import com.db2ghsync.entity.SyncEntry;
import com.db2ghsync.exception.DropboxSyncException;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;

/**
 * Dropbox APIと連携し、ファイル・フォルダの変更検知や情報取得を行うクライアントクラス。
 * <p>
 * 設定値はAppConfigから取得し、CursorServiceを使用してカーソル情報を管理する。
 * ファイル拡張子やディレクトリのフィルタリング、メタデータ変換も担当。
 */
public class DropboxClient implements DropboxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DropboxClient.class);

    private static final String APP_NAME = "db2ghsync-app";

    // DropBox公式のクライアント
    private DbxClientV2 client;
    private final DbxRequestConfig requestConfig;
    private final DbxCredential credential;

    // 更新対象とする拡張子一覧（不変）
    private final List<String> extensions;

    // 対象とするディレクトリ（不変）
    private final List<String> directories;

    // ローカルリポジトリパス
    private final String localRepoPath;

    // カーソルサービス
    private final CursorService cursorService;

    /**
     * コンストラクタ。依存関係を注入してDropbox APIクライアントを初期化する。
     * 
     * @param config       アプリケーション設定
     * @param cursorService カーソル管理サービス
     */
    public DropboxClient(AppConfig config, CursorService cursorService) {
        Objects.requireNonNull(config, "AppConfig must not be null");
        Objects.requireNonNull(cursorService, "CursorService must not be null");

        this.cursorService = cursorService;
        this.localRepoPath = config.getLocalRepoPath();

        this.requestConfig = DbxRequestConfig.newBuilder(APP_NAME).build();
        String refreshToken = config.getDropboxRefreshToken();
        String clientId = config.getDropboxClientId();
        String clientSecret = config.getDropboxClientSecret();
        String accessToken = config.getDropboxAccessToken();

        if (!refreshToken.isEmpty() && !clientId.isEmpty() && !clientSecret.isEmpty()) {
            this.credential = new DbxCredential(
                    accessToken != null ? accessToken : "",
                    -1L,
                    refreshToken,
                    clientId,
                    clientSecret);
            this.client = new DbxClientV2(requestConfig, credential);
        } else {
            this.credential = null;
            this.client = new DbxClientV2(requestConfig, accessToken);
        }
        this.extensions = Collections.unmodifiableList(config.getTargetFileExtensions());
        this.directories = Collections.unmodifiableList(config.getTargetDirectories());
    }

    /**
     * ルートディレクトリ配下のディレクトリ一覧から、対象ディレクトリに一致するものを返却する。
     *
     * @return 拡張子一致ファイルのSyncEntryリスト
     * @throws DropboxSyncException ディレクトリ取得失敗時
     */
    public List<String> getTargetDirectories() throws DropboxSyncException {

        try {
            LOGGER.debug("Fetching target directories from Dropbox");
            // 管理下にあるディレクトリ一覧を取得
            ListFolderResult result = client.files().listFolder("");

            // ディレクトリの中で対象のディレクトリを取得
            List<String> targetDirs = new ArrayList<String>();
            for (Metadata metadata : result.getEntries()) {
                if (directories.contains(metadata.getPathDisplay())) {
                    targetDirs.add(metadata.getPathDisplay());
                }
            }

            LOGGER.info("Found {} target directories", targetDirs.size());
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
            LOGGER.debug("Fetching changes with cursor for directory: {}", targetDir);
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

            LOGGER.info("Found {} changed entries for directory: {}", changedEntries.size(), targetDir);

            // 一時カーソルファイルのデータを更新
            try {
                cursorService.writeTmpCursor(branchName, result.getCursor());
            } catch (DropboxSyncException e) {
                LOGGER.error("Failed to write temporary cursor for branch: {}", branchName, e);
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
            LOGGER.debug("Fetching all target files for directory: {}", targetDir);
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

            LOGGER.info("Found {} target files for directory: {}", changedEntries.size(), targetDir);

            // 一時カーソルファイルのデータを更新
            // 対象ディレクトリのプッシュ完了後に本ファイルに反映

            cursorService.writeTmpCursor(branchName, result.getCursor());
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
    @Override
    public void downloadFiles(List<SyncEntry> syncEntries) throws DropboxSyncException {

        LOGGER.info("Downloading {} files from Dropbox", syncEntries.size());
        String gitPath = localRepoPath;

        for (SyncEntry entry : syncEntries) {
            if (entry.getAction().equals(SyncAction.CREATE_OR_UPDATE)) {
                downloadFile(entry.getDropboxPath(), gitPath);
            } else {
                deleteFile(entry.getDropboxPath(), gitPath);
            }
        }
        LOGGER.info("Download completed for {} files", syncEntries.size());
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

        LOGGER.debug("Downloading file: {}", dropboxPath);

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

        LOGGER.debug("Downloaded file: {} to {}", dropboxPath, path);
    }

    private void deleteFile(String dropboxPath, String gitPath)
            throws DropboxSyncException {

        LOGGER.debug("Deleting file: {}", dropboxPath);

        // Windows環境で動作不良を起こす可能性があるため、"/"をtrim
        String relativePath = dropboxPath.startsWith("/") ? dropboxPath.substring(1) : dropboxPath;
        int firstSlash = relativePath.indexOf("/");
        if (firstSlash != -1) {
            relativePath = relativePath.substring(firstSlash + 1);
        }

        Path path = Paths.get(gitPath, relativePath);

        try {
            Files.delete(path);
            LOGGER.debug("Deleted file: {}", path);

        } catch (IOException e) {
            throw new DropboxSyncException("Deleting file failed.", e);
        }
    }

    @Override
    public void uploadFile(Path localFilePath, String dropboxPath) throws DropboxSyncException {
        if (localFilePath == null || !Files.isRegularFile(localFilePath)) {
            throw new DropboxSyncException("Local file does not exist: " + localFilePath);
        }

        try {
            uploadInternal(localFilePath, dropboxPath);
        } catch (DbxException e) {
            if (credential != null) {
                try {
                    LOGGER.info("Dropbox upload failed once. Trying to refresh credential...");
                    credential.refresh(requestConfig);
                    this.client = new DbxClientV2(requestConfig, credential);
                    uploadInternal(localFilePath, dropboxPath);
                    return;
                } catch (Exception refreshException) {
                    throw new DropboxSyncException("Dropbox upload failed after refresh.", refreshException);
                }
            }
            throw new DropboxSyncException("Dropbox upload failed.", e);
        } catch (IOException e) {
            throw new DropboxSyncException("Failed to read local file for upload: " + localFilePath, e);
        }
    }

    private void uploadInternal(Path localFilePath, String dropboxPath) throws IOException, DbxException {
        try (InputStream in = Files.newInputStream(localFilePath)) {
            long lastModified = Files.getLastModifiedTime(localFilePath).toMillis();
            client.files()
                    .uploadBuilder(dropboxPath)
                    .withMode(WriteMode.OVERWRITE)
                    .withClientModified(new Date(lastModified))
                    .uploadAndFinish(in);
            LOGGER.info("Uploaded {} to {}", localFilePath, dropboxPath);
        }
    }
}
