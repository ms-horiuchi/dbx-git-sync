package com.db2ghsync.common;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * アプリケーション設定を保持する不変オブジェクト。
 * すべての設定値を型安全に管理する。
 */
public final class AppConfig {

    private final String dropboxRefreshToken;
    private final String dropboxClientId;
    private final String dropboxClientSecret;
    private final String dropboxAccessToken;
    private final String githubPat;
    private final String githubUsername;
    private final String githubRemoteUrl;
    private final String localRepoPath;
    private final String cursorFilePath;
    private final List<String> targetFileExtensions;
    private final List<String> targetDirectories;
    private final String syncTargetDir;

    /**
     * プライベートコンストラクタ。Builder経由でのみインスタンス化可能。
     */
    private AppConfig(Builder builder) {
        this.dropboxRefreshToken = builder.dropboxRefreshToken;
        this.dropboxClientId = builder.dropboxClientId;
        this.dropboxClientSecret = builder.dropboxClientSecret;
        this.dropboxAccessToken = builder.dropboxAccessToken;
        this.githubPat = Objects.requireNonNull(builder.githubPat, "github.pat must not be null");
        this.githubUsername = Objects.requireNonNull(builder.githubUsername, "github.username must not be null");
        this.githubRemoteUrl = Objects.requireNonNull(builder.githubRemoteUrl, "github.remote.url must not be null");
        this.localRepoPath = Objects.requireNonNull(builder.localRepoPath, "local.repo.path must not be null");
        this.cursorFilePath = Objects.requireNonNull(builder.cursorFilePath, "cursor.file.path must not be null");
        this.targetFileExtensions = Collections.unmodifiableList(
                Objects.requireNonNull(builder.targetFileExtensions, "target.file.extensions must not be null"));
        this.targetDirectories = Collections.unmodifiableList(
                Objects.requireNonNull(builder.targetDirectories, "target.directories must not be null"));
        this.syncTargetDir = Objects.requireNonNull(builder.syncTargetDir, "sync.target.dir must not be null");
    }

    public String getDropboxRefreshToken() {
        return dropboxRefreshToken;
    }

    public String getDropboxClientId() {
        return dropboxClientId;
    }

    public String getDropboxClientSecret() {
        return dropboxClientSecret;
    }

    public String getDropboxAccessToken() {
        return dropboxAccessToken;
    }

    public String getGithubPat() {
        return githubPat;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public String getGithubRemoteUrl() {
        return githubRemoteUrl;
    }

    public String getLocalRepoPath() {
        return localRepoPath;
    }

    public String getCursorFilePath() {
        return cursorFilePath;
    }

    public List<String> getTargetFileExtensions() {
        return new java.util.ArrayList<>(targetFileExtensions);
    }

    public List<String> getTargetDirectories() {
        return Collections.unmodifiableList(List.copyOf(targetDirectories));
    }

    public String getSyncTargetDir() {
        return syncTargetDir;
    }

    /**
     * AppConfigのBuilderクラス。
     */
    public static class Builder {
        private String dropboxRefreshToken = "";
        private String dropboxClientId = "";
        private String dropboxClientSecret = "";
        private String dropboxAccessToken = "";
        private String githubPat;
        private String githubUsername;
        private String githubRemoteUrl;
        private String localRepoPath;
        private String cursorFilePath;
        private List<String> targetFileExtensions;
        private List<String> targetDirectories;
        private String syncTargetDir;

        public Builder dropboxRefreshToken(String dropboxRefreshToken) {
            this.dropboxRefreshToken = dropboxRefreshToken != null ? dropboxRefreshToken : "";
            return this;
        }

        public Builder dropboxClientId(String dropboxClientId) {
            this.dropboxClientId = dropboxClientId != null ? dropboxClientId : "";
            return this;
        }

        public Builder dropboxClientSecret(String dropboxClientSecret) {
            this.dropboxClientSecret = dropboxClientSecret != null ? dropboxClientSecret : "";
            return this;
        }

        public Builder dropboxAccessToken(String dropboxAccessToken) {
            this.dropboxAccessToken = dropboxAccessToken != null ? dropboxAccessToken : "";
            return this;
        }

        public Builder githubPat(String githubPat) {
            this.githubPat = githubPat;
            return this;
        }

        public Builder githubUsername(String githubUsername) {
            this.githubUsername = githubUsername;
            return this;
        }

        public Builder githubRemoteUrl(String githubRemoteUrl) {
            this.githubRemoteUrl = githubRemoteUrl;
            return this;
        }

        public Builder localRepoPath(String localRepoPath) {
            this.localRepoPath = localRepoPath;
            return this;
        }

        public Builder cursorFilePath(String cursorFilePath) {
            this.cursorFilePath = cursorFilePath;
            return this;
        }

        public Builder targetFileExtensions(List<String> targetFileExtensions) {
            this.targetFileExtensions = targetFileExtensions;
            return this;
        }

        public Builder targetDirectories(List<String> targetDirectories) {
            this.targetDirectories = targetDirectories;
            return this;
        }

        public Builder syncTargetDir(String syncTargetDir) {
            this.syncTargetDir = syncTargetDir;
            return this;
        }

        public AppConfig build() {
            return new AppConfig(this);
        }
    }
}

