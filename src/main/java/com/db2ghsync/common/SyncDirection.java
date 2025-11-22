package com.db2ghsync.common;

import java.util.Locale;

/**
 * 同期方向を表す列挙体。CLI引数で指定された値を正規化して扱う。
 */
public enum SyncDirection {

    DBX_TO_GIT("dbx-to-git"),
    GIT_TO_DBX("git-to-dbx");

    private final String cliValue;

    SyncDirection(String cliValue) {
        this.cliValue = cliValue;
    }

    public String getCliValue() {
        return cliValue;
    }

    /**
     * CLI引数の文字列表現をSyncDirectionに変換する。
     *
     * @param value CLIで指定された同期方向
     * @return SyncDirection
     */
    public static SyncDirection fromArgument(String value) {
        if (value == null) {
            throw new IllegalArgumentException("direction argument must not be null.");
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        for (SyncDirection direction : values()) {
            if (direction.cliValue.equals(normalized)) {
                return direction;
            }
        }

        throw new IllegalArgumentException("Unknown direction argument: " + value);
    }
}

