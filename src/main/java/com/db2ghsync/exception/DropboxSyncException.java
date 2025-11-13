package com.db2ghsync.exception;

public class DropboxSyncException extends Exception {

    // デフォルトコンストラクタ
    public DropboxSyncException() {
        super();
    }

    // メッセージのみ
    public DropboxSyncException(String message) {
        super(message);
    }

    // メッセージ + 原因例外
    public DropboxSyncException(String message, Throwable cause) {
        super(message, cause);
    }

    // 原因例外のみ
    public DropboxSyncException(Throwable cause) {
        super(cause);
    }

    // メッセージ + 原因例外 + 抑圧された例外・スタックトレース
    protected DropboxSyncException(String message, Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
