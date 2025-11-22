package com.db2ghsync.git;

import java.util.List;
import java.util.Set;

import com.db2ghsync.exception.GithubSyncException;

/**
 * Gitリポジトリ操作を行うサービスのインターフェース。
 * クローン・ブランチ操作・コミット・プッシュなどを提供する。
 */
public interface GitService {

    /**
     * ローカルリポジトリが存在しない場合はGitHubからクローンし、存在する場合は何もしない。
     * 
     * @throws GithubSyncException クローン失敗時
     */
    void cloneOrOpenRepository() throws GithubSyncException;

    /**
     * 指定ブランチにチェックアウトする。存在しない場合は新規作成。
     * 
     * @param branchName チェックアウトするブランチ名
     * @throws GithubSyncException Git操作失敗時
     */
    void checkoutBranch(String branchName) throws GithubSyncException;

    /**
     * ワークツリー内の全ファイルをaddし、コミットする。
     * 
     * @throws GithubSyncException Git操作失敗時
     */
    void addAndCommit() throws GithubSyncException;

    /**
     * コミット済み内容をGitHubリモートリポジトリへプッシュする。
     * 
     * @throws GithubSyncException Git操作失敗時
     */
    void push() throws GithubSyncException;

    /**
     * ローカルに存在するブランチ一覧を取得する。
     *
     * @return ブランチ名のリスト
     * @throws GithubSyncException Git操作失敗時
     */
    List<String> listLocalBranches() throws GithubSyncException;

    /**
     * 現在のブランチを最新化し、更新されたファイルパスのセットを返す。
     *
     * @return 更新ファイルの相対パス集合
     * @throws GithubSyncException Git操作失敗時
     */
    Set<String> pullLatestChanges() throws GithubSyncException;

    /**
     * 使用済みリソースを解放する。
     */
    void close();
}

