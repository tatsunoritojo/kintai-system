# エラーハンドリング設計書

- バージョン: 1.0
- 作成日: 2025年12月30日
- 最終更新日: 2025年12月30日
- 作成者: Project Manager

---

## 1. 概要

本ドキュメントは、勤怠管理システムにおけるエラーハンドリング戦略を定義する。一貫性のあるエラーレスポンス、適切なHTTPステータスコード、リトライ戦略、フォールバック処理を包括的に記述する。

## 2. エラーレスポンス設計（RFC 7807準拠）

### 2.1. RFC 7807 (Problem Details for HTTP APIs)

すべてのエラーレスポンスは[RFC 7807](https://tools.ietf.org/html/rfc7807)に準拠した形式で返す。

#### 標準エラーレスポンス構造
```json
{
  "type": "https://api.attendance-system.example.com/errors/employee-not-found",
  "title": "Employee Not Found",
  "status": 404,
  "detail": "Employee with ID 999 does not exist in the system",
  "instance": "/api/v1/employees/999",
  "timestamp": "2025-12-30T10:00:00Z",
  "traceId": "abc123xyz"
}
```

#### フィールド説明

| フィールド | 必須 | 説明 |
|-----------|------|------|
| `type` | はい | エラータイプを識別するURI。エラーの種類ごとに一意 |
| `title` | はい | エラーの簡潔な説明（人間が読める形式） |
| `status` | はい | HTTPステータスコード |
| `detail` | はい | エラーの詳細説明（このインスタンス固有の情報） |
| `instance` | いいえ | エラーが発生したリクエストURI |
| `timestamp` | いいえ | エラー発生日時（ISO 8601形式） |
| `traceId` | いいえ | 分散トレーシング用のトレースID |

#### バリデーションエラーの拡張
```json
{
  "type": "https://api.attendance-system.example.com/errors/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Input validation failed for one or more fields",
  "instance": "/api/v1/employees",
  "timestamp": "2025-12-30T10:00:00Z",
  "traceId": "abc123xyz",
  "errors": [
    {
      "field": "email",
      "code": "INVALID_FORMAT",
      "message": "Email format is invalid",
      "rejectedValue": "invalid-email"
    },
    {
      "field": "name",
      "code": "NOT_BLANK",
      "message": "Name must not be blank",
      "rejectedValue": ""
    }
  ]
}
```

### 2.2. ErrorResponseクラス

```java
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonProperty("type")
    private String type;

    @JsonProperty("title")
    private String title;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("detail")
    private String detail;

    @JsonProperty("instance")
    private String instance;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("traceId")
    private String traceId;

    @JsonProperty("errors")
    private List<FieldError> errors;

    @JsonProperty("retryAfter")
    private Integer retryAfter;  // 503 Service Unavailable時のリトライ推奨時間（秒）

    @Data
    @Builder
    public static class FieldError {
        private String field;
        private String code;
        private String message;
        private Object rejectedValue;
    }
}
```

## 3. HTTPステータスコード一覧

### 3.1. 成功レスポンス（2xx）

| コード | 意味 | 使用例 |
|-------|------|--------|
| **200 OK** | リクエスト成功 | GET, PUT, DELETE成功時 |
| **201 Created** | リソース作成成功 | POST成功時 |
| **202 Accepted** | リクエスト受理（非同期処理） | 手動同期APIのキューイング |
| **204 No Content** | リクエスト成功（レスポンスボディなし） | DELETE成功時 |

### 3.2. クライアントエラー（4xx）

| コード | 意味 | 使用例 |
|-------|------|--------|
| **400 Bad Request** | 不正なリクエスト | バリデーションエラー、パラメータ不正 |
| **401 Unauthorized** | 認証が必要 | JWT未提供、JWT無効 |
| **403 Forbidden** | 認可されていない | 権限不足（RBAC違反） |
| **404 Not Found** | リソースが存在しない | 指定されたIDのemployeeが存在しない |
| **409 Conflict** | リソースの競合 | 一意制約違反（重複メールアドレス） |
| **422 Unprocessable Entity** | ビジネスロジック違反 | 給与計算期間が無効 |
| **429 Too Many Requests** | レート制限超過 | APIリクエスト数の上限超過 |

### 3.3. サーバーエラー（5xx）

| コード | 意味 | 使用例 |
|-------|------|--------|
| **500 Internal Server Error** | 予期しないサーバーエラー | 例外処理されなかったエラー |
| **503 Service Unavailable** | サービス一時利用不可 | Google API障害、DB接続エラー |

## 4. グローバル例外ハンドラ

### 4.1. GlobalExceptionHandler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. リソースが見つからない場合（404）
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
            .type("https://api.attendance-system.example.com/errors/" + ex.getErrorCode())
            .title(ex.getTitle())
            .status(404)
            .detail(ex.getMessage())
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .build();

        log.warn("Resource not found: {}", ex.getMessage());

        return ResponseEntity.status(404).body(error);
    }

    // 2. バリデーションエラー（400）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> ErrorResponse.FieldError.builder()
                .field(fe.getField())
                .code(fe.getCode())
                .message(fe.getDefaultMessage())
                .rejectedValue(fe.getRejectedValue())
                .build()
            )
            .toList();

        ErrorResponse error = ErrorResponse.builder()
            .type("https://api.attendance-system.example.com/errors/validation-failed")
            .title("Validation Failed")
            .status(400)
            .detail("Input validation failed for one or more fields")
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .errors(fieldErrors)
            .build();

        log.warn("Validation error: {} field(s) failed", fieldErrors.size());

        return ResponseEntity.status(400).body(error);
    }

    // 3. 認証エラー（401）
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationError(
            AuthenticationException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
            .type("https://api.attendance-system.example.com/errors/authentication-failed")
            .title("Authentication Failed")
            .status(401)
            .detail("Authentication is required to access this resource")
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .build();

        log.warn("Authentication failed: {}", ex.getMessage());

        return ResponseEntity.status(401).body(error);
    }

    // 4. 認可エラー（403）
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
            .type("https://api.attendance-system.example.com/errors/access-denied")
            .title("Access Denied")
            .status(403)
            .detail("You do not have permission to access this resource")
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .build();

        log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity.status(403).body(error);
    }

    // 5. 一意制約違反（409）
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
            .type("https://api.attendance-system.example.com/errors/conflict")
            .title("Resource Conflict")
            .status(409)
            .detail("A resource with the same unique identifier already exists")
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .build();

        log.warn("Data integrity violation: {}", ex.getMessage());

        return ResponseEntity.status(409).body(error);
    }

    // 6. ビジネスロジック違反（422）
    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogicError(
            BusinessLogicException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
            .type("https://api.attendance-system.example.com/errors/" + ex.getErrorCode())
            .title(ex.getTitle())
            .status(422)
            .detail(ex.getMessage())
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .build();

        log.warn("Business logic error: {}", ex.getMessage());

        return ResponseEntity.status(422).body(error);
    }

    // 7. Google API障害（503）
    @ExceptionHandler(GoogleApiException.class)
    public ResponseEntity<ErrorResponse> handleGoogleApiError(
            GoogleApiException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
            .type("https://api.attendance-system.example.com/errors/external-service-unavailable")
            .title("External Service Unavailable")
            .status(503)
            .detail("Google API is temporarily unavailable. Please try again later.")
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .retryAfter(300)  // 5分後に再試行
            .build();

        log.error("Google API error: {}", ex.getMessage(), ex);

        return ResponseEntity
            .status(503)
            .header("Retry-After", "300")
            .body(error);
    }

    // 8. 予期しないエラー（500）
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(
            Exception ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
            .type("https://api.attendance-system.example.com/errors/internal-server-error")
            .title("Internal Server Error")
            .status(500)
            .detail("An unexpected error occurred. Please contact support if this persists.")
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .build();

        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(500).body(error);
    }
}
```

### 4.2. カスタム例外クラス

```java
// リソースが見つからない
@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String errorCode;
    private final String title;

    public ResourceNotFoundException(String errorCode, String title, String message) {
        super(message);
        this.errorCode = errorCode;
        this.title = title;
    }
}

