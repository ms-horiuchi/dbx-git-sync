package com.db2ghsync.common;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 設定ファイル(config.properties)の読み込み・管理を行うユーティリティクラス。
 * 設定値の取得・必須項目の検証などを提供する。
 */
public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    /**
     * 設定情報を保持するマップ。<br>
     * キーはConfigKey、値は設定値文字列。
     */
    private static Map<ConfigKey, String> propMap;

    /**
     * 設定ファイルを読み込み、設定値を保持するMapを生成する。
     * 
     * @param configFilePath 設定ファイルのパス
     * @throws RuntimeException         ファイル読み込み失敗時
     * @throws IllegalArgumentException 必須設定項目不足時
     */
    public static void loadConfig(String configFilePath) {

        logger.info("Loading configuration from file: {}", configFilePath);
        Properties confProperties = new Properties();

        try (InputStream inputStream = new FileInputStream(configFilePath);
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            confProperties.load(reader);
            logger.debug("Configuration file read successfully");
        } catch (Exception e) {
            logger.error("Failed to read config file: {}", configFilePath, e);
            throw new RuntimeException("Failed to read config file: " + configFilePath, e);
        }

        ConfigManager.propMap = Collections.unmodifiableMap(asMap(confProperties));
        logger.info("Configuration loaded successfully with {} properties", propMap.size());
    }

    /**
     * 設定値を取得する。事前にloadConfig()を呼び出しておくこと。
     * 
     * @param configKey 取得したい設定項目
     * @return 設定値文字列
     * @throws IllegalStateException 設定が未ロードの場合
     */
    public static String getProperty(ConfigKey configKey) {
        ensureLoaded();
        return propMap.get(configKey);
    }

    /**
     * 設定ファイルが読み込まれていることを保証するメソッド。
     * 未ロードの場合は例外をスローする。
     */
    private static void ensureLoaded() {
        if (Objects.isNull(propMap)) {
            throw new IllegalStateException("Config not loaded. Call loadConfig() first.");
        }
    }

    /**
     * 設定ファイル情報(Properties)をConfigKey→値のマップに変換する。
     * 
     * @param confProperties Propertiesオブジェクト
     * @return 設定値マップ
     * @throws IllegalArgumentException 必須項目不足時
     */
    private static Map<ConfigKey, String> asMap(Properties confProperties) {

        Map<ConfigKey, String> map = new HashMap<>();
        map.put(ConfigKey.DROPBOX_ACCESS_TOKEN,
                getRequiredProperty(confProperties, ConfigKey.DROPBOX_ACCESS_TOKEN.getKey()));
        map.put(ConfigKey.GITHUB_PAT, getRequiredProperty(confProperties, ConfigKey.GITHUB_PAT.getKey()));
        map.put(ConfigKey.GITHUB_USERNAME, getRequiredProperty(confProperties, ConfigKey.GITHUB_USERNAME.getKey()));
        map.put(ConfigKey.GITHUB_REMOTE_URL, getRequiredProperty(confProperties, ConfigKey.GITHUB_REMOTE_URL.getKey()));
        map.put(ConfigKey.LOCAL_REPO_PATH, getRequiredProperty(confProperties, ConfigKey.LOCAL_REPO_PATH.getKey()));
        map.put(ConfigKey.CURSOR_FILE_PATH, getRequiredProperty(confProperties, ConfigKey.CURSOR_FILE_PATH.getKey()));
        map.put(ConfigKey.TARGET_DIRECTORIES,
                getRequiredProperty(confProperties, ConfigKey.TARGET_DIRECTORIES.getKey()));
        map.put(ConfigKey.TARGET_FILE_EXTENSIONS,
                getRequiredProperty(confProperties, ConfigKey.TARGET_FILE_EXTENSIONS.getKey()));

        return map;
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

        if ("".equals(val)) {
            throw new IllegalArgumentException("Required property is missing: " + key);
        }

        return val;
    }
}