package com.db2ghsync.git;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.db2ghsync.common.AppConfig;
import com.db2ghsync.exception.GithubSyncException;

/**
 * GitRepositoryManagerクラスのテスト。
 * モックを使用してGit操作を検証する。
 * 
 * 注意: GitRepositoryManagerは実際のJGitを使用しているため、
 * 完全なモックテストは困難です。基本的なコンストラクタテストと
 * nullチェックを中心に実装します。
 * 実際のGit操作のテストは統合テストとして別途実装する必要があります。
 */
class GitRepositoryManagerTest {

    @Test
    void testConstructorWithNullConfigThrowsException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new GitRepositoryManager(null);
        });
    }

    @Test
    void testConstructorWithValidConfigCreatesManager() {
        // Given
        AppConfig config = createTestAppConfig();

        // When
        GitRepositoryManager manager = new GitRepositoryManager(config);

        // Then
        assertNotNull(manager);
    }

    /**
     * 注意: 実際のGit操作を呼び出すメソッド（cloneOrOpenRepository、checkoutBranch等）の
     * テストは、実際のGitリポジトリとの統合テストとして別途実装する必要があります。
     * JGitのモック化は複雑なため、統合テストでカバーすることを推奨します。
     * 
     * 統合テストでは以下のケースをテストする必要があります:
     * - cloneOrOpenRepository()が既存リポジトリを検出すること
     * - cloneOrOpenRepository()が新規リポジトリをクローンすること
     * - checkoutBranch()が既存ブランチにチェックアウトすること
     * - checkoutBranch()が新規ブランチを作成すること
     * - addAndCommit()がファイルを追加・コミットすること
     * - push()がリモートにプッシュすること
     * - 例外が適切にGithubSyncExceptionに変換されること
     */

    private AppConfig createTestAppConfig() {
        return new AppConfig.Builder()
                .githubPat("test_github_pat")
                .githubUsername("testuser")
                .githubRemoteUrl("https://github.com/testuser/testrepo.git")
                .localRepoPath("/tmp/testrepo")
                .cursorFilePath("/tmp/cursor")
                .targetFileExtensions(Arrays.asList(".txt", ".md"))
                .targetDirectories(Arrays.asList("/dir1", "/dir2"))
                .syncTargetDir("review")
                .build();
    }
}