// 使用例
throw new ResourceNotFoundException(
    "employee-not-found",
    "Employee Not Found",
    "Employee with ID " + id + " does not exist in the system"
);

// ビジネスロジック違反
@Getter
public class BusinessLogicException extends RuntimeException {
    private final String errorCode;
    private final String title;

    public BusinessLogicException(String errorCode, String title, String message) {
        super(message);
        this.errorCode = errorCode;
        this.title = title;
    }
}

// 使用例
throw new BusinessLogicException(
    "invalid-payroll-period",
    "Invalid Payroll Period",
    "Start date must be before end date"
);
```

## 5. リトライ戦略

### 5.1. Google API呼び出しのリトライ

Google Calendar API、Google Sheets APIの呼び出しに対して、指数バックオフでリトライを実装する。

#### Spring Retryの設定

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
</dependency>
```

```java
@Configuration
@EnableRetry
public class RetryConfig {
}
```

#### リトライ実装

```java
@Service
@Slf4j
public class GoogleCalendarService {

    @Retryable(
        value = {GoogleApiException.class, IOException.class},
        maxAttempts = 3,
        backoff = @Backoff(
            delay = 1000,       // 初回リトライは1秒後
            multiplier = 2,     // 指数バックオフ（2倍ずつ増加）
            maxDelay = 10000    // 最大遅延は10秒
        )
    )
    public List<Event> getEvents(Employee employee, Instant start, Instant end) {
        log.info("Fetching calendar events for employee {}", employee.getId());

        try {
            Calendar service = createCalendarService(employee);
            Events events = service.events()
                .list("primary")
                .setTimeMin(new DateTime(start.toEpochMilli()))
                .setTimeMax(new DateTime(end.toEpochMilli()))
                .setSingleEvents(true)
                .setOrderBy("startTime")
                .execute();

            return events.getItems();

        } catch (IOException e) {
            log.warn("Google Calendar API call failed (will retry): {}", e.getMessage());
            throw new GoogleApiException("Failed to fetch calendar events", e);
        }
    }

    @Recover
    public List<Event> recover(GoogleApiException ex, Employee employee, Instant start, Instant end) {
        log.error("Google Calendar API call failed after {} retries for employee: {}",
                  3, employee.getId(), ex);

        // アラート送信
        alertService.sendAlert(
            "Google Calendar API failure",
            "Failed to sync calendar for employee: " + employee.getName()
        );

        // Dead Letter Queueに保存（後で手動リトライ可能）
        dlqRepository.save(new FailedSyncJob(
            "calendar_sync",
            employee.getId(),
            start,
            end,
            ex.getMessage()
        ));

        // 空のリストを返して処理を継続
        return Collections.emptyList();
    }
}
```

