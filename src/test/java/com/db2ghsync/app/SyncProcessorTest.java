package com.db2ghsync.app;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.db2ghsync.common.SyncAction;
import com.db2ghsync.dropbox.CursorService;
import com.db2ghsync.dropbox.DropboxService;
import com.db2ghsync.entity.SyncEntry;
import com.db2ghsync.exception.DropboxSyncException;
import com.db2ghsync.exception.GithubSyncException;
import com.db2ghsync.git.GitService;

/**
 * SyncProcessorクラスのテスト。
 * モックを使用して同期処理のロジックを検証する。
 */
@ExtendWith(MockitoExtension.class)
class SyncProcessorTest {

    @Mock
    private DropboxService dropboxService;

    @Mock
    private GitService gitService;

    @Mock
    private CursorService cursorService;

    private SyncProcessor syncProcessor;

    @BeforeEach
    void setUp() {
        syncProcessor = new SyncProcessor(dropboxService, gitService, cursorService);
    }

    @Test
    void testStartWithNoCursorFetchesAllFiles() throws Exception {
        // Given
        String targetDir = "/dir1";
        List<String> targetDirs = Arrays.asList(targetDir);
        List<SyncEntry> entries = Arrays.asList(
                createSyncEntry("/dir1/file1.txt", "file1.txt", targetDir, SyncAction.CREATE_OR_UPDATE));

        when(dropboxService.getTargetDirectories()).thenReturn(targetDirs);
        when(cursorService.readCursor(targetDir)).thenReturn("");
        when(dropboxService.getTargetFiles(targetDir)).thenReturn(entries);

        // When
        syncProcessor.start();

        // Then
        verify(dropboxService).getTargetFiles(targetDir);
        verify(dropboxService, never()).getChangesWithCursor(anyString(), anyString());
        verify(dropboxService).downloadFiles(entries);
        verify(gitService).checkoutBranch(targetDir);
        verify(gitService).addAndCommit();
        verify(gitService).push();
        verify(gitService).checkoutBranch("main");
        verify(cursorService).writeCursor(targetDir);
    }

    @Test
    void testStartWithCursorFetchesChangesOnly() throws Exception {
        // Given
        String targetDir = "/dir1";
        String cursor = "cursor123";
        List<String> targetDirs = Arrays.asList(targetDir);
        List<SyncEntry> entries = Arrays.asList(
                createSyncEntry("/dir1/file1.txt", "file1.txt", targetDir, SyncAction.CREATE_OR_UPDATE));

        when(dropboxService.getTargetDirectories()).thenReturn(targetDirs);
        when(cursorService.readCursor(targetDir)).thenReturn(cursor);
        when(dropboxService.getChangesWithCursor(targetDir, cursor)).thenReturn(entries);

        // When
        syncProcessor.start();

        // Then
        verify(dropboxService, never()).getTargetFiles(anyString());
        verify(dropboxService).getChangesWithCursor(targetDir, cursor);
        verify(dropboxService).downloadFiles(entries);
        verify(gitService).checkoutBranch(targetDir);
        verify(gitService).addAndCommit();
        verify(gitService).push();
        verify(gitService).checkoutBranch("main");
        verify(cursorService).writeCursor(targetDir);
    }

    @Test
    void testStartWithNoChangesSkipsGitOperations() throws Exception {
        // Given
        String targetDir = "/dir1";
        List<String> targetDirs = Arrays.asList(targetDir);
        List<SyncEntry> emptyEntries = Collections.emptyList();

        when(dropboxService.getTargetDirectories()).thenReturn(targetDirs);
        when(cursorService.readCursor(targetDir)).thenReturn("cursor123");
        when(dropboxService.getChangesWithCursor(targetDir, "cursor123")).thenReturn(emptyEntries);

        // When
        syncProcessor.start();

        // Then
        verify(dropboxService, never()).downloadFiles(any());
        verify(gitService, never()).checkoutBranch(targetDir);
        verify(gitService, never()).addAndCommit();
        verify(gitService, never()).push();
        verify(cursorService).writeCursor(targetDir);
    }

    @Test
    void testStartProcessesMultipleDirectories() throws Exception {
        // Given
        String dir1 = "/dir1";
        String dir2 = "/dir2";
        List<String> targetDirs = Arrays.asList(dir1, dir2);
        List<SyncEntry> entries1 = Arrays.asList(
                createSyncEntry("/dir1/file1.txt", "file1.txt", dir1, SyncAction.CREATE_OR_UPDATE));
        List<SyncEntry> entries2 = Arrays.asList(
                createSyncEntry("/dir2/file2.txt", "file2.txt", dir2, SyncAction.CREATE_OR_UPDATE));

        when(dropboxService.getTargetDirectories()).thenReturn(targetDirs);
        when(cursorService.readCursor(dir1)).thenReturn("");
        when(cursorService.readCursor(dir2)).thenReturn("cursor2");
        when(dropboxService.getTargetFiles(dir1)).thenReturn(entries1);
        when(dropboxService.getChangesWithCursor(dir2, "cursor2")).thenReturn(entries2);

        // When
        syncProcessor.start();

        // Then
        verify(dropboxService).getTargetFiles(dir1);
        verify(dropboxService).getChangesWithCursor(dir2, "cursor2");
        verify(dropboxService).downloadFiles(entries1);
        verify(dropboxService).downloadFiles(entries2);
        verify(gitService, times(4)).checkoutBranch(anyString());
        verify(gitService, times(2)).addAndCommit();
        verify(gitService, times(2)).push();
        verify(cursorService).writeCursor(dir1);
        verify(cursorService).writeCursor(dir2);
    }

