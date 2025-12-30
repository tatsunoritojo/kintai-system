# 詳細設計書

- バージョン: 2.0
- 作成日: 2025年12月29日
- 作成者: Project Manager

---

## 1. 概要
本ドキュメントは、「アーキテクチャ設計書 v2.0」に基づき、実装に必要なAPI仕様、DTO、コンポーネントロジックを詳細に定義するものである。

## 2. DTO (Data Transfer Object) 定義

### 2.1. 基本DTO
- **`EmployeeDto`**:
  ```json
  { "id": 1, "email": "user@example.com", "name": "Taro Yamada" }
  ```
- **`WorkTypeDto`**:
  ```json
  {
    "id": 1,
    "name": "個別指導",
    "calendarKeyword": "個別",
    "isPayrollTarget": true,
    "rateType": "STUDENT_LEVEL_BASED"
  }
  ```
- **`StudentLevelDto`**:
  ```json
  { "id": 1, "levelName": "中学生" }
  ```
- **`StudentDto`**:
  ```json
  { "id": 1, "name": "A", "studentLevelId": 1 }
  ```
- **`HourlyWageDto`**:
  ```json
  {
    "id": 1,
    "workTypeId": 1,
    "studentLevelId": 1,
    "wage": 3000
  }
  ```
### 2.2. ページネーション用DTO
- **`PagedResultDto<T>`**:
  ```json
  {
    "content": [ /* Tの配列 */ ],
    "page": 0,
    "size": 20,
    "totalPages": 5,
    "totalElements": 98
  }
  ```

### 2.3. ペイロード用DTO
- **`PayrollDto`**: 給与レポート
  ```json
  {
    "employee": {
      "id": 1,
      "name": "山田太郎",
      "email": "yamada@example.com"
    },
    "period": {
      "start": "2025-11-01",
      "end": "2025-11-30"
    },
    "summary": {
      "totalWorkMinutes": 5400,
      "totalWorkHours": 90.0,
      "totalPayment": 216000,
      "currency": "JPY",
      "calculatedAt": "2025-12-01T00:15:32.123Z"
    },
    "paymentDetails": [
      {
        "workTypeName": "個別指導",
        "studentLevelName": "中学生",
        "minutes": 3600,
        "hours": 60.0,
        "appliedWage": 3000,
        "subtotal": 180000
      },
      {
        "workTypeName": "自習室",
        "studentLevelName": null,
        "minutes": 1800,
        "hours": 30.0,
        "appliedWage": 1200,
        "subtotal": 36000
      }
    ],
    "warnings": [
      {
        "code": "WAGE_NOT_FOUND",
        "message": "生徒 '鈴木一郎' (中学生) の単価が見つかりません。デフォルト単価 2000円/時 を適用しました。",
        "workRecordId": 42,
        "timestamp": "2025-12-01T00:15:30.456Z"
      }
    ],
    "errors": []
  }
  ```

### 2.4. エラーレスポンス (RFC 7807準拠)

すべてのエラーレスポンスは、**RFC 7807 (Problem Details for HTTP APIs)** に準拠した標準形式を使用する。

#### 2.4.1. エラーレスポンスの基本構造

```json
{
  "type": "https://api.example.com/errors/resource-not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Employee with ID 999 not found",
  "instance": "/api/v1/employees/999",
  "timestamp": "2025-12-30T12:34:56.789Z",
  "errors": []
}
```

**フィールド説明**:
- `type`: エラータイプを識別するURI（ドキュメントへのリンク）
- `title`: エラーの人間可読なタイトル（エラータイプごとに固定）
- `status`: HTTPステータスコード
- `detail`: エラーの具体的な説明（インスタンスごとに変わる）
- `instance`: エラーが発生したリクエストのパス
- `timestamp`: エラー発生時刻（ISO 8601形式）
- `errors`: バリデーションエラーの詳細（配列、オプショナル）

#### 2.4.2. バリデーションエラー (400 Bad Request)

```json
{
  "type": "https://api.example.com/errors/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed for 2 fields",
  "instance": "/api/v1/employees",
  "timestamp": "2025-12-30T12:34:56.789Z",
  "errors": [
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "Email must be a valid email address"
    },
    {
      "field": "name",
      "rejectedValue": "",
      "message": "Name must not be blank"
    }
  ]
}
```

#### 2.4.3. 認証エラー (401 Unauthorized)

```json
{
  "type": "https://api.example.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "JWT token has expired",
  "instance": "/api/v1/employees",
  "timestamp": "2025-12-30T12:34:56.789Z",
  "errors": []
}
```

#### 2.4.4. 権限エラー (403 Forbidden)

```json
{
  "type": "https://api.example.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "User does not have permission to access this resource. Required role: ADMIN",
  "instance": "/api/v1/employees/5",
  "timestamp": "2025-12-30T12:34:56.789Z",
  "errors": []
}
```

#### 2.4.5. リソース未検出 (404 Not Found)

```json
{
  "type": "https://api.example.com/errors/resource-not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Employee with ID 999 not found",
  "instance": "/api/v1/employees/999",
  "timestamp": "2025-12-30T12:34:56.789Z",
  "errors": []
}
```

#### 2.4.6. 競合エラー (409 Conflict)

```json
{
  "type": "https://api.example.com/errors/conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "Employee with email 'yamada@example.com' already exists",
  "instance": "/api/v1/employees",
  "timestamp": "2025-12-30T12:34:56.789Z",
  "errors": []
}
```