#### リトライスケジュール
1. 1回目のリトライ: **1秒後**
2. 2回目のリトライ: **2秒後**（1秒 × 2）
3. 3回目のリトライ: **4秒後**（2秒 × 2）

最大3回リトライしても失敗した場合は、`@Recover`メソッドが実行される。

### 5.2. データベース接続エラーのリトライ

```java
@Service
public class EmployeeService {

    @Retryable(
        value = {CannotGetJdbcConnectionException.class, TransientDataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public Employee getEmployee(Integer id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "employee-not-found",
                "Employee Not Found",
                "Employee with ID " + id + " does not exist"
            ));
    }

    @Recover
    public Employee recover(CannotGetJdbcConnectionException ex, Integer id) {
        log.error("Database connection failed after retries for employee ID: {}", id, ex);
        throw new ServiceUnavailableException("Database is temporarily unavailable");
    }
}
```

## 6. Circuit Breaker（サーキットブレーカー）

繰り返し失敗する外部サービスへの呼び出しを一時的に停止し、システムを保護する。

### 6.1. Resilience4jの設定

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      googleApi:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 60s
        failureRateThreshold: 50
        slowCallRateThreshold: 100
        slowCallDurationThreshold: 5s
```

```java
@Service
public class GoogleCalendarService {

    @CircuitBreaker(name = "googleApi", fallbackMethod = "fallbackGetEvents")
    @Retryable(...)
    public List<Event> getEvents(Employee employee, Instant start, Instant end) {
        // Google API呼び出し
    }

