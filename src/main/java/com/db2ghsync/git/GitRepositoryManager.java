package com.db2ghsync.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.db2ghsync.common.ConfigKey;
import com.db2ghsync.common.ConfigManager;
import com.db2ghsync.exception.GithubSyncException;

/**
 * Gitリポジトリのクローン・ブランチ操作・コミット・プッシュなどを管理するクラス。
 * Dropbox連携バッチのGit操作を一括で提供する。
 */
public class GitRepositoryManager {

    private static final Logger logger = LoggerFactory.getLogger(GitRepositoryManager.class);

    private File localRepoDir;

    private String ghName = ConfigManager.getProperty(ConfigKey.GITHUB_USERNAME);
    private String ghPat = ConfigManager.getProperty(ConfigKey.GITHUB_PAT);

    /**
     * ローカルリポジトリが存在しない場合はGitHubからクローンし、存在する場合は何もしない。
     * 
     * @throws GithubSyncException クローン失敗時
     */
    public void cloneOrOpenRepository() throws GithubSyncException {

        // ローカルリポジトリのパスを取得
        String localRepoPath = ConfigManager.getProperty(ConfigKey.LOCAL_REPO_PATH);

        // リポジトリのルートパスとgitディレクトリを取得
        localRepoDir = new File(localRepoPath);
        File localGitDir = new File(localRepoPath, ".git");

        try {
            if (RepositoryCache.FileKey.isGitRepository(localGitDir, FS.DETECTED)) {
                logger.info("Git repository already exists at: {}", localRepoPath);
                // すでにリポジトリがある場合は終了
            } else {
                logger.info("Cloning Git repository from: {}", ConfigManager.getProperty(ConfigKey.GITHUB_REMOTE_URL));
                // ローカルにGitリポジトリをクローン
                Git.cloneRepository()
                        .setURI(ConfigManager.getProperty(ConfigKey.GITHUB_REMOTE_URL))
                        .setDirectory(localRepoDir)
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(ghName, ghPat))
                        .call();
                logger.info("Git repository cloned successfully to: {}", localRepoPath);
            }
        } catch (GitAPIException e) {
            logger.error("Failed to clone repository. Directory: {}, Repository: {}",
                    localRepoPath, ConfigManager.getProperty(ConfigKey.GITHUB_REMOTE_URL), e);
            throw new GithubSyncException("Cloning repository failed.", e);
        }
    }

    /**
     * 指定ブランチにチェックアウトする。存在しない場合は新規作成。
     * 
     * @param branchName チェックアウトするブランチ名
     * @throws Exception Git操作失敗時
     */
    public void checkoutBranch(String branchName) throws GithubSyncException {

        // 先頭のスラッシュをトリムする一行処理
        branchName = branchName.replaceFirst("^/", "");

        logger.debug("Checking out branch failed.: {}", branchName);

        try (Git git = Git.open(localRepoDir)) {

            List<Ref> branches = git.branchList().call();
            boolean branchExists = false;

            for (Ref branch : branches) {
                if (branch.getName().equals("refs/heads/" + branchName)) {
                    branchExists = true;
                    break;
                }
            }

            // チェックアウトコマンドの構築
            CheckoutCommand checkoutCmd = git.checkout().setName(branchName);

            if (branchExists) {
                logger.debug("Branch '{}' exists. Checking out and pulling latest changes.", branchName);
                // ブランチがすでにある場合、切り替えて最新化
                checkoutCmd.call();
                git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(ghName, ghPat)).call();
            } else {
                logger.debug("Branch '{}' does not exist. Creating new branch.", branchName);
                // 新規ブランチの場合、新規としてチェックアウト
                checkoutCmd.setCreateBranch(true);
                checkoutCmd.call();
            }

            logger.info("Checked out branch: {}", branchName);

        } catch (IOException | GitAPIException e) {
            logger.error("Failed to checkout branch: {}", branchName, e);
            throw new GithubSyncException("Checking out branch failed.", e);
        }
    }

    /**
     * ワークツリー内の全ファイルをaddし、コミットする。
     * 
     * @throws NoFilepatternException ファイルパターン不正時
     * @throws GitAPIException        Git操作失敗時
     * @throws IOException            ファイル操作失敗時
     */
    public void addAndCommit() throws GithubSyncException {

        logger.debug("Adding and Committing files");

        try (Git git = Git.open(localRepoDir)) {
            Path repoPath = localRepoDir.toPath();
            try (Stream<Path> stream = Files.walk(repoPath)) {
                for (Path path : stream.filter(Files::isRegularFile).collect(Collectors.toList())) {
                    String relative = repoPath.relativize(path).toString().replace("\\", "/");
                    git.add().addFilepattern(relative).call();
                }
            }
            git.commit().setMessage("Commit.").call();
            logger.info("Files added and committed successfully");

        } catch (IOException | GitAPIException e) {
            logger.error("Failed to add and commit files", e);
            throw new GithubSyncException("Adding or Committing failed.", e);
        }
    }

    /**
     * コミット済み内容をGitHubリモートリポジトリへプッシュする。
     * 
     * @throws IOException            ファイル操作失敗時
     * @throws InvalidRemoteException リモート不正時
     * @throws TransportException     通信失敗時
     * @throws GitAPIException        Git操作失敗時
     */
    public void push() throws GithubSyncException {

        logger.debug("Pushing changes to remote repository");

        try (Git git = Git.open(localRepoDir)) {
            git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(ghName, ghPat)).call();
            logger.info("Changes pushed successfully to remote repository");
        } catch (IOException | GitAPIException e) {
            logger.error("Failed to push changes to remote repository", e);
            throw new GithubSyncException("Pushing to remote repository failed. ", e);
        }
    }
}
