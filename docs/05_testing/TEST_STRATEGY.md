# テスト戦略書

- バージョン: 1.0
- 作成日: 2025年12月30日
- 最終更新日: 2025年12月30日
- 作成者: Project Manager

---

## 1. 概要

本ドキュメントは、勤怠管理システムの包括的なテスト戦略を定義する。単体テスト、統合テスト、E2Eテスト、性能テスト、セキュリティテストの計画と実施方法を記述する。

## 2. テスト方針

### 2.1. テストピラミッド

```
        /\
       /E2E\       ← 少数の重要なシナリオ（5-10件）
      /------\
     /統合テスト\    ← 中程度の数（50-100件）
    /----------\
   /  単体テスト  \   ← 多数（500+件）、カバレッジ80%以上
  /--------------\
```

### 2.2. テストレベルと目標

| テストレベル | 目標 | 実施タイミング |
|------------|------|--------------|
| 単体テスト | カバレッジ80%以上 | 各PR作成時 |
| 統合テスト | 主要なAPI・DB連携の検証 | 各PR作成時 |
| E2Eテスト | 重要なユーザーシナリオの検証 | mainブランチマージ前 |
| 性能テスト | NFR-003の性能要件を満たす | リリース前 |
| セキュリティテスト | OWASP Top 10への対策確認 | リリース前 |

## 3. 単体テスト（Unit Test）

### 3.1. テストツール

- **フレームワーク**: JUnit 5 (Jupiter)
- **モック**: Mockito
- **アサーション**: AssertJ
- **カバレッジ**: JaCoCo

### 3.2. テスト対象

- すべてのServiceクラス
- すべてのRepositoryクラス（カスタムクエリ）
- すべてのUtilityクラス
- すべてのValidatorクラス

### 3.3. 単体テストの例

#### PayrollServiceのテスト

