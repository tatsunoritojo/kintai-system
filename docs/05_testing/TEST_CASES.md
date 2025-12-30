# テストケース定義書

- バージョン: 1.0
- 作成日: 2025年12月30日
- 最終更新日: 2025年12月30日
- 作成者: Project Manager

---

## 1. 概要

本ドキュメントは、勤怠管理システムの包括的なテストケースを定義する。単体テスト、統合テスト、E2Eテスト、セキュリティテスト、パフォーマンステストの各レベルで実施すべきテストケースを記載する。

---

## 2. テストレベル概要

| テストレベル | 目的 | ツール | カバレッジ目標 | 実行タイミング |
|------------|------|-------|-------------|-------------|
| **単体テスト** | 個別クラス・メソッドの動作検証 | JUnit 5, Mockito | 80%以上 | コミット毎 |
| **統合テスト** | コンポーネント間の連携検証 | Spring Boot Test, Testcontainers | 60%以上 | PR作成時 |
| **E2Eテスト** | エンドツーエンドのシナリオ検証 | REST Assured | 主要シナリオ網羅 | リリース前 |
| **セキュリティテスト** | 脆弱性検証 | OWASP ZAP, SonarQube | OWASP Top 10対応 | 週次 |
| **パフォーマンステスト** | 性能要件検証 | JMeter, Gatling | NFR-002準拠 | リリース前 |

---

## 3. 単体テスト（Unit Test）

### 3.1. PayrollService テストケース

**テスト対象**: `com.example.attendance.service.PayrollService`

| ID | テストケース名 | 説明 | 入力 | 期待結果 |
|----|-------------|------|------|---------|
| **UT-PS-001** | 正常系_固定単価適用 | 固定単価の勤務形態で給与計算 | workType: 自習室(固定1200円), 勤務時間: 60分 | totalPayment: 1200円 |
| **UT-PS-002** | 正常系_生徒レベル別単価適用 | 生徒レベルに応じた動的単価で計算 | workType: 個別指導, student: 中学生, 勤務時間: 60分 | 中学生単価(3000円)が適用される |
| **UT-PS-003** | 正常系_複数勤務記録の集計 | 複数の勤務記録を正しく集計 | 3件の勤務記録（個別×2、自習室×1） | 各勤務形態別に集計され、合計額が正しい |
| **UT-PS-004** | 正常系_未払い記録のみ計算 | is_paid=falseの記録のみ計算 | is_paid=true 2件, is_paid=false 3件 | 3件のみ計算対象 |
| **UT-PS-005** | 異常系_従業員が存在しない | 存在しない従業員IDで計算 | employeeId: 9999 | `EmployeeNotFoundException` |
| **UT-PS-006** | 異常系_期間が不正 | 開始日 > 終了日 | start: 2025-12-31, end: 2025-12-01 | `IllegalArgumentException` |
| **UT-PS-007** | 異常系_単価が見つからない | 対応する単価マスタが存在しない | workType: 個別, studentLevel: 新規レベル | 警告付きでデフォルト単価適用 |
| **UT-PS-008** | 境界値_0分勤務 | 勤務時間が0分 | start: 10:00, end: 10:00 | totalPayment: 0円 |
| **UT-PS-009** | 境界値_6時間勤務 | 最大拘束時間ちょうど | 勤務時間: 360分 | 正常に計算される |
| **UT-PS-010** | 境界値_6時間超過 | 最大拘束時間超過 | 勤務時間: 361分 | 警告付きで計算される |

**実装例**:

