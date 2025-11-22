package com.db2ghsync.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ConfigManagerクラスのテスト。
 * 設定ファイル読み込み、AppConfig生成、例外処理を検証する。
 */
class ConfigManagerTest {

    @TempDir
    Path tempDir;

    private String validConfigPath;
    private String invalidConfigPath;
    private String nonExistentConfigPath;

    @BeforeEach
    void setUp() throws IOException {
        // 正常な設定ファイルを作成
        validConfigPath = createValidConfigFile();
        // 必須項目が不足している設定ファイルを作成
        invalidConfigPath = createInvalidConfigFile();
        // 存在しないファイルパス
        nonExistentConfigPath = tempDir.resolve("nonexistent.properties").toString();
    }

    @AfterEach
    void tearDown() {
        // 静的フィールドをリセットするため、リフレクションを使用
        try {
            java.lang.reflect.Field field = ConfigManager.class.getDeclaredField("appConfig");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            // リフレクションエラーは無視（テストの独立性に影響しない）
        }
    }

    @Test
    void testLoadConfigWithValidFile() {
        // When
        ConfigManager.loadConfig(validConfigPath);

        // Then - 例外がスローされないことを確認
        AppConfig config = ConfigManager.getAppConfig();
        assertNotNull(config);
    }

    @Test
    void testGetAppConfigReturnsCorrectValues() {
        // Given
        ConfigManager.loadConfig(validConfigPath);

        // When
        AppConfig config = ConfigManager.getAppConfig();

        // Then
        assertNotNull(config);
        assertEquals("test_refresh_token", config.getDropboxRefreshToken());
        assertEquals("test_client_id", config.getDropboxClientId());
        assertEquals("test_client_secret", config.getDropboxClientSecret());
        assertEquals("test_access_token", config.getDropboxAccessToken());
        assertEquals("test_github_pat", config.getGithubPat());
        assertEquals("testuser", config.getGithubUsername());
        assertEquals("https://github.com/testuser/testrepo.git", config.getGithubRemoteUrl());
        assertEquals("/tmp/testrepo", config.getLocalRepoPath());
        assertEquals("/tmp/cursor", config.getCursorFilePath());

        List<String> extensions = config.getTargetFileExtensions();
        assertEquals(Arrays.asList(".txt", ".md", ".java"), extensions);

        List<String> directories = config.getTargetDirectories();
        assertEquals(Arrays.asList("/dir1", "/dir2"), directories);
        assertEquals("review", config.getSyncTargetDir());
    }

    @Test
    void testLoadConfigWithMissingRequiredPropertyThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ConfigManager.loadConfig(invalidConfigPath);
        });
        assertTrue(exception.getMessage().contains("Required property is missing"));
    }

    @Test
    void testLoadConfigWithNonExistentFileThrowsException() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ConfigManager.loadConfig(nonExistentConfigPath);
        });
        assertTrue(exception.getMessage().contains("Failed to read config file"));
    }

    @Test
    void testGetAppConfigBeforeLoadConfigThrowsException() {
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ConfigManager.getAppConfig();
        });
        assertTrue(exception.getMessage().contains("Config not loaded"));
    }

    @Test
    void testStaticGetterMethodsWorkCorrectly() {
        // Given
        ConfigManager.loadConfig(validConfigPath);

        // When & Then
        assertEquals("test_refresh_token", ConfigManager.getDropboxRefreshToken());
        assertEquals("test_client_id", ConfigManager.getDropboxClientId());
        assertEquals("test_client_secret", ConfigManager.getDropboxClientSecret());
        assertEquals("test_access_token", ConfigManager.getDropboxAccessToken());
        assertEquals("test_github_pat", ConfigManager.getGithubPat());
        assertEquals("testuser", ConfigManager.getGithubUsername());
        assertEquals("https://github.com/testuser/testrepo.git", ConfigManager.getGithubRemoteUrl());
        assertEquals("/tmp/testrepo", ConfigManager.getLocalRepoPath());
        assertEquals("/tmp/cursor", ConfigManager.getCursorFilePath());

        List<String> extensions = ConfigManager.getTargetFileExtensions();
        assertEquals(Arrays.asList(".txt", ".md", ".java"), extensions);

        List<String> directories = ConfigManager.getTargetDirectories();
        assertEquals(Arrays.asList("/dir1", "/dir2"), directories);
        assertEquals("review", ConfigManager.getSyncTargetDir());
    }

    @Test
    void testLoadConfigWithEmptyOptionalProperties() throws IOException {
        // Given - オプション項目が空の設定ファイル
        String configPath = tempDir.resolve("config-empty-optional.properties").toString();
        try (FileWriter writer = new FileWriter(configPath)) {
            writer.write("dropbox.refresh.token=\n");
            writer.write("dropbox.client.id=\n");
            writer.write("dropbox.client.secret=\n");
            writer.write("dropbox.access.token=\n");
            writer.write("github.pat=test_pat\n");
            writer.write("github.username=testuser\n");
            writer.write("github.remote.url=https://github.com/test/repo.git\n");
            writer.write("local.repo.path=/tmp/repo\n");
            writer.write("cursor.file.path=/tmp/cursor\n");
            writer.write("target.file.extensions=.txt\n");
            writer.write("target.directories=/dir1\n");
            writer.write("sync.target.dir=review\n");
        }

        // When
        ConfigManager.loadConfig(configPath);
        AppConfig config = ConfigManager.getAppConfig();

        // Then
        assertEquals("", config.getDropboxRefreshToken());
        assertEquals("", config.getDropboxClientId());
        assertEquals("", config.getDropboxClientSecret());
        assertEquals("", config.getDropboxAccessToken());
    }

    private String createValidConfigFile() throws IOException {
        File configFile = tempDir.resolve("config.properties").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("dropbox.refresh.token=test_refresh_token\n");
            writer.write("dropbox.client.id=test_client_id\n");
            writer.write("dropbox.client.secret=test_client_secret\n");
            writer.write("dropbox.access.token=test_access_token\n");
            writer.write("github.pat=test_github_pat\n");
            writer.write("github.username=testuser\n");
            writer.write("github.remote.url=https://github.com/testuser/testrepo.git\n");
            writer.write("local.repo.path=/tmp/testrepo\n");
            writer.write("cursor.file.path=/tmp/cursor\n");
            writer.write("target.file.extensions=.txt,.md,.java\n");
            writer.write("target.directories=/dir1,/dir2\n");
            writer.write("sync.target.dir=review\n");
        }
        return configFile.getAbsolutePath();
    }

    private String createInvalidConfigFile() throws IOException {
        File configFile = tempDir.resolve("config-invalid.properties").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("dropbox.refresh.token=test_refresh_token\n");
            writer.write("dropbox.client.id=test_client_id\n");
            writer.write("dropbox.client.secret=test_client_secret\n");
            writer.write("dropbox.access.token=test_access_token\n");
            // github.pat を意図的に欠落させる
            writer.write("github.username=testuser\n");
            writer.write("github.remote.url=https://github.com/testuser/testrepo.git\n");
            writer.write("local.repo.path=/tmp/testrepo\n");
            writer.write("cursor.file.path=/tmp/cursor\n");
            writer.write("target.file.extensions=.txt,.md,.java\n");
            writer.write("target.directories=/dir1,/dir2\n");
            writer.write("sync.target.dir=review\n");
        }
        return configFile.getAbsolutePath();
    }
}

