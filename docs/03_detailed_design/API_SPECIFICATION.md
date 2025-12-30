# API仕様書

- バージョン: 1.0
- 作成日: 2025年12月30日
- 最終更新日: 2025年12月30日
- 作成者: Project Manager

---

## 1. 概要

本ドキュメントは、勤怠管理システムのRESTful API仕様を詳細に定義する。すべてのエンドポイント、リクエスト・レスポンス形式、認証・認可要件を包括的に記述する。

## 2. API基本仕様

### 2.1. ベースURL

| 環境 | ベースURL |
|------|----------|
| ローカル | `http://localhost:8080` |
| 開発 | `https://dev-api.attendance-system.example.com` |
| ステージング | `https://staging-api.attendance-system.example.com` |
| 本番 | `https://api.attendance-system.example.com` |

### 2.2. バージョニング

- URLパスにバージョンを含める: `/api/v1/...`
- 現在のバージョン: **v1**

### 2.3. データフォーマット

- **Content-Type**: `application/json`
- **文字エンコーディング**: UTF-8
- **日付フォーマット**: ISO 8601 (`YYYY-MM-DD`)
- **日時フォーマット**: ISO 8601 (`YYYY-MM-DDTHH:mm:ssZ`)
- **キー命名規則**: camelCase

### 2.4. 認証

すべての `/api/v1/*` エンドポイント（`/api/v1/auth/*`を除く）は認証が必須。

```http
Authorization: Bearer <JWT_ACCESS_TOKEN>
```

### 2.5. 共通レスポンスヘッダー

```http
X-Trace-Id: abc123xyz
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1735564800
```

---

## 3. 認証エンドポイント

### 3.1. POST /api/v1/auth/login

OAuth 2.0認可フローを開始する。

#### リクエスト

```http
POST /api/v1/auth/login
Content-Type: application/json
```

**リクエストボディ**: なし

#### レスポンス（200 OK）

```json
{
  "authorizationUrl": "https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=...&code_challenge=..."
}
```

#### エラーレスポンス

- **500 Internal Server Error**: OAuth設定エラー

---

### 3.2. POST /api/v1/auth/callback

Google OAuthコールバックを処理し、JWTトークンを発行する。

#### リクエスト

```http
POST /api/v1/auth/callback
Content-Type: application/json
```

```json
{
  "code": "4/0AX4XfWh...",
  "codeVerifier": "abc123..."
}
```

#### レスポンス（200 OK）

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Set-Cookie**:
```
refresh_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...; HttpOnly; Secure; SameSite=Strict; Max-Age=604800; Path=/
```

#### エラーレスポンス

- **400 Bad Request**: 無効な認可コード
- **401 Unauthorized**: Google OAuth認証失敗

---

### 3.3. POST /api/v1/auth/refresh

リフレッシュトークンを使用して新しいアクセストークンを取得する。

#### リクエスト

```http
POST /api/v1/auth/refresh
Cookie: refresh_token=...
```

#### レスポンス（200 OK）

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Set-Cookie**: 新しいリフレッシュトークン

#### エラーレスポンス

- **401 Unauthorized**: 無効なリフレッシュトークン
- **403 Forbidden**: ブラックリストに登録されたトークン

---

### 3.4. POST /api/v1/auth/logout

リフレッシュトークンを無効化し、ログアウトする。

#### リクエスト

```http
POST /api/v1/auth/logout
Authorization: Bearer <ACCESS_TOKEN>
Cookie: refresh_token=...
```

#### レスポンス（200 OK）

```json
{
  "message": "Logged out successfully"
}
```

---

## 4. 従業員管理エンドポイント

### 4.1. GET /api/v1/employees

従業員の一覧を取得する（ページネーション付き）。

#### 権限

- **ADMIN**: すべての従業員を取得可能
- **USER**: 自分自身のみ取得可能

#### リクエスト

```http
GET /api/v1/employees?page=0&size=20&sort=name,asc
Authorization: Bearer <ACCESS_TOKEN>
```

**クエリパラメータ**:

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|----------|-----|------|----------|------|
| `page` | integer | いいえ | 0 | ページ番号（0-indexed） |
| `size` | integer | いいえ | 20 | 1ページあたりの件数 |
| `sort` | string | いいえ | id,asc | ソート条件（例: `name,desc`） |

#### レスポンス（200 OK）

```json
{
  "content": [
    {
      "id": 1,
      "email": "user@example.com",
      "name": "Taro Yamada",
      "role": "USER",
      "isActive": true,
      "lastSyncedAt": "2025-12-30T10:00:00Z",
      "createdAt": "2025-01-01T00:00:00Z",
      "updatedAt": "2025-12-30T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalPages": 5,
  "totalElements": 98
}
```

