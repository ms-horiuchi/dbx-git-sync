# デザインパターンサマリー

## クイックリファレンス

### 使用パターン一覧

| パターン | カテゴリ | 使用箇所 | 目的 |
|---------|---------|---------|------|
| **Builder** | 生成 | `AppConfig.Builder` | 複雑なオブジェクトの構築 |
| **依存性注入** | 構造 | 全サービスクラス | 疎結合な設計 |
| **Strategy** | 振る舞い | `SyncWorkflow` | 同期方向に応じた処理の切り替え |
| **インターフェース分離** | 原則 | `*Service`インターフェース | クライアントに不要な依存を強制しない |
| **不変オブジェクト** | 構造 | `AppConfig` | スレッドセーフと予測可能性 |
| **キャッシュ** | パフォーマンス | `ConfigManager` | 繰り返しアクセスを高速化 |
| **ファクトリー** | 生成 | `SyncEntryFactory` | オブジェクト生成の複雑さを隠蔽 |
| **アダプター** | 構造 | `DropboxClient` | 外部APIを内部インターフェースに適合 |
| **ユーティリティクラス** | 構造 | `FilterUtils`, `ConfigManager` | 再利用可能な機能を提供 |
| **列挙型** | 構造 | `SyncAction`, `SyncDirection` | 定数の型安全な管理 |

---

## パターン別実装例

### Builderパターン

```java
AppConfig config = new AppConfig.Builder()
    .githubPat("token")
    .githubUsername("user")
    .build();
```

### 依存性注入

```java
public SyncProcessor(
    DropboxService dropboxService,
    GitService gitService,
    CursorService cursorService
) {
    this.dropboxService = Objects.requireNonNull(dropboxService);
    // ...
}
```

### 不変オブジェクト

```java
public final class AppConfig {
    private final String githubPat;  // finalフィールド
    private final List<String> extensions;  // 不変リスト
    
    // setterなし、getterのみ
}
```

### ファクトリーパターン

```java
SyncEntry entry = SyncEntryFactory.convertMetadataToSyncEntry(metadata);
```

### Strategyパターン

```java
// 同期方向に応じて戦略を選択
SyncWorkflow workflow = createWorkflow(direction, config);
workflow.execute();

// 各戦略の実装
public class DropboxToGitWorkflow implements SyncWorkflow { /* ... */ }
public class GitToDropboxWorkflow implements SyncWorkflow { /* ... */ }
```

### キャッシュパターン

```java
public class ConfigManager {
    private static AppConfig appConfig;  // キャッシュ
    
    public static AppConfig getAppConfig() {
        return appConfig;  // キャッシュから返す（O(1)）
    }
}
```

### アダプターパターン

```java
public class DropboxClient implements DropboxService {
    private final DbxClientV2 client;  // 外部API
    
    // 外部APIを内部インターフェースに適合
}
```

---

## アーキテクチャレイヤー

```
Application Layer
    ↓
Service Layer (インターフェース)
    ↓
Implementation Layer (実装)
    ↓
Domain Layer (エンティティ)
    ↓
Infrastructure Layer (ユーティリティ)
```

---

## パターンの組み合わせ

1. **Builder + 不変オブジェクト**: `AppConfig`
2. **依存性注入 + インターフェース分離**: 全サービス
3. **Strategy + 依存性注入**: `SyncWorkflow`実装クラス
4. **キャッシュ + ユーティリティクラス**: `ConfigManager`
5. **ファクトリー + アダプター**: `SyncEntryFactory` + `DropboxClient`

---

詳細は [DESIGN_PATTERNS.md](./DESIGN_PATTERNS.md) を参照してください。