```java
@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private WorkRecordRepository workRecordRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private HourlyWageRepository hourlyWageRepository;

    @InjectMocks
    private PayrollService payrollService;

    @Test
    @DisplayName("UT-PS-001: 正常系_固定単価適用")
    void calculatePayroll_FixedWage_Success() {
        // Given
        Integer employeeId = 1;
        LocalDate start = LocalDate.of(2025, 11, 1);
        LocalDate end = LocalDate.of(2025, 11, 30);

        Employee employee = Employee.builder()
            .id(employeeId)
            .name("山田太郎")
            .email("yamada@example.com")
            .build();

        WorkType workType = WorkType.builder()
            .id(1)
            .name("自習室")
            .rateType(RateType.FIXED)
            .fixedWage(1200)
            .build();

        WorkRecord record = WorkRecord.builder()
            .id(1)
            .employeeId(employeeId)
            .startTime(Instant.parse("2025-11-15T10:00:00Z"))
            .endTime(Instant.parse("2025-11-15T11:00:00Z"))
            .workTypeId(1)
            .calculatedMinutes(60)
            .isPaid(false)
            .build();

        when(employeeRepository.findById(employeeId))
            .thenReturn(Optional.of(employee));
        when(workRecordRepository.findByEmployeeIdAndPeriod(employeeId, start, end))
            .thenReturn(List.of(record));
        when(workTypeRepository.findById(1))
            .thenReturn(Optional.of(workType));

        // When
        PayrollDto result = payrollService.calculatePayroll(employeeId, start, end);

        // Then
        assertThat(result.getSummary().getTotalPayment()).isEqualTo(1200);
        assertThat(result.getSummary().getTotalWorkMinutes()).isEqualTo(60);
        assertThat(result.getPaymentDetails()).hasSize(1);
        assertThat(result.getPaymentDetails().get(0).getWorkTypeName()).isEqualTo("自習室");
        assertThat(result.getPaymentDetails().get(0).getSubtotal()).isEqualTo(1200);
    }

    @Test
    @DisplayName("UT-PS-005: 異常系_従業員が存在しない")
    void calculatePayroll_EmployeeNotFound_ThrowsException() {
        // Given
        Integer employeeId = 9999;
        LocalDate start = LocalDate.of(2025, 11, 1);
        LocalDate end = LocalDate.of(2025, 11, 30);

        when(employeeRepository.findById(employeeId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> payrollService.calculatePayroll(employeeId, start, end))
            .isInstanceOf(EmployeeNotFoundException.class)
            .hasMessageContaining("Employee with ID 9999 not found");
    }
}
```

---

### 3.2. CalendarSyncService テストケース

**テスト対象**: `com.example.attendance.service.CalendarSyncService`

| ID | テストケース名 | 説明 | 入力 | 期待結果 |
|----|-------------|------|------|---------|
| **UT-CS-001** | 正常系_カレンダーイベント同期 | Google Calendarからイベントを取得し、DBに保存 | 3件の新規イベント | 3件のwork_recordsが作成される |
| **UT-CS-002** | 正常系_重複イベントスキップ | 既にDBに存在するイベントは無視 | google_event_id重複 | 新規0件、重複3件 |
| **UT-CS-003** | 正常系_生徒名抽出 | イベントタイトルから生徒名を抽出 | "個別(山田太郎)" | extractedStudentName: "山田太郎" |
| **UT-CS-004** | 正常系_勤務形態マッチング | キーワードで勤務形態を特定 | "個別(A)" → 個別指導 | workTypeId: 1 |
| **UT-CS-005** | 異常系_Google API エラー | API呼び出しが失敗 | GoogleApiException | リトライ後、例外をスロー |
| **UT-CS-006** | 異常系_リフレッシュトークンなし | 従業員のトークンが未設定 | google_refresh_token = null | `TokenNotFoundException` |
| **UT-CS-007** | 境界値_0件のイベント | カレンダーにイベントなし | イベント0件 | 正常終了、新規0件 |
| **UT-CS-008** | 境界値_大量イベント | 一度に100件のイベント | 100件のイベント | すべて正常に処理される |

---

### 3.3. ValidationService テストケース

| ID | テストケース名 | 説明 | 入力 | 期待結果 |
|----|-------------|------|------|---------|
| **UT-VS-001** | 正常系_Email形式検証 | 正しいメールアドレス | "user@example.com" | true |
| **UT-VS-002** | 異常系_Email形式不正 | 不正なメールアドレス | "invalid-email" | `ValidationException` |
| **UT-VS-003** | 正常系_勤務時間検証 | 開始 < 終了 | start: 10:00, end: 11:00 | true |
| **UT-VS-004** | 異常系_勤務時間逆転 | 開始 > 終了 | start: 11:00, end: 10:00 | `ValidationException` |
| **UT-VS-005** | 境界値_6時間ちょうど | 最大拘束時間 | 勤務時間: 360分 | true |
| **UT-VS-006** | 境界値_6時間超過 | 最大拘束時間超過 | 勤務時間: 361分 | 警告を返す |

---

## 4. 統合テスト（Integration Test）