---

### 4.2. GET /api/v1/employees/{id}

指定されたIDの従業員を取得する。

#### 権限

- **ADMIN**: すべての従業員を取得可能
- **USER**: 自分自身のみ取得可能

#### リクエスト

```http
GET /api/v1/employees/1
Authorization: Bearer <ACCESS_TOKEN>
```

#### レスポンス（200 OK）

```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "Taro Yamada",
  "role": "USER",
  "isActive": true,
  "lastSyncedAt": "2025-12-30T10:00:00Z",
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-12-30T10:00:00Z"
}
```

#### エラーレスポンス

- **403 Forbidden**: 権限不足（USERが他のユーザーを取得）
- **404 Not Found**: 従業員が存在しない

---

### 4.3. POST /api/v1/employees

新規従業員を登録する。

#### 権限

- **ADMIN**: 実行可能

#### リクエスト

```http
POST /api/v1/employees
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

```json
{
  "email": "new@example.com",
  "name": "New Employee",
  "role": "USER"
}
```

**バリデーション**:

| フィールド | 必須 | 制約 |
|----------|------|------|
| `email` | はい | Email形式、一意 |
| `name` | はい | 1-255文字 |
| `role` | はい | `ADMIN` or `USER` |

#### レスポンス（201 Created）

```json
{
  "id": 42,
  "email": "new@example.com",
  "name": "New Employee",
  "role": "USER",
  "isActive": true,
  "createdAt": "2025-12-30T10:00:00Z",
  "updatedAt": "2025-12-30T10:00:00Z"
}
```

#### エラーレスポンス

- **400 Bad Request**: バリデーションエラー
- **403 Forbidden**: 権限不足
- **409 Conflict**: メールアドレス重複

---

### 4.4. PUT /api/v1/employees/{id}

従業員情報を更新する。

#### 権限

- **ADMIN**: 実行可能

#### リクエスト

```http
PUT /api/v1/employees/1
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

```json
{
  "name": "Updated Name",
  "role": "ADMIN",
  "isActive": true
}
```

#### レスポンス（200 OK）

```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "Updated Name",
  "role": "ADMIN",
  "isActive": true,
  "updatedAt": "2025-12-30T10:05:00Z"
}
```

---

### 4.5. DELETE /api/v1/employees/{id}

従業員を削除する（論理削除）。

#### 権限

- **ADMIN**: 実行可能

#### リクエスト

```http
DELETE /api/v1/employees/1
Authorization: Bearer <ACCESS_TOKEN>
```

#### レスポンス（204 No Content）

---

## 5. 給与計算エンドポイント

### 5.1. GET /api/v1/payrolls

指定された従業員・期間の給与を計算する。

#### 権限

- **ADMIN**: すべての従業員の給与計算が可能
- **USER**: 自分自身のみ可能

#### リクエスト

```http
GET /api/v1/payrolls?employeeId=1&startDate=2025-11-01&endDate=2025-11-30
Authorization: Bearer <ACCESS_TOKEN>
```

**クエリパラメータ**:

| パラメータ | 型 | 必須 | 説明 |
|----------|-----|------|------|
| `employeeId` | integer | はい | 従業員ID |
| `startDate` | date | はい | 期間開始日（YYYY-MM-DD） |
| `endDate` | date | はい | 期間終了日（YYYY-MM-DD） |

#### レスポンス（200 OK）

```json
{
  "employee": {
    "id": 1,
    "name": "Taro Yamada",
    "email": "user@example.com"
  },
  "period": {
    "start": "2025-11-01",
    "end": "2025-11-30"
  },
  "summary": {
    "totalWorkMinutes": 5400,
    "totalWorkHours": 90.0,
    "totalPayment": {
      "amount": 216000,
      "currency": "JPY"
    },
    "calculatedAt": "2025-12-30T10:00:00Z",
    "calculatedBy": "admin@example.com"
  },
  "paymentDetails": [
    {
      "workTypeName": "個別指導",
      "studentLevelName": "中学生",
      "recordCount": 12,
      "totalMinutes": 3600,
      "totalHours": 60.0,
      "appliedWage": {
        "amount": 3000,
        "currency": "JPY",
        "unit": "HOUR"
      },
      "subtotal": {
        "amount": 180000,
        "currency": "JPY"
      }
    },
    {
      "workTypeName": "自習室監督",
      "studentLevelName": null,
      "recordCount": 10,
      "totalMinutes": 1800,
      "totalHours": 30.0,
      "appliedWage": {
        "amount": 1200,
        "currency": "JPY",
        "unit": "HOUR"
      },
      "subtotal": {
        "amount": 36000,
        "currency": "JPY"
      }
    }
  ],
  "warnings": [
    {
      "code": "STUDENT_NOT_FOUND",
      "message": "生徒「B」がマスタに存在しないため、該当記録は計算から除外されました",
      "affectedRecordIds": [123, 456]
    }
  ],
  "errors": []
}
```

