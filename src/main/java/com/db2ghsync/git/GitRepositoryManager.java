package com.db2ghsync.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.db2ghsync.common.AppConfig;
import com.db2ghsync.exception.GithubSyncException;

/**
 * Gitリポジトリのクローン・ブランチ操作・コミット・プッシュなどを管理するクラス。
 * Dropbox連携バッチのGit操作を一括で提供する。
 */
public class GitRepositoryManager implements GitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitRepositoryManager.class);

    private static final String MAIN_BRANCH = "main";

    private final File localRepoDir;
    private final String githubUsername;
    private final String githubPat;
    private final String githubRemoteUrl;

    /**
     * コンストラクタ。依存関係を注入してGitRepositoryManagerを初期化する。
     * 
     * @param config アプリケーション設定
     */
    public GitRepositoryManager(AppConfig config) {
        Objects.requireNonNull(config, "AppConfig must not be null");
        this.localRepoDir = new File(config.getLocalRepoPath());
        this.githubUsername = config.getGithubUsername();
        this.githubPat = config.getGithubPat();
        this.githubRemoteUrl = config.getGithubRemoteUrl();
    }

    /**
     * ローカルリポジトリが存在しない場合はGitHubからクローンし、存在する場合は何もしない。
     * 
     * @throws GithubSyncException クローン失敗時
     */
    @Override
    public void cloneOrOpenRepository() throws GithubSyncException {

        // リポジトリのルートパスとgitディレクトリを取得
        File localGitDir = new File(localRepoDir, ".git");
        String localRepoPath = localRepoDir.getAbsolutePath();

        try {
            if (RepositoryCache.FileKey.isGitRepository(localGitDir, FS.DETECTED)) {
                LOGGER.info("Git repository already exists at: {}", localRepoPath);
                // すでにリポジトリがある場合は終了
            } else {
                LOGGER.info("Cloning Git repository from: {}", githubRemoteUrl);
                // ローカルにGitリポジトリをクローン
                Git.cloneRepository()
                        .setURI(githubRemoteUrl)
                        .setDirectory(localRepoDir)
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubUsername, githubPat))
                        .call();
                LOGGER.info("Git repository cloned successfully to: {}", localRepoPath);
            }
        } catch (GitAPIException e) {
            LOGGER.error("Failed to clone repository. Directory: {}, Repository: {}",
                    localRepoPath, githubRemoteUrl, e);
            throw new GithubSyncException("Cloning repository failed.", e);
        }
    }

    /**
     * 指定ブランチにチェックアウトする。存在しない場合は新規作成。
     * 
     * @param branchName チェックアウトするブランチ名
     * @throws GithubSyncException Git操作失敗時
     */
    @Override
    public void checkoutBranch(String branchName) throws GithubSyncException {

        // 先頭のスラッシュをトリムする一行処理
        branchName = branchName.replaceFirst("^/", "");

        LOGGER.debug("Checking out branch failed.: {}", branchName);

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
                LOGGER.debug("Branch '{}' exists. Checking out.", branchName);
                // ブランチがすでにある場合、切り替えるだけ
                // pullはpullLatestChanges()で実行されるため、ここでは不要
                checkoutCmd.call();
            } else {
                LOGGER.debug("Branch '{}' does not exist. Creating new branch.", branchName);
                // 新規ブランチの場合、新規としてチェックアウト
                checkoutCmd.setCreateBranch(true);
                checkoutCmd.call();
            }

            LOGGER.info("Checked out branch: {}", branchName);

        } catch (IOException | GitAPIException e) {
            LOGGER.error("Failed to checkout branch: {}", branchName, e);
            throw new GithubSyncException("Checking out branch failed.", e);
        }
    }

    /**
     * ワークツリー内の全ファイルをaddし、コミットする。
     * 
     * @throws GithubSyncException Git操作失敗時
     */
    @Override
    public void addAndCommit() throws GithubSyncException {

        LOGGER.debug("Adding and Committing files");

        try (Git git = Git.open(localRepoDir)) {
            Path repoPath = localRepoDir.toPath();
            try (Stream<Path> stream = Files.walk(repoPath)) {
                for (Path path : stream.filter(Files::isRegularFile).collect(Collectors.toList())) {
                    String relative = repoPath.relativize(path).toString().replace("\\", "/");
                    git.add().addFilepattern(relative).call();
                }
            }
            git.commit().setMessage("Commit.").call();
            LOGGER.info("Files added and committed successfully");

        } catch (IOException | GitAPIException e) {
            LOGGER.error("Failed to add and commit files", e);
            throw new GithubSyncException("Adding or Committing failed.", e);
        }
    }

    /**
     * コミット済み内容をGitHubリモートリポジトリへプッシュする。
     * 
     * @throws GithubSyncException Git操作失敗時
     */
    @Override
    public void push() throws GithubSyncException {

        LOGGER.debug("Pushing changes to remote repository");

        try (Git git = Git.open(localRepoDir)) {
            git.push().setCredentialsProvider(buildCredentialsProvider()).call();
            LOGGER.info("Changes pushed successfully to remote repository");
        } catch (IOException | GitAPIException e) {
            LOGGER.error("Failed to push changes to remote repository", e);
            throw new GithubSyncException("Pushing to remote repository failed. ", e);
        }
    }

    @Override
    public List<String> listLocalBranches() throws GithubSyncException {
        LOGGER.debug("Listing local branches");
        try (Git git = Git.open(localRepoDir)) {
            List<Ref> branches = git.branchList().call();
            List<String> result = new ArrayList<>(branches.size());
            for (Ref branch : branches) {
                result.add(Repository.shortenRefName(branch.getName()));
            }
            return result;
        } catch (IOException | GitAPIException e) {
            LOGGER.error("Failed to list local branches", e);
            throw new GithubSyncException("Listing local branches failed.", e);
        }
    }

    @Override
    public Set<String> pullLatestChanges() throws GithubSyncException {
        LOGGER.info("Pulling latest changes on current branch");
        try (Git git = Git.open(localRepoDir)) {
            ObjectId oldHead = git.getRepository().resolve("HEAD");
            LOGGER.info("Old HEAD: {}", oldHead != null ? oldHead.getName() : "null");
            
            PullResult pullResult;
            CredentialsProvider provider = buildCredentialsProvider();
            if (provider != null) {
                pullResult = git.pull().setCredentialsProvider(provider).call();
            } else {
                pullResult = git.pull().call();
            }

            LOGGER.info("Pull result - successful: {}, fetched from: {}", 
                    pullResult.isSuccessful(), 
                    pullResult.getFetchedFrom());
            
            if (!pullResult.isSuccessful()) {
                LOGGER.warn("Pull failed for branch: {}", git.getRepository().getBranch());
                return new HashSet<>();
            }

            ObjectId newHead = git.getRepository().resolve("HEAD");
            LOGGER.info("New HEAD: {}", newHead != null ? newHead.getName() : "null");
            
            if (oldHead != null && oldHead.equals(newHead)) {
                LOGGER.info("HEAD unchanged - no new commits detected");
                return new HashSet<>();
            }

            Set<String> changedFiles = getChangedFilesBetweenCommits(git, oldHead, newHead);
            LOGGER.info("Detected {} changed files between commits", changedFiles.size());
            if (!changedFiles.isEmpty()) {
                LOGGER.info("Changed files: {}", changedFiles);
            }
            
            return changedFiles;
        } catch (IOException | GitAPIException e) {
            LOGGER.error("Failed to pull latest changes", e);
            throw new GithubSyncException("Pulling latest changes failed.", e);
        }
    }

    @Override
    public void close() {
        // Git instances are opened per operation; nothing to close.
    }

    private CredentialsProvider buildCredentialsProvider() {
        if (githubUsername == null || githubUsername.isBlank()
                || githubPat == null || githubPat.isBlank()) {
            return null;
        }
        return new UsernamePasswordCredentialsProvider(githubUsername, githubPat);
    }

    private Set<String> getChangedFilesBetweenCommits(Git git, ObjectId oldCommitId, ObjectId newCommitId)
            throws IOException, GitAPIException {
        LOGGER.info("Computing diff between commits: {} -> {}", 
                oldCommitId != null ? oldCommitId.abbreviate(7).name() : "null",
                newCommitId != null ? newCommitId.abbreviate(7).name() : "null");
        
        Set<String> changedFiles = new HashSet<>();

        try (RevWalk revWalk = new RevWalk(git.getRepository())) {
            RevCommit oldCommit = oldCommitId != null ? revWalk.parseCommit(oldCommitId) : null;
            RevCommit newCommit = newCommitId != null ? revWalk.parseCommit(newCommitId) : null;

            AbstractTreeIterator oldTreeIterator = (oldCommit != null)
                    ? prepareTreeParser(git, oldCommit)
                    : new EmptyTreeIterator();
            AbstractTreeIterator newTreeIterator = prepareTreeParser(git, newCommit);

            List<DiffEntry> diffs = git.diff()
                    .setOldTree(oldTreeIterator)
                    .setNewTree(newTreeIterator)
                    .call();

            LOGGER.info("Git diff returned {} entries", diffs.size());
            
            for (DiffEntry diff : diffs) {
                LOGGER.debug("DiffEntry: type={}, oldPath={}, newPath={}", 
                        diff.getChangeType(), diff.getOldPath(), diff.getNewPath());
                
                if (diff.getOldPath() != null && !"/dev/null".equals(diff.getOldPath())) {
                    changedFiles.add(diff.getOldPath());
                }
                if (diff.getNewPath() != null && !"/dev/null".equals(diff.getNewPath())) {
                    changedFiles.add(diff.getNewPath());
                }
            }
        }

        LOGGER.info("Total unique changed files: {}", changedFiles.size());
        return changedFiles;
    }

    private AbstractTreeIterator prepareTreeParser(Git git, RevCommit commit) throws IOException {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            treeParser.reset(git.getRepository().newObjectReader(), commit.getTree());
            walk.dispose();
            return treeParser;
        }
    }
}
