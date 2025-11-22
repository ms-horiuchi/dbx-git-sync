# デザインパターン整理資料

## 概要

このドキュメントは、dbx-git-syncプロジェクトで使用されているデザインパターンとアーキテクチャパターンを整理した資料です。

## 使用されているデザインパターン一覧

| パターン名 | カテゴリ | 使用箇所 | 目的 |
|-----------|---------|---------|------|
| Builder | 生成 | `AppConfig.Builder` | 複雑なオブジェクトの構築を簡潔に |
| 依存性注入 | 構造 | 全サービスクラス | 疎結合な設計を実現 |
| Strategy | 振る舞い | `SyncWorkflow` | 同期方向に応じた処理の切り替え |
| インターフェース分離 | 原則 | `DropboxService`, `GitService`, `CursorService` | クライアントに不要な依存を強制しない |
| 不変オブジェクト | 構造 | `AppConfig` | スレッドセーフと予測可能性を確保 |
| キャッシュ | パフォーマンス | `ConfigManager` | 繰り返しアクセスを高速化 |
| ファクトリー | 生成 | `SyncEntryFactory` | オブジェクト生成の複雑さを隠蔽 |
| アダプター | 構造 | `DropboxClient` | 外部APIを内部インターフェースに適合 |
| ユーティリティクラス | 構造 | `FilterUtils`, `ConfigManager` | 再利用可能な機能を提供 |
| 列挙型 | 構造 | `SyncAction`, `SyncDirection` | 定数の型安全な管理 |

---

## 1. Builderパターン

### 概要
複雑なオブジェクトの構築を段階的に行い、可読性と柔軟性を向上させるパターン。

### 実装箇所
- **クラス**: `AppConfig.Builder`
- **ファイル**: `src/main/java/com/db2ghsync/common/AppConfig.java`

### 実装例

```java
public final class AppConfig {
    private final String githubPat;
    private final String githubUsername;
    // ... 他のフィールド

    private AppConfig(Builder builder) {
        this.githubPat = Objects.requireNonNull(builder.githubPat, "github.pat must not be null");
        // ... 他のフィールドの初期化
    }

    public static class Builder {
        private String githubPat;
        private String githubUsername;
        // ... 他のフィールド

        public Builder githubPat(String githubPat) {
            this.githubPat = githubPat;
            return this;
        }

        public Builder githubUsername(String githubUsername) {
            this.githubUsername = githubUsername;
            return this;
        }

        public AppConfig build() {
            return new AppConfig(this);
        }
    }
}
```

### 使用例

```java
AppConfig config = new AppConfig.Builder()
    .githubPat("token")
    .githubUsername("user")
    .githubRemoteUrl("https://github.com/user/repo.git")
    .localRepoPath("/path/to/repo")
    .cursorFilePath("/path/to/cursor")
    .targetFileExtensions(Arrays.asList(".txt", ".md"))
    .targetDirectories(Arrays.asList("/dir1", "/dir2"))
    .build();
```

### メリット
- **可読性**: メソッドチェーンで意図が明確
- **柔軟性**: オプション項目の設定が容易
- **型安全性**: コンパイル時にエラーを検出
- **検証**: `build()`メソッドで必須項目の検証が可能

### 適用理由
- 多くの設定項目（11個）を持つオブジェクト
- 必須項目とオプション項目の区別が必要
- オブジェクトの不変性を保証したい

---

## 2. 依存性注入（Dependency Injection）

### 概要
オブジェクトが必要な依存関係を外部から注入することで、疎結合な設計を実現するパターン。

### 実装箇所
- **コンストラクタインジェクション**: すべてのサービスクラス
  - `SyncProcessor`
  - `GitToDropboxProcessor`
  - `DropboxToGitWorkflow`
  - `GitToDropboxWorkflow`
  - `DropboxClient`
  - `GitRepositoryManager`
  - `CursorManager`

### 実装例

