package com.db2ghsync.common;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 設定ファイル(config.properties)の読み込み・管理を行うユーティリティクラス。
 * 設定値の取得・必須項目の検証などを提供する。
 */
public class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);

    /**
     * キャッシュされたAppConfigオブジェクト。
     */
    private static AppConfig appConfig;

    /**
     * 設定ファイルを読み込み、AppConfigを生成してキャッシュする。
     * 
     * @param configFilePath 設定ファイルのパス
     * @throws RuntimeException         ファイル読み込み失敗時
     * @throws IllegalArgumentException 必須設定項目不足時
     */
    public static void loadConfig(String configFilePath) {

        LOGGER.info("Loading configuration from file: {}", configFilePath);
        Properties confProperties = new Properties();

        try (InputStream inputStream = new FileInputStream(configFilePath);
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            confProperties.load(reader);
            LOGGER.debug("Configuration file read successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to read config file: {}", configFilePath, e);
            throw new RuntimeException("Failed to read config file: " + configFilePath, e);
        }

        ConfigManager.appConfig = buildAppConfig(confProperties);
        LOGGER.info("Configuration loaded successfully");
    }

    /**
     * Dropboxリフレッシュトークンを取得する。
     * 
     * @return Dropboxリフレッシュトークン
     */
    public static String getDropboxRefreshToken() {
        return getAppConfig().getDropboxRefreshToken();
    }

    /**
     * DropboxクライアントIDを取得する。
     * 
     * @return DropboxクライアントID
     */
    public static String getDropboxClientId() {
        return getAppConfig().getDropboxClientId();
    }

    /**
     * Dropboxクライアントシークレットを取得する。
     * 
     * @return Dropboxクライアントシークレット
     */
    public static String getDropboxClientSecret() {
        return getAppConfig().getDropboxClientSecret();
    }

    /**
     * Dropboxアクセストークンを取得する。
     * 
     * @return Dropboxアクセストークン
     */
    public static String getDropboxAccessToken() {
        return getAppConfig().getDropboxAccessToken();
    }

    /**
     * GitHub Personal Access Tokenを取得する。
     * 
     * @return GitHub PAT
     */
    public static String getGithubPat() {
        return getAppConfig().getGithubPat();
    }

    /**
     * GitHubユーザー名を取得する。
     * 
     * @return GitHubユーザー名
     */
    public static String getGithubUsername() {
        return getAppConfig().getGithubUsername();
    }

    /**
     * GitHubリモートリポジトリURLを取得する。
     * 
     * @return GitHubリモートリポジトリURL
     */
    public static String getGithubRemoteUrl() {
        return getAppConfig().getGithubRemoteUrl();
    }

    /**
     * ローカルGitリポジトリパスを取得する。
     * 
     * @return ローカルGitリポジトリパス
     */
    public static String getLocalRepoPath() {
        return getAppConfig().getLocalRepoPath();
    }

    /**
     * カーソルファイルパスを取得する。
     * 
     * @return カーソルファイルパス
     */
    public static String getCursorFilePath() {
        return getAppConfig().getCursorFilePath();
    }

    /**
     * 対象ファイル拡張子のリストを取得する。
     * 
     * @return ファイル拡張子のリスト
     */
    public static List<String> getTargetFileExtensions() {
        return getAppConfig().getTargetFileExtensions();
    }

    /**
     * 対象ディレクトリのリストを取得する。
     * 
     * @return ディレクトリのリスト
     */
    public static List<String> getTargetDirectories() {
        return getAppConfig().getTargetDirectories();
    }

    /**
     * Git->Dropbox反映対象ディレクトリを取得する。
     * 
     * @return 対象ディレクトリ
     */
    public static String getSyncTargetDir() {
        return getAppConfig().getSyncTargetDir();
    }

    /**
     * キャッシュされたAppConfigオブジェクトを取得する。
     * 
     * @return AppConfigオブジェクト
     * @throws IllegalStateException 設定が未ロードの場合
     */
    public static AppConfig getAppConfig() {
        if (Objects.isNull(appConfig)) {
            throw new IllegalStateException("Config not loaded. Call loadConfig() first.");
        }
        return appConfig;
    }

    /**
     * Propertiesオブジェクトから直接AppConfigを生成する。
     * 
     * @param props Propertiesオブジェクト
     * @return AppConfigオブジェクト
     * @throws IllegalArgumentException 必須項目不足時
     */
    private static AppConfig buildAppConfig(Properties props) {
        return new AppConfig.Builder()
                .dropboxRefreshToken(props.getProperty("dropbox.refresh.token", ""))
                .dropboxClientId(props.getProperty("dropbox.client.id", ""))
                .dropboxClientSecret(props.getProperty("dropbox.client.secret", ""))
                .dropboxAccessToken(props.getProperty("dropbox.access.token", ""))
                .githubPat(getRequiredProperty(props, "github.pat"))
                .githubUsername(getRequiredProperty(props, "github.username"))
                .githubRemoteUrl(getRequiredProperty(props, "github.remote.url"))
                .localRepoPath(getRequiredProperty(props, "local.repo.path"))
                .cursorFilePath(getRequiredProperty(props, "cursor.file.path"))
                .targetFileExtensions(Arrays.asList(
                        getRequiredProperty(props, "target.file.extensions").split(",")))
                .targetDirectories(Arrays.asList(
                        getRequiredProperty(props, "target.directories").split(",")))
                .syncTargetDir(getRequiredProperty(props, "sync.target.dir"))
                .build();
    }

    /**
     * Propertiesから必須項目を取得し、存在しない場合は例外をスローするヘルパーメソッド。
     * 
     * @param props Propertiesオブジェクト
     * @param key   取得するキー
     * @return 設定値文字列
     * @throws IllegalArgumentException キーがnull、または値が空の場合
     */
    private static String getRequiredProperty(Properties props, String key) {

        if (Objects.isNull(key)) {
            throw new IllegalArgumentException("Property key is null: " + key);
        }

        String val = props.getProperty(key);

        if (Objects.isNull(val) || "".equals(val)) {
            throw new IllegalArgumentException("Required property is missing: " + key);
        }

        return val;
    }
}