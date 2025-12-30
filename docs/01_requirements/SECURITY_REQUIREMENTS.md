# セキュリティ要件定義書

- バージョン: 1.0
- 作成日: 2025年12月30日
- 最終更新日: 2025年12月30日
- 作成者: Project Manager

---

## 1. 概要

本ドキュメントは、勤怠管理システムのセキュリティ要件を詳細に定義する。本システムは従業員の個人情報および給与情報を扱うため、厳格なセキュリティ対策が必須である。

## 2. セキュリティ目標

### 2.1. 機密性（Confidentiality）
- 個人情報、給与情報への不正アクセスを防止する
- データ送信時および保存時の暗号化を実施する

### 2.2. 完全性（Integrity）
- データの改ざんを検知・防止する
- すべての変更操作を監査ログに記録する

### 2.3. 可用性（Availability）
- 正当な利用者が必要な時にシステムにアクセスできる
- DoS攻撃やシステム障害からの迅速な復旧を可能にする

## 3. 認証・認可要件

### SEC-001: 認証方式
**優先度**: Critical

#### 要件
- Google OAuth 2.0 Authorization Code Flow with PKCE を使用する
- パスワード認証は使用しない（Googleアカウントに委任）
- セッション管理にはJWT（JSON Web Token）を使用する

#### 技術仕様
```yaml
OAuth 2.0:
  プロバイダ: Google
  フロー: Authorization Code Flow with PKCE
  スコープ:
    - https://www.googleapis.com/auth/calendar.readonly
    - https://www.googleapis.com/auth/spreadsheets.readonly
    - https://www.googleapis.com/auth/userinfo.email
    - https://www.googleapis.com/auth/userinfo.profile

JWT:
  署名アルゴリズム: RS256 (非対称暗号化)
  禁止アルゴリズム: HS256, none
  発行者（iss）: https://api.attendance-system.example.com
  アクセストークン有効期限: 15分
  リフレッシュトークン有効期限: 7日
```

#### 検証項目
- [ ] OAuth 2.0 PKCE フローの実装
- [ ] JWT署名検証の実装
- [ ] トークン有効期限の検証
- [ ] 発行者（issuer）の検証

---

### SEC-002: アクセス制御（RBAC）
**優先度**: Critical

#### 要件
- ロールベースアクセス制御（RBAC）を実装する
- 最小権限の原則に基づき、必要最小限の権限のみを付与する

#### ロール定義

| ロール | 説明 | 権限 |
|--------|------|------|
| **ADMIN** | システム管理者 | すべての操作が可能 |
| **USER** | 一般ユーザー（従業員） | 自分の給与情報の閲覧のみ可能 |

#### 権限マトリクス

| 操作 | ADMIN | USER |
|------|-------|------|
| 従業員情報の閲覧（全員） | ✅ | ❌ |
| 従業員情報の閲覧（自分） | ✅ | ✅ |
| 従業員情報の作成・更新・削除 | ✅ | ❌ |
| 勤務形態・単価マスタの管理 | ✅ | ❌ |
| 生徒情報の管理 | ✅ | ❌ |
| 給与計算の実行（全員） | ✅ | ❌ |
| 給与計算の実行（自分） | ✅ | ✅ |
| 給与履歴の閲覧（全員） | ✅ | ❌ |
| 給与履歴の閲覧（自分） | ✅ | ✅ |
| 手動同期の実行 | ✅ | ❌ |
| 監査ログの閲覧 | ✅ | ❌ |

#### 実装方法
```java
// Spring Securityの@PreAuthorizeアノテーションを使用
@PreAuthorize("hasRole('ADMIN')")
public void deleteEmployee(Integer id) { ... }

@PreAuthorize("hasRole('ADMIN') or #employeeId == authentication.principal.id")
public PayrollDto calculatePayroll(Integer employeeId, LocalDate start, LocalDate end) { ... }
```

---

### SEC-003: セッション管理
**優先度**: Critical

#### 要件
- セッションハイジャック、セッション固定攻撃を防止する
- トークンのローテーション（リフレッシュトークン使用時）を実装する

#### JWT設計