#### 2.4.7. レート制限エラー (429 Too Many Requests)

```json
{
  "type": "https://api.example.com/errors/rate-limit-exceeded",
  "title": "Rate Limit Exceeded",
  "status": 429,
  "detail": "Rate limit exceeded: 100 requests per minute. Please retry after 45 seconds.",
  "instance": "/api/v1/employees",
  "timestamp": "2025-12-30T12:34:56.789Z",
  "retryAfter": 45,
  "errors": []
}
```

#### 2.4.8. 内部サーバーエラー (500 Internal Server Error)

```json
{
  "type": "https://api.example.com/errors/internal-server-error",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred. Please contact support with error ID: abc-123-def",
  "instance": "/api/v1/payrolls",
  "timestamp": "2025-12-30T12:34:56.789Z",
  "errorId": "abc-123-def",
  "errors": []
}
```

#### 2.4.9. 外部API連携エラー (502 Bad Gateway)

```json
{
  "type": "https://api.example.com/errors/external-service-error",
  "title": "External Service Error",
  "status": 502,
  "detail": "Failed to connect to Google Calendar API after 3 retry attempts",
  "instance": "/api/v1/syncs/employees/1",
  "timestamp": "2025-12-30T12:34:56.789Z",
  "externalService": "Google Calendar API",
  "retryAttempts": 3,
  "errors": []
}
```

## 3. APIエンドポイント詳細設計
ベースURL: `/api/v1`

### 3.1. 一覧取得APIの共通仕様
`GET`リクエストによるすべての一覧取得APIは、以下のクエリパラメータを受け付ける。
- `page`: 取得ページ番号 (0-indexed, default: 0)
- `size`: 1ページあたりの件数 (default: 20)
- `sort`: ソート条件 (例: `name,asc` or `id,desc`)
- **レスポンス:** `PagedResultDto<T>`

### 3.2. `GET /employees`
- **説明:** 従業員の一覧をページネーション付きで取得する。
- **レスポンス (200 OK):** `PagedResultDto<EmployeeDto>`

### 3.3. `POST /employees`
- **説明:** 新規従業員を登録する。
- **バリデーション:** `email`は必須かつユニーク、`name`は必須。
- **レスポンス (201 Created):** `EmployeeDto`

---
*※ `/work_types`, `/students`, `/student_levels`, `/hourly_wages` の各リソースも、上記に倣った標準的なCRUDエンドポイントを提供する。*
---

### 3.4. `POST /syncs/employees/{id}`
- **説明:** 管理者が、特定の従業員のカレンダー同期を手動で実行する。
- **レスポンス (202 Accepted):**
  ```json
  {
    "message": "Sync job queued successfully",
    "employeeId": 1,
    "jobId": "sync-job-abc-123"
  }
  ```
- **エラーレスポンス (403 Forbidden):** 管理者権限がない場合（RFC 7807形式）。
  ```json
  {
    "type": "https://api.example.com/errors/forbidden",
    "title": "Forbidden",
    "status": 403,
    "detail": "User does not have permission to trigger sync jobs. Required role: ADMIN",
    "instance": "/api/v1/syncs/employees/1",
    "timestamp": "2025-12-30T12:34:56.789Z",
    "errors": []
  }
  ```

### 3.5. `GET /payrolls`
- **説明:** 指定した従業員・期間の給与計算を実行し、レポートを生成する。
- **クエリパラメータ:** `employeeId`, `startDate`, `endDate`
- **レスポンス (200 OK):** `PayrollDto`

## 4. 主要コンポーネントのロジック

### 4.1. `CalendarSyncJob`
- **トリガー:** 定期実行 (`@Scheduled`)
- **処理フロー:**
  1. `work_types`から`calendarKeyword`を持つものをすべて取得する。
  2. `employees`ごとにループし、最新の勤務記録以降のGoogleカレンダーイベントを取得。
  3. イベントタイトルが`calendarKeyword`と前方一致するかを判定。
  4. 一致した場合、イベント情報を`work_records`に保存する。タイトルから生徒名らしき文字列も抽出・保存する（例：「個別（A）」→「A」）。

### 4.2. `SheetSyncJob` (新規)
- **トリガー:** 定期実行 (`@Scheduled`)
- **処理フロー:**
  1. 設定ファイルで指定されたGoogleスプレッドシートIDと範囲を読み込む。
  2. Google Sheets APIを呼び出し、生徒名と学校種別のリストを取得。
  3. `student_levels`テーブルと`students`テーブルを洗い替え（または差分更新）する。

### 4.3. `PayrollService`
- **`calculatePayroll` メソッド:**
  1. `employeeId`と期間で`work_records`を取得。
  2. 各`work_record`についてループ。
     a. `work_types`を`calendarKeyword`で特定。
     b. `is_payroll_target`が`false`ならスキップ。
     c. `rate_type`が`FIXED`なら、`work_type`に紐づく固定単価を取得。
     d. `rate_type`が`STUDENT_LEVEL_BASED`なら、`work_record`の生徒名から`students`テーブルを検索し、`student_level_id`を取得。`work_type_id`と`student_level_id`で`hourly_wages`から単価を検索。
     e. (勤務時間 × 単価) を計算し、`paymentDetails`に蓄積。
  3. 全記録を集計し、`PayrollDto`を構築して返す。
