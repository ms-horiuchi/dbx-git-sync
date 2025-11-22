package com.db2ghsync.app;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.db2ghsync.dropbox.CursorService;
import com.db2ghsync.dropbox.DropboxService;
import com.db2ghsync.entity.SyncEntry;
import com.db2ghsync.exception.DropboxSyncException;
import com.db2ghsync.exception.GithubSyncException;
import com.db2ghsync.git.GitService;
import com.google.common.base.Strings;

/**
 * DropboxとGitリポジトリ間の同期処理を管理するクラス。
 * 各ディレクトリごとにファイルのダウンロード・Git操作を行う。
 */
public class SyncProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncProcessor.class);

    private static final String MAIN_BRANCH = "main";

    private final DropboxService dropboxService;
    private final GitService gitService;
    private final CursorService cursorService;

    /**
     * コンストラクタ。依存関係を注入してSyncProcessorを初期化する。
     * 
     * @param dropboxService Dropboxサービス
     * @param gitService     Gitサービス
     * @param cursorService  カーソルサービス
     */
    public SyncProcessor(DropboxService dropboxService, GitService gitService, CursorService cursorService) {
        this.dropboxService = Objects.requireNonNull(dropboxService, "DropboxService must not be null");
        this.gitService = Objects.requireNonNull(gitService, "GitService must not be null");
        this.cursorService = Objects.requireNonNull(cursorService, "CursorService must not be null");
    }

    /**
     * 同期処理のエントリポイント。
     * 設定されたディレクトリごとにDropboxからファイルを取得し、Gitリポジトリへ反映する。
     *
     * @throws GithubSyncException  各種処理失敗時
     * @throws DropboxSyncException
     * @throws IOException
     */
    public void start() throws GithubSyncException, DropboxSyncException, IOException {

        LOGGER.info("Starting synchronization process");

        List<String> targetDirs = dropboxService.getTargetDirectories();
        LOGGER.info("Found {} target directories to process", targetDirs.size());

        gitService.cloneOrOpenRepository();

        for (String targetDir : targetDirs) {

            LOGGER.info("Processing directory: {}", targetDir);
            List<SyncEntry> targetEntries = null;
            String cursor = cursorService.readCursor(targetDir);

            if (Strings.isNullOrEmpty(cursor)) {
                LOGGER.info("No cursor found for directory: {}. Fetching all files.", targetDir);
                targetEntries = dropboxService.getTargetFiles(targetDir);
            } else {
                LOGGER.info("Cursor found for directory: {}. Fetching changes since last sync.", targetDir);
                targetEntries = dropboxService.getChangesWithCursor(targetDir, cursor);
            }

            LOGGER.info("Found {} entries to sync for directory: {}", targetEntries.size(), targetDir);

            if (!targetEntries.isEmpty()) {
                dropboxService.downloadFiles(targetEntries);
                manageGit(targetDir);
            } else {
                LOGGER.info("No changes detected for directory: {}", targetDir);
            }

            cursorService.writeCursor(targetDir);
            LOGGER.info("Completed processing directory: {}", targetDir);

        }

        LOGGER.info("Synchronization process completed successfully");

    }

    /**
     * 指定ディレクトリのファイルをGitリポジトリへコミット・プッシュする。
     * 
     * @param targetDir 対象ディレクトリ名（ブランチ名としても利用）
     * @throws GithubSyncException Git操作失敗時
     */
    private void manageGit(String targetDir) throws GithubSyncException {

        LOGGER.debug("Starting Git operations for directory: {}", targetDir);
        gitService.checkoutBranch(targetDir);
        gitService.addAndCommit();
        gitService.push();
        gitService.checkoutBranch(MAIN_BRANCH);
        LOGGER.debug("Git operations completed for directory: {}", targetDir);
    }

}