**アクセストークン**:
```json
{
  "iss": "https://api.attendance-system.example.com",
  "sub": "user@example.com",
  "aud": "attendance-client",
  "exp": 1735564800,
  "iat": 1735563900,
  "jti": "abc123...",
  "role": "USER",
  "employee_id": 42
}
```

- **有効期限**: 15分
- **保存場所**: クライアント側メモリのみ（LocalStorage/SessionStorage禁止）
- **用途**: API呼び出し時のAuthorizationヘッダーに使用

**リフレッシュトークン**:
- **有効期限**: 7日
- **保存場所**: HttpOnly Cookie (Secure, SameSite=Strict)
- **ローテーション**: リフレッシュ実行時に新しいリフレッシュトークンを発行し、古いものを無効化

#### トークン失効管理
- **方式**: Redisにブラックリストを保存
- **TTL**: トークンの有効期限まで
- **ログアウト時**: リフレッシュトークンをブラックリストに追加

```java
// ログアウト処理例
public void logout(String refreshToken) {
    redisTemplate.opsForValue().set(
        "blacklist:" + refreshToken,
        "true",
        Duration.ofDays(7)
    );
}
```

---

## 4. データ保護要件

### SEC-004: 通信の暗号化
**優先度**: Critical

#### 要件
- すべてのHTTP通信はTLS 1.2以上で暗号化する
- TLS 1.0、1.1は禁止
- データベース接続もTLS/SSLで暗号化する

#### 実装
```yaml
# application.yml
server:
  ssl:
    enabled: true
    protocol: TLSv1.2
    enabled-protocols: TLSv1.2,TLSv1.3
    ciphers: TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,...

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/attendance_db?ssl=true&sslmode=require
```

---

### SEC-005: データ保存時の暗号化
**優先度**: Critical

#### 要件
- 個人情報（メールアドレス）は暗号化して保存する
- Google OAuthリフレッシュトークンは暗号化して保存する

#### 暗号化仕様

| データ | 暗号化アルゴリズム | 鍵管理 |
|--------|-------------------|--------|
| email | AES-256-GCM | 環境変数 or AWS KMS |
| google_refresh_token_encrypted | AES-256-GCM | AWS KMS / HashiCorp Vault |

#### 実装例
```java
@Service
public class EncryptionService {

    @Value("${encryption.key}")
    private String encryptionKey;

    public String encrypt(String plainText) {
        // AES-256-GCM暗号化
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        // ... 実装
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decrypt(String cipherText) {
        // 復号化
        // ... 実装
        return decryptedText;
    }
}
```

#### 鍵管理
- **開発環境**: 環境変数に設定
- **本番環境**: AWS KMS または HashiCorp Vault を使用
- **鍵ローテーション**: 年1回実施

---

### SEC-006: パスワードポリシー
**優先度**: N/A（本システムはパスワード認証を使用しない）

本システムはGoogle OAuthを使用するため、パスワード管理はGoogleに委任する。

---

## 5. 入力検証・出力エスケープ

### SEC-007: 入力検証
**優先度**: High

#### 要件
- すべてのユーザー入力に対してバリデーションを実施する
- SQLインジェクション、XSSを防止する

#### 実装方法
```java
// Bean Validationを使用
public class EmployeeCreateRequest {
    @NotBlank(message = "名前は必須です")
    @Size(max = 255, message = "名前は255文字以内で入力してください")
    private String name;

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;
}

// コントローラー
@PostMapping("/employees")
public ResponseEntity<EmployeeDto> createEmployee(
    @Valid @RequestBody EmployeeCreateRequest request
) {
    // ...
}
```

#### 検証項目
- [ ] すべてのDTOに@Validアノテーションを適用
- [ ] カスタムバリデータで業務ロジックを検証
- [ ] JPA使用によるSQLインジェクション対策（生SQL禁止）

---

### SEC-008: 出力エスケープ
**優先度**: Medium

#### 要件
- JSON出力は自動的にエスケープされる（Spring MVC標準）
- ログ出力時にCRLFインジェクションを防止する

```java
// ログ出力時のサニタイズ
log.info("User logged in: {}", sanitize(username));

private String sanitize(String input) {
    return input.replaceAll("[\\r\\n]", "");
}
```

