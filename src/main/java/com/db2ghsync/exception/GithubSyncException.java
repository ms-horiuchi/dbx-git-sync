package com.db2ghsync.exception;

public class GithubSyncException extends Exception {

    // デフォルトコンストラクタ
    public GithubSyncException() {
        super();
    }

    // メッセージのみ
    public GithubSyncException(String message) {
        super(message);
    }

    // メッセージ + 原因例外
    public GithubSyncException(String message, Throwable cause) {
        super(message, cause);
    }

    // 原因例外のみ
    public GithubSyncException(Throwable cause) {
        super(cause);
    }

    // メッセージ + 原因例外 + 抑圧された例外・スタックトレース
    protected GithubSyncException(String message, Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