```java
public class SyncProcessor {
    private final DropboxService dropboxService;
    private final GitService gitService;
    private final CursorService cursorService;

    public SyncProcessor(
            DropboxService dropboxService,
            GitService gitService,
            CursorService cursorService) {
        this.dropboxService = Objects.requireNonNull(dropboxService, "DropboxService must not be null");
        this.gitService = Objects.requireNonNull(gitService, "GitService must not be null");
        this.cursorService = Objects.requireNonNull(cursorService, "CursorService must not be null");
    }
}
```

### 依存関係の構築（アプリケーションエントリーポイント）

```java
// DropboxGitSyncApplication.java
AppConfig config = ConfigManager.getAppConfig();
CursorService cursorService = new CursorManager(config);
DropboxService dropboxService = new DropboxClient(config, cursorService);
GitService gitService = new GitRepositoryManager(config);

SyncProcessor processor = new SyncProcessor(
    dropboxService,
    gitService,
    cursorService
);
```

### メリット
- **疎結合**: クラス間の依存関係が明確
- **テスタビリティ**: モックオブジェクトを注入可能
- **柔軟性**: 実装を容易に差し替え可能
- **単一責任**: 各クラスが明確な責任を持つ

### 適用理由
- テスト容易性の向上
- 将来の実装変更への対応
- コードの可読性と保守性の向上

---

## 3. インターフェース分離原則（Interface Segregation Principle）

### 概要
クライアントに不要なメソッドへの依存を強制しないよう、インターフェースを小さく分離する原則。

### 実装箇所
- **`DropboxService`**: Dropbox操作のインターフェース
- **`GitService`**: Git操作のインターフェース
- **`CursorService`**: カーソル管理のインターフェース

### 実装例

```java
public interface DropboxService {
    List<String> getTargetDirectories() throws DropboxSyncException;
    List<SyncEntry> getChangesWithCursor(String targetDir, String cursor) throws DropboxSyncException;
    List<SyncEntry> getTargetFiles(String targetDir) throws DropboxSyncException;
    void downloadFiles(List<SyncEntry> syncEntries) throws DropboxSyncException;
}

public interface GitService {
    void cloneOrOpenRepository() throws GithubSyncException;
    void checkoutBranch(String branchName) throws GithubSyncException;
    void addAndCommit() throws GithubSyncException;
    void push() throws GithubSyncException;
}

public interface CursorService {
    String readCursor(String branchName);
    void writeTmpCursor(String branchName, String currentCursor) throws DropboxSyncException;
    void writeCursor(String branchName) throws DropboxSyncException;
}
```

### メリット
- **明確な契約**: 各インターフェースが明確な責任を持つ
- **実装の柔軟性**: インターフェースごとに異なる実装が可能
- **テスト容易性**: インターフェース単位でモック化可能
- **拡張性**: 新しい実装を追加しやすい

### 適用理由
- 各サービスの責任を明確に分離
- 将来の機能拡張に対応
- テストコードの簡潔化

---

## 4. 不変オブジェクトパターン（Immutable Object）

### 概要
オブジェクトの状態を変更できないようにすることで、スレッドセーフティと予測可能性を確保するパターン。

### 実装箇所
- **クラス**: `AppConfig`
- **ファイル**: `src/main/java/com/db2ghsync/common/AppConfig.java`

### 実装例

```java
public final class AppConfig {
    // すべてのフィールドをfinalで宣言
    private final String githubPat;
    private final String githubUsername;
    private final List<String> targetFileExtensions;
    private final List<String> targetDirectories;

    private AppConfig(Builder builder) {
        this.githubPat = Objects.requireNonNull(builder.githubPat, "github.pat must not be null");
        // コレクションを不変リストに変換
        this.targetFileExtensions = Collections.unmodifiableList(
            Objects.requireNonNull(builder.targetFileExtensions, "target.file.extensions must not be null"));
        this.targetDirectories = Collections.unmodifiableList(
            Objects.requireNonNull(builder.targetDirectories, "target.directories must not be null"));
    }

    // getterのみ提供（setterなし）
    public String getGithubPat() {
        return githubPat;
    }

    public List<String> getTargetFileExtensions() {
        return targetFileExtensions; // 不変リストを返す
    }
}
```