### 4.1. API統合テストケース

**テスト対象**: REST APIエンドポイント全体

#### 4.1.1. 認証エンドポイント

| ID | テストケース名 | Method | Path | 入力 | 期待結果 |
|----|-------------|--------|------|------|---------|
| **IT-AUTH-001** | 正常系_OAuth認可フロー開始 | GET | `/api/v1/auth/login` | - | 302 Redirect to Google |
| **IT-AUTH-002** | 正常系_OAuthコールバック | GET | `/api/v1/auth/callback?code=xxx` | 認可コード | 200 OK, JWTトークン返却 |
| **IT-AUTH-003** | 正常系_トークンリフレッシュ | POST | `/api/v1/auth/refresh` | リフレッシュトークン | 200 OK, 新しいアクセストークン |
| **IT-AUTH-004** | 正常系_ログアウト | POST | `/api/v1/auth/logout` | アクセストークン | 200 OK, トークン無効化 |
| **IT-AUTH-005** | 異常系_無効なトークン | POST | `/api/v1/auth/refresh` | 無効なトークン | 401 Unauthorized |

**実装例**:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthenticationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("attendance_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword());
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("IT-AUTH-002: 正常系_OAuthコールバック")
    void oauthCallback_Success_ReturnsJwtToken() {
        // Given
        String authCode = "valid_auth_code";

        // When
        ResponseEntity<TokenResponse> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/auth/callback?code=" + authCode,
            TokenResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();
        assertThat(response.getBody().getRefreshToken()).isNotBlank();
        assertThat(response.getBody().getExpiresIn()).isEqualTo(900); // 15分
    }
}
```

---

#### 4.1.2. 従業員管理エンドポイント

| ID | テストケース名 | Method | Path | 入力 | 期待結果 |
|----|-------------|--------|------|------|---------|
| **IT-EMP-001** | 正常系_従業員一覧取得 | GET | `/api/v1/employees` | - | 200 OK, PagedResultDto |
| **IT-EMP-002** | 正常系_従業員詳細取得 | GET | `/api/v1/employees/1` | - | 200 OK, EmployeeDto |
| **IT-EMP-003** | 正常系_従業員作成 | POST | `/api/v1/employees` | email, name | 201 Created, EmployeeDto |
| **IT-EMP-004** | 正常系_従業員更新 | PUT | `/api/v1/employees/1` | name変更 | 200 OK, 更新されたEmployeeDto |
| **IT-EMP-005** | 正常系_従業員削除 | DELETE | `/api/v1/employees/1` | - | 204 No Content |
| **IT-EMP-006** | 異常系_存在しない従業員取得 | GET | `/api/v1/employees/9999` | - | 404 Not Found, RFC 7807エラー |
| **IT-EMP-007** | 異常系_メール重複 | POST | `/api/v1/employees` | 既存メール | 409 Conflict |
| **IT-EMP-008** | 異常系_バリデーションエラー | POST | `/api/v1/employees` | email空欄 | 400 Bad Request, バリデーションエラー |
| **IT-EMP-009** | 権限_ADMIN以外は他人を削除不可 | DELETE | `/api/v1/employees/2` | USERロール | 403 Forbidden |

---

#### 4.1.3. 給与計算エンドポイント

| ID | テストケース名 | Method | Path | 入力 | 期待結果 |
|----|-------------|--------|------|------|---------|
| **IT-PAY-001** | 正常系_給与計算実行 | GET | `/api/v1/payrolls?employeeId=1&startDate=2025-11-01&endDate=2025-11-30` | - | 200 OK, PayrollDto |
| **IT-PAY-002** | 正常系_複数勤務形態の集計 | GET | `/api/v1/payrolls?...` | 個別×2、自習室×1 | 勤務形態別に集計 |
| **IT-PAY-003** | 正常系_未払い記録のみ計算 | GET | `/api/v1/payrolls?...` | - | is_paid=falseのみ |
| **IT-PAY-004** | 異常系_存在しない従業員 | GET | `/api/v1/payrolls?employeeId=9999&...` | - | 404 Not Found |
| **IT-PAY-005** | 異常系_不正な期間 | GET | `/api/v1/payrolls?startDate=2025-12-31&endDate=2025-12-01` | - | 400 Bad Request |
| **IT-PAY-006** | 権限_自分の給与のみ閲覧可 | GET | `/api/v1/payrolls?employeeId=2` | USERロール(employeeId=1) | 403 Forbidden |

---

### 4.2. データベース統合テストケース

| ID | テストケース名 | 説明 | 期待結果 |
|----|-------------|------|---------|
| **IT-DB-001** | Flyway マイグレーション | すべてのマイグレーションスクリプトが正常実行 | エラーなし、全テーブル作成 |
| **IT-DB-002** | トリガー_updated_at自動更新 | レコード更新時にupdated_atが自動更新 | updated_atが現在時刻に更新 |
| **IT-DB-003** | 制約_work_records時刻範囲 | start_time >= end_time で挿入 | CHECK制約違反エラー |
| **IT-DB-004** | 制約_hourly_wages正の値 | wage = -1000 で挿入 | CHECK制約違反エラー |
| **IT-DB-005** | インデックス_work_records検索 | employee_id, start_timeで検索 | インデックスが使用される（EXPLAIN確認） |
| **IT-DB-006** | 外部キー_cascade削除 | employeeを削除 | 関連work_recordsも削除 |

---

## 5. E2Eテスト（End-to-End Test）

### 5.1. ユーザーシナリオテスト

#### シナリオ1: 新規従業員の登録から給与計算まで

| ステップ | アクション | 期待結果 |
|---------|----------|---------|
| 1 | 管理者がログイン | 認証成功、ダッシュボード表示 |
| 2 | 新規従業員を登録 | 従業員レコード作成、メール: test@example.com |
| 3 | 従業員がGoogle OAuth認証 | リフレッシュトークン保存 |
| 4 | CalendarSyncJobを手動実行 | Google Calendarから勤務記録取得、3件のwork_records作成 |
| 5 | 給与計算API呼び出し | PayrollDto返却、totalPayment: 12000円 |
| 6 | work_recordsを「支払済み」にマーク | is_paid = true に更新 |

**REST Assured実装例**:

```java
@Test
@DisplayName("E2E-001: 新規従業員の登録から給与計算まで")
void newEmployeeToPayrollCalculation() {
    // Step 1: 管理者ログイン
    String adminToken = given()
        .contentType(ContentType.JSON)
        .body(Map.of("email", "admin@example.com", "password", "admin123"))
    .when()
        .post("/api/v1/auth/login")
    .then()
        .statusCode(200)
        .extract().path("accessToken");

    // Step 2: 新規従業員登録
    Integer employeeId = given()
        .header("Authorization", "Bearer " + adminToken)
        .contentType(ContentType.JSON)
        .body(Map.of(
            "email", "test@example.com",
            "name", "テスト太郎"
        ))
    .when()
        .post("/api/v1/employees")
    .then()
        .statusCode(201)
        .extract().path("id");

    // Step 3: OAuth認証（省略: モック使用）

    // Step 4: CalendarSyncJob実行
    given()
        .header("Authorization", "Bearer " + adminToken)
    .when()
        .post("/api/v1/admin/batch/calendar-sync")
    .then()
        .statusCode(202);

    // Step 5: 給与計算
    given()
        .header("Authorization", "Bearer " + adminToken)
        .queryParam("employeeId", employeeId)
        .queryParam("startDate", "2025-11-01")
        .queryParam("endDate", "2025-11-30")
    .when()
        .get("/api/v1/payrolls")
    .then()
        .statusCode(200)
        .body("summary.totalPayment", equalTo(12000))
        .body("paymentDetails", hasSize(2));
}
```

---

#### シナリオ2: 権限による制御確認

| ステップ | アクション | 期待結果 |
|---------|----------|---------|
| 1 | USERロールでログイン | 認証成功 |
| 2 | 自分の給与を閲覧 | 200 OK、給与情報表示 |
| 3 | 他人の給与を閲覧試行 | 403 Forbidden |
| 4 | 従業員削除試行 | 403 Forbidden |
| 5 | ADMINロールでログイン | 認証成功 |
| 6 | 他人の給与を閲覧 | 200 OK、給与情報表示 |
| 7 | 従業員削除 | 204 No Content |

---

#### シナリオ3: エラーハンドリング確認

| ステップ | アクション | 期待結果 |
|---------|----------|---------|
| 1 | 存在しない従業員IDで給与計算 | 404 Not Found, RFC 7807形式エラー |
| 2 | 無効なトークンでAPI呼び出し | 401 Unauthorized |
| 3 | バリデーションエラー（メール不正） | 400 Bad Request, errorsフィールドに詳細 |
| 4 | Google API障害時のカレンダー同期 | 502 Bad Gateway, リトライ後失敗 |

---

## 6. セキュリティテスト

### 6.1. OWASP Top 10 対応テスト

| ID | カテゴリ | テストケース | 検証方法 | 期待結果 |
|----|---------|------------|---------|---------|
| **ST-001** | Broken Access Control | 他ユーザーのリソースへのアクセス試行 | USER権限で他人の給与を取得 | 403 Forbidden |
| **ST-002** | Cryptographic Failures | HTTPSなしでのアクセス | HTTPでAPI呼び出し | 自動的にHTTPSにリダイレクト |
| **ST-003** | Injection | SQLインジェクション試行 | `employeeId=1' OR '1'='1` | JPA使用のため無効化、エラーなし |
| **ST-004** | Insecure Design | レート制限確認 | 1分間に101回リクエスト | 101回目で429 Too Many Requests |
| **ST-005** | Security Misconfiguration | 不要なHTTPメソッド | OPTIONS, TRACE | 405 Method Not Allowed |
| **ST-006** | Vulnerable Components | 脆弱な依存ライブラリ | OWASP Dependency Check | Criticalな脆弱性0件 |
| **ST-007** | Authentication Failures | ブルートフォース攻撃 | 10回連続ログイン失敗 | アカウントロック |
| **ST-008** | Software Integrity | 改ざんされたJWTトークン | 署名を改ざん | 401 Unauthorized |
| **ST-009** | Logging Failures | 認証失敗ログの記録 | 認証失敗を発生させる | audit_logsに記録される |
| **ST-010** | SSRF | 外部URLへのリクエスト | Google API以外へのアクセス試行 | ホワイトリストで制限 |

### 6.2. ペネトレーションテスト

**OWASP ZAP自動スキャン**:

```bash
# ZAPでAPIをスキャン
docker run -v $(pwd):/zap/wrk/:rw -t owasp/zap2docker-stable \
    zap-api-scan.py \
    -t https://api.example.com/openapi.json \
    -f openapi \
    -r zap-report.html
