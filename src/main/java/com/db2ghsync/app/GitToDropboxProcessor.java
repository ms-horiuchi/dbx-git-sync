package com.db2ghsync.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.db2ghsync.common.AppConfig;
import com.db2ghsync.dropbox.DropboxService;
import com.db2ghsync.exception.DropboxSyncException;
import com.db2ghsync.exception.GithubSyncException;
import com.db2ghsync.git.GitService;

/**
 * Git->Dropbox同期の本体処理を担当するクラス。
 */
public class GitToDropboxProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitToDropboxProcessor.class);

    private final AppConfig config;
    private final GitService gitService;
    private final DropboxService dropboxService;

    public GitToDropboxProcessor(AppConfig config, GitService gitService, DropboxService dropboxService) {
        this.config = config;
        this.gitService = gitService;
        this.dropboxService = dropboxService;
    }

    public void start() throws DropboxSyncException, GithubSyncException, IOException {
        try {
            gitService.cloneOrOpenRepository();
            List<String> branches = gitService.listLocalBranches();
            LOGGER.info("Processing {} branches for git-to-dbx sync", branches.size());

            for (String branch : branches) {
                processBranch(branch);
            }
        } finally {
            gitService.close();
        }
    }

    private void processBranch(String branch)
            throws GithubSyncException, IOException, DropboxSyncException {

        LOGGER.info("Processing branch: {}", branch);
        gitService.checkoutBranch(branch);
        Set<String> updatedFiles = gitService.pullLatestChanges();
        
        LOGGER.info("Branch {}: pullLatestChanges returned {} files", branch, updatedFiles.size());
        if (!updatedFiles.isEmpty()) {
            LOGGER.info("Updated files list: {}", updatedFiles);
        }
        
        if (updatedFiles.isEmpty()) {
            LOGGER.info("No changes detected for branch {}", branch);
            return;
        }

        Path repoRoot = Paths.get(config.getLocalRepoPath()).toAbsolutePath().normalize();
        String targetDir = ensureTrailingSlash(normalizeRelativePath(config.getSyncTargetDir()));
        LOGGER.info("Target directory filter: '{}' (files must start with this path)", targetDir);

        int uploadCount = 0;
        int filteredOutCount = 0;
        int nonFileCount = 0;
        
        for (String updatedFile : updatedFiles) {
            String normalized = normalizeRelativePath(updatedFile);
            LOGGER.debug("Checking file: '{}' -> normalized: '{}'", updatedFile, normalized);
            
            if (!normalized.startsWith(targetDir)) {
                LOGGER.debug("File '{}' filtered out (does not start with target dir '{}')", 
                        normalized, targetDir);
                filteredOutCount++;
                continue;
            }

            Path localFilePath = repoRoot.resolve(updatedFile).normalize();
            if (!Files.isRegularFile(localFilePath)) {
                LOGGER.debug("Skipping non-file path: {}", localFilePath);
                nonFileCount++;
                continue;
            }

            Path relative = repoRoot.relativize(localFilePath);
            String dropboxPath = buildDropboxPath(branch, relative);
            LOGGER.info("Uploading file: {} -> Dropbox: {}", localFilePath, dropboxPath);
            dropboxService.uploadFile(localFilePath, dropboxPath);
            uploadCount++;
        }

        LOGGER.info("Branch {} summary: {} files uploaded, {} filtered out by target dir, {} non-files skipped", 
                branch, uploadCount, filteredOutCount, nonFileCount);
    }

    private String normalizeRelativePath(String path) {
        return path.replace("\\", "/").replaceAll("//+", "/").replaceFirst("^/", "");
    }

    private String ensureTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    private String buildDropboxPath(String branchName, Path relativePath) {
        String sanitizedBranch = branchName.replace("\\", "/").replaceAll("//+", "/").replaceFirst("^/", "");
        return "/" + sanitizedBranch + "/" + relativePath.toString().replace("\\", "/");
    }
}

