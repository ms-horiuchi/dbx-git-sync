package com.db2ghsync.app;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.db2ghsync.dropbox.CursorManager;
import com.db2ghsync.dropbox.DropboxClient;
import com.db2ghsync.entity.SyncEntry;
import com.db2ghsync.exception.DropboxSyncException;
import com.db2ghsync.exception.GithubSyncException;
import com.db2ghsync.git.GitRepositoryManager;
import com.google.common.base.Strings;

/**
 * DropboxとGitリポジトリ間の同期処理を管理するクラス。
 * 各ディレクトリごとにファイルのダウンロード・Git操作を行う。
 */
public class SyncProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SyncProcessor.class);

    /**
     * 同期処理のエントリポイント。
     * 設定されたディレクトリごとにDropboxからファイルを取得し、Gitリポジトリへ反映する。
     * 
     * @throws GithubSyncException  各種処理失敗時
     * @throws DropboxSyncException
     * @throws IOException
     */
    public void start() throws GithubSyncException, DropboxSyncException, IOException {

        logger.info("Starting synchronization process");

        DropboxClient dbClient = new DropboxClient();
        GitRepositoryManager repositoryManager = new GitRepositoryManager();

        List<String> targetDirs = dbClient.getTargetDirectories();
        logger.info("Found {} target directories to process", targetDirs.size());

        repositoryManager.cloneOrOpenRepository();

        for (String targetDir : targetDirs) {

            logger.info("Processing directory: {}", targetDir);
            List<SyncEntry> targetEntries = null;
            String cursor = CursorManager.readCursor(targetDir);

            if (Strings.isNullOrEmpty(cursor)) {
                logger.info("No cursor found for directory: {}. Fetching all files.", targetDir);
                targetEntries = dbClient.getTargetFiles(targetDir);
            } else {
                logger.info("Cursor found for directory: {}. Fetching changes since last sync.", targetDir);
                targetEntries = dbClient.getChangesWithCursor(targetDir, cursor);
            }

            logger.info("Found {} entries to sync for directory: {}", targetEntries.size(), targetDir);

            if (!targetEntries.isEmpty()) {
                dbClient.downloadFiles(targetEntries);
                manageGit(targetDir, repositoryManager);
            } else {
                logger.info("No changes detected for directory: {}", targetDir);
            }

            CursorManager.writeCursor(targetDir);
            logger.info("Completed processing directory: {}", targetDir);

        }

        logger.info("Synchronization process completed successfully");

    }

    /**
     * 指定ディレクトリのファイルをGitリポジトリへコミット・プッシュする。
     * 
     * @param targetDir         対象ディレクトリ名（ブランチ名としても利用）
     * @param repositoryManager Git操作管理クラス
     * @throws GithubSyncException Git操作失敗時
     */
    private void manageGit(String targetDir, GitRepositoryManager repositoryManager) throws GithubSyncException {

        logger.debug("Starting Git operations for directory: {}", targetDir);
        repositoryManager.checkoutBranch(targetDir);
        repositoryManager.addAndCommit();
        repositoryManager.push();
        repositoryManager.checkoutBranch("main");
        logger.debug("Git operations completed for directory: {}", targetDir);
    }

}
