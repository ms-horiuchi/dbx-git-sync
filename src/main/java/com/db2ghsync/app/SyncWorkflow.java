package com.db2ghsync.app;

import java.io.IOException;

import com.db2ghsync.exception.DropboxSyncException;
import com.db2ghsync.exception.GithubSyncException;

/**
 * 同期処理の戦略インターフェース。
 */
public interface SyncWorkflow {

    void execute() throws DropboxSyncException, GithubSyncException, IOException;
}