### 不変性の保証
1. **クラスをfinal**: サブクラス化を防止
2. **フィールドをfinal**: 再代入を防止
3. **setterメソッドなし**: 状態変更を防止
4. **コレクションを不変化**: `Collections.unmodifiableList()`でラップ

### メリット
- **スレッドセーフ**: 複数スレッドから安全にアクセス可能
- **予測可能性**: オブジェクトの状態が変更されないことを保証
- **バグの削減**: 意図しない状態変更を防止
- **共有の安全性**: オブジェクトを安全に共有可能

### 適用理由
- 設定情報はアプリケーション全体で共有される
- 設定の変更を防止したい
- マルチスレッド環境での安全性を確保

---

## 5. キャッシュパターン（Cache Pattern）

### 概要
頻繁にアクセスされる値を一度計算・生成した後にメモリに保持し、以降のアクセスで再利用することでパフォーマンスを向上させるパターン。

### 実装箇所
- **クラス**: `ConfigManager`
- **ファイル**: `src/main/java/com/db2ghsync/common/ConfigManager.java`

### 実装例

```java
public class ConfigManager {
    // キャッシュされたAppConfigオブジェクト
    private static AppConfig appConfig;

    public static void loadConfig(String configFilePath) {
        Properties props = // ... ファイルから読み込み
        // 一度だけAppConfigを生成してキャッシュ
        ConfigManager.appConfig = buildAppConfig(props);
    }

    public static AppConfig getAppConfig() {
        if (Objects.isNull(appConfig)) {
            throw new IllegalStateException("Config not loaded. Call loadConfig() first.");
        }
        // キャッシュされたオブジェクトを返す（再生成しない）
        return appConfig;
    }

    private static AppConfig buildAppConfig(Properties props) {
        // Propertiesから直接AppConfigを生成
        return new AppConfig.Builder()
            .githubPat(getRequiredProperty(props, "github.pat"))
            .githubUsername(getRequiredProperty(props, "github.username"))
            .targetFileExtensions(Arrays.asList(
                getRequiredProperty(props, "target.file.extensions").split(",")))
            .build();
    }
}
```

### 使用例

```java
// アプリケーション起動時に一度だけ読み込み
ConfigManager.loadConfig("config.properties");

// 何度呼んでも同じインスタンスが返される（O(1)）
AppConfig config1 = ConfigManager.getAppConfig();  // キャッシュから取得
AppConfig config2 = ConfigManager.getAppConfig();  // 同じインスタンス
assert config1 == config2;  // true

// 個別getterメソッドも内部的にキャッシュを利用
String pat = ConfigManager.getGithubPat();  // getAppConfig()経由でキャッシュから取得
```

### パフォーマンス比較

| 操作 | キャッシュなし | キャッシュあり |
|------|---------------|---------------|
| `getAppConfig()`呼び出し | O(n) - 毎回生成 | O(1) - キャッシュ参照 |
| Mapルックアップ | 11回/呼び出し | 0回 |
| 文字列split | 2回/呼び出し | 0回 |
| オブジェクト生成 | 毎回 | 1回のみ |
| メモリ使用量 | Map + AppConfig | AppConfig のみ |

### メリット
- **パフォーマンス向上**: 繰り返しアクセスが高速（O(1)）
- **メモリ効率**: 設定情報を単一のオブジェクトとして保持
- **CPU効率**: 無駄な再計算・再生成を回避
- **一貫性**: 同じインスタンスが常に返される

### 適用理由
- 設定情報は一度読み込めば変更されない
- アプリケーション全体で頻繁にアクセスされる
- 生成コストが高い（11フィールド + 検証 + split処理）

---

## 6. ファクトリーパターン（Factory Pattern）

