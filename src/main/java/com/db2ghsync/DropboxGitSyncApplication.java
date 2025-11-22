package com.db2ghsync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.db2ghsync.app.DropboxToGitWorkflow;
import com.db2ghsync.app.GitToDropboxProcessor;
import com.db2ghsync.app.GitToDropboxWorkflow;
import com.db2ghsync.app.SyncProcessor;
import com.db2ghsync.app.SyncWorkflow;
import com.db2ghsync.common.AppConfig;
import com.db2ghsync.common.ConfigManager;
import com.db2ghsync.common.SyncDirection;
import com.db2ghsync.dropbox.CursorManager;
import com.db2ghsync.dropbox.CursorService;
import com.db2ghsync.dropbox.DropboxClient;
import com.db2ghsync.dropbox.DropboxService;
import com.db2ghsync.exception.DropboxSyncException;
import com.db2ghsync.exception.GithubSyncException;
import com.db2ghsync.git.GitRepositoryManager;
import com.db2ghsync.git.GitService;

import java.io.IOException;

/**
 * DropboxとGitHub間の同期バッチアプリケーションのエントリポイント。
 * 設定ファイルを読み込み、依存関係を構築して同期処理を開始する。
 */
public class DropboxGitSyncApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(DropboxGitSyncApplication.class);

    private static final int EXIT_CODE_SUCCESS = 0;
    private static final int EXIT_CODE_ERROR = 1;

    private static final String ARG_CONFIG = "--config";
    private static final String ARG_DIRECTION = "--direction";

    /**
     * アプリケーションのメインメソッド。
     * 
     * @param args コマンドライン引数（設定ファイルパスを1件指定）
     */
    public static void main(String[] args) {

        int exitCode = EXIT_CODE_ERROR;

        try {
            LOGGER.info("Dropbox-GitHub Sync Application started");

            CommandLineOptions options = CommandLineOptions.parse(args);

            LOGGER.info("Loading configuration from: {}", options.configPath());
            ConfigManager.loadConfig(options.configPath());
            LOGGER.info("Configuration loaded successfully");

            // 依存関係の構築
            AppConfig config = ConfigManager.getAppConfig();
            SyncDirection direction = options.direction();
            LOGGER.info("Selected sync direction: {}", direction);

            SyncWorkflow workflow = createWorkflow(direction, config);
            workflow.execute();

            LOGGER.info("Dropbox-GitHub Sync Application completed");
            exitCode = EXIT_CODE_SUCCESS;

        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid argument: {}", e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.error("Configuration loading failed", e);
        } catch (DropboxSyncException e) {
            LOGGER.error("Dropbox synchronization failed", e);
        } catch (GithubSyncException e) {
            LOGGER.error("GitHub synchronization failed", e);
        } catch (IOException e) {
            LOGGER.error("I/O operation failed", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error occurred", e);
        } finally {
            System.exit(exitCode);
        }
    }

    private static SyncWorkflow createWorkflow(SyncDirection direction, AppConfig config) {
        CursorService cursorService = new CursorManager(config);
        DropboxService dropboxService = new DropboxClient(config, cursorService);
        GitService gitService = new GitRepositoryManager(config);

        if (direction == SyncDirection.DBX_TO_GIT) {
            SyncProcessor processor = new SyncProcessor(dropboxService, gitService, cursorService);
            return new DropboxToGitWorkflow(processor);
        }

        GitToDropboxProcessor processor = new GitToDropboxProcessor(config, gitService, dropboxService);
        return new GitToDropboxWorkflow(processor);
    }

    /**
     * CLIオプションを保持するヘルパークラス。テストから参照できるようpackage-private。
     */
    static final class CommandLineOptions {

        private final String configPath;
        private final SyncDirection direction;

        private CommandLineOptions(String configPath, SyncDirection direction) {
            this.configPath = configPath;
            this.direction = direction;
        }

        String configPath() {
            return configPath;
        }

        SyncDirection direction() {
            return direction;
        }

        static CommandLineOptions parse(String[] args) {
            if (args == null || args.length == 0) {
                throw new IllegalArgumentException("arguments are not provided.");
            }

            String configPath = null;
            String directionValue = null;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case ARG_CONFIG -> {
                        ensureValueAvailable(args, i, ARG_CONFIG);
                        configPath = args[++i];
                    }
                    case ARG_DIRECTION -> {
                        ensureValueAvailable(args, i, ARG_DIRECTION);
                        directionValue = args[++i];
                    }
                    default -> throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            if (configPath == null) {
                throw new IllegalArgumentException("--config <path> is required.");
            }
            if (directionValue == null) {
                throw new IllegalArgumentException("--direction <dbx-to-git|git-to-dbx> is required.");
            }

            return new CommandLineOptions(configPath, SyncDirection.fromArgument(directionValue));
        }

        private static void ensureValueAvailable(String[] args, int index, String option) {
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException(option + " requires a value.");
            }
        }
    }
}
