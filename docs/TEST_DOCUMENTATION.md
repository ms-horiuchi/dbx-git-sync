# テストケースとパターン整理資料

## 概要

このドキュメントは、dbx-git-syncプロジェクトのテストクラスとテストパターンを整理した資料です。

## テスト環境

- **テストフレームワーク**: JUnit Jupiter 5.12.1
- **モックフレームワーク**: Mockito 5.14.2
- **Javaバージョン**: 17

## テストクラス一覧

### 1. AppConfigTest
**パッケージ**: `com.db2ghsync.common`  
**目的**: 不変オブジェクトとBuilderパターンのテスト

#### テストケース

| テストメソッド | テスト内容 | 検証項目 |
|--------------|----------|---------|
| `testBuilderCreatesValidAppConfig()` | Builderで正常にAppConfigを作成 | すべてのgetterが正しい値を返すこと |
| `testBuilderWithNullGithubPatThrowsException()` | githubPatがnullの場合 | NullPointerExceptionがスローされること |
| `testBuilderWithNullGithubUsernameThrowsException()` | githubUsernameがnullの場合 | NullPointerExceptionがスローされること |
| `testBuilderWithNullGithubRemoteUrlThrowsException()` | githubRemoteUrlがnullの場合 | NullPointerExceptionがスローされること |
| `testBuilderWithNullLocalRepoPathThrowsException()` | localRepoPathがnullの場合 | NullPointerExceptionがスローされること |
| `testBuilderWithNullCursorFilePathThrowsException()` | cursorFilePathがnullの場合 | NullPointerExceptionがスローされること |
| `testBuilderWithNullTargetFileExtensionsThrowsException()` | targetFileExtensionsがnullの場合 | NullPointerExceptionがスローされること |
| `testBuilderWithNullTargetDirectoriesThrowsException()` | targetDirectoriesがnullの場合 | NullPointerExceptionがスローされること |
| `testImmutabilityOfCollections()` | コレクションの不変性 | getterで返されるコレクションが変更不可であること |
| `testOptionalDropboxFieldsCanBeEmpty()` | オプション項目が空文字列 | 正常に作成できること |
| `testBuilderWithNullOptionalFieldsDefaultsToEmpty()` | オプション項目がnull | 空文字列にデフォルト化されること |

#### 使用パターン
- **Builderパターンのテスト**: フルエントAPIの検証
- **不変性のテスト**: `UnsupportedOperationException`を期待したコレクション変更テスト
- **Null安全性のテスト**: 必須項目のnullチェック検証

---

### 2. ConfigManagerTest
**パッケージ**: `com.db2ghsync.common`  
**目的**: 設定ファイル読み込みとAppConfig生成のテスト

#### テストケース

| テストメソッド | テスト内容 | 検証項目 |
|--------------|----------|---------|
| `testLoadConfigWithValidFile()` | 正常な設定ファイルを読み込み | 例外がスローされないこと |
| `testGetAppConfigReturnsCorrectValues()` | AppConfigの値が正しい | すべての設定値が正しく読み込まれること |
| `testLoadConfigWithMissingRequiredPropertyThrowsException()` | 必須項目が不足 | IllegalArgumentExceptionがスローされること |
| `testLoadConfigWithNonExistentFileThrowsException()` | 存在しないファイル | RuntimeExceptionがスローされること |
| `testGetAppConfigBeforeLoadConfigThrowsException()` | ロード前にgetAppConfig呼び出し | IllegalStateExceptionがスローされること |
| `testStaticGetterMethodsWorkCorrectly()` | 静的getterメソッド | すべてのgetterが正しい値を返すこと |
| `testLoadConfigWithEmptyOptionalProperties()` | オプション項目が空 | 正常に読み込まれること |

#### 使用パターン
- **一時ファイルの使用**: `@TempDir`アノテーションでテスト用ファイルを作成
- **リフレクション**: `@AfterEach`で静的フィールドをリセットしてテストの独立性を確保
- **例外テスト**: `assertThrows()`で例外の種類とメッセージを検証

#### テストリソース
- `src/test/resources/config.properties` - 正常系の設定ファイル
- `src/test/resources/config-invalid.properties` - 異常系の設定ファイル（必須項目不足）

---

### 3. SyncProcessorTest
**パッケージ**: `com.db2ghsync.app`  
**目的**: 同期処理のロジックテスト（モック使用）

#### テストケース

| テストメソッド | テスト内容 | 検証項目 |
|--------------|----------|---------|
| `testStartWithNoCursorFetchesAllFiles()` | カーソルがない場合 | 全ファイルを取得すること |
| `testStartWithCursorFetchesChangesOnly()` | カーソルがある場合 | 変更のみを取得すること |
| `testStartWithNoChangesSkipsGitOperations()` | 変更がない場合 | Git操作がスキップされること |
| `testStartProcessesMultipleDirectories()` | 複数ディレクトリの処理 | 各ディレクトリごとに処理が実行されること |
| `testStartWithCorrectOrderOfOperations()` | 操作の順序 | 正しい順序でメソッドが呼ばれること |
| `testStartPropagatesDropboxSyncException()` | Dropbox例外の伝播 | DropboxSyncExceptionが伝播されること |
| `testStartPropagatesGithubSyncException()` | GitHub例外の伝播 | GithubSyncExceptionが伝播されること |
| `testStartPropagatesGithubSyncExceptionFromClone()` | クローン時の例外 | GithubSyncExceptionが伝播されること |
| `testStartCallsCloneOrOpenRepositoryOnce()` | クローン呼び出し回数 | 1回のみ呼ばれること |