### 概要
オブジェクトの生成ロジックをカプセル化し、生成の複雑さを隠蔽するパターン。

### 実装箇所
- **クラス**: `SyncEntryFactory`
- **ファイル**: `src/main/java/com/db2ghsync/dropbox/SyncEntryFactory.java`

### 実装例

```java
public class SyncEntryFactory {
    /**
     * DropboxのMetadataをSyncEntryに変換する
     */
    public static SyncEntry convertMetadataToSyncEntry(Metadata metadata) {
        String dropboxPath = metadata.getPathLower();
        String subDirectoryKey = FilterUtils.getFirstDir(dropboxPath);
        String name = metadata.getName();

        SyncAction action = null;
        if (metadata instanceof DeletedMetadata) {
            action = SyncAction.DELETE;
        } else {
            action = SyncAction.CREATE_OR_UPDATE;
        }

        return new SyncEntry(
            dropboxPath,
            name,
            subDirectoryKey,
            action
        );
    }
}
```

### 使用例

```java
// DropboxClient内で使用
for (Metadata metadata : result.getEntries()) {
    if (metadata.getName().endsWith(extension)) {
        changedEntries.add(
            SyncEntryFactory.convertMetadataToSyncEntry(metadata)
        );
    }
}
```

### メリット
- **責任の分離**: 変換ロジックを一箇所に集約
- **再利用性**: 変換ロジックを複数箇所で再利用可能
- **保守性**: 変換ロジックの変更が容易
- **テスト容易性**: 変換ロジックを独立してテスト可能

### 適用理由
- Dropbox APIの`Metadata`から`SyncEntry`への変換が複雑
- 変換ロジックを複数箇所で使用
- 変換ロジックの変更に備える

---

## 7. アダプターパターン（Adapter Pattern）

### 概要
既存のクラスのインターフェースを、クライアントが期待するインターフェースに変換するパターン。

### 実装箇所
- **クラス**: `DropboxClient`
- **インターフェース**: `DropboxService`
- **外部API**: `DbxClientV2` (Dropbox SDK)

### 実装例

```java
public class DropboxClient implements DropboxService {
    // 外部APIのクライアント
    private final DbxClientV2 client;

    public DropboxClient(AppConfig config, CursorService cursorService) {
        // 外部APIの初期化
        DbxRequestConfig dbxConfig = DbxRequestConfig.newBuilder(APP_NAME).build();
        this.client = new DbxClientV2(dbxConfig, accessToken);
    }

    @Override
    public List<SyncEntry> getTargetFiles(String targetDir) throws DropboxSyncException {
        try {
            // 外部APIを呼び出し
            ListFolderResult result = client.files().listFolderBuilder(targetDir)
                .withRecursive(true).start();
            
            // 結果を内部形式に変換
            List<SyncEntry> entries = new ArrayList<>();
            for (Metadata metadata : result.getEntries()) {
                entries.add(SyncEntryFactory.convertMetadataToSyncEntry(metadata));
            }
            return entries;
        } catch (DbxException e) {
            // 外部例外を内部例外に変換
            throw new DropboxSyncException("Getting targeting-file-list, Error happened.", e);
        }
    }
}
```

### メリット
- **疎結合**: 外部APIへの直接依存を回避
- **互換性**: 外部APIの変更を内部に影響させない
- **テスト容易性**: インターフェースをモック化可能
- **例外の統一**: 外部例外を内部例外に変換

### 適用理由
- Dropbox SDKの`DbxClientV2`を内部インターフェースに適合
- 外部APIの変更から内部コードを保護
- テストコードの簡潔化

---

## 8. ユーティリティクラスパターン（Utility Class）

### 概要
関連する静的メソッドを集約し、再利用可能な機能を提供するパターン。

### 実装箇所
- **`FilterUtils`**: ファイル名やパスのフィルタリング処理
- **`ConfigManager`**: 設定ファイルの読み込みと管理

### 実装例