```java
@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private WorkRecordRepository workRecordRepository;

    @Mock
    private WorkTypeRepository workTypeRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private HourlyWageRepository hourlyWageRepository;

    @InjectMocks
    private PayrollService payrollService;

    @Test
    @DisplayName("給与計算_正常系_動的単価適用")
    void calculatePayroll_正常系_動的単価適用() {
        // Given
        Integer employeeId = 1;
        LocalDate start = LocalDate.of(2025, 11, 1);
        LocalDate end = LocalDate.of(2025, 11, 30);

        Employee employee = createTestEmployee(employeeId, "Taro Yamada");
        List<WorkRecord> workRecords = Arrays.asList(
            createWorkRecord(1, employeeId, "個別（A）", 60),  // 中学生、60分
            createWorkRecord(2, employeeId, "個別（B）", 120)  // 高校生、120分
        );

        WorkType workType = createWorkType(1, "個別指導", "個別", "STUDENT_LEVEL_BASED");
        Student studentA = createStudent(1, "A", 2);  // 中学生
        Student studentB = createStudent(2, "B", 3);  // 高校生
        HourlyWage wageMiddle = createHourlyWage(1, 1, 2, 3000);  // 中学生: 3000円
        HourlyWage wageHigh = createHourlyWage(2, 1, 3, 3500);    // 高校生: 3500円

        when(workRecordRepository.findByEmployeeIdAndTimeBetween(employeeId, start, end))
            .thenReturn(workRecords);
        when(workTypeRepository.findByCalendarKeyword("個別")).thenReturn(Optional.of(workType));
        when(studentRepository.findByName("A")).thenReturn(Optional.of(studentA));
        when(studentRepository.findByName("B")).thenReturn(Optional.of(studentB));
        when(hourlyWageRepository.findByWorkTypeIdAndStudentLevelId(1, 2))
            .thenReturn(Optional.of(wageMiddle));
        when(hourlyWageRepository.findByWorkTypeIdAndStudentLevelId(1, 3))
            .thenReturn(Optional.of(wageHigh));

        // When
        PayrollDto result = payrollService.calculatePayroll(employeeId, start, end);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmployee().getId()).isEqualTo(employeeId);
        assertThat(result.getTotalWorkMinutes()).isEqualTo(180);  // 60 + 120
        assertThat(result.getTotalPayment().getAmount()).isEqualTo(10000);  // 3000 + 7000

        assertThat(result.getPaymentDetails()).hasSize(2);
        assertThat(result.getPaymentDetails().get(0).getStudentLevelName()).isEqualTo("中学生");
        assertThat(result.getPaymentDetails().get(0).getSubtotal().getAmount()).isEqualTo(3000);
        assertThat(result.getPaymentDetails().get(1).getStudentLevelName()).isEqualTo("高校生");
        assertThat(result.getPaymentDetails().get(1).getSubtotal().getAmount()).isEqualTo(7000);
    }

    @Test
    @DisplayName("給与計算_異常系_従業員が存在しない")
    void calculatePayroll_異常系_従業員が存在しない() {
        // Given
        Integer employeeId = 999;
        LocalDate start = LocalDate.of(2025, 11, 1);
        LocalDate end = LocalDate.of(2025, 11, 30);

        when(workRecordRepository.findByEmployeeIdAndTimeBetween(employeeId, start, end))
            .thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> payrollService.calculatePayroll(employeeId, start, end))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Employee with ID 999 does not exist");
    }

    @Test
    @DisplayName("給与計算_境界値_期間開始日と終了日が同じ")
    void calculatePayroll_境界値_期間開始日と終了日が同じ() {
        // Given
        Integer employeeId = 1;
        LocalDate date = LocalDate.of(2025, 11, 15);

        when(workRecordRepository.findByEmployeeIdAndTimeBetween(employeeId, date, date))
            .thenReturn(Collections.emptyList());

        // When
        PayrollDto result = payrollService.calculatePayroll(employeeId, date, date);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalWorkMinutes()).isEqualTo(0);
        assertThat(result.getTotalPayment().getAmount()).isEqualTo(0);
    }

    // テストヘルパーメソッド
    private Employee createTestEmployee(Integer id, String name) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(name);
        return employee;
    }

    private WorkRecord createWorkRecord(Integer id, Integer employeeId, String title, int minutes) {
        WorkRecord record = new WorkRecord();
        record.setId(id);
        record.setEmployeeId(employeeId);
        record.setEventTitle(title);
        record.setStartTime(Instant.now());
        record.setEndTime(Instant.now().plus(minutes, ChronoUnit.MINUTES));
        return record;
    }

    // ... その他のヘルパーメソッド
}
```

### 3.4. カバレッジ目標

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>  <!-- 80%以上 -->
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 4. 統合テスト（Integration Test）

### 4.1. テストツール

- **フレームワーク**: Spring Boot Test
- **データベース**: Testcontainers (PostgreSQL)
- **APIテスト**: MockMvc / REST Assured

### 4.2. テスト対象

- REST APIエンドポイント（すべて）
- データベース連携（Repository層）
- 外部API連携（モック使用）

### 4.3. Testcontainersの設定

```java
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class EmployeeControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
    }

    @Test
    @DisplayName("従業員一覧取得_正常系")
    void getEmployees_正常系() throws Exception {
        // Given
        Employee employee1 = createEmployee("user1@example.com", "User 1");
        Employee employee2 = createEmployee("user2@example.com", "User 2");
        employeeRepository.saveAll(Arrays.asList(employee1, employee2));

        // When & Then
        mockMvc.perform(get("/api/v1/employees")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].name").value("User 1"))
            .andExpect(jsonPath("$.content[1].name").value("User 2"))
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("従業員作成_正常系")
    @WithMockUser(roles = "ADMIN")
    void createEmployee_正常系() throws Exception {
        // Given
        String requestBody = """
            {
              "email": "new@example.com",
              "name": "New Employee",
              "role": "USER"
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.email").value("new@example.com"))
            .andExpect(jsonPath("$.name").value("New Employee"));

        // データベース確認
        List<Employee> employees = employeeRepository.findAll();
        assertThat(employees).hasSize(1);
        assertThat(employees.get(0).getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("従業員作成_異常系_バリデーションエラー")
    @WithMockUser(roles = "ADMIN")
    void createEmployee_異常系_バリデーションエラー() throws Exception {
        // Given: メールアドレスが無効
        String requestBody = """
            {
              "email": "invalid-email",
              "name": "Test User"
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value(containsString("validation-failed")))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0].field").value("email"))
            .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));
    }

    private Employee createEmployee(String email, String name) {
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setName(name);
        employee.setRole("USER");
        employee.setIsActive(true);
        return employee;
    }
}
```