#### エラーレスポンス

- **400 Bad Request**: 無効な日付形式
- **403 Forbidden**: 権限不足
- **404 Not Found**: 従業員が存在しない
- **422 Unprocessable Entity**: 開始日 > 終了日

---

## 6. 同期エンドポイント

### 6.1. POST /api/v1/syncs/employees/{id}/calendar

指定された従業員のGoogleカレンダーを手動同期する。

#### 権限

- **ADMIN**: 実行可能

#### リクエスト

```http
POST /api/v1/syncs/employees/1/calendar
Authorization: Bearer <ACCESS_TOKEN>
```

#### レスポンス（202 Accepted）

```json
{
  "message": "Calendar sync job has been queued",
  "jobId": "abc123",
  "employeeId": 1
}
```

#### エラーレスポンス

- **403 Forbidden**: 権限不足
- **404 Not Found**: 従業員が存在しない

---

### 6.2. POST /api/v1/syncs/students

Googleスプレッドシートから生徒情報を手動同期する。

#### 権限

- **ADMIN**: 実行可能

#### リクエスト

```http
POST /api/v1/syncs/students
Authorization: Bearer <ACCESS_TOKEN>
```

#### レスポンス（202 Accepted）

```json
{
  "message": "Student sync job has been queued",
  "jobId": "def456"
}
```

---

## 7. 勤務形態管理エンドポイント

### 7.1. GET /api/v1/work-types

勤務形態の一覧を取得する。

#### リクエスト

```http
GET /api/v1/work-types?page=0&size=20
Authorization: Bearer <ACCESS_TOKEN>
```

#### レスポンス（200 OK）

```json
{
  "content": [
    {
      "id": 1,
      "name": "個別指導",
      "calendarKeyword": "個別",
      "isPayrollTarget": true,
      "rateType": "STUDENT_LEVEL_BASED",
      "fixedWage": null
    },
    {
      "id": 2,
      "name": "自習室監督",
      "calendarKeyword": "自習室",
      "isPayrollTarget": true,
      "rateType": "FIXED",
      "fixedWage": 1200
    }
  ],
  "page": 0,
  "size": 20,
  "totalPages": 1,
  "totalElements": 2
}
```

---

### 7.2. POST /api/v1/work-types

新規勤務形態を登録する。

#### 権限

- **ADMIN**: 実行可能

#### リクエスト

```json
{
  "name": "集団授業",
  "calendarKeyword": "集団",
  "isPayrollTarget": true,
  "rateType": "STUDENT_LEVEL_BASED",
  "fixedWage": null
}
```

#### レスポンス（201 Created）

```json
{
  "id": 3,
  "name": "集団授業",
  "calendarKeyword": "集団",
  "isPayrollTarget": true,
  "rateType": "STUDENT_LEVEL_BASED",
  "fixedWage": null,
  "createdAt": "2025-12-30T10:00:00Z"
}
```

---

## 8. 生徒管理エンドポイント

### 8.1. GET /api/v1/students

生徒の一覧を取得する。

#### リクエスト

```http
GET /api/v1/students?page=0&size=20&sort=name,asc
Authorization: Bearer <ACCESS_TOKEN>
```

#### レスポンス（200 OK）

```json
{
  "content": [
    {
      "id": 1,
      "name": "A",
      "studentLevelId": 2,
      "studentLevelName": "中学生",
      "isActive": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalPages": 10,
  "totalElements": 195
}
```

---

## 9. 時給マスタ管理エンドポイント

### 9.1. GET /api/v1/hourly-wages

時給マスタの一覧を取得する。

#### リクエスト

```http
GET /api/v1/hourly-wages?page=0&size=20
Authorization: Bearer <ACCESS_TOKEN>
```

#### レスポンス（200 OK）

```json
{
  "content": [
    {
      "id": 1,
      "workTypeId": 1,
      "workTypeName": "個別指導",
      "studentLevelId": 2,
      "studentLevelName": "中学生",
      "wage": 3000,
      "effectiveFrom": "2025-01-01",
      "effectiveTo": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalPages": 3,
  "totalElements": 50
}
```

---

## 10. 管理者用エンドポイント

### 10.1. GET /api/v1/admin/sync-status

同期ジョブのステータスを確認する。

#### 権限

- **ADMIN**: 実行可能