    private List<Event> fallbackGetEvents(Employee employee, Instant start, Instant end, Exception ex) {
        log.error("Circuit breaker activated for Google Calendar API", ex);
        // フォールバック処理: 空のリストを返す、またはキャッシュから取得
        return Collections.emptyList();
    }
}
```

## 7. タイムアウト設定

### 7.1. HTTPクライアントタイムアウト

```java
@Configuration
public class GoogleApiConfig {

    @Bean
    public HttpRequestFactory httpRequestFactory() {
        HttpTransport httpTransport = new NetHttpTransport();
        return new HttpCredentialsAdapter(credentials)
            .setConnectTimeout(10000)   // 接続タイムアウト: 10秒
            .setReadTimeout(30000);     // 読み取りタイムアウト: 30秒
    }
}
```

### 7.2. APIエンドポイントタイムアウト

```java
@RestController
@RequestMapping("/api/v1/payrolls")
public class PayrollController {

    @GetMapping
    @Timeout(value = 5, unit = TimeUnit.SECONDS)  // 最大5秒
    public ResponseEntity<PayrollDto> calculatePayroll(
        @RequestParam Integer employeeId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        PayrollDto payroll = payrollService.calculatePayroll(employeeId, startDate, endDate);
        return ResponseEntity.ok(payroll);
    }
}
```

## 8. ロギング

### 8.1. エラーログレベル

| レベル | 使用例 |
|--------|--------|
| **ERROR** | 即時対応が必要なエラー（DB接続失敗、予期しない例外） |
| **WARN** | 注意が必要だが処理は継続（リトライ実行、リソース未検出） |
| **INFO** | 重要なイベント（API呼び出し成功、給与計算完了） |
| **DEBUG** | デバッグ情報（SQLクエリ、APIレスポンス詳細） |

### 8.2. 構造化ログ

```java
log.error("Failed to process payroll calculation",
    kv("employeeId", employeeId),
    kv("startDate", startDate),
    kv("endDate", endDate),
    kv("traceId", MDC.get("traceId")),
    ex
);
```

## 9. エラーモニタリング・アラート

### 9.1. Prometheusメトリクス

```java
@Component
public class ErrorMetrics {

    private final Counter errorCounter;

    public ErrorMetrics(MeterRegistry registry) {
        this.errorCounter = Counter.builder("api_errors_total")
            .description("Total number of API errors")
            .tag("application", "attendance-management")
            .register(registry);
    }

    public void recordError(String errorType, int statusCode) {
        errorCounter.increment(
            Tag.of("error_type", errorType),
            Tag.of("status_code", String.valueOf(statusCode))
        );
    }
}
```

### 9.2. アラート条件

- エラー率が5%を超えた場合
- Google API呼び出しが連続して3回失敗した場合
- データベース接続エラーが発生した場合

## 10. まとめ

本エラーハンドリング設計は以下を実現する:

1. ✅ **一貫性**: RFC 7807準拠の統一されたエラーレスポンス
2. ✅ **可視性**: 分散トレーシング、構造化ログ
3. ✅ **回復性**: リトライ、サーキットブレーカー、フォールバック処理
4. ✅ **監視性**: Prometheusメトリクス、アラート
5. ✅ **デバッグ性**: トレースID、詳細なエラー情報

すべてのエラーは適切にハンドリングされ、システムの安定性と可用性を確保する。