```

**期待結果**: High以上のアラート0件

---

## 7. パフォーマンステスト

### 7.1. 負荷テストシナリオ

詳細は `PERFORMANCE_TEST.md` を参照。

| ID | シナリオ | 同時ユーザー数 | 期待レスポンス時間 | 期待スループット |
|----|---------|-------------|----------------|--------------|
| **PT-001** | 通常負荷_給与計算 | 10 | P95 < 500ms | 20 req/s |
| **PT-002** | 高負荷_給与計算 | 50 | P95 < 1s | 80 req/s |
| **PT-003** | ピーク負荷_従業員一覧 | 100 | P95 < 800ms | 150 req/s |
| **PT-004** | 耐久テスト_24時間連続 | 20 | エラー率 < 0.1% | - |

---

## 8. テストデータ管理

### 8.1. テストデータセット

**employees**:

| id | email | name | role | is_active |
|----|-------|------|------|-----------|
| 1 | admin@example.com | 管理者 | ADMIN | true |
| 2 | user1@example.com | 山田太郎 | USER | true |
| 3 | user2@example.com | 佐藤花子 | USER | true |
| 4 | inactive@example.com | 退職者 | USER | false |

**work_types**:

| id | name | calendar_keyword | rate_type | fixed_wage |
|----|------|-----------------|-----------|-----------|
| 1 | 個別指導 | 個別 | STUDENT_LEVEL_BASED | NULL |
| 2 | 自習室 | 自習室 | FIXED | 1200 |
| 3 | ミーティング | MTG | FIXED | 0 |

**student_levels**:

| id | level_name | display_order |
|----|-----------|--------------|
| 1 | 小学生 | 1 |
| 2 | 中学生 | 2 |
| 3 | 高校生 | 3 |

**students**:

| id | name | student_level_id |
|----|------|-----------------|
| 1 | 生徒A | 1 |
| 2 | 生徒B | 2 |
| 3 | 生徒C | 3 |

**hourly_wages**:

| id | work_type_id | student_level_id | wage |
|----|-------------|-----------------|------|
| 1 | 1 | 1 | 2500 |
| 2 | 1 | 2 | 3000 |
| 3 | 1 | 3 | 3500 |

### 8.2. テストデータ投入スクリプト

```sql
-- test-data.sql