```java
// FilterUtils.java
public class FilterUtils {
    /**
     * ファイル名が指定拡張子リストのいずれかで終わるか判定
     */
    public static boolean matchExtension(String fileName, List<String> extensions) {
        for (String ext : extensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * パス文字列の一番目のディレクトリ名を取得
     */
    public static String getFirstDir(String path) {
        if (path != null && path.startsWith("/")) {
            String[] parts = path.split("/");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return "";
    }
}
```

```java
// ConfigManager.java
public class ConfigManager {
    private static AppConfig appConfig;  // キャッシュ

    public static void loadConfig(String configFilePath) {
        // 設定ファイルを読み込み、AppConfigを生成してキャッシュ
        Properties props = // ... ファイルから読み込み
        ConfigManager.appConfig = buildAppConfig(props);
    }

    public static AppConfig getAppConfig() {
        // キャッシュされたAppConfigを返す
        if (Objects.isNull(appConfig)) {
            throw new IllegalStateException("Config not loaded. Call loadConfig() first.");
        }
        return appConfig;
    }

    public static String getGithubPat() {
        // キャッシュされたAppConfigから設定値を取得
        return getAppConfig().getGithubPat();
    }

    private static AppConfig buildAppConfig(Properties props) {
        // Propertiesから直接AppConfigを生成
        return new AppConfig.Builder()
            .githubPat(getRequiredProperty(props, "github.pat"))
            // ... 他のプロパティ
            .build();
    }
}
```

### 使用例

```java
// 設定ファイルを一度だけ読み込み
ConfigManager.loadConfig("config.properties");

// 何度呼んでもキャッシュから返される（効率的）
AppConfig config1 = ConfigManager.getAppConfig();
AppConfig config2 = ConfigManager.getAppConfig();  // 同じインスタンス（キャッシュ）

// 個別getterメソッドも内部的にキャッシュを利用
String pat = ConfigManager.getGithubPat();  // キャッシュから取得
```

### メリット
- **再利用性**: 複数箇所で使用可能
- **一貫性**: 同じロジックを統一して使用
- **保守性**: ロジックの変更が容易
- **効率性**: キャッシュによりパフォーマンス向上（O(1)でアクセス）
- **メモリ効率**: 設定情報を単一のオブジェクトとして保持

### 適用理由
- 複数クラスから使用される共通機能
- 設定情報は一度読み込めば変更されない
- キャッシュによる効率化が可能

---

## 9. 列挙型パターン（Enum Pattern）

### 概要
関連する定数を型安全に管理するためのパターン。

### 実装箇所
- **クラス**: `SyncAction`
- **ファイル**: `src/main/java/com/db2ghsync/common/SyncAction.java`

### 実装例

```java
/**
 * DropBox上のファイルステータス
 */
public enum SyncAction {
    CREATE_OR_UPDATE,
    DELETE;
}
```

### 使用例

```java
// SyncEntryFactory内で使用
SyncAction action = null;
if (metadata instanceof DeletedMetadata) {
    action = SyncAction.DELETE;
} else {
    action = SyncAction.CREATE_OR_UPDATE;
}

// DropboxClient内で使用
if (entry.getAction().equals(SyncAction.CREATE_OR_UPDATE)) {
    downloadFile(entry.getDropboxPath(), gitPath);
} else {
    deleteFile(entry.getDropboxPath(), gitPath);
}
```

### メリット
- **型安全性**: コンパイル時にエラーを検出
- **可読性**: マジックナンバーや文字列リテラルを回避
- **IDE支援**: オートコンプリートが利用可能
- **リファクタリング**: 名前の変更が容易

### 適用理由
- ファイルの状態を表現する定数
- 型安全性を確保したい
- 将来の拡張に対応

---

## 10. Strategyパターン（Strategy Pattern）

### 概要
アルゴリズムのファミリーを定義し、それぞれをカプセル化して交換可能にするパターン。実行時に異なる振る舞いを選択できるようにします。

