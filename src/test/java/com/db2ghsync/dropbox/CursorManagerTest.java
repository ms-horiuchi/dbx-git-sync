package com.db2ghsync.dropbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.db2ghsync.common.AppConfig;
import com.db2ghsync.exception.DropboxSyncException;

/**
 * CursorManagerクラスのテスト。
 * ファイル操作、例外処理を検証する。
 */
class CursorManagerTest {

    @TempDir
    Path tempDir;

    private CursorManager cursorManager;
    private String cursorFilePath;

    @BeforeEach
    void setUp() {
        cursorFilePath = tempDir.toString();
        AppConfig config = new AppConfig.Builder()
                .githubPat("test_pat")
                .githubUsername("testuser")
                .githubRemoteUrl("https://github.com/test/repo.git")
                .localRepoPath("/tmp/repo")
                .cursorFilePath(cursorFilePath)
                .targetFileExtensions(java.util.Arrays.asList(".txt"))
                .targetDirectories(java.util.Arrays.asList("/dir1"))
                .syncTargetDir("review")
                .build();
        cursorManager = new CursorManager(config);
    }

    @AfterEach
    void tearDown() {
        // クリーンアップは@TempDirが自動的に行う
    }

    @Test
    void testReadCursorFromExistingFile() throws IOException {
        // Given
        String branchName = "test-branch";
        String expectedCursor = "cursor123";
        Path cursorFile = tempDir.resolve(branchName);
        Files.writeString(cursorFile, expectedCursor);

        // When
        String actualCursor = cursorManager.readCursor(branchName);

        // Then
        assertEquals(expectedCursor, actualCursor);
    }

    @Test
    void testReadCursorFromNonExistentFileReturnsEmptyString() {
        // Given
        String branchName = "non-existent-branch";

        // When
        String cursor = cursorManager.readCursor(branchName);

        // Then
        assertEquals("", cursor);
    }

    @Test
    void testWriteTmpCursorCreatesTemporaryFile() throws Exception {
        // Given
        String branchName = "test-branch";
        String cursor = "cursor123";

        // When
        cursorManager.writeTmpCursor(branchName, cursor);

        // Then
        Path tmpFile = tempDir.resolve(branchName + ".tmp");
        assertTrue(Files.exists(tmpFile));
        String content = Files.readString(tmpFile);
        assertEquals(cursor, content);
    }

    @Test
    void testWriteCursorSavesCursorAndDeletesTmpFile() throws Exception {
        // Given
        String branchName = "test-branch";
        String cursor = "cursor123";
        Path tmpFile = tempDir.resolve(branchName + ".tmp");
        Files.writeString(tmpFile, cursor);

        // When
        cursorManager.writeCursor(branchName);

        // Then
        Path cursorFile = tempDir.resolve(branchName);
        assertTrue(Files.exists(cursorFile));
        String savedCursor = Files.readString(cursorFile);
        assertEquals(cursor, savedCursor);
        assertFalse(Files.exists(tmpFile));
    }

    @Test
    void testWriteCursorThrowsExceptionWhenTmpFileDoesNotExist() {
        // Given
        String branchName = "test-branch";

        // When & Then
        assertThrows(DropboxSyncException.class, () -> {
            cursorManager.writeCursor(branchName);
        });
    }

    @Test
    void testWriteTmpCursorThrowsExceptionOnWriteFailure() {
        // Given - 読み取り専用ディレクトリをシミュレート（実際には難しいため、無効なパスを使用）
        String invalidPath = "/invalid/path/that/does/not/exist";
        AppConfig invalidConfig = new AppConfig.Builder()
                .githubPat("test_pat")
                .githubUsername("testuser")
                .githubRemoteUrl("https://github.com/test/repo.git")
                .localRepoPath("/tmp/repo")
                .cursorFilePath(invalidPath)
                .targetFileExtensions(java.util.Arrays.asList(".txt"))
                .targetDirectories(java.util.Arrays.asList("/dir1"))
                .syncTargetDir("review")
                .build();
        CursorManager invalidManager = new CursorManager(invalidConfig);

        String branchName = "test-branch";
        String cursor = "cursor123";

        // When & Then
        assertThrows(DropboxSyncException.class, () -> {
            invalidManager.writeTmpCursor(branchName, cursor);
        });
    }

    @Test
    void testReadCursorHandlesMultipleBranches() throws IOException {
        // Given
        String branch1 = "branch1";
        String branch2 = "branch2";
        String cursor1 = "cursor1";
        String cursor2 = "cursor2";

        Files.writeString(tempDir.resolve(branch1), cursor1);
        Files.writeString(tempDir.resolve(branch2), cursor2);

        // When
        String readCursor1 = cursorManager.readCursor(branch1);
        String readCursor2 = cursorManager.readCursor(branch2);

        // Then
        assertEquals(cursor1, readCursor1);
        assertEquals(cursor2, readCursor2);
    }

    @Test
    void testWriteCursorOverwritesExistingCursor() throws Exception {
        // Given
        String branchName = "test-branch";
        String oldCursor = "old-cursor";
        String newCursor = "new-cursor";

        // 既存のカーソルファイルを作成
        Path cursorFile = tempDir.resolve(branchName);
        Files.writeString(cursorFile, oldCursor);

        // 一時ファイルを作成
        Path tmpFile = tempDir.resolve(branchName + ".tmp");
        Files.writeString(tmpFile, newCursor);

        // When
        cursorManager.writeCursor(branchName);

        // Then
        String savedCursor = Files.readString(cursorFile);
        assertEquals(newCursor, savedCursor);
    }

    @Test
    void testConstructorWithNullConfigThrowsException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new CursorManager(null);
        });
    }
}