    @Test
    void testStartWithCorrectOrderOfOperations() throws Exception {
        // Given
        String targetDir = "/dir1";
        List<String> targetDirs = Arrays.asList(targetDir);
        List<SyncEntry> entries = Arrays.asList(
                createSyncEntry("/dir1/file1.txt", "file1.txt", targetDir, SyncAction.CREATE_OR_UPDATE));

        when(dropboxService.getTargetDirectories()).thenReturn(targetDirs);
        when(cursorService.readCursor(targetDir)).thenReturn("");
        when(dropboxService.getTargetFiles(targetDir)).thenReturn(entries);

        // When
        syncProcessor.start();

        // Then - 操作の順序を検証
        InOrder inOrder = inOrder(dropboxService, gitService, cursorService);
        inOrder.verify(dropboxService).getTargetDirectories();
        inOrder.verify(gitService).cloneOrOpenRepository();
        inOrder.verify(cursorService).readCursor(targetDir);
        inOrder.verify(dropboxService).getTargetFiles(targetDir);
        inOrder.verify(dropboxService).downloadFiles(entries);
        inOrder.verify(gitService).checkoutBranch(targetDir);
        inOrder.verify(gitService).addAndCommit();
        inOrder.verify(gitService).push();
        inOrder.verify(gitService).checkoutBranch("main");
        inOrder.verify(cursorService).writeCursor(targetDir);
    }

    @Test
    void testStartPropagatesDropboxSyncException() throws Exception {
        // Given
        String targetDir = "/dir1";
        List<String> targetDirs = Arrays.asList(targetDir);

        when(dropboxService.getTargetDirectories()).thenReturn(targetDirs);
        when(cursorService.readCursor(targetDir)).thenReturn("");
        when(dropboxService.getTargetFiles(targetDir))
                .thenThrow(new DropboxSyncException("Dropbox error"));

        // When & Then
        assertThrows(DropboxSyncException.class, () -> syncProcessor.start());
    }

    @Test
    void testStartPropagatesGithubSyncException() throws Exception {
        // Given
        String targetDir = "/dir1";
        List<String> targetDirs = Arrays.asList(targetDir);
        List<SyncEntry> entries = Arrays.asList(
                createSyncEntry("/dir1/file1.txt", "file1.txt", targetDir, SyncAction.CREATE_OR_UPDATE));

        when(dropboxService.getTargetDirectories()).thenReturn(targetDirs);
        when(cursorService.readCursor(targetDir)).thenReturn("");
        when(dropboxService.getTargetFiles(targetDir)).thenReturn(entries);
        doThrow(new GithubSyncException("Git error"))
                .when(gitService).checkoutBranch(targetDir);

        // When & Then
        assertThrows(GithubSyncException.class, () -> syncProcessor.start());
    }

    @Test
    void testStartPropagatesGithubSyncExceptionFromClone() throws Exception {
        // Given
        String targetDir = "/dir1";
        List<String> targetDirs = Arrays.asList(targetDir);

        when(dropboxService.getTargetDirectories()).thenReturn(targetDirs);
        doThrow(new GithubSyncException("Clone error"))
                .when(gitService).cloneOrOpenRepository();

        // When & Then
        assertThrows(GithubSyncException.class, () -> syncProcessor.start());
    }

    @Test
    void testStartCallsCloneOrOpenRepositoryOnce() throws Exception {
        // Given
        String targetDir = "/dir1";
        List<String> targetDirs = Arrays.asList(targetDir);
        List<SyncEntry> entries = Arrays.asList(
                createSyncEntry("/dir1/file1.txt", "file1.txt", targetDir, SyncAction.CREATE_OR_UPDATE));

        when(dropboxService.getTargetDirectories()).thenReturn(targetDirs);
        when(cursorService.readCursor(targetDir)).thenReturn("");
        when(dropboxService.getTargetFiles(targetDir)).thenReturn(entries);

        // When
        syncProcessor.start();

        // Then
        verify(gitService, times(1)).cloneOrOpenRepository();
    }

    private SyncEntry createSyncEntry(String dropboxPath, String name, String subDirectoryKey,
            SyncAction action) {
        return new SyncEntry(dropboxPath, name, subDirectoryKey, action);
    }
}