### 実装箇所
- **インターフェース**: `SyncWorkflow`
- **実装クラス**: 
  - `DropboxToGitWorkflow` - Dropbox→Git同期戦略
  - `GitToDropboxWorkflow` - Git→Dropbox同期戦略
- **ファイル**: `src/main/java/com/db2ghsync/app/`

### 実装例

```java
// 戦略インターフェース
public interface SyncWorkflow {
    void execute() throws DropboxSyncException, GithubSyncException, IOException;
}

// Dropbox→Git同期戦略
public class DropboxToGitWorkflow implements SyncWorkflow {
    private final SyncProcessor syncProcessor;

    public DropboxToGitWorkflow(SyncProcessor syncProcessor) {
        this.syncProcessor = syncProcessor;
    }

    @Override
    public void execute() throws DropboxSyncException, GithubSyncException, IOException {
        syncProcessor.start();
    }
}

// Git→Dropbox同期戦略
public class GitToDropboxWorkflow implements SyncWorkflow {
    private final GitToDropboxProcessor processor;

    public GitToDropboxWorkflow(GitToDropboxProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void execute() throws DropboxSyncException, GithubSyncException, IOException {
        processor.start();
    }
}
```

### コンテキスト（戦略を使用する側）

```java
// DropboxGitSyncApplication.java
public class DropboxGitSyncApplication {
    
    private static SyncWorkflow createWorkflow(SyncDirection direction, AppConfig config) {
        CursorService cursorService = new CursorManager(config);
        DropboxService dropboxService = new DropboxClient(config, cursorService);
        GitService gitService = new GitRepositoryManager(config);

        // 同期方向に応じて適切な戦略を選択
        if (direction == SyncDirection.DBX_TO_GIT) {
            SyncProcessor processor = new SyncProcessor(dropboxService, gitService, cursorService);
            return new DropboxToGitWorkflow(processor);
        }

        GitToDropboxProcessor processor = new GitToDropboxProcessor(config, gitService, dropboxService);
        return new GitToDropboxWorkflow(processor);
    }

    public static void main(String[] args) {
        CommandLineOptions options = CommandLineOptions.parse(args);
        SyncWorkflow workflow = createWorkflow(options.direction(), ConfigManager.getAppConfig());
        
        // 選択された戦略を実行
        workflow.execute();
    }
}
```

### 列挙型による方向制御

```java
public enum SyncDirection {
    DBX_TO_GIT("dbx-to-git"),
    GIT_TO_DBX("git-to-dbx");

    private final String argument;

    SyncDirection(String argument) {
        this.argument = argument;
    }

    public static SyncDirection fromArgument(String arg) {
        return Arrays.stream(SyncDirection.values())
                .filter(direction -> direction.argument.equalsIgnoreCase(arg))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown sync direction: " + arg));
    }
}
```

### メリット
- **拡張性**: 新しい同期方向を追加しやすい（例: Git→Git、Dropbox→Dropbox）
- **保守性**: 各同期方向のロジックが独立しているため、変更の影響範囲が限定される
- **可読性**: どの戦略を使用しているかが明確
- **テスト容易性**: 各戦略を独立してテスト可能
- **単一責任**: 各戦略クラスが1つの同期方向のみを担当

### 適用理由
- 双方向同期機能の実装が必要
- Dropbox→GitとGit→Dropboxで処理フローが大きく異なる
- コマンドライン引数で同期方向を切り替える必要がある
- 将来的に他の同期方向を追加する可能性がある

### 責務分離

各戦略は、実際の処理をプロセッサクラスに委譲することで、さらに責務を分離：

- **`DropboxToGitWorkflow`**: Dropbox→Git同期の実行フロー制御
  - 実際の処理は`SyncProcessor`に委譲
- **`GitToDropboxWorkflow`**: Git→Dropbox同期の実行フロー制御
  - 実際の処理は`GitToDropboxProcessor`に委譲

---

## アーキテクチャパターン

### レイヤードアーキテクチャ

プロジェクトは以下のレイヤーに分離されています：