#### 使用パターン
- **モックオブジェクト**: `@Mock`アノテーションで依存関係をモック化
- **動作検証**: `verify()`でメソッドの呼び出し回数と引数を検証
- **順序検証**: `InOrder`でメソッド呼び出しの順序を検証
- **例外テスト**: `doThrow()`でvoidメソッドの例外をモック

#### モック対象
- `DropboxService`
- `GitService`
- `CursorService`

---

### 4. CursorManagerTest
**パッケージ**: `com.db2ghsync.dropbox`  
**目的**: カーソル管理のテスト

#### テストケース

| テストメソッド | テスト内容 | 検証項目 |
|--------------|----------|---------|
| `testReadCursorFromExistingFile()` | 既存ファイルから読み込み | カーソルが正しく読み込まれること |
| `testReadCursorFromNonExistentFileReturnsEmptyString()` | ファイルが存在しない | 空文字列が返されること |
| `testWriteTmpCursorCreatesTemporaryFile()` | 一時ファイルへの書き込み | 一時ファイルが作成されること |
| `testWriteCursorSavesCursorAndDeletesTmpFile()` | カーソル保存と一時ファイル削除 | カーソルが保存され、一時ファイルが削除されること |
| `testWriteCursorThrowsExceptionWhenTmpFileDoesNotExist()` | 一時ファイルがない場合 | DropboxSyncExceptionがスローされること |
| `testWriteTmpCursorThrowsExceptionOnWriteFailure()` | 書き込み失敗 | DropboxSyncExceptionがスローされること |
| `testReadCursorHandlesMultipleBranches()` | 複数ブランチの処理 | 各ブランチのカーソルが正しく読み込まれること |
| `testWriteCursorOverwritesExistingCursor()` | 既存カーソルの上書き | カーソルが正しく上書きされること |
| `testConstructorWithNullConfigThrowsException()` | null設定での初期化 | NullPointerExceptionがスローされること |

#### 使用パターン
- **一時ディレクトリ**: `@TempDir`でテスト用ディレクトリを作成
- **ファイル操作のテスト**: 実際のファイルI/Oをテスト
- **例外テスト**: ファイル操作の例外が適切に処理されることを検証

---

### 5. DropboxClientTest
**パッケージ**: `com.db2ghsync.dropbox`  
**目的**: Dropbox操作のテスト（モック使用）

#### テストケース

| テストメソッド | テスト内容 | 検証項目 |
|--------------|----------|---------|
| `testConstructorWithNullConfigThrowsException()` | null設定での初期化 | NullPointerExceptionがスローされること |
| `testConstructorWithNullCursorServiceThrowsException()` | nullカーソルサービス | NullPointerExceptionがスローされること |
| `testConstructorWithAccessTokenCreatesClient()` | アクセストークンでの初期化 | クライアントが作成されること |
| `testConstructorWithRefreshTokenCreatesClient()` | リフレッシュトークンでの初期化 | クライアントが作成されること |

#### 使用パターン
- **コンストラクタテスト**: 依存性注入の検証
- **Null安全性**: nullチェックの検証

#### 注意事項
- DropboxClientは実際の`DbxClientV2`を使用しているため、完全なモックテストは困難
- 実際のDropbox APIを呼び出すメソッドのテストは統合テストとして別途実装が必要

---

### 6. GitRepositoryManagerTest
**パッケージ**: `com.db2ghsync.git`  
**目的**: Git操作のテスト（モック使用）

#### テストケース

| テストメソッド | テスト内容 | 検証項目 |
|--------------|----------|---------|
| `testConstructorWithNullConfigThrowsException()` | null設定での初期化 | NullPointerExceptionがスローされること |
| `testConstructorWithValidConfigCreatesManager()` | 正常な設定での初期化 | マネージャーが作成されること |

#### 使用パターン
- **コンストラクタテスト**: 依存性注入の検証

#### 注意事項
- GitRepositoryManagerは実際のJGitを使用しているため、完全なモックテストは困難
- 実際のGit操作のテストは統合テストとして別途実装が必要

---

## テストパターン一覧

### 1. モックパターン

#### 使用ライブラリ
- Mockito 5.14.2
- Mockito JUnit Jupiter Extension

#### パターン

**基本モック**
```java
@ExtendWith(MockitoExtension.class)
class TestClass {
    @Mock
    private Service service;
}
```

**動作のスタブ化**
```java
when(service.method()).thenReturn(value);
```

