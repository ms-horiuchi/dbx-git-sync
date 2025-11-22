package com.db2ghsync.app;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.db2ghsync.common.AppConfig;
import com.db2ghsync.dropbox.DropboxService;
import com.db2ghsync.exception.DropboxSyncException;
import com.db2ghsync.exception.GithubSyncException;
import com.db2ghsync.git.GitService;

class GitToDropboxProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadsOnlyFilesUnderConfiguredDirectory() throws Exception {
        Path repo = tempDir.resolve("repo");
        Path reviewDir = repo.resolve("review");
        Files.createDirectories(reviewDir);
        Path reviewFile = reviewDir.resolve("file1.txt");
        Files.writeString(reviewFile, "test");

        AppConfig config = new AppConfig.Builder()
                .dropboxRefreshToken("")
                .dropboxClientId("")
                .dropboxClientSecret("")
                .dropboxAccessToken("")
                .githubPat("pat")
                .githubUsername("user")
                .githubRemoteUrl("https://example.com/repo.git")
                .localRepoPath(repo.toString())
                .cursorFilePath(repo.resolve("cursor").toString())
                .targetFileExtensions(List.of(".txt"))
                .targetDirectories(List.of("/dir"))
                .syncTargetDir("/review")
                .build();

        GitService gitService = mock(GitService.class);
        DropboxService dropboxService = mock(DropboxService.class);

        when(gitService.listLocalBranches()).thenReturn(List.of("feature"));
        when(gitService.pullLatestChanges()).thenReturn(Set.of("review/file1.txt", "other/file2.txt"));

        GitToDropboxProcessor processor = new GitToDropboxProcessor(config, gitService, dropboxService);

        processor.start();

        verify(gitService).cloneOrOpenRepository();
        verify(gitService).listLocalBranches();
        verify(gitService).checkoutBranch("feature");
        verify(gitService).pullLatestChanges();
        Path expectedPath = repo.toAbsolutePath().normalize().resolve("review/file1.txt").normalize();
        verify(dropboxService, times(1)).uploadFile(eq(expectedPath), eq("/feature/review/file1.txt"));
        verifyNoMoreInteractions(dropboxService);
        verify(gitService).close();
    }

    @Test
    void ensuresGitServiceClosedOnFailure() throws IOException, GithubSyncException {
        AppConfig config = new AppConfig.Builder()
                .dropboxRefreshToken("")
                .dropboxClientId("")
                .dropboxClientSecret("")
                .dropboxAccessToken("")
                .githubPat("pat")
                .githubUsername("user")
                .githubRemoteUrl("https://example.com/repo.git")
                .localRepoPath(tempDir.toString())
                .cursorFilePath(tempDir.resolve("cursor").toString())
                .targetFileExtensions(List.of(".txt"))
                .targetDirectories(List.of("/dir"))
                .syncTargetDir("review")
                .build();

        GitService gitService = mock(GitService.class);
        DropboxService dropboxService = mock(DropboxService.class);

        doThrow(new GithubSyncException("failed")).when(gitService).listLocalBranches();

        GitToDropboxProcessor processor = new GitToDropboxProcessor(config, gitService, dropboxService);

        assertThrows(GithubSyncException.class, processor::start);
        verify(gitService).cloneOrOpenRepository();
        verify(gitService).listLocalBranches();
        verify(gitService).close();
    }
}