## 5. E2Eテスト（End-to-End Test）

### 5.1. テストツール

- **フレームワーク**: REST Assured
- **テスト対象**: 本番に近い環境（Dockerコンテナ）

### 5.2. 重要シナリオ

#### シナリオ1: ログイン → カレンダー同期 → 給与計算 → レポート取得

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PayrollE2ETest {

    @LocalServerPort
    private int port;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        RestAssured.port = port;
    }

    @Test
    @DisplayName("E2Eシナリオ: ログイン → 給与計算 → レポート取得")
    void payrollCalculationE2E() {
        // 1. ログイン
        String accessToken = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "email": "admin@example.com",
                  "password": "test-password"
                }
                """)
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("accessToken");

        // 2. カレンダー手動同期
        given()
            .header("Authorization", "Bearer " + accessToken)
        .when()
            .post(baseUrl + "/api/v1/syncs/employees/1")
        .then()
            .statusCode(202);

        // 3. 同期完了を待機（ポーリング）
        await().atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .until(() -> isSyncCompleted(1, accessToken));

        // 4. 給与計算
        PayrollDto payroll = given()
            .header("Authorization", "Bearer " + accessToken)
            .queryParam("employeeId", 1)
            .queryParam("startDate", "2025-11-01")
            .queryParam("endDate", "2025-11-30")
        .when()
            .get(baseUrl + "/api/v1/payrolls")
        .then()
            .statusCode(200)
            .extract()
            .as(PayrollDto.class);

        // 5. 検証
        assertThat(payroll.getEmployee().getId()).isEqualTo(1);
        assertThat(payroll.getTotalWorkMinutes()).isGreaterThan(0);
        assertThat(payroll.getTotalPayment().getAmount()).isGreaterThan(0);
        assertThat(payroll.getPaymentDetails()).isNotEmpty();
    }

    private boolean isSyncCompleted(Integer employeeId, String accessToken) {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
        .when()
            .get(baseUrl + "/api/v1/admin/sync-status?employeeId=" + employeeId);

        return response.statusCode() == 200 &&
               "COMPLETED".equals(response.path("status"));
    }
}
```

## 6. 性能テスト（Performance Test）

### 6.1. テストツール

- **ツール**: Apache JMeter / Gatling
- **目標**: NFR-003の性能要件を満たす

### 6.2. 性能要件（再掲）

| API | 目標レスポンスタイム |
|-----|-------------------|
| 通常のAPI | 平均500ms以内、P95で1秒以内 |
| 給与計算API | 最大5秒以内 |
| バッチ処理 | 最大50分以内 |

### 6.3. JMeterテストシナリオ

```xml
<!-- test-plan.jmx（概要） -->
<jmeterTestPlan>
  <hashTree>
    <TestPlan>
      <stringProp name="TestPlan.comments">給与計算API性能テスト</stringProp>
      <ThreadGroup>
        <stringProp name="ThreadGroup.num_threads">100</stringProp>  <!-- 100同時ユーザー -->
        <stringProp name="ThreadGroup.ramp_time">60</stringProp>     <!-- 60秒でランプアップ -->
        <stringProp name="ThreadGroup.duration">300</stringProp>     <!-- 5分間実行 -->
      </ThreadGroup>
      <HTTPSamplerProxy>
        <stringProp name="HTTPSampler.path">/api/v1/payrolls</stringProp>
        <stringProp name="HTTPSampler.method">GET</stringProp>
      </HTTPSamplerProxy>
    </TestPlan>
  </hashTree>
</jmeterTestPlan>
```

#### 実行コマンド
```bash
jmeter -n -t test-plan.jmx -l results.jtl -e -o report/
```

### 6.4. Gatlingテストスクリプト

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class PayrollSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .authorizationHeader("Bearer ${accessToken}")

  val scn = scenario("Payroll Calculation")
    .exec(
      http("Get Payroll")
        .get("/api/v1/payrolls")
        .queryParam("employeeId", "1")
        .queryParam("startDate", "2025-11-01")
        .queryParam("endDate", "2025-11-30")
        .check(status.is(200))
        .check(responseTimeInMillis.lte(5000))  // 5秒以内
    )

  setUp(
    scn.inject(
      rampUsers(100) during (60 seconds),  // 100ユーザーを60秒でランプアップ
      constantUsersPerSec(50) during (5 minutes)  // 5分間、秒間50リクエスト
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.max.lt(5000),  // 最大5秒
     global.responseTime.mean.lt(500),  // 平均500ms
     global.successfulRequests.percent.gt(99)  // 成功率99%以上
   )
}
```

## 7. セキュリティテスト（Security Test)

### 7.1. テストツール

- **OWASP ZAP**: 自動脆弱性スキャン
- **SonarQube**: 静的コード解析
- **OWASP Dependency Check**: 依存ライブラリの脆弱性スキャン

### 7.2. OWASP ZAPスキャン

```bash
# Docker経由でOWASP ZAPを実行
docker run -t owasp/zap2docker-stable zap-baseline.py \
  -t http://localhost:8080/api \
  -r zap-report.html
```

### 7.3. SonarQubeスキャン

```bash
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=attendance-management \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=${SONAR_TOKEN}
```

### 7.4. 手動セキュリティテスト

| テスト項目 | 実施内容 |
|----------|---------|
| SQLインジェクション | すべてのAPIエンドポイントでSQL特殊文字を送信 |
| XSS | `<script>alert(1)</script>` などを送信 |
| 認証バイパス | 無効なトークンでAPIにアクセス |
| 権限昇格 | USERロールでADMIN専用APIにアクセス |
| CSRF | SameSite Cookie設定の確認 |

## 8. CI/CDでのテスト自動化

### 8.1. GitHub Actionsワークフロー

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Run unit tests
        run: mvn test

      - name: Run integration tests
        run: mvn verify -P integration-test

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml

      - name: Run OWASP Dependency Check
        run: mvn dependency-check:check

      - name: SonarQube Scan
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn sonar:sonar
```

## 9. テストデータ管理

### 9.1. テストフィクスチャ

```java
@Component
public class TestDataFactory {

    public Employee createEmployee(String email, String name, String role) {
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setName(name);
        employee.setRole(role);
        employee.setIsActive(true);
        return employee;
    }

    public WorkRecord createWorkRecord(Integer employeeId, String title, Instant start, Instant end) {
        WorkRecord record = new WorkRecord();
        record.setEmployeeId(employeeId);
        record.setEventTitle(title);
        record.setStartTime(start);
        record.setEndTime(end);
        record.setGoogleEventId(UUID.randomUUID().toString());
        return record;
    }

    // ... その他のファクトリメソッド
}
```

## 10. テストレポート

### 10.1. カバレッジレポート

- **JaCoCo**: `target/site/jacoco/index.html`
- **目標**: LINE coverage 80%以上

### 10.2. テスト結果レポート

- **JUnit**: `target/surefire-reports/`
- **CI**: GitHub Actions Summaryに結果を表示

## 11. まとめ

本テスト戦略は以下を実現する:

1. ✅ **品質保証**: 80%以上のコードカバレッジ
2. ✅ **回帰防止**: すべてのPRで自動テスト実行
3. ✅ **性能保証**: JMeter/Gatlingで性能要件を検証
4. ✅ **セキュリティ**: OWASP ZAP、SonarQubeで脆弱性スキャン
5. ✅ **CI/CD統合**: GitHub Actionsで完全自動化

すべてのテストは定期的に実行され、品質の維持と向上を保証する。