---

## 6. レート制限・DoS対策

### SEC-009: APIレート制限
**優先度**: High

#### 要件
- APIへの過度なリクエストを制限し、DoS攻撃を防止する

#### レート制限設定

| 対象 | 制限 |
|------|------|
| 認証済みユーザー | 100 req/min |
| 未認証 | 10 req/min |
| ログインエンドポイント | 5 req/5min（同一IPアドレス） |

#### 実装
```java
// Bucket4jライブラリを使用
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        String key = getClientIdentifier(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket());

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            response.setStatus(429); // Too Many Requests
            return false;
        }
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
```

---

## 7. 監査・ログ要件

### SEC-010: 監査ログ
**優先度**: Critical

#### 要件
- すべてのデータ変更操作を記録する
- 給与計算実行を記録する
- ログの改ざんを防止する

#### 記録対象
- 操作種別（INSERT, UPDATE, DELETE）
- 操作対象テーブル、レコードID
- 変更前後の値（個人情報はマスキング）
- 操作者ID、IPアドレス、User-Agent
- 操作日時

#### 保持期間
- **最低5年間**保持する
- 自動削除スクリプトで古いログを定期的にアーカイブ

#### 実装
```java
@Component
@Aspect
public class AuditLogAspect {

    @AfterReturning("@annotation(Auditable)")
    public void logAudit(JoinPoint joinPoint) {
        AuditLog log = new AuditLog();
        log.setOperation("UPDATE");
        log.setTableName("employees");
        // ...
        auditLogRepository.save(log);
    }
}
```

---

### SEC-011: セキュリティイベントログ
**優先度**: High

#### 要件
以下のセキュリティイベントをログに記録する:
- ログイン成功/失敗
- 認可失敗（403 Forbidden）
- トークン検証失敗
- レート制限超過（429 Too Many Requests）
- 異常なAPIアクセスパターン

#### ログフォーマット
```json
{
  "timestamp": "2025-12-30T10:00:00.123Z",
  "level": "WARN",
  "event": "LOGIN_FAILED",
  "userId": "unknown",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "reason": "Invalid credentials"
}
```

---

## 8. 脆弱性対策（OWASP Top 10）

### SEC-012: OWASP Top 10 対策状況

| 脅威 | 対策 | 実装状況 |
|------|------|----------|
| **A01: Broken Access Control** | RBAC実装、@PreAuthorize使用 | ✅ 実装予定 |
| **A02: Cryptographic Failures** | TLS 1.2+、AES-256-GCM暗号化 | ✅ 実装予定 |
| **A03: Injection** | JPA使用、入力検証 | ✅ 実装予定 |
| **A04: Insecure Design** | セキュアな設計レビュー実施 | ✅ 実施済み |
| **A05: Security Misconfiguration** | セキュリティ設定ガイドライン作成 | ✅ 実装予定 |
| **A06: Vulnerable Components** | 依存ライブラリの定期更新、OWASP Dependency Check | ✅ 実装予定 |
| **A07: Authentication Failures** | OAuth 2.0 + JWT、リフレッシュトークンローテーション | ✅ 実装予定 |
| **A08: Software and Data Integrity** | 監査ログ、コード署名 | ✅ 実装予定 |
| **A09: Logging Failures** | 構造化ログ、セキュリティイベント記録 | ✅ 実装予定 |
| **A10: SSRF** | 外部API呼び出しをGoogle APIのみに制限 | ✅ リスク低 |

---

## 9. CORS設定

### SEC-013: CORS（Cross-Origin Resource Sharing）
**優先度**: High

#### 要件
- 許可されたオリジンからのみAPIアクセスを許可する

#### 設定
```java
@Configuration
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
            "https://app.example.com",
            "http://localhost:3000"  // 開発環境のみ
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

---

## 10. CSRF対策

### SEC-014: CSRF（Cross-Site Request Forgery）
**優先度**: Medium

#### 要件
- RESTful APIはステートレスなため、CSRF対策は基本的に不要
- ただし、Cookie使用時はSameSite属性を設定する

#### 設定
```java
// Cookieの設定
response.addCookie(createCookie("refresh_token", refreshToken));