-- employees
INSERT INTO employees (id, email, name, role, is_active, created_at, updated_at)
VALUES
(1, 'admin@example.com', '管理者', 'ADMIN', true, NOW(), NOW()),
(2, 'user1@example.com', '山田太郎', 'USER', true, NOW(), NOW()),
(3, 'user2@example.com', '佐藤花子', 'USER', true, NOW(), NOW()),
(4, 'inactive@example.com', '退職者', 'USER', false, NOW(), NOW());

-- work_types
INSERT INTO work_types (id, name, calendar_keyword, is_payroll_target, rate_type, fixed_wage, created_at, updated_at)
VALUES
(1, '個別指導', '個別', true, 'STUDENT_LEVEL_BASED', NULL, NOW(), NOW()),
(2, '自習室', '自習室', true, 'FIXED', 1200, NOW(), NOW()),
(3, 'ミーティング', 'MTG', false, 'FIXED', 0, NOW(), NOW());

-- student_levels
INSERT INTO student_levels (id, level_name, display_order, created_at, updated_at)
VALUES
(1, '小学生', 1, NOW(), NOW()),
(2, '中学生', 2, NOW(), NOW()),
(3, '高校生', 3, NOW(), NOW());

-- students
INSERT INTO students (id, name, student_level_id, is_active, created_at, updated_at)
VALUES
(1, '生徒A', 1, true, NOW(), NOW()),
(2, '生徒B', 2, true, NOW(), NOW()),
(3, '生徒C', 3, true, NOW(), NOW());