#### リクエスト

```http
GET /api/v1/admin/sync-status?employeeId=1
Authorization: Bearer <ACCESS_TOKEN>
```

#### レスポンス（200 OK）

```json
{
  "employeeId": 1,
  "status": "COMPLETED",
  "startedAt": "2025-12-30T10:00:00Z",
  "completedAt": "2025-12-30T10:05:00Z",
  "successCount": 25,
  "failureCount": 0
}
```

---

### 10.2. GET /api/v1/admin/audit-logs

監査ログを取得する。

#### 権限

- **ADMIN**: 実行可能

#### リクエスト

```http
GET /api/v1/admin/audit-logs?page=0&size=50&tableName=employees
Authorization: Bearer <ACCESS_TOKEN>
```

#### レスポンス（200 OK）

```json
{
  "content": [
    {
      "id": 1,
      "tableName": "employees",
      "recordId": 42,
      "operation": "UPDATE",
      "oldValues": {"name": "Old Name"},
      "newValues": {"name": "New Name"},
      "changedBy": "admin@example.com",
      "changedAt": "2025-12-30T10:00:00Z",
      "ipAddress": "192.168.***.***"
    }
  ],
  "page": 0,
  "size": 50,
  "totalPages": 20,
  "totalElements": 985
}
```

---

## 11. ヘルスチェックエンドポイント

### 11.1. GET /health

アプリケーションの死活監視。

#### リクエスト

```http
GET /health
```

#### レスポンス（200 OK）

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 393047969792,
        "threshold": 10485760
      }
    }
  }
}
```

---

### 11.2. GET /metrics

Prometheusメトリクス（認証不要）。

#### リクエスト

```http
GET /metrics
```

#### レスポンス（200 OK）

```
# HELP http_server_requests_seconds
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",uri="/api/v1/employees",status="200",} 1523.0
http_server_requests_seconds_sum{method="GET",uri="/api/v1/employees",status="200",} 45.234
...
```

---

## 12. エラーレスポンス（共通）

すべてのエラーレスポンスはRFC 7807形式に準拠。

### 400 Bad Request

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
    }
  ]
}
```

### 401 Unauthorized

```json
{
  "type": "https://api.attendance-system.example.com/errors/authentication-failed",
  "title": "Authentication Failed",
  "status": 401,
  "detail": "Authentication is required to access this resource",
  "instance": "/api/v1/employees",
  "timestamp": "2025-12-30T10:00:00Z",
  "traceId": "abc123xyz"
}
```

### 403 Forbidden

```json
{
  "type": "https://api.attendance-system.example.com/errors/access-denied",
  "title": "Access Denied",
  "status": 403,
  "detail": "You do not have permission to access this resource",
  "instance": "/api/v1/employees",
  "timestamp": "2025-12-30T10:00:00Z",
  "traceId": "abc123xyz"
}
```

### 404 Not Found

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

### 429 Too Many Requests

```json
{
  "type": "https://api.attendance-system.example.com/errors/rate-limit-exceeded",
  "title": "Rate Limit Exceeded",
  "status": 429,
  "detail": "You have exceeded the rate limit. Please try again later.",
  "instance": "/api/v1/employees",
  "timestamp": "2025-12-30T10:00:00Z",
  "traceId": "abc123xyz",
  "retryAfter": 60
}
```

### 503 Service Unavailable

```json
{
  "type": "https://api.attendance-system.example.com/errors/external-service-unavailable",
  "title": "External Service Unavailable",
  "status": 503,
  "detail": "Google API is temporarily unavailable. Please try again later.",
  "instance": "/api/v1/syncs/employees/1/calendar",
  "timestamp": "2025-12-30T10:00:00Z",
  "traceId": "abc123xyz",
  "retryAfter": 300
}
```

---

## 13. レート制限

| ユーザー種別 | 制限 |
|------------|------|
| 認証済みユーザー | 100 req/min |
| 未認証 | 10 req/min |
| ログインエンドポイント | 5 req/5min（同一IP） |

レート制限超過時のレスポンスヘッダー:

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1735564800
Retry-After: 60
```

---

## 14. まとめ

本API仕様は以下を実現する:

1. ✅ **RESTful原則**: リソース指向、適切なHTTPメソッド
2. ✅ **一貫性**: すべてのエンドポイントで統一された形式
3. ✅ **セキュリティ**: JWT認証、RBAC、レート制限
4. ✅ **エラーハンドリング**: RFC 7807準拠
5. ✅ **ページネーション**: 大量データの効率的な取得
6. ✅ **トレーサビリティ**: すべてのレスポンスにトレースID

すべてのエンドポイントは実装可能な詳細度で定義されている。