**voidメソッドの例外モック**
```java
doThrow(new Exception()).when(service).voidMethod();
```

**呼び出し検証**
```java
verify(service, times(2)).method();
verify(service, never()).method();
```

**順序検証**
```java
InOrder inOrder = inOrder(service1, service2);
inOrder.verify(service1).method1();
inOrder.verify(service2).method2();
```

### 2. 一時ファイル/ディレクトリパターン

#### JUnit 5の`@TempDir`
```java
@TempDir
Path tempDir;

@Test
void test() {
    Path file = tempDir.resolve("test.txt");
    Files.writeString(file, "content");
}
```

**利点**
- テスト後に自動的にクリーンアップ
- テストの独立性を確保
- プラットフォーム非依存

### 3. リフレクションパターン

#### 静的フィールドのリセット
```java
@AfterEach
void tearDown() {
    try {
        Field field = ClassName.class.getDeclaredField("staticField");
        field.setAccessible(true);
        field.set(null, null);
    } catch (Exception e) {
        // エラー処理
    }
}
```

**使用ケース**
- テスト間で状態が共有される静的フィールドのリセット
- テストの独立性を確保

### 4. 例外テストパターン

#### JUnit 5の`assertThrows`
```java
@Test
void testException() {
    Exception exception = assertThrows(
        ExpectedException.class,
        () -> target.method()
    );
    assertTrue(exception.getMessage().contains("expected message"));
}
```

### 5. Given-When-Thenパターン

#### 構造
```java
@Test
void testMethod() {
    // Given - テストの前提条件を設定
    String input = "test";
    
    // When - テスト対象のメソッドを実行
    String result = target.method(input);
    
    // Then - 結果を検証
    assertEquals("expected", result);
}
```

**利点**
- テストの可読性が向上
- テストの構造が明確

### 6. 不変性テストパターン

#### コレクションの不変性検証
```java
@Test
void testImmutability() {
    List<String> list = config.getList();
    
    assertThrows(UnsupportedOperationException.class, () -> {
        list.add("new item");
    });
}
```

---

## テストカバレッジ

### カバレッジ対象

| クラス | テストクラス | カバレッジ |
|--------|------------|-----------|
| `AppConfig` | `AppConfigTest` | 高（Builder、不変性、nullチェック） |
| `ConfigManager` | `ConfigManagerTest` | 高（設定読み込み、例外処理） |
| `SyncProcessor` | `SyncProcessorTest` | 高（同期ロジック、例外伝播） |
| `CursorManager` | `CursorManagerTest` | 高（ファイル操作、例外処理） |
| `DropboxClient` | `DropboxClientTest` | 低（コンストラクタのみ、統合テスト必要） |
| `GitRepositoryManager` | `GitRepositoryManagerTest` | 低（コンストラクタのみ、統合テスト必要） |

### テスト統計

- **総テスト数**: 42
- **成功**: 42
- **失敗**: 0
- **スキップ**: 0

---

## テスト実行方法

### すべてのテストを実行
```bash
.\gradlew.bat test
```

### 特定のテストクラスを実行
```bash
.\gradlew.bat test --tests "com.db2ghsync.common.AppConfigTest"
```

### 特定のテストメソッドを実行
```bash
.\gradlew.bat test --tests "com.db2ghsync.common.AppConfigTest.testBuilderCreatesValidAppConfig"
```

### テストレポートの確認
```bash
# HTMLレポート
build/reports/tests/test/index.html

# XMLレポート
build/test-results/test/
```

---

## ベストプラクティス

### 1. テストの独立性
- 各テストは独立して実行可能であること
- 静的フィールドは`@AfterEach`でリセット
- テストデータは`@TempDir`で管理

### 2. テストの可読性
- Given-When-Thenパターンを使用
- テストメソッド名は意図が明確に
- コメントで複雑なロジックを説明

### 3. モックの適切な使用
- 外部依存をモック化
- 実際の動作をテストする場合は統合テストを検討
- モックの検証を適切に行う

### 4. 例外テスト
- 例外の種類とメッセージを検証
- 正常系と異常系の両方をテスト

### 5. テストデータの管理
- テスト用リソースファイルを使用
- 一時ファイル/ディレクトリは`@TempDir`を使用
- テストデータはクリーンアップすること

---

## 今後の改善点

### 1. 統合テストの追加
- `DropboxClient`の実際のDropbox APIとの統合テスト
- `GitRepositoryManager`の実際のGit操作の統合テスト

### 2. テストカバレッジの向上
- カバレッジツール（JaCoCo等）の導入
- カバレッジレポートの定期確認

### 3. パラメータ化テスト
- `@ParameterizedTest`を使用した複数パターンのテスト

### 4. テストのリファクタリング
- 共通処理の抽出
- テストヘルパークラスの作成

---

## 参考資料

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [JUnit 5 @TempDir](https://junit.org/junit5/docs/current/api/org.junit.jupiter.api/org/junit/jupiter/api/io/TempDir.html)

---

**最終更新日**: 2025-11-22  
**作成者**: テスト実装チーム

