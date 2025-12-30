# コーディング規約

## 目次
- [基本方針](#基本方針)
- [コードフォーマット](#コードフォーマット)
- [命名規則](#命名規則)
- [パッケージ構成](#パッケージ構成)
- [クラス設計原則](#クラス設計原則)
- [例外処理](#例外処理)
- [ロギング](#ロギング)
- [テストコード規約](#テストコード規約)
- [JavaDoc規約](#javadoc規約)
- [セキュリティ要件](#セキュリティ要件)

---

## 基本方針

### 1. コードの可読性を最優先

```java
// ❌ Bad: 変数名が不明瞭
double c = h * r;

// ✅ Good: 意図が明確
double totalPayment = workHours * hourlyRate;
```

### 2. SOLID原則の遵守

- **S**ingle Responsibility Principle (単一責任の原則)
- **O**pen/Closed Principle (開放/閉鎖の原則)
- **L**iskov Substitution Principle (リスコフの置換原則)
- **I**nterface Segregation Principle (インターフェース分離の原則)
- **D**ependency Inversion Principle (依存性逆転の原則)

### 3. DRY (Don't Repeat Yourself)

同じロジックを繰り返さない。共通化できる処理はユーティリティクラスやヘルパーメソッドに切り出す。

### 4. YAGNI (You Aren't Gonna Need It)

将来使うかもしれない機能は実装しない。現在必要な機能のみを実装する。

---

## コードフォーマット

### Google Java Style Guideを採用

**自動フォーマッター設定**:
- IntelliJ IDEA: [google-java-format plugin](https://plugins.jetbrains.com/plugin/8527-google-java-format)
- Eclipse: [google-java-format.xml](https://github.com/google/styleguide/blob/gh-pages/eclipse-java-google-style.xml)

### インデント

- **スペース4つ** でインデント (タブ禁止)
- 継続行は **スペース8つ** でインデント

```java
// ✅ Good
public PayrollDto calculatePayroll(
        String employeeId,
        LocalDate startDate,
        LocalDate endDate) {
    return payrollService.calculate(
            employeeId, startDate, endDate);
}
```

### 行の長さ

- **最大100文字** (厳守)
- 長いメソッドチェーンは改行して見やすく

```java
// ✅ Good
List<EmployeeDto> employees = employeeRepository.findAll().stream()
        .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE)
        .map(employeeMapper::toDto)
        .collect(Collectors.toList());
```

### 中括弧の配置 (K&Rスタイル)

```java
// ✅ Good
if (condition) {
    doSomething();
} else {
    doOtherThing();
}

// ❌ Bad
if (condition)
{
    doSomething();
}
```

### 空白行の使用

```java
public class PayrollService {
    // フィールド宣言後に1行空ける
    private final PayrollRepository payrollRepository;
    private final WorkRecordRepository workRecordRepository;

    // コンストラクタの前に1行空ける
    public PayrollService(
            PayrollRepository payrollRepository,
            WorkRecordRepository workRecordRepository) {
        this.payrollRepository = payrollRepository;
        this.workRecordRepository = workRecordRepository;
    }

    // メソッド間に1行空ける
    public PayrollDto calculatePayroll(String employeeId) {
        // ロジックのブロック間に1行空ける
        List<WorkRecord> records = workRecordRepository
                .findByEmployeeId(employeeId);

        double totalPayment = calculateTotalPayment(records);

        return PayrollDto.builder()
                .totalPayment(totalPayment)
                .build();
    }
}
```

---

## 命名規則

### クラス名

- **UpperCamelCase** (PascalCase)
- 名詞または名詞句
- 略語は避ける (例外: DTO, API, URL, ID)

```java
// ✅ Good
public class PayrollCalculationService { }
public class EmployeeDto { }
public class WorkRecordRepository { }

// ❌ Bad
public class payrollService { }  // 小文字始まり
public class PCS { }  // 略語すぎる
public class CalculatePayroll { }  // 動詞始まり
```

### メソッド名

- **lowerCamelCase**
- 動詞または動詞句
- boolean型を返すメソッドは `is`, `has`, `can`, `should` で始める

```java
// ✅ Good
public PayrollDto calculatePayroll(String employeeId) { }
public void sendNotification(String email) { }
public boolean isActiveEmployee(String employeeId) { }
public boolean hasWorkRecords(String employeeId) { }

// ❌ Bad
public PayrollDto payroll(String employeeId) { }  // 動詞がない
public boolean active(String employeeId) { }  // is/has/canがない
```

### 変数名

- **lowerCamelCase**
- 意味のある名前 (1文字変数は禁止、例外: ループカウンタ `i`, `j`)

```java
// ✅ Good
String employeeId;
LocalDate startDate;
double hourlyRate;

// ループカウンタ (例外的に許可)
for (int i = 0; i < list.size(); i++) { }

// ❌ Bad
String e;  // 短すぎる
String employeeIdentificationNumber;  // 長すぎる
double hr;  // 略語
```

### 定数名

- **UPPER_SNAKE_CASE**
- `static final` で宣言

```java
// ✅ Good
public static final int MAX_RETRY_ATTEMPTS = 3;
public static final String DEFAULT_TIMEZONE = "Asia/Tokyo";
private static final Logger logger = LoggerFactory.getLogger(PayrollService.class);

// ❌ Bad
public static final int maxRetryAttempts = 3;
```

### パッケージ名

- **小文字のみ** (アンダースコア禁止)
- ドメイン名の逆順 + 機能名

```
com.example.attendance
com.example.attendance.api.controller
com.example.attendance.domain.service
com.example.attendance.infrastructure.repository
```

---

## パッケージ構成

### レイヤードアーキテクチャ

```
src/main/java/com/example/attendance/
│
├── api/                          # API層 (プレゼンテーション層)
│   ├── controller/               # REST APIコントローラ
│   │   ├── EmployeeController.java
│   │   └── PayrollController.java
│   ├── request/                  # リクエストDTO
│   │   ├── CreateEmployeeRequest.java
│   │   └── CalculatePayrollRequest.java
│   └── response/                 # レスポンスDTO
│       ├── ErrorResponse.java
│       └── PayrollResponse.java
│
├── domain/                       # ドメイン層 (ビジネスロジック)
│   ├── model/                    # ドメインモデル (エンティティ)
│   │   ├── Employee.java
│   │   ├── WorkRecord.java
│   │   └── Payroll.java
│   ├── service/                  # ドメインサービス
│   │   ├── PayrollCalculationService.java
│   │   ├── CalendarSyncService.java
│   │   └── SheetSyncService.java
│   ├── repository/               # リポジトリインターフェース
│   │   ├── EmployeeRepository.java
│   │   └── PayrollRepository.java
│   └── exception/                # ドメイン例外
│       ├── EmployeeNotFoundException.java
│       └── PayrollCalculationException.java
│
├── infrastructure/               # インフラ層
│   ├── repository/               # リポジトリ実装 (JPA)
│   │   └── jpa/
│   │       ├── EmployeeRepositoryImpl.java
│   │       └── PayrollRepositoryImpl.java
│   ├── external/                 # 外部API連携
│   │   ├── google/
│   │   │   ├── GoogleCalendarClient.java
│   │   │   └── GoogleSheetsClient.java
│   │   └── GoogleOAuthService.java
│   └── config/                   # 設定クラス
│       ├── SecurityConfig.java
│       ├── JwtConfig.java
│       └── DatabaseConfig.java
│
├── batch/                        # バッチ処理
│   ├── CalendarSyncJob.java
│   └── SheetSyncJob.java
│
├── security/                     # セキュリティ関連
│   ├── JwtTokenProvider.java
│   ├── CustomUserDetailsService.java
│   └── SecurityUtils.java
│
├── util/                         # ユーティリティ
│   ├── DateTimeUtils.java
│   └── EncryptionUtils.java
│
└── AttendanceApplication.java    # メインクラス
```

---

## クラス設計原則

### 1. 責務の分離

各クラスは単一の責務を持つこと。

```java
// ❌ Bad: 複数の責務を持つ
public class EmployeeService {
    public void saveEmployee(Employee employee) { }
    public void sendEmail(String email) { }  // メール送信は別クラスの責務
    public void logActivity(String message) { }  // ロギングは別クラスの責務
}

// ✅ Good: 単一責務
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;
    private final AuditLogger auditLogger;

    public void createEmployee(Employee employee) {
        employeeRepository.save(employee);
        emailService.sendWelcomeEmail(employee.getEmail());
        auditLogger.logEmployeeCreated(employee.getId());
    }
}
```

### 2. Immutableオブジェクト推奨

DTOやValueObjectは不変にする。

```java
// ✅ Good: Immutable DTO
@Value  // Lombok: すべてのフィールドをfinalにしてgetterのみ生成
@Builder
public class PayrollDto {
    String employeeId;
    LocalDate startDate;
    LocalDate endDate;
    double totalPayment;
}

// ❌ Bad: Mutable DTO
@Data  // すべてのフィールドにsetterが生成される
public class PayrollDto {
    private String employeeId;
    private double totalPayment;
}
```

### 3. コンストラクタインジェクション

依存性注入はコンストラクタで行う (フィールドインジェクション禁止)。

```java
// ✅ Good: コンストラクタインジェクション
@Service
@RequiredArgsConstructor  // Lombokでfinalフィールドのコンストラクタ自動生成
public class PayrollService {
    private final PayrollRepository payrollRepository;
    private final WorkRecordRepository workRecordRepository;

    public PayrollDto calculatePayroll(String employeeId) {
        // ビジネスロジック
    }
}

// ❌ Bad: フィールドインジェクション
@Service
public class PayrollService {
    @Autowired  // テストしにくい、不変性が保証されない
    private PayrollRepository payrollRepository;
}
```

---

## 例外処理

### 1. 例外クラスの階層構造

```java
// カスタム例外の基底クラス
public abstract class AttendanceException extends RuntimeException {
    protected AttendanceException(String message) {
        super(message);
    }

    protected AttendanceException(String message, Throwable cause) {
        super(message, cause);
    }
}

// ビジネスロジック例外
public class EmployeeNotFoundException extends AttendanceException {
    public EmployeeNotFoundException(String employeeId) {
        super(String.format("Employee not found: %s", employeeId));
    }
}

// 外部API例外
public class GoogleApiException extends AttendanceException {
    public GoogleApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 2. 例外のスロー

```java
// ✅ Good: 具体的な例外メッセージ
public Employee findEmployeeById(String employeeId) {
    return employeeRepository.findById(employeeId)
            .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
}

// ❌ Bad: 汎用的すぎる
public Employee findEmployeeById(String employeeId) {
    return employeeRepository.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Error"));
}
```

### 3. 例外のキャッチ

```java
// ✅ Good: 具体的な例外をキャッチして適切に処理
try {
    payrollService.calculatePayroll(employeeId);
} catch (EmployeeNotFoundException e) {
    logger.warn("Employee not found: {}", employeeId, e);
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found", e);
} catch (PayrollCalculationException e) {
    logger.error("Payroll calculation failed: {}", employeeId, e);
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Calculation failed", e);
}

// ❌ Bad: 汎用的すぎる、例外を握りつぶす
try {
    payrollService.calculatePayroll(employeeId);
} catch (Exception e) {
    // 何もしない (例外を握りつぶす)
}
```

### 4. チェック例外 vs 非チェック例外

- **ビジネスロジック例外**: `RuntimeException` を継承 (非チェック例外)
- **外部API例外**: `RuntimeException` を継承 (非チェック例外)
- **リトライ可能な例外**: 明示的にマーク

```java
@Retryable(
    value = {GoogleApiException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public List<CalendarEvent> fetchCalendarEvents(String calendarId) {
    // Google Calendar API呼び出し
}
```

---

## ロギング

### 1. SLF4J + Logback使用

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PayrollService {
    private static final Logger logger = LoggerFactory.getLogger(PayrollService.class);

    public PayrollDto calculatePayroll(String employeeId) {
        logger.info("Calculating payroll for employee: {}", employeeId);

        try {
            PayrollDto result = doCalculation(employeeId);
            logger.info("Payroll calculation completed: employeeId={}, totalPayment={}",
                    employeeId, result.getTotalPayment());
            return result;
        } catch (Exception e) {
            logger.error("Payroll calculation failed: employeeId={}", employeeId, e);
            throw e;
        }
    }
}
```

### 2. ログレベルの使い分け

| レベル | 用途 | 例 |
|--------|------|-----|
| **ERROR** | システムエラー、例外 | データベース接続失敗、外部API呼び出し失敗 |
| **WARN** | 警告、異常だが処理継続可能 | リトライ発生、デフォルト値使用 |
| **INFO** | 重要なビジネスイベント | ユーザーログイン、給与計算完了 |
| **DEBUG** | デバッグ情報 | メソッド開始/終了、変数の値 |
| **TRACE** | 詳細なデバッグ情報 | ループ内の処理、SQLパラメータ |

### 3. 構造化ロギング (JSON)

```java
// ✅ Good: 構造化されたログ
logger.info("Payroll calculated",
        kv("employeeId", employeeId),
        kv("totalPayment", totalPayment),
        kv("processingTimeMs", duration.toMillis()));

// 出力例 (JSON):
// {"timestamp":"2024-01-15T10:30:00Z","level":"INFO","message":"Payroll calculated","employeeId":"emp-001","totalPayment":150000,"processingTimeMs":234}

// ❌ Bad: 文字列連結
logger.info("Payroll calculated for " + employeeId + " with total payment " + totalPayment);
```

### 4. 個人情報のマスキング

```java
// ✅ Good: 個人情報をマスキング
logger.info("Email sent to: {}", maskEmail(email));

private String maskEmail(String email) {
    // example@test.com → e****e@test.com
    if (email == null || !email.contains("@")) {
        return "****";
    }
    String[] parts = email.split("@");
    String local = parts[0];
    return local.charAt(0) + "****" + local.charAt(local.length() - 1) + "@" + parts[1];
}

// ❌ Bad: 個人情報をそのまま出力
logger.info("Email sent to: {}", email);  // 禁止
```

---

## テストコード規約

### 1. テストクラスの命名

```
{テスト対象クラス名}Test.java
```

例:
- `PayrollService.java` → `PayrollServiceTest.java`
- `EmployeeController.java` → `EmployeeControllerTest.java`

### 2. テストメソッドの命名 (日本語推奨)

```java
@Test
@DisplayName("UT-PS-001: 正常系_固定単価適用")
void calculatePayroll_FixedWage_Success() {
    // テストコード
}

@Test
@DisplayName("UT-PS-002: 異常系_従業員が存在しない")
void calculatePayroll_EmployeeNotFound_ThrowsException() {
    // テストコード
}
```

### 3. AAA (Arrange-Act-Assert) パターン

```java
@Test
@DisplayName("UT-PS-001: 正常系_固定単価適用")
void calculatePayroll_FixedWage_Success() {
    // Arrange (準備)
    String employeeId = "emp-001";
    LocalDate startDate = LocalDate.of(2024, 1, 1);
    LocalDate endDate = LocalDate.of(2024, 1, 31);

    Employee employee = Employee.builder()
            .id(employeeId)
            .name("Test Employee")
            .build();

    when(employeeRepository.findById(employeeId))
            .thenReturn(Optional.of(employee));

    // Act (実行)
    PayrollDto result = payrollService.calculatePayroll(
            employeeId, startDate, endDate);

    // Assert (検証)
    assertThat(result).isNotNull();
    assertThat(result.getEmployeeId()).isEqualTo(employeeId);
    assertThat(result.getTotalPayment()).isGreaterThan(0);

    verify(employeeRepository).findById(employeeId);
}
```

### 4. AssertJの使用 (JUnit標準assertより推奨)

```java
// ✅ Good: AssertJ
assertThat(result.getTotalPayment()).isEqualTo(150000);
assertThat(result.getEmployeeId()).isNotNull();
assertThat(employees).hasSize(3)
        .extracting("name")
        .contains("Alice", "Bob", "Charlie");

// ❌ Bad: JUnit標準assert (非推奨)
assertEquals(150000, result.getTotalPayment());
assertNotNull(result.getEmployeeId());
```

### 5. モックの使用

```java
@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private WorkRecordRepository workRecordRepository;

    @InjectMocks
    private PayrollService payrollService;

    @Test
    void testCalculatePayroll() {
        // Mockの振る舞いを定義
        when(workRecordRepository.findByEmployeeId(anyString()))
                .thenReturn(Collections.emptyList());

        // テスト実行
        PayrollDto result = payrollService.calculatePayroll("emp-001");

        // Mockが呼ばれたことを検証
        verify(workRecordRepository).findByEmployeeId("emp-001");
    }
}
```

### 6. テストデータビルダー

```java
// テストデータビルダーパターン
public class EmployeeTestDataBuilder {
    private String id = "emp-001";
    private String name = "Test Employee";
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    public EmployeeTestDataBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public EmployeeTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public Employee build() {
        return Employee.builder()
                .id(id)
                .name(name)
                .status(status)
                .build();
    }
}

// 使用例
@Test
void testFindActiveEmployees() {
    Employee activeEmployee = new EmployeeTestDataBuilder()
            .withId("emp-001")
            .withName("Active Employee")
            .build();

    when(employeeRepository.findById("emp-001"))
            .thenReturn(Optional.of(activeEmployee));
}
```

---

## JavaDoc規約

### 1. クラスレベルのJavaDoc

```java
/**
 * 給与計算サービス.
 *
 * <p>勤務記録に基づいて従業員の給与を計算します。
 * 時給制・固定給制の両方に対応しています。
 *
 * @author Attendance Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PayrollService {
    // クラス実装
}
```

### 2. メソッドレベルのJavaDoc

```java
/**
 * 指定期間の給与を計算します.
 *
 * <p>従業員IDと期間を指定して給与を計算します。
 * 勤務記録が存在しない場合は空のPayrollDtoを返します。
 *
 * @param employeeId 従業員ID
 * @param startDate 計算開始日
 * @param endDate 計算終了日
 * @return 給与計算結果
 * @throws EmployeeNotFoundException 従業員が存在しない場合
 * @throws PayrollCalculationException 給与計算に失敗した場合
 */
public PayrollDto calculatePayroll(
        String employeeId,
        LocalDate startDate,
        LocalDate endDate) {
    // メソッド実装
}
```

### 3. JavaDocを書くべき対象

**必須**:
- すべてのpublicクラス
- すべてのpublicメソッド
- すべてのpublic定数

**任意**:
- privateメソッド (複雑なロジックの場合のみ)
- package-privateクラス

**不要**:
- getter/setter (Lombokで自動生成される場合)
- オーバーライドメソッド (親クラスに記述がある場合)

---

## セキュリティ要件

### 1. SQLインジェクション対策

```java
// ✅ Good: PreparedStatementを使用 (Spring Data JPAは自動的に対策済み)
@Query("SELECT e FROM Employee e WHERE e.name = :name")
List<Employee> findByName(@Param("name") String name);

// ❌ Bad: 文字列連結 (絶対禁止)
String sql = "SELECT * FROM employees WHERE name = '" + name + "'";
```

### 2. XSS対策

```java
// ✅ Good: Thymeleafのth:textを使用 (自動エスケープ)
<p th:text="${employee.name}"></p>

// ❌ Bad: th:utext (エスケープなし、禁止)
<p th:utext="${employee.name}"></p>
```

### 3. 認証・認可チェック

```java
// ✅ Good: メソッドレベルでセキュリティチェック
@PreAuthorize("hasRole('ADMIN')")
public void deleteEmployee(String employeeId) {
    employeeRepository.deleteById(employeeId);
}

// ✅ Good: カスタム権限チェック
@PreAuthorize("@securityService.canAccessEmployee(#employeeId)")
public EmployeeDto getEmployee(String employeeId) {
    // 実装
}
```

### 4. 機密情報の暗号化

```java
// ✅ Good: 暗号化して保存
@Service
public class EncryptionService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    public String encrypt(String plainText, SecretKey key) {
        // AES-256-GCM暗号化
    }

    public String decrypt(String encryptedText, SecretKey key) {
        // 復号化
    }
}

// ❌ Bad: 平文で保存
employee.setApiToken(token);  // トークンは暗号化すべき
```

### 5. ログに機密情報を出力しない

```java
// ✅ Good: 機密情報をマスク
logger.info("User logged in: email={}", maskEmail(email));

// ❌ Bad: 機密情報をそのまま出力
logger.info("User logged in: email={}, password={}", email, password);  // 絶対禁止
```

---

## チェックリスト

コードレビュー時に以下をチェックする:

### コード品質
- [ ] クラス名・メソッド名・変数名が適切か
- [ ] メソッドは単一責任を持っているか
- [ ] 同じロジックが重複していないか (DRY原則)
- [ ] マジックナンバーを定数化しているか

### テスト
- [ ] ユニットテストが書かれているか
- [ ] カバレッジ80%以上を達成しているか
- [ ] 正常系・異常系の両方をテストしているか

### セキュリティ
- [ ] SQLインジェクション対策が施されているか
- [ ] XSS対策が施されているか
- [ ] 認証・認可チェックが適切か
- [ ] ログに機密情報が含まれていないか

### ロギング
- [ ] 適切なログレベルで出力しているか
- [ ] 個人情報をマスキングしているか
- [ ] 構造化ロギングを使用しているか

### 例外処理
- [ ] 適切な例外クラスをスローしているか
- [ ] 例外を握りつぶしていないか
- [ ] 例外メッセージが具体的か

### JavaDoc
- [ ] publicクラス・メソッドにJavaDocが書かれているか
- [ ] パラメータと戻り値の説明があるか
- [ ] スローされる例外が記載されているか

---

## 参考リンク

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Effective Java (Third Edition)](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/)
- [Spring Boot Best Practices](https://spring.io/guides)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
