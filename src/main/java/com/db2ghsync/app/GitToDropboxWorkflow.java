package com.db2ghsync.app;

import java.io.IOException;

import com.db2ghsync.exception.DropboxSyncException;
import com.db2ghsync.exception.GithubSyncException;

/**
 * Git->Dropbox同期のワークフロー実装。
 */
public class GitToDropboxWorkflow implements SyncWorkflow {

    private final GitToDropboxProcessor processor;

    public GitToDropboxWorkflow(GitToDropboxProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void execute() throws DropboxSyncException, GithubSyncException, IOException {
        processor.start();
    }
}

