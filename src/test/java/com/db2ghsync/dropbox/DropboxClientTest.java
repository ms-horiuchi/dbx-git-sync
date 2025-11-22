package com.db2ghsync.dropbox;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.db2ghsync.common.AppConfig;
import com.db2ghsync.exception.DropboxSyncException;

/**
 * DropboxClientクラスのテスト。
 * モックを使用してDropbox操作を検証する。
 * 
 * 注意: DropboxClientは実際のDbxClientV2を使用しているため、
 * 完全なモックテストは困難です。基本的なコンストラクタテストと
 * nullチェックを中心に実装します。
 */
@ExtendWith(MockitoExtension.class)
class DropboxClientTest {

    @Mock
    private CursorService cursorService;

    @Test
    void testConstructorWithNullConfigThrowsException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new DropboxClient(null, cursorService);
        });
    }

    @Test
    void testConstructorWithNullCursorServiceThrowsException() {
        // Given
        AppConfig config = createTestAppConfig();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new DropboxClient(config, null);
        });
    }

    @Test
    void testConstructorWithAccessTokenCreatesClient() {
        // Given
        AppConfig config = createTestAppConfig();

        // When
        DropboxClient client = new DropboxClient(config, cursorService);

        // Then
        assertNotNull(client);
    }

    @Test
    void testConstructorWithRefreshTokenCreatesClient() {
        // Given
        AppConfig config = new AppConfig.Builder()
                .dropboxRefreshToken("refresh_token")
                .dropboxClientId("client_id")
                .dropboxClientSecret("client_secret")
                .dropboxAccessToken("")
                .githubPat("github_pat")
                .githubUsername("testuser")
                .githubRemoteUrl("https://github.com/test/repo.git")
                .localRepoPath("/tmp/repo")
                .cursorFilePath("/tmp/cursor")
                .targetFileExtensions(Arrays.asList(".txt", ".md"))
                .targetDirectories(Arrays.asList("/dir1", "/dir2"))
                .syncTargetDir("review")
                .build();

        // When
        DropboxClient client = new DropboxClient(config, cursorService);

        // Then
        assertNotNull(client);
    }

    /**
     * 注意: 実際のDropbox APIを呼び出すメソッド（getTargetDirectories、getTargetFiles等）の
     * テストは、実際のDropbox APIとの統合テストとして別途実装する必要があります。
     * モックを使用した完全なテストは、DbxClientV2がfinalクラスであるため困難です。
     */

    private AppConfig createTestAppConfig() {
        return new AppConfig.Builder()
                .dropboxRefreshToken("")
                .dropboxClientId("")
                .dropboxClientSecret("")
                .dropboxAccessToken("test_access_token")
                .githubPat("github_pat")
                .githubUsername("testuser")
                .githubRemoteUrl("https://github.com/test/repo.git")
                .localRepoPath("/tmp/repo")
                .cursorFilePath("/tmp/cursor")
                .targetFileExtensions(Arrays.asList(".txt", ".md"))
                .targetDirectories(Arrays.asList("/dir1", "/dir2"))
                .syncTargetDir("review")
                .build();
    }
}

