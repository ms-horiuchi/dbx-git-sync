package com.db2ghsync.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * AppConfigクラスのテスト。
 * Builderパターン、不変性、nullチェックを検証する。
 */
class AppConfigTest {

    @Test
    void testBuilderCreatesValidAppConfig() {
        // Given
        List<String> extensions = Arrays.asList(".txt", ".md");
        List<String> directories = Arrays.asList("dir1", "dir2");

        // When
        AppConfig config = new AppConfig.Builder()
                .dropboxRefreshToken("refresh_token")
                .dropboxClientId("client_id")
                .dropboxClientSecret("client_secret")
                .dropboxAccessToken("access_token")
                .githubPat("github_pat")
                .githubUsername("testuser")
                .githubRemoteUrl("https://github.com/test/repo.git")
                .localRepoPath("/path/to/repo")
                .cursorFilePath("/path/to/cursor")
                .targetFileExtensions(extensions)
                .targetDirectories(directories)
                .syncTargetDir("review")
                .build();

        // Then
        assertNotNull(config);
        assertEquals("refresh_token", config.getDropboxRefreshToken());
        assertEquals("client_id", config.getDropboxClientId());
        assertEquals("client_secret", config.getDropboxClientSecret());
        assertEquals("access_token", config.getDropboxAccessToken());
        assertEquals("github_pat", config.getGithubPat());
        assertEquals("testuser", config.getGithubUsername());
        assertEquals("https://github.com/test/repo.git", config.getGithubRemoteUrl());
        assertEquals("/path/to/repo", config.getLocalRepoPath());
        assertEquals("/path/to/cursor", config.getCursorFilePath());
        assertEquals(extensions, config.getTargetFileExtensions());
        assertEquals(directories, config.getTargetDirectories());
        assertEquals("review", config.getSyncTargetDir());
    }