-- hourly_wages
INSERT INTO hourly_wages (id, work_type_id, student_level_id, wage, effective_from, created_at, updated_at)
VALUES
(1, 1, 1, 2500, '2025-01-01', NOW(), NOW()),
(2, 1, 2, 3000, '2025-01-01', NOW(), NOW()),
(3, 1, 3, 3500, '2025-01-01', NOW(), NOW());
```

---

## 9. CI/CDでのテスト実行

### 9.1. GitHub Actionsでの自動実行

```yaml
# .github/workflows/test.yml

name: Test Suite

on: [push, pull_request]

jobs:
  unit-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
      - name: Run unit tests
        run: ./mvnw test
      - name: Generate coverage report
        run: ./mvnw jacoco:report
      - name: Check coverage threshold (80%)
        run: ./mvnw jacoco:check

  integration-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: attendance_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
    steps:
      - uses: actions/checkout@v4
      - name: Run integration tests
        run: ./mvnw verify -P integration-test

  e2e-test:
    runs-on: ubuntu-latest
    needs: [unit-test, integration-test]
    steps:
      - uses: actions/checkout@v4
      - name: Run E2E tests
        run: ./mvnw verify -P e2e-test

  security-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: OWASP Dependency Check
        run: ./mvnw dependency-check:check
      - name: SonarQube Scan
        run: ./mvnw sonar:sonar
```

---

## 10. テスト実施チェックリスト

### 10.1. リリース前チェックリスト

- [ ] 全単体テストがパス
- [ ] コードカバレッジ80%以上
- [ ] 全統合テストがパス
- [ ] E2Eテスト（主要シナリオ）がパス
- [ ] セキュリティスキャンでCriticalな脆弱性0件
- [ ] パフォーマンステストがNFR-002を満たす
- [ ] Staging環境でスモークテスト完了
- [ ] 手動テスト（UI/UX確認）完了

---

## 11. まとめ

本テストケース定義書により、以下を実現する:

1. ✅ **包括的なテストカバレッジ**: 単体から E2Eまで全レベルをカバー
2. ✅ **自動化**: CI/CDで継続的にテスト実行
3. ✅ **品質保証**: 80%以上のコードカバレッジとセキュリティ対策
4. ✅ **再現性**: テストデータ管理により一貫したテスト環境
5. ✅ **トレーサビリティ**: テストIDにより要件とテストケースを紐付け

すべてのテストケースが実装され、継続的に実行されることで、高品質なシステムが保証される。