private Cookie createCookie(String name, String value) {
    Cookie cookie = new Cookie(name, value);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);  // HTTPS only
    cookie.setSameSite("Strict");
    cookie.setPath("/");
    cookie.setMaxAge(7 * 24 * 60 * 60);  // 7 days
    return cookie;
}
```

---

## 11. 依存ライブラリの脆弱性管理

### SEC-015: 依存ライブラリのスキャン
**優先度**: High

#### 要件
- CI/CDパイプラインで自動的に脆弱性スキャンを実施する
- CVSSスコア7.0以上の脆弱性が検出された場合はビルド失敗とする

#### ツール
- **OWASP Dependency Check**: Maven plugin
- **Snyk**: 継続的な脆弱性監視
- **SonarQube**: コード品質・セキュリティ分析

#### CI/CD統合
```yaml
# GitHub Actions
- name: Run OWASP Dependency Check
  run: mvn org.owasp:dependency-check-maven:check

- name: Check for high severity vulnerabilities
  run: |
    if grep -q "CVSS Score.*([7-9]\|10)" target/dependency-check-report.html; then
      echo "High severity vulnerabilities found!"
      exit 1
    fi
```

---

## 12. セキュリティテスト

### SEC-016: セキュリティテスト実施
**優先度**: High

#### 要件
- 本番リリース前にセキュリティテストを実施する

#### テスト項目
1. **認証・認可テスト**
   - [ ] 無効なトークンでのアクセス拒否
   - [ ] 権限のない操作の拒否
   - [ ] トークン有効期限の検証

2. **インジェクション攻撃テスト**
   - [ ] SQLインジェクション（JPA使用により低リスク）
   - [ ] ログインジェクション

3. **XSS（Cross-Site Scripting）テスト**
   - [ ] 入力値のサニタイズ確認

4. **CSRF攻撃テスト**
   - [ ] SameSite Cookie設定の確認

5. **ペネトレーションテスト**
   - [ ] OWASP ZAPによる自動スキャン
   - [ ] 手動でのセキュリティ診断（オプション）

---

## 13. インシデント対応

### SEC-017: セキュリティインシデント対応手順
**優先度**: High

#### インシデント検知
- 異常なログパターンの監視（ログイン失敗多発、大量のAPI呼び出しなど）
- アラート通知（Slack、メールなど）

#### 対応フロー
1. **検知**: 監視システムが異常を検知
2. **初動対応**: 管理者に通知、影響範囲の特定
3. **封じ込め**: 該当ユーザーの無効化、トークン失効
4. **調査**: 監査ログ、アクセスログの分析
5. **復旧**: システムの正常化、セキュリティパッチ適用
6. **報告**: インシデントレポートの作成、再発防止策の策定

---

## 14. コンプライアンス

### SEC-018: 個人情報保護法への準拠
**優先度**: Critical

#### 要件
- 個人情報の取り扱いについて、個人情報保護法に準拠する
- 個人情報の利用目的を明示する
- 本人同意なく第三者に提供しない

#### 個人情報の定義
本システムで扱う個人情報:
- 従業員の氏名、メールアドレス
- 給与情報
- 勤務記録

#### 対策
- [ ] プライバシーポリシーの作成
- [ ] 個人情報の暗号化
- [ ] アクセス制御の実装
- [ ] 監査ログの記録

---

## 15. まとめ

本セキュリティ要件定義書は、以下の観点から包括的なセキュリティ対策を定義している:

1. ✅ **認証・認可**: OAuth 2.0 + JWT + RBAC
2. ✅ **データ保護**: TLS暗号化、AES-256-GCM暗号化
3. ✅ **入力検証**: Bean Validation、SQLインジェクション対策
4. ✅ **レート制限**: DoS攻撃防止
5. ✅ **監査**: すべての変更操作を記録
6. ✅ **OWASP Top 10対策**: 主要な脆弱性に対応
7. ✅ **脆弱性管理**: 依存ライブラリの定期スキャン
8. ✅ **コンプライアンス**: 個人情報保護法への準拠

すべての要件は実装時に厳守し、セキュリティテストで検証すること。