```
┌─────────────────────────────────────┐
│   Application Layer                 │
│   - DropboxGitSyncApplication       │
│   - SyncProcessor                   │
│   - GitToDropboxProcessor           │
│   - SyncWorkflow (interface)        │
│   - DropboxToGitWorkflow            │
│   - GitToDropboxWorkflow            │
└─────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│   Service Layer                     │
│   - DropboxService (interface)      │
│   - GitService (interface)          │
│   - CursorService (interface)       │
└─────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│   Implementation Layer              │
│   - DropboxClient                   │
│   - GitRepositoryManager            │
│   - CursorManager                   │
└─────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│   Domain Layer                      │
│   - AppConfig                       │
│   - SyncEntry                       │
│   - SyncAction                      │
│   - SyncDirection                   │
└─────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│   Infrastructure Layer              │
│   - ConfigManager                   │
│   - FilterUtils                     │
│   - SyncEntryFactory                │
└─────────────────────────────────────┘
```

### 依存関係の方向

- **上位レイヤー -> 下位レイヤー**: 依存関係は一方向
- **インターフェース経由**: 実装への直接依存を回避
- **依存性注入**: コンストラクタで依存関係を注入

---

## パターンの組み合わせ

### 1. Builder + 不変オブジェクト

`AppConfig`はBuilderパターンで構築され、不変オブジェクトとして実装されています。

```java
// Builderで構築
AppConfig config = new AppConfig.Builder()
    .githubPat("token")
    .build();

// 不変性が保証される
List<String> extensions = config.getTargetFileExtensions();
// extensions.add(".java"); // UnsupportedOperationException
```

### 2. 依存性注入 + インターフェース分離

サービスはインターフェース経由で注入され、実装の詳細が隠蔽されます。

```java
// インターフェース経由で注入
public SyncProcessor(
    DropboxService dropboxService,  // インターフェース
    GitService gitService,           // インターフェース
    CursorService cursorService      // インターフェース
) {
    // 実装の詳細を知る必要がない
}
```

### 3. ファクトリー + アダプター

`SyncEntryFactory`が外部APIのオブジェクトを内部形式に変換します。

```java
// 外部APIのMetadataを内部形式に変換
SyncEntry entry = SyncEntryFactory.convertMetadataToSyncEntry(metadata);
```

---

## パターン選択の理由

### なぜBuilderパターンを使ったか
- 11個の設定項目があり、コンストラクタが複雑になる
- 必須項目とオプション項目の区別が必要
- オブジェクトの不変性を保証したい

### なぜ依存性注入を使ったか
- テスト容易性を向上させるため
- 将来の実装変更に対応するため
- コードの可読性と保守性を向上させるため

### なぜインターフェース分離を使ったか
- 各サービスの責任を明確に分離するため
- テストコードを簡潔にするため
- 将来の機能拡張に対応するため

### なぜ不変オブジェクトを使ったか
- 設定情報はアプリケーション全体で共有される
- 設定の変更を防止したい
- マルチスレッド環境での安全性を確保

---

## ベストプラクティス

### 1. パターンの適切な使用
- パターンは目的を持って使用する
- 過度な抽象化を避ける
- プロジェクトの規模に応じた選択

### 2. 一貫性の維持
- 同じパターンは同じ方法で実装
- 命名規則を統一
- ドキュメントを整備

### 3. テスト容易性の確保
- インターフェース経由で依存関係を定義
- コンストラクタで依存関係を注入
- モック化しやすい設計

---

## 参考資料

- [Design Patterns: Elements of Reusable Object-Oriented Software](https://en.wikipedia.org/wiki/Design_Patterns)
- [Effective Java (3rd Edition)](https://www.oreilly.com/library/view/effective-java/9780134686097/)
- [Clean Architecture](https://www.amazon.com/Clean-Architecture-Craftsmans-Software-Structure/dp/0134494164)

---

**最終更新日**: 2025-11-22  
**作成者**: アーキテクチャチーム

