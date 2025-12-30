# ローカル開発環境構築手順

## 目次
- [前提条件](#前提条件)
- [環境構築手順](#環境構築手順)
- [IDEセットアップ](#ideセットアップ)
- [アプリケーション起動](#アプリケーション起動)
- [データベース初期化](#データベース初期化)
- [外部API設定](#外部api設定)
- [トラブルシューティング](#トラブルシューティング)

---

## 前提条件

### 必須ソフトウェア

| ソフトウェア | バージョン | 用途 |
|------------|-----------|------|
| **Java (JDK)** | 17 (LTS) | アプリケーション実行 |
| **Maven** | 3.9.x以上 | ビルドツール |
| **Docker Desktop** | 20.x以上 | PostgreSQL/Redis実行 |
| **Git** | 2.x以上 | バージョン管理 |
| **IDE** | IntelliJ IDEA 2024.x / Eclipse 2024-03 | 開発環境 |

### 推奨ソフトウェア

- **Postman** / **Insomnia**: API動作確認
- **DBeaver** / **pgAdmin**: データベース管理
- **curl**: コマンドラインでのAPI確認

---

## 環境構築手順

### Step 1: リポジトリクローン

```bash
# HTTPSでクローン
git clone https://github.com/your-org/attendance-management-system-java.git
cd attendance-management-system-java

# または SSHでクローン
git clone git@github.com:your-org/attendance-management-system-java.git
cd attendance-management-system-java
```

### Step 2: Java 17のインストール確認

```bash
# Javaバージョン確認
java -version
# 出力例: openjdk version "17.0.9" 2023-10-17

# JAVA_HOME環境変数確認
echo $JAVA_HOME  # Linux/Mac
echo %JAVA_HOME%  # Windows
```

**Java 17がインストールされていない場合**:
- **Windows**: [Adoptium Temurin 17](https://adoptium.net/) からインストール
- **macOS**: `brew install openjdk@17`
- **Linux (Ubuntu)**: `sudo apt install openjdk-17-jdk`

### Step 3: Mavenのインストール確認

```bash
# Mavenバージョン確認
mvn -version
# 出力例: Apache Maven 3.9.6
```

**Mavenがインストールされていない場合**:
- **Windows**: [Maven公式サイト](https://maven.apache.org/download.cgi) からダウンロード
- **macOS**: `brew install maven`
- **Linux (Ubuntu)**: `sudo apt install maven`

### Step 4: Docker Desktopのインストール確認

```bash
# Dockerバージョン確認
docker --version
# 出力例: Docker version 24.0.7

# Docker Composeバージョン確認
docker-compose --version
# 出力例: Docker Compose version v2.23.3
```

**Docker Desktopがインストールされていない場合**:
- [Docker Desktop公式サイト](https://www.docker.com/products/docker-desktop/) からインストール

---

## データベース初期化

### Step 1: PostgreSQLコンテナ起動

```bash
# docker-compose.ymlを使用してPostgreSQL起動
docker-compose up -d postgres

# コンテナ起動確認
docker-compose ps
# 出力例:
# NAME                         STATUS
# attendance-postgres          Up 10 seconds
```

### Step 2: データベース接続確認

```bash
# PostgreSQLコンテナに接続
docker-compose exec postgres psql -U attendance_user -d attendance_db

# 接続成功後、以下のコマンドでテーブル一覧表示
\dt

# 終了
\q
```

**接続情報**:
- **Host**: localhost
- **Port**: 5432
- **Database**: attendance_db
- **Username**: attendance_user
- **Password**: attendance_password (開発環境のみ)

### Step 3: Flywayマイグレーション実行

```bash
# Mavenを使用してFlywayマイグレーション実行
mvn flyway:migrate

# マイグレーション履歴確認
mvn flyway:info
```

**Flywayマイグレーションスクリプト配置場所**:
```
src/main/resources/db/migration/
├── V1__create_initial_schema.sql
├── V2__add_audit_columns.sql
└── V3__add_indexes.sql
```

---

## 外部API設定

### Google OAuth 2.0 設定

#### 1. Google Cloud Consoleでプロジェクト作成

1. [Google Cloud Console](https://console.cloud.google.com/) にアクセス
2. 新しいプロジェクトを作成: `attendance-management-local`
3. 「APIとサービス」→「認証情報」に移動

#### 2. OAuth 2.0 クライアントID作成

1. 「認証情報を作成」→「OAuth クライアント ID」を選択
2. アプリケーションの種類: **ウェブアプリケーション**
3. 承認済みのリダイレクトURI:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
4. **クライアントID**と**クライアントシークレット**をコピー

#### 3. application-local.ymlに設定

`src/main/resources/application-local.yml` を作成:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_CLIENT_ID_HERE
            client-secret: YOUR_CLIENT_SECRET_HERE
            scope:
              - openid
              - profile
              - email
              - https://www.googleapis.com/auth/calendar
              - https://www.googleapis.com/auth/spreadsheets.readonly
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
```

### Google Calendar API 有効化

1. Google Cloud Consoleで「APIとサービス」→「ライブラリ」に移動
2. 「Google Calendar API」を検索して有効化
3. 「Google Sheets API」も同様に有効化

---

## JWT鍵ペア生成

### RS256用の秘密鍵・公開鍵生成

```bash
# 秘密鍵生成 (RSA 2048bit)
openssl genrsa -out src/main/resources/jwt-private.pem 2048

# 公開鍵生成
openssl rsa -in src/main/resources/jwt-private.pem -pubout -out src/main/resources/jwt-public.pem

# 鍵ファイル確認
ls -la src/main/resources/jwt-*.pem
```

**application-local.ymlに追加**:

```yaml
app:
  security:
    jwt:
      private-key: classpath:jwt-private.pem
      public-key: classpath:jwt-public.pem
      token-validity: 3600000  # 1時間 (ミリ秒)
      refresh-token-validity: 604800000  # 7日間 (ミリ秒)
```

**⚠️ 重要**: `jwt-private.pem` は `.gitignore` に追加してコミットしないこと

```bash
# .gitignoreに追加
echo "src/main/resources/jwt-private.pem" >> .gitignore
echo "src/main/resources/jwt-public.pem" >> .gitignore
echo "src/main/resources/application-local.yml" >> .gitignore
```

---

## IDEセットアップ

### IntelliJ IDEA

#### 1. プロジェクトインポート

1. IntelliJ IDEAを起動
2. **File** → **Open** → `attendance-management-system-java` フォルダを選択
3. 「Maven project detected」と表示されたら **Import**
4. Maven依存関係の自動ダウンロード完了を待つ

#### 2. Java SDKの設定

1. **File** → **Project Structure** → **Project**
2. **SDK**: Java 17を選択 (なければ **Add SDK** から追加)
3. **Language level**: 17 - Sealed types, always-strict floating-point semantics

#### 3. コードスタイルの設定

1. **File** → **Settings** → **Editor** → **Code Style** → **Java**
2. **Scheme**: **Google Java Style Guide** をインポート
   - [google-java-format.xml](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml) をダウンロード
   - **Import Scheme** → **IntelliJ IDEA code style XML**
3. **Editor** → **Inspections**: **Spring**関連のインスペクションを有効化

#### 4. Lombok プラグイン有効化

1. **File** → **Settings** → **Plugins**
2. 「Lombok」で検索してインストール
3. **Settings** → **Build, Execution, Deployment** → **Compiler** → **Annotation Processors**
4. **Enable annotation processing** にチェック

#### 5. Spring Boot 実行構成

1. **Run** → **Edit Configurations**
2. **+** → **Spring Boot**
3. **Name**: `AttendanceApplication (Local)`
4. **Main class**: `com.example.attendance.AttendanceApplication`
5. **Active profiles**: `local`
6. **Environment variables**:
   ```
   SPRING_PROFILES_ACTIVE=local
   ```

### Eclipse

#### 1. プロジェクトインポート

1. Eclipseを起動
2. **File** → **Import** → **Maven** → **Existing Maven Projects**
3. **Root Directory**: `attendance-management-system-java` フォルダを選択
4. **Finish**

#### 2. Lombokのインストール

```bash
# lombok.jarをダウンロード
curl -O https://projectlombok.org/downloads/lombok.jar

# インストーラー実行
java -jar lombok.jar

# Eclipseのインストールディレクトリを指定してインストール
```

Eclipse再起動後、Lombokアノテーションが認識されます。

#### 3. Spring Tools 4のインストール

1. **Help** → **Eclipse Marketplace**
2. 「Spring Tools 4」で検索してインストール
3. Eclipse再起動

#### 4. 実行構成

1. **Run** → **Run Configurations**
2. **Spring Boot App** → **New Configuration**
3. **Project**: `attendance-management-system-java`
4. **Main Type**: `com.example.attendance.AttendanceApplication`
5. **Profile**: `local`

---

## アプリケーション起動

### 方法1: IDE経由で起動 (推奨)

**IntelliJ IDEA**:
1. `AttendanceApplication.java` を開く
2. `main` メソッドの左の緑の矢印をクリック
3. **Run 'AttendanceApplication'** を選択

**Eclipse**:
1. `AttendanceApplication.java` を右クリック
2. **Run As** → **Spring Boot App**

### 方法2: Maven経由で起動

```bash
# ローカルプロファイルで起動
mvn spring-boot:run -Dspring-boot.run.profiles=local

# またはjarをビルドしてから起動
mvn clean package -DskipTests
java -jar target/attendance-management-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 方法3: Docker Compose経由で起動

```bash
# アプリケーション含めて全コンテナ起動
docker-compose up -d

# ログ確認
docker-compose logs -f app
```

### 起動確認

**ヘルスチェック**:
```bash
curl http://localhost:8080/actuator/health
# 出力例:
# {"status":"UP"}
```

**アクセスURL**:
- 管理画面: http://localhost:8080
- API: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator

---

## ホットリロード設定

### Spring Boot DevToolsの有効化

`pom.xml` に依存関係が含まれていることを確認:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### IntelliJ IDEAでの設定

1. **File** → **Settings** → **Build, Execution, Deployment** → **Compiler**
2. **Build project automatically** にチェック
3. **Ctrl+Shift+A** (Windows/Linux) / **Cmd+Shift+A** (Mac) で「Registry」を検索
4. `compiler.automake.allow.when.app.running` にチェック

これでJavaファイル保存時に自動リビルド・再起動されます。

### Eclipseでの設定

1. **Project** → **Build Automatically** にチェック
2. Javaファイル保存時に自動的にリビルドされます

---

## デバッグ設定

### IntelliJ IDEA

1. ブレークポイントを設定 (行番号の左をクリック)
2. **Run** → **Debug 'AttendanceApplication'**
3. リクエスト実行時にブレークポイントで停止

**リモートデバッグ (Dockerコンテナ)**:

`docker-compose.yml` に以下を追加:

```yaml
services:
  app:
    environment:
      - JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
    ports:
      - "5005:5005"
```

IntelliJ IDEAで:
1. **Run** → **Edit Configurations** → **+** → **Remote JVM Debug**
2. **Host**: localhost
3. **Port**: 5005
4. **Debug**

---

## トラブルシューティング

### 問題1: ポート8080が既に使用されている

**エラーメッセージ**:
```
Web server failed to start. Port 8080 was already in use.
```

**解決方法**:

**Option A: 別のポートを使用**

`application-local.yml`:
```yaml
server:
  port: 8081
```

**Option B: 既存プロセスを停止**

Windows:
```cmd
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

macOS/Linux:
```bash
lsof -i :8080
kill -9 <PID>
```

### 問題2: PostgreSQL接続エラー

**エラーメッセージ**:
```
Connection to localhost:5432 refused.
```

**解決方法**:

```bash
# PostgreSQLコンテナ状態確認
docker-compose ps postgres

# コンテナが停止している場合は起動
docker-compose up -d postgres

# ログ確認
docker-compose logs postgres

# 接続テスト
docker-compose exec postgres pg_isready -U attendance_user
```

### 問題3: Flywayマイグレーションエラー

**エラーメッセージ**:
```
FlywayException: Validate failed: Migration checksum mismatch
```

**解決方法**:

```bash
# Flyway履歴をクリーン (開発環境のみ！)
mvn flyway:clean

# マイグレーション再実行
mvn flyway:migrate

# または、データベースを完全に再構築
docker-compose down -v
docker-compose up -d postgres
mvn flyway:migrate
```

### 問題4: Google OAuth認証エラー

**エラーメッセージ**:
```
OAuth2AuthenticationException: invalid_client
```

**チェックリスト**:
- [ ] Google Cloud Consoleで正しいクライアントIDとシークレットをコピーしたか
- [ ] `application-local.yml` にクライアントIDとシークレットを設定したか
- [ ] リダイレクトURIが `http://localhost:8080/login/oauth2/code/google` と完全一致するか
- [ ] Google Calendar APIとGoogle Sheets APIを有効化したか

### 問題5: Lombokが動作しない

**症状**: `@Data`, `@Builder` などのアノテーションでコンパイルエラー

**解決方法**:

**IntelliJ IDEA**:
1. Lombokプラグインがインストールされているか確認
2. **Settings** → **Annotation Processors** → **Enable annotation processing**

**Eclipse**:
```bash
# Lombokを再インストール
java -jar lombok.jar
```

**Maven**:
```bash
# プロジェクトをクリーンビルド
mvn clean compile
```

### 問題6: テスト実行でTestcontainersエラー

**エラーメッセージ**:
```
Could not find a valid Docker environment.
```

**解決方法**:

1. Docker Desktopが起動しているか確認
2. Dockerデーモンが動作しているか確認:
   ```bash
   docker ps
   ```
3. Testcontainersがローカルホストにアクセスできるか確認:
   ```bash
   docker run --rm -it alpine ping host.docker.internal
   ```

---

## 開発Tips

### 1. ログレベル調整

`application-local.yml`:
```yaml
logging:
  level:
    root: INFO
    com.example.attendance: DEBUG  # アプリケーションログをDEBUGレベルに
    org.springframework.web: DEBUG  # Spring WebのログをDEBUGレベルに
    org.hibernate.SQL: DEBUG  # SQLログを表示
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # バインドパラメータ表示
```

### 2. データベース初期データ投入

`src/main/resources/data-local.sql` を作成:

```sql
-- 管理者ユーザー追加
INSERT INTO users (id, email, name, role, created_at, updated_at)
VALUES ('admin-001', 'admin@example.com', 'Admin User', 'ADMIN', NOW(), NOW());

-- テスト用従業員追加
INSERT INTO employees (id, user_id, employee_number, name, created_at, updated_at)
VALUES ('emp-001', 'admin-001', 'EMP001', 'Test Employee', NOW(), NOW());
```

`application-local.yml`:
```yaml
spring:
  sql:
    init:
      mode: always
      data-locations: classpath:data-local.sql
```

### 3. API動作確認用curlコマンド

```bash
# ヘルスチェック
curl http://localhost:8080/actuator/health

# 従業員一覧取得 (要認証)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/v1/employees

# 給与計算実行
curl -X POST \
     -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"employeeId":"emp-001","startDate":"2024-01-01","endDate":"2024-01-31"}' \
     http://localhost:8080/api/v1/payrolls/calculate
```

### 4. データベースGUI接続 (DBeaver)

1. DBeaver起動
2. **Database** → **New Database Connection** → **PostgreSQL**
3. 接続情報:
   - **Host**: localhost
   - **Port**: 5432
   - **Database**: attendance_db
   - **Username**: attendance_user
   - **Password**: attendance_password
4. **Test Connection** → **Finish**

---

## 次のステップ

1. [コーディング規約](./CODING_STANDARDS.md) を確認
2. [Git運用ルール](./GIT_WORKFLOW.md) を確認
3. [API仕様書](../02_basic_design/API_SPECIFICATION.md) を参照して実装開始
4. [テスト戦略](../05_testing/TEST_STRATEGY.md) に従ってテストコード作成

---

## 参考リンク

- [Spring Boot Reference Documentation](https://spring.io/projects/spring-boot#learn)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Google OAuth 2.0 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Lombok Features](https://projectlombok.org/features/all)