    @Test
    void testBuilderWithNullGithubPatThrowsException() {
        // Given
        List<String> extensions = Arrays.asList(".txt");
        List<String> directories = Arrays.asList("dir1");

        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            new AppConfig.Builder()
                    .githubUsername("testuser")
                    .githubRemoteUrl("https://github.com/test/repo.git")
                    .localRepoPath("/path/to/repo")
                    .cursorFilePath("/path/to/cursor")
                    .targetFileExtensions(extensions)
                    .targetDirectories(directories)
                    .syncTargetDir("review")
                    .build();
        });
        assertTrue(exception.getMessage().contains("github.pat must not be null"));
    }

    @Test
    void testBuilderWithNullGithubUsernameThrowsException() {
        // Given
        List<String> extensions = Arrays.asList(".txt");
        List<String> directories = Arrays.asList("dir1");

        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            new AppConfig.Builder()
                    .githubPat("github_pat")
                    .githubRemoteUrl("https://github.com/test/repo.git")
                    .localRepoPath("/path/to/repo")
                    .cursorFilePath("/path/to/cursor")
                    .targetFileExtensions(extensions)
                    .targetDirectories(directories)
                    .syncTargetDir("review")
                    .build();
        });
        assertTrue(exception.getMessage().contains("github.username must not be null"));
    }

    @Test
    void testBuilderWithNullGithubRemoteUrlThrowsException() {
        // Given
        List<String> extensions = Arrays.asList(".txt");
        List<String> directories = Arrays.asList("dir1");

        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            new AppConfig.Builder()
                    .githubPat("github_pat")
                    .githubUsername("testuser")
                    .localRepoPath("/path/to/repo")
                    .cursorFilePath("/path/to/cursor")
                    .targetFileExtensions(extensions)
                    .targetDirectories(directories)
                    .syncTargetDir("review")
                    .build();
        });
        assertTrue(exception.getMessage().contains("github.remote.url must not be null"));
    }

    @Test
    void testBuilderWithNullLocalRepoPathThrowsException() {
        // Given
        List<String> extensions = Arrays.asList(".txt");
        List<String> directories = Arrays.asList("dir1");

        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            new AppConfig.Builder()
                    .githubPat("github_pat")
                    .githubUsername("testuser")
                    .githubRemoteUrl("https://github.com/test/repo.git")
                    .cursorFilePath("/path/to/cursor")
                    .targetFileExtensions(extensions)
                    .targetDirectories(directories)
                    .syncTargetDir("review")
                    .build();
        });
        assertTrue(exception.getMessage().contains("local.repo.path must not be null"));
    }

    @Test
    void testBuilderWithNullCursorFilePathThrowsException() {
        // Given
        List<String> extensions = Arrays.asList(".txt");
        List<String> directories = Arrays.asList("dir1");

        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            new AppConfig.Builder()
                    .githubPat("github_pat")
                    .githubUsername("testuser")
                    .githubRemoteUrl("https://github.com/test/repo.git")
                    .localRepoPath("/path/to/repo")
                    .targetFileExtensions(extensions)
                    .targetDirectories(directories)
                    .syncTargetDir("review")
                    .build();
        });
        assertTrue(exception.getMessage().contains("cursor.file.path must not be null"));
    }

    @Test
    void testBuilderWithNullTargetFileExtensionsThrowsException() {
        // Given
        List<String> directories = Arrays.asList("dir1");

        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            new AppConfig.Builder()
                    .githubPat("github_pat")
                    .githubUsername("testuser")
                    .githubRemoteUrl("https://github.com/test/repo.git")
                    .localRepoPath("/path/to/repo")
                    .cursorFilePath("/path/to/cursor")
                    .targetDirectories(directories)
                    .syncTargetDir("review")
                    .build();
        });
        assertTrue(exception.getMessage().contains("target.file.extensions must not be null"));
    }

    @Test
    void testBuilderWithNullTargetDirectoriesThrowsException() {
        // Given
        List<String> extensions = Arrays.asList(".txt");

        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            new AppConfig.Builder()
                    .githubPat("github_pat")
                    .githubUsername("testuser")
                    .githubRemoteUrl("https://github.com/test/repo.git")
                    .localRepoPath("/path/to/repo")
                    .cursorFilePath("/path/to/cursor")
                    .targetFileExtensions(extensions)
                    .syncTargetDir("review")
                    .build();
        });
        assertTrue(exception.getMessage().contains("target.directories must not be null"));
    }

    @Test
    void testBuilderWithNullSyncTargetDirThrowsException() {
        // Given
        List<String> extensions = Arrays.asList(".txt");
        List<String> directories = Arrays.asList("dir1");

        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            new AppConfig.Builder()
                    .githubPat("github_pat")
                    .githubUsername("testuser")
                    .githubRemoteUrl("https://github.com/test/repo.git")
                    .localRepoPath("/path/to/repo")
                    .cursorFilePath("/path/to/cursor")
                    .targetFileExtensions(extensions)
                    .targetDirectories(directories)
                    .syncTargetDir(null)
                    .build();
        });
        assertTrue(exception.getMessage().contains("sync.target.dir must not be null"));
    }

    @Test
    void testImmutabilityOfCollections() {
        // Given
        List<String> extensions = Arrays.asList(".txt", ".md");
        List<String> directories = Arrays.asList("dir1", "dir2");

        // When
        AppConfig config = new AppConfig.Builder()
                .githubPat("github_pat")
                .githubUsername("testuser")
                .githubRemoteUrl("https://github.com/test/repo.git")
                .localRepoPath("/path/to/repo")
                .cursorFilePath("/path/to/cursor")
                .targetFileExtensions(extensions)
                .targetDirectories(directories)
                .syncTargetDir("review")
                .build();

        List<String> returnedExtensions = config.getTargetFileExtensions();
        List<String> returnedDirectories = config.getTargetDirectories();

        // Then - 返却されたコレクションが変更不可であることを確認
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedExtensions.add(".java");
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            returnedDirectories.add("dir3");
        });
    }

    @Test
    void testOptionalDropboxFieldsCanBeEmpty() {
        // Given
        List<String> extensions = Arrays.asList(".txt");
        List<String> directories = Arrays.asList("dir1");

        // When
        AppConfig config = new AppConfig.Builder()
                .dropboxRefreshToken("")
                .dropboxClientId("")
                .dropboxClientSecret("")
                .dropboxAccessToken("")
                .githubPat("github_pat")
                .githubUsername("testuser")
                .githubRemoteUrl("https://github.com/test/repo.git")
                .localRepoPath("/path/to/repo")
                .cursorFilePath("/path/to/cursor")
                .targetFileExtensions(extensions)
                .targetDirectories(directories)
                .syncTargetDir("review")
                .build();

        // Then
        assertEquals("", config.getDropboxRefreshToken());
        assertEquals("", config.getDropboxClientId());
        assertEquals("", config.getDropboxClientSecret());
        assertEquals("", config.getDropboxAccessToken());
        assertEquals("review", config.getSyncTargetDir());
    }

    @Test
    void testBuilderWithNullOptionalFieldsDefaultsToEmpty() {
        // Given
        List<String> extensions = Arrays.asList(".txt");
        List<String> directories = Arrays.asList("dir1");

        // When
        AppConfig config = new AppConfig.Builder()
                .dropboxRefreshToken(null)
                .dropboxClientId(null)
                .dropboxClientSecret(null)
                .dropboxAccessToken(null)
                .githubPat("github_pat")
                .githubUsername("testuser")
                .githubRemoteUrl("https://github.com/test/repo.git")
                .localRepoPath("/path/to/repo")
                .cursorFilePath("/path/to/cursor")
                .targetFileExtensions(extensions)
                .targetDirectories(directories)
                .syncTargetDir("review")
                .build();

        // Then
        assertEquals("", config.getDropboxRefreshToken());
        assertEquals("", config.getDropboxClientId());
        assertEquals("", config.getDropboxClientSecret());
        assertEquals("", config.getDropboxAccessToken());
        assertEquals("review", config.getSyncTargetDir());
    }
}

