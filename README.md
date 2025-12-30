# 勤怠管理システム（Attendance Management System）

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📋 目次

- [概要](#概要)
- [主な機能](#主な機能)
- [技術スタック](#技術スタック)
- [クイックスタート](#クイックスタート)
- [プロジェクト構成](#プロジェクト構成)
- [ドキュメント](#ドキュメント)
- [開発ガイド](#開発ガイド)
- [ライセンス](#ライセンス)

---

## 概要

**勤怠管理システム**は、Google CalendarとGoogle Sheetsと連携し、勤務時間の自動取得と給与の自動計算を行うバックエンドシステムです。

### ビジネス価値

- 📅 **自動勤怠取得**: Google Calendarから勤務記録を自動同期
- 💰 **動的給与計算**: 勤務形態と生徒レベルに応じた単価で給与を自動計算
- 👥 **生徒マスタ同期**: Google Sheetsから生徒情報を自動取得
- 🔒 **セキュア**: OAuth 2.0認証、JWT、AES-256暗号化
- 📊 **監視・ログ**: Prometheus/Grafana、構造化JSONログ

---

## 主な機能

### 1. Google OAuth 2.0 認証

- ユーザーのGoogleアカウントで認証
- リフレッシュトークンの安全な保存（AES-256-GCM暗号化）
- JWT（RS256）によるセッション管理

### 2. 自動同期（バッチ処理）

- **CalendarSyncJob**: Google Calendarから勤務記録を定期的に取得
- **SheetSyncJob**: Google Sheetsから生徒マスタを定期的に取得
- ShedLockによる分散ロック（複数インスタンス対応）

### 3. 動的給与計算

- 勤務形態（個別指導、自習室など）ごとに異なる計算ロジック
- 生徒の学校種別（小学生、中学生、高校生）に応じた単価適用
- 時給マスタの有効期間管理

### 4. RESTful API

- CRUD操作（従業員、勤務記録、単価マスタ、生徒マスタ）
- ページネーション、ソート対応
- RFC 7807準拠のエラーレスポンス

### 5. 管理画面（Thymeleaf）

- ダッシュボード（勤務時間、未払い給与の概要）
- 従業員管理（一覧、詳細、編集、削除）
- 給与計算実行・結果表示（PDF/CSV出力）
- マスタデータ管理（単価、生徒、勤務形態）

---

## 技術スタック

### バックエンド

| 技術 | バージョン | 用途 |
|------|----------|------|
| **Java** | 17 (LTS) | プログラミング言語 |
| **Spring Boot** | 3.2.x | アプリケーションフレームワーク |
| **Spring Security** | 6.x | 認証・認可 |
| **Spring Data JPA** | - | データアクセス層 |
| **Hibernate** | - | ORM |
| **Thymeleaf** | - | テンプレートエンジン |

### データベース

| 技術 | バージョン | 用途 |
|------|----------|------|
| **PostgreSQL** | 15.x | データベース（Docker） |
| **Flyway** | - | DBマイグレーション |
| **HikariCP** | - | コネクションプール |

### 認証・セキュリティ

| 技術 | 用途 |
|------|------|
| **OAuth 2.0** | Google認証 |
| **JWT (JJWT)** | トークンベース認証（RS256） |
| **AES-256-GCM** | データ暗号化 |

### 外部API

| サービス | 用途 |
|---------|------|
| **Google Calendar API** | 勤務記録取得 |
| **Google Sheets API** | 生徒マスタ取得 |

### 監視・ログ

| 技術 | 用途 |
|------|------|
| **Prometheus** | メトリクス収集 |
| **Grafana** | 可視化ダッシュボード |
| **Logstash Encoder** | 構造化JSONログ |

### テスト

| 技術 | 用途 |
|------|------|
| **JUnit 5** | 単体テスト |
| **Mockito** | モック |
| **Testcontainers** | 統合テスト（PostgreSQL） |
| **REST Assured** | E2Eテスト |
| **JaCoCo** | コードカバレッジ（目標: 80%） |
| **JMeter / Gatling** | パフォーマンステスト |

### ビルド・デプロイ

| 技術 | 用途 |
|------|------|
| **Maven** | ビルドツール |
| **Docker** | コンテナ化 |
| **Docker Compose** | ローカル開発環境 |
| **GitHub Actions** | CI/CD |
| **AWS ECS (Fargate)** | 本番環境 |
| **AWS RDS (PostgreSQL)** | 本番データベース |

---

## クイックスタート

### 前提条件

- **Java 17** 以上
- **Maven 3.9.x** 以上
- **Docker** & **Docker Compose**
- **Git**

### 1. リポジトリクローン

```bash
git clone https://github.com/your-org/attendance-management-system-java.git
cd attendance-management-system-java
```

### 2. Docker Composeで起動

```bash
# PostgreSQLとアプリケーションを起動
docker-compose up -d

# ログ確認
docker-compose logs -f app
```

### 3. アクセス

- **管理画面**: http://localhost:8080
- **API**: http://localhost:8080/api/v1
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Actuator**: http://localhost:8080/actuator

### 4. 停止

```bash
docker-compose down
```

---

## プロジェクト構成

```
attendance-management-system-java/
├── docs/                           # ドキュメント
│   ├── 01_requirements/            # 要件定義
│   │   ├── REQUIREMENTS_DEFINITION.md
│   │   ├── SECURITY_REQUIREMENTS.md
│   │   └── DATA_RETENTION_POLICY.md
│   ├── 02_architecture_design/     # アーキテクチャ設計
│   │   ├── ARCHITECTURE_DESIGN.md
│   │   ├── DATABASE_DESIGN_DETAIL.md
│   │   └── SECURITY_ARCHITECTURE.md
│   ├── 03_detailed_design/         # 詳細設計
│   │   ├── DETAILED_DESIGN.md
│   │   ├── API_SPECIFICATION.md
│   │   ├── BATCH_JOB_DESIGN.md
│   │   ├── ERROR_HANDLING.md
│   │   └── SCREEN_DESIGN.md
│   ├── 04_operations/              # 運用
│   │   ├── DEPLOYMENT.md
│   │   ├── MONITORING.md
│   │   ├── LOGGING.md
│   │   └── RUNBOOK.md
│   ├── 05_testing/                 # テスト
│   │   ├── TEST_STRATEGY.md
│   │   ├── TEST_CASES.md
│   │   └── PERFORMANCE_TEST.md
│   └── 06_development/             # 開発ガイド
│       ├── LOCAL_DEVELOPMENT.md
│       ├── CODING_STANDARDS.md
│       └── GIT_WORKFLOW.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/attendance/
│   │   │       ├── config/         # 設定クラス
│   │   │       ├── controller/     # REST API、画面コントローラー
│   │   │       ├── service/        # ビジネスロジック
│   │   │       ├── repository/     # データアクセス
│   │   │       ├── entity/         # JPAエンティティ
│   │   │       ├── dto/            # DTO
│   │   │       ├── batch/          # バッチジョブ
│   │   │       ├── security/       # セキュリティ
│   │   │       └── exception/      # 例外ハンドリング
│   │   ├── resources/
│   │   │   ├── db/migration/       # Flywayマイグレーション
│   │   │   ├── templates/          # Thymeleafテンプレート
│   │   │   ├── static/             # CSS, JS, 画像
│   │   │   └── application.yml     # 設定ファイル
│   └── test/
│       ├── java/                   # テストコード
│       └── resources/              # テスト用リソース
├── docker-compose.yml              # ローカル開発環境
├── Dockerfile                      # コンテナイメージ定義
├── pom.xml                         # Maven設定
└── README.md                       # このファイル
```

---

## ドキュメント

### 設計ドキュメント

| カテゴリ | ドキュメント | 説明 |
|---------|------------|------|
| **要件定義** | [REQUIREMENTS_DEFINITION.md](docs/01_requirements/REQUIREMENTS_DEFINITION.md) | 機能要件・非機能要件 |
| | [SECURITY_REQUIREMENTS.md](docs/01_requirements/SECURITY_REQUIREMENTS.md) | セキュリティ要件（OWASP Top 10対応） |
| | [DATA_RETENTION_POLICY.md](docs/01_requirements/DATA_RETENTION_POLICY.md) | データ保持・削除ポリシー |
| **アーキテクチャ** | [ARCHITECTURE_DESIGN.md](docs/02_architecture_design/ARCHITECTURE_DESIGN.md) | システム構成、ER図 |
| | [DATABASE_DESIGN_DETAIL.md](docs/02_architecture_design/DATABASE_DESIGN_DETAIL.md) | データベース設計（DDL、インデックス） |
| | [SECURITY_ARCHITECTURE.md](docs/02_architecture_design/SECURITY_ARCHITECTURE.md) | JWT設計、暗号化 |
| **詳細設計** | [DETAILED_DESIGN.md](docs/03_detailed_design/DETAILED_DESIGN.md) | DTO、クラス設計 |
| | [API_SPECIFICATION.md](docs/03_detailed_design/API_SPECIFICATION.md) | REST API仕様 |
| | [BATCH_JOB_DESIGN.md](docs/03_detailed_design/BATCH_JOB_DESIGN.md) | バッチ処理設計 |
| | [ERROR_HANDLING.md](docs/03_detailed_design/ERROR_HANDLING.md) | エラーハンドリング（RFC 7807） |
| | [SCREEN_DESIGN.md](docs/03_detailed_design/SCREEN_DESIGN.md) | 画面設計（Thymeleaf） |

### 運用ドキュメント

| カテゴリ | ドキュメント | 説明 |
|---------|------------|------|
| **デプロイ** | [DEPLOYMENT.md](docs/04_operations/DEPLOYMENT.md) | CI/CD、環境構成 |
| **監視** | [MONITORING.md](docs/04_operations/MONITORING.md) | Prometheus/Grafana設定 |
| **ログ** | [LOGGING.md](docs/04_operations/LOGGING.md) | 構造化ログ、保持期間 |
| **運用手順** | [RUNBOOK.md](docs/04_operations/RUNBOOK.md) | 障害対応、バックアップ |

### テストドキュメント

| カテゴリ | ドキュメント | 説明 |
|---------|------------|------|
| **テスト戦略** | [TEST_STRATEGY.md](docs/05_testing/TEST_STRATEGY.md) | テストレベル、カバレッジ目標 |
| **テストケース** | [TEST_CASES.md](docs/05_testing/TEST_CASES.md) | 単体・統合・E2Eテストケース |
| **パフォーマンス** | [PERFORMANCE_TEST.md](docs/05_testing/PERFORMANCE_TEST.md) | 負荷テスト計画 |

### 開発ガイド

| カテゴリ | ドキュメント | 説明 |
|---------|------------|------|
| **環境構築** | [LOCAL_DEVELOPMENT.md](docs/06_development/LOCAL_DEVELOPMENT.md) | ローカル開発環境セットアップ |
| **コーディング規約** | [CODING_STANDARDS.md](docs/06_development/CODING_STANDARDS.md) | Java/Spring Bootコーディング規約 |
| **Git運用** | [GIT_WORKFLOW.md](docs/06_development/GIT_WORKFLOW.md) | ブランチ戦略、コミットルール |

---

## 開発ガイド

### 開発環境セットアップ

詳細は [LOCAL_DEVELOPMENT.md](docs/06_development/LOCAL_DEVELOPMENT.md) を参照してください。

### コーディング規約

詳細は [CODING_STANDARDS.md](docs/06_development/CODING_STANDARDS.md) を参照してください。

### Git運用ルール

詳細は [GIT_WORKFLOW.md](docs/06_development/GIT_WORKFLOW.md) を参照してください。

---

## API仕様

### 認証エンドポイント

| Method | Path | 説明 |
|--------|------|------|
| GET | `/api/v1/auth/login` | OAuth認可フロー開始 |
| GET | `/api/v1/auth/callback` | OAuthコールバック |
| POST | `/api/v1/auth/refresh` | トークンリフレッシュ |
| POST | `/api/v1/auth/logout` | ログアウト |

### 従業員管理

| Method | Path | 説明 |
|--------|------|------|
| GET | `/api/v1/employees` | 従業員一覧取得 |
| GET | `/api/v1/employees/{id}` | 従業員詳細取得 |
| POST | `/api/v1/employees` | 従業員作成 |
| PUT | `/api/v1/employees/{id}` | 従業員更新 |
| DELETE | `/api/v1/employees/{id}` | 従業員削除 |

### 給与計算

| Method | Path | 説明 |
|--------|------|------|
| GET | `/api/v1/payrolls` | 給与計算実行 |

詳細は [API_SPECIFICATION.md](docs/03_detailed_design/API_SPECIFICATION.md) を参照してください。

---

## テスト

### 単体テスト実行

```bash
./mvnw test
```

### 統合テスト実行

```bash
./mvnw verify -P integration-test
```

### コードカバレッジ確認

```bash
./mvnw jacoco:report
# target/site/jacoco/index.html をブラウザで開く
```

### パフォーマンステスト実行

```bash
./mvnw gatling:test
```

---

## デプロイ

### ローカル環境

```bash
docker-compose up -d
```

### Staging環境

```bash
git push origin main
# GitHub Actionsが自動デプロイ
```

### Production環境

```bash
git tag v1.0.0
git push origin v1.0.0
# GitHub Actionsが自動デプロイ（Blue/Green）
```

詳細は [DEPLOYMENT.md](docs/04_operations/DEPLOYMENT.md) を参照してください。

---

## 監視

### メトリクス

- **Prometheus**: http://prometheus:9090
- **Grafana**: http://grafana:3000

### ログ

- **アプリケーションログ**: CloudWatch Logs
- **監査ログ**: PostgreSQL `audit_logs` テーブル

詳細は [MONITORING.md](docs/04_operations/MONITORING.md) を参照してください。

---

## トラブルシューティング

### よくある問題

| 問題 | 原因 | 解決方法 |
|------|------|---------|
| `Port 8080 already in use` | ポート衝突 | `docker-compose down` または別ポート使用 |
| `Connection refused (PostgreSQL)` | DBコンテナ未起動 | `docker-compose up -d postgres` |
| `JWT signature verification failed` | 公開鍵/秘密鍵不一致 | `src/main/resources/keys/` の鍵ペア確認 |
| `Google API authentication failed` | OAuth設定ミス | `application.yml` のクライアントID/Secret確認 |

詳細は [RUNBOOK.md](docs/04_operations/RUNBOOK.md) を参照してください。

---

## コントリビューション

プルリクエストを歓迎します！以下の手順に従ってください:

1. このリポジトリをフォーク
2. フィーチャーブランチを作成 (`git checkout -b feature/amazing-feature`)
3. コミット (`git commit -m 'Add amazing feature'`)
4. プッシュ (`git push origin feature/amazing-feature`)
5. プルリクエストを作成

コーディング規約、コミットメッセージルールは以下を参照:
- [CODING_STANDARDS.md](docs/06_development/CODING_STANDARDS.md)
- [GIT_WORKFLOW.md](docs/06_development/GIT_WORKFLOW.md)

---

## ライセンス

このプロジェクトはMITライセンスの下でライセンスされています。詳細は [LICENSE](LICENSE) ファイルを参照してください。

---

## 連絡先

- **プロジェクトオーナー**: [Your Name](mailto:your-email@example.com)
- **イシュー**: [GitHub Issues](https://github.com/your-org/attendance-management-system-java/issues)
- **ドキュメント**: [GitHub Wiki](https://github.com/your-org/attendance-management-system-java/wiki)

---

## 謝辞

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Google APIs](https://developers.google.com/)
- [PostgreSQL](https://www.postgresql.org/)
- [Bootstrap](https://getbootstrap.com/)

---

**© 2025 Attendance Management System. All rights reserved.**
