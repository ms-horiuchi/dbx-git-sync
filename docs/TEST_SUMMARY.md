# テストケースサマリー

## クイックリファレンス

### テストクラス一覧

| テストクラス | テスト数 | カバレッジ | 主要パターン |
|------------|---------|-----------|------------|
| `AppConfigTest` | 11 | 高 | Builder、不変性、nullチェック |
| `ConfigManagerTest` | 7 | 高 | ファイルI/O、例外処理、リフレクション |
| `SyncProcessorTest` | 9 | 高 | モック、順序検証、例外伝播 |
| `CursorManagerTest` | 9 | 高 | ファイル操作、一時ディレクトリ |
| `DropboxClientTest` | 4 | 低 | コンストラクタテスト |
| `GitRepositoryManagerTest` | 2 | 低 | コンストラクタテスト |

**合計**: 42テスト

---

## テストパターンクイックリファレンス

### モックパターン

```java
// モックの作成
@Mock
private Service service;

// 動作のスタブ化
when(service.method()).thenReturn(value);

// voidメソッドの例外
doThrow(new Exception()).when(service).voidMethod();

// 呼び出し検証
verify(service).method();
verify(service, times(2)).method();
verify(service, never()).method();
```

### 一時ファイル/ディレクトリ

```java
@TempDir
Path tempDir;

Path file = tempDir.resolve("test.txt");
Files.writeString(file, "content");
```

### 例外テスト

```java
Exception exception = assertThrows(
    ExpectedException.class,
    () -> target.method()
);
assertTrue(exception.getMessage().contains("message"));
```

### リフレクション（静的フィールドリセット）

```java
@AfterEach
void tearDown() {
    Field field = ClassName.class.getDeclaredField("staticField");
    field.setAccessible(true);
    field.set(null, null);
}
```

---

## テスト実行コマンド

```bash
# すべてのテスト
.\gradlew.bat test

# 特定のクラス
.\gradlew.bat test --tests "com.db2ghsync.common.AppConfigTest"

# 特定のメソッド
.\gradlew.bat test --tests "*testBuilderCreatesValidAppConfig"
```

---

## テストカバレッジ

- ✅ **高カバレッジ**: AppConfig, ConfigManager, SyncProcessor, CursorManager
- ⚠️ **低カバレッジ**: DropboxClient, GitRepositoryManager（統合テスト必要）

---

詳細は [TEST_DOCUMENTATION.md](./TEST_DOCUMENTATION.md) を参照してください。

