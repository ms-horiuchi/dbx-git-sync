package com.db2ghsync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.db2ghsync.app.SyncProcessor;
import com.db2ghsync.common.ConfigManager;

/**
 * DropboxとGitHub間の同期バッチアプリケーションのエントリポイント。
 * 設定ファイルを読み込み、同期処理を開始する。
 */
public class DropboxGitSyncApplication {

    private static final Logger logger = LoggerFactory.getLogger(DropboxGitSyncApplication.class);

    /**
     * アプリケーションのメインメソッド。
     * 
     * @param args コマンドライン引数（設定ファイルパスを1件指定）
     * @throws Exception 各種処理失敗時
     */
    public static void main(String[] args) {

        try {
            logger.info("Dropbox-GitHub Sync Application started");

            if (args.length != 1) {
                logger.error("Invalid arguments. Expected 1 argument (config file path), got {}", args.length);
                throw new IllegalArgumentException("args are not correct.");
            }

            logger.info("Loading configuration from: {}", args[0]);
            ConfigManager.loadConfig(args[0]);
            logger.info("Configuration loaded successfully");

            SyncProcessor processor = new SyncProcessor();
            processor.start();

            logger.info("Dropbox-GitHub Sync Application completed");

        } catch (Exception e) {
            logger.error("Application failed", e);
            System.exit(1);
        }
    }
}
