package com.db2ghsync.app;

import java.io.IOException;

import com.db2ghsync.exception.DropboxSyncException;
import com.db2ghsync.exception.GithubSyncException;

/**
 * Dropbox->Git同期向けの実装。既存SyncProcessorをラップする。
 */
public class DropboxToGitWorkflow implements SyncWorkflow {

    private final SyncProcessor syncProcessor;

    public DropboxToGitWorkflow(SyncProcessor syncProcessor) {
        this.syncProcessor = syncProcessor;
    }

    @Override
    public void execute() throws DropboxSyncException, GithubSyncException, IOException {
        syncProcessor.start();
    }
}

