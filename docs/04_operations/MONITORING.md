# 監視戦略書

- バージョン: 1.0
- 作成日: 2025年12月30日
- 最終更新日: 2025年12月30日
- 作成者: Project Manager

---

## 1. 概要

本ドキュメントは、勤怠管理システムの監視戦略を定義する。Prometheus/Grafanaによるメトリクス収集、アラート設定、ダッシュボード設計を記載し、システムの健全性を継続的に監視する体制を構築する。

---

## 2. 監視の目的と目標

### 2.1. 監視の目的

1. **可用性の確保**: システムダウンを早期検知し、迅速に復旧する
2. **パフォーマンス維持**: レスポンス時間やスループットを監視し、劣化を防ぐ
3. **リソース最適化**: CPU/メモリ/DBコネクションの使用状況を把握し、適切にスケーリング
4. **ビジネス監視**: 給与計算実行回数、バッチ成功率など、業務指標を追跡
5. **セキュリティ**: 異常なアクセスパターンを検知

### 2.2. SLO（Service Level Objectives）

| 指標 | 目標値 | 測定方法 |
|------|--------|---------|
| **稼働率** | 99.5% 以上 | ヘルスチェック成功率 |
| **APIレスポンス時間** | P95 < 500ms | HTTPリクエストメトリクス |
| **バッチ成功率** | 95% 以上 | バッチジョブ履歴テーブル |
| **エラー率** | < 1% | HTTP 5xxエラー率 |
| **DB接続プール使用率** | < 80% | HikariCP メトリクス |

---

## 3. メトリクス収集アーキテクチャ

```mermaid
graph LR
    subgraph "Spring Boot Application"
        App[Application]
        Actuator[Spring Boot Actuator]
        Micrometer[Micrometer]
    end

    subgraph "メトリクス基盤"
        Prometheus[Prometheus Server]
        AlertManager[Alert Manager]
        Grafana[Grafana]
    end

    subgraph "通知"
        Slack[Slack]
        Email[Email]
        PagerDuty[PagerDuty]
    end

    App --> Actuator
    Actuator --> Micrometer
    Prometheus --> |Scrape| Micrometer
    Prometheus --> AlertManager
    Prometheus --> Grafana
    AlertManager --> Slack
    AlertManager --> Email
    AlertManager --> PagerDuty
```

---

## 4. Spring Boot Actuator設定

### 4.1. pom.xml

```xml
<dependencies>
    <!-- Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Prometheus -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- カスタムメトリクス用 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-core</artifactId>
    </dependency>
</dependencies>
```

### 4.2. application.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
    metrics:
      enabled: true
    prometheus:
      enabled: true

  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      sla:
        http.server.requests: 100ms,200ms,500ms,1s,2s

  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true

spring:
  application:
    name: attendance-management-api
```

### 4.3. カスタムメトリクス実装

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class CustomMetrics {

    private final Counter payrollCalculationCounter;
    private final Counter calendarSyncSuccessCounter;
    private final Counter calendarSyncFailureCounter;
    private final Timer payrollCalculationTimer;

    public CustomMetrics(MeterRegistry registry) {
        // 給与計算実行回数
        this.payrollCalculationCounter = Counter.builder("payroll.calculation.total")
            .description("Total number of payroll calculations")
            .tag("type", "calculation")
            .register(registry);

        // カレンダー同期成功回数
        this.calendarSyncSuccessCounter = Counter.builder("calendar.sync.success")
            .description("Successful calendar sync count")
            .register(registry);

        // カレンダー同期失敗回数
        this.calendarSyncFailureCounter = Counter.builder("calendar.sync.failure")
            .description("Failed calendar sync count")
            .register(registry);

        // 給与計算処理時間
        this.payrollCalculationTimer = Timer.builder("payroll.calculation.duration")
            .description("Time taken to calculate payroll")
            .register(registry);
    }

    public void incrementPayrollCalculation() {
        payrollCalculationCounter.increment();
    }

    public void recordCalendarSyncSuccess() {
        calendarSyncSuccessCounter.increment();
    }

    public void recordCalendarSyncFailure() {
        calendarSyncFailureCounter.increment();
    }

    public Timer.Sample startPayrollTimer() {
        return Timer.start();
    }

    public void stopPayrollTimer(Timer.Sample sample) {
        sample.stop(payrollCalculationTimer);
    }
}
```

**使用例**:

```java
@Service
@RequiredArgsConstructor
public class PayrollService {

    private final CustomMetrics metrics;
    private final WorkRecordRepository workRecordRepository;

    public PayrollDto calculatePayroll(Integer employeeId, LocalDate start, LocalDate end) {
        Timer.Sample sample = metrics.startPayrollTimer();

        try {
            // 給与計算ロジック
            PayrollDto result = performCalculation(employeeId, start, end);

            metrics.incrementPayrollCalculation();
            return result;
        } finally {
            metrics.stopPayrollTimer(sample);
        }
    }
}
```

---

## 5. Prometheus設定

### 5.1. prometheus.yml

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'attendance-management'
    environment: 'production'

# Alertmanager設定
alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

# アラートルール読み込み
rule_files:
  - "alerts/*.yml"

# スクレイプ設定
scrape_configs:
  # Spring Boot アプリケーション
  - job_name: 'attendance-api-prod'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'attendance-api-1:8080'
          - 'attendance-api-2:8080'
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
      - source_labels: [__address__]
        regex: '([^:]+):.*'
        target_label: host
        replacement: '$1'

  # PostgreSQL Exporter
  - job_name: 'postgres'
    static_configs:
      - targets:
          - 'postgres-exporter:9187'

  # Node Exporter（サーバーメトリクス）
  - job_name: 'node'
    static_configs:
      - targets:
          - 'node-exporter:9100'
```

### 5.2. PostgreSQL Exporter設定

**docker-compose.yml（監視スタック）**:

```yaml
version: '3.8'

services:
  postgres-exporter:
    image: prometheuscommunity/postgres-exporter:latest
    environment:
      DATA_SOURCE_NAME: "postgresql://postgres:password@postgres:5432/attendance_prod?sslmode=disable"
    ports:
      - "9187:9187"
    depends_on:
      - postgres

  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus/alerts:/etc/prometheus/alerts
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'

  alertmanager:
    image: prom/alertmanager:latest
    volumes:
      - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    ports:
      - "9093:9093"

  grafana:
    image: grafana/grafana:latest
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./grafana/datasources:/etc/grafana/provisioning/datasources
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD}
      GF_INSTALL_PLUGINS: grafana-piechart-panel

volumes:
  prometheus-data:
  grafana-data:
```

---

## 6. アラートルール

### 6.1. alerts/api-alerts.yml

```yaml
groups:
  - name: api_alerts
    interval: 30s
    rules:
      # 高エラー率
      - alert: HighErrorRate
        expr: |
          (
            sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
            /
            sum(rate(http_server_requests_seconds_count[5m]))
          ) > 0.05
        for: 5m
        labels:
          severity: critical
          component: api
        annotations:
          summary: "High HTTP 5xx error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }}. More than 5% of requests are failing."

      # 高レスポンス時間
      - alert: HighResponseTime
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
          ) > 0.5
        for: 10m
        labels:
          severity: warning
          component: api
        annotations:
          summary: "High API response time"
          description: "P95 response time for {{ $labels.uri }} is {{ $value }}s (threshold: 0.5s)"

      # APIダウン
      - alert: APIDown
        expr: up{job="attendance-api-prod"} == 0
        for: 2m
        labels:
          severity: critical
          component: api
        annotations:
          summary: "API instance is down"
          description: "Instance {{ $labels.instance }} has been down for more than 2 minutes"

      # 高トラフィック
      - alert: HighTraffic
        expr: |
          sum(rate(http_server_requests_seconds_count[5m])) > 100
        for: 5m
        labels:
          severity: info
          component: api
        annotations:
          summary: "High traffic detected"
          description: "Receiving {{ $value }} requests per second"
```

### 6.2. alerts/database-alerts.yml

```yaml
groups:
  - name: database_alerts
    interval: 30s
    rules:
      # DB接続プール枯渇
      - alert: DatabaseConnectionPoolExhausted
        expr: |
          hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 5m
        labels:
          severity: critical
          component: database
        annotations:
          summary: "Database connection pool is exhausted"
          description: "{{ $value | humanizePercentage }} of connection pool is in use"

      # DB接続失敗
      - alert: DatabaseConnectionFailure
        expr: |
          increase(hikaricp_connections_timeout_total[5m]) > 5
        for: 2m
        labels:
          severity: critical
          component: database
        annotations:
          summary: "Database connection timeouts detected"
          description: "{{ $value }} connection timeout events in the last 5 minutes"

      # スロークエリ
      - alert: SlowQueries
        expr: |
          histogram_quantile(0.95,
            sum(rate(spring_data_repository_invocations_seconds_bucket[5m])) by (le, repository)
          ) > 1.0
        for: 10m
        labels:
          severity: warning
          component: database
        annotations:
          summary: "Slow database queries detected"
          description: "P95 query time for {{ $labels.repository }} is {{ $value }}s"

      # PostgreSQL ダウン
      - alert: PostgreSQLDown
        expr: up{job="postgres"} == 0
        for: 2m
        labels:
          severity: critical
          component: database
        annotations:
          summary: "PostgreSQL is down"
          description: "PostgreSQL has been unreachable for more than 2 minutes"
```

### 6.3. alerts/batch-alerts.yml

```yaml
groups:
  - name: batch_alerts
    interval: 1m
    rules:
      # カレンダー同期失敗
      - alert: CalendarSyncFailureHigh
        expr: |
          sum(increase(calendar_sync_failure[1h])) > 10
        for: 5m
        labels:
          severity: warning
          component: batch
        annotations:
          summary: "Calendar sync job failing repeatedly"
          description: "{{ $value }} calendar sync failures in the last hour"

      # カレンダー同期成功率低下
      - alert: CalendarSyncSuccessRateLow
        expr: |
          (
            sum(increase(calendar_sync_success[1h]))
            /
            (sum(increase(calendar_sync_success[1h])) + sum(increase(calendar_sync_failure[1h])))
          ) < 0.9
        for: 10m
        labels:
          severity: critical
          component: batch
        annotations:
          summary: "Calendar sync success rate is low"
          description: "Success rate is {{ $value | humanizePercentage }} (threshold: 90%)"

      # バッチ処理時間超過
      - alert: BatchJobTooLong
        expr: |
          calendar_sync_duration_seconds > 3600
        for: 5m
        labels:
          severity: warning
          component: batch
        annotations:
          summary: "Batch job taking too long"
          description: "Calendar sync has been running for {{ $value | humanizeDuration }}"
```

### 6.4. alerts/business-alerts.yml

```yaml
groups:
  - name: business_alerts
    interval: 5m
    rules:
      # 給与計算回数異常（通常より少ない）
      - alert: PayrollCalculationAnomalyLow
        expr: |
          sum(increase(payroll_calculation_total[1d])) < 10
        for: 1h
        labels:
          severity: warning
          component: business
        annotations:
          summary: "Unusually low payroll calculation count"
          description: "Only {{ $value }} payroll calculations today (expected: >10)"

      # 給与計算エラー率高
      - alert: PayrollCalculationErrorRateHigh
        expr: |
          (
            sum(increase(payroll_calculation_error_total[1h]))
            /
            sum(increase(payroll_calculation_total[1h]))
          ) > 0.1
        for: 10m
        labels:
          severity: critical
          component: business
        annotations:
          summary: "High payroll calculation error rate"
          description: "{{ $value | humanizePercentage }} of payroll calculations are failing"
```

---

## 7. Alertmanager設定

### 7.1. alertmanager/alertmanager.yml

```yaml
global:
  resolve_timeout: 5m
  slack_api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'

# ルート設定
route:
  receiver: 'default'
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h

  routes:
    # Critical: Slack + PagerDuty + Email
    - match:
        severity: critical
      receiver: 'critical-alerts'
      continue: true

    # Warning: Slack + Email
    - match:
        severity: warning
      receiver: 'warning-alerts'

    # Info: Slackのみ
    - match:
        severity: info
      receiver: 'info-alerts'

# 受信者設定
receivers:
  - name: 'default'
    slack_configs:
      - channel: '#alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

  - name: 'critical-alerts'
    slack_configs:
      - channel: '#alerts-critical'
        color: 'danger'
        title: ':fire: CRITICAL: {{ .GroupLabels.alertname }}'
        text: |
          *Alert:* {{ .GroupLabels.alertname }}
          *Severity:* {{ .GroupLabels.severity }}
          *Description:* {{ range .Alerts }}{{ .Annotations.description }}{{ end }}
          *Dashboard:* <https://grafana.example.com/d/attendance-api|View Dashboard>

    pagerduty_configs:
      - service_key: 'YOUR_PAGERDUTY_SERVICE_KEY'
        description: '{{ .GroupLabels.alertname }}: {{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

    email_configs:
      - to: 'oncall@example.com'
        from: 'alertmanager@example.com'
        smarthost: 'smtp.gmail.com:587'
        auth_username: 'alertmanager@example.com'
        auth_password: 'YOUR_PASSWORD'
        headers:
          Subject: '[CRITICAL] {{ .GroupLabels.alertname }}'

  - name: 'warning-alerts'
    slack_configs:
      - channel: '#alerts'
        color: 'warning'
        title: ':warning: WARNING: {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

    email_configs:
      - to: 'team@example.com'
        from: 'alertmanager@example.com'
        smarthost: 'smtp.gmail.com:587'
        auth_username: 'alertmanager@example.com'
        auth_password: 'YOUR_PASSWORD'

  - name: 'info-alerts'
    slack_configs:
      - channel: '#monitoring'
        color: 'good'
        title: ':information_source: INFO: {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

# アラート抑制ルール
inhibit_rules:
  # APIダウン時は他のアラートを抑制
  - source_match:
      alertname: 'APIDown'
    target_match_re:
      alertname: '(HighErrorRate|HighResponseTime)'
    equal: ['instance']

  # DBダウン時は関連アラートを抑制
  - source_match:
      alertname: 'PostgreSQLDown'
    target_match_re:
      alertname: '(DatabaseConnectionFailure|SlowQueries)'
```

---

## 8. Grafanaダッシュボード

### 8.1. データソース設定

**grafana/datasources/prometheus.yml**:

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

### 8.2. ダッシュボード定義

**grafana/dashboards/dashboard-config.yml**:

```yaml
apiVersion: 1

providers:
  - name: 'Attendance Management API'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
```

### 8.3. メインダッシュボード構成

#### パネル1: システム概要

| メトリクス | クエリ | 可視化 |
|-----------|-------|--------|
| リクエスト数/秒 | `sum(rate(http_server_requests_seconds_count[5m]))` | グラフ |
| エラー率 | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))` | ゲージ |
| P95レスポンス時間 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))` | グラフ |
| アクティブユーザー数 | `count(count by (user) (http_server_requests_seconds_count))` | 数値 |

#### パネル2: API詳細

| メトリクス | クエリ | 可視化 |
|-----------|-------|--------|
| エンドポイント別リクエスト数 | `sum(rate(http_server_requests_seconds_count[5m])) by (uri)` | テーブル |
| エンドポイント別レスポンス時間 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))` | ヒートマップ |
| HTTPステータスコード分布 | `sum(rate(http_server_requests_seconds_count[5m])) by (status)` | 円グラフ |

#### パネル3: データベース

| メトリクス | クエリ | 可視化 |
|-----------|-------|--------|
| DB接続プール使用率 | `hikaricp_connections_active / hikaricp_connections_max * 100` | ゲージ |
| DB接続待ち時間 | `histogram_quantile(0.95, sum(rate(hikaricp_connections_acquire_seconds_bucket[5m])) by (le))` | グラフ |
| クエリ実行時間 | `histogram_quantile(0.95, sum(rate(spring_data_repository_invocations_seconds_bucket[5m])) by (le, repository))` | グラフ |
| PostgreSQL接続数 | `pg_stat_database_numbackends` | グラフ |

#### パネル4: JVMメトリクス

| メトリクス | クエリ | 可視化 |
|-----------|-------|--------|
| ヒープメモリ使用率 | `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100` | ゲージ |
| GC時間 | `rate(jvm_gc_pause_seconds_sum[5m])` | グラフ |
| スレッド数 | `jvm_threads_live_threads` | グラフ |
| CPU使用率 | `process_cpu_usage * 100` | ゲージ |

#### パネル5: ビジネスメトリクス

| メトリクス | クエリ | 可視化 |
|-----------|-------|--------|
| 給与計算実行回数（1日） | `increase(payroll_calculation_total[1d])` | 数値 |
| カレンダー同期成功率 | `sum(increase(calendar_sync_success[1h])) / (sum(increase(calendar_sync_success[1h])) + sum(increase(calendar_sync_failure[1h]))) * 100` | ゲージ |
| バッチ処理時間 | `calendar_sync_duration_seconds` | グラフ |
| アクティブ従業員数 | `count(employees{is_active="true"})` | 数値 |

---

## 9. ヘルスチェックエンドポイント

### 9.1. カスタムヘルスインジケータ

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class GoogleApiHealthIndicator implements HealthIndicator {

    private final GoogleCalendarService googleCalendarService;

    public GoogleApiHealthIndicator(GoogleCalendarService googleCalendarService) {
        this.googleCalendarService = googleCalendarService;
    }

    @Override
    public Health health() {
        try {
            // Google APIへの疎通確認
            boolean isReachable = googleCalendarService.checkConnection();

            if (isReachable) {
                return Health.up()
                    .withDetail("google-calendar", "Available")
                    .build();
            } else {
                return Health.down()
                    .withDetail("google-calendar", "Unreachable")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("google-calendar", "Error")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 9.2. ヘルスチェックレスポンス例

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
        "total": 250685575168,
        "free": 100000000000,
        "threshold": 10485760,
        "exists": true
      }
    },
    "googleApi": {
      "status": "UP",
      "details": {
        "google-calendar": "Available"
      }
    },
    "livenessState": {
      "status": "UP"
    },
    "readinessState": {
      "status": "UP"
    }
  }
}
```

---

## 10. ログベース監視

### 10.1. エラーログ監視

**CloudWatch Logs Insightsクエリ例**:

```sql
-- 過去1時間のエラーログ集計
fields @timestamp, level, logger, message
| filter level = "ERROR"
| stats count() by logger
| sort count desc
```

```sql
-- 給与計算エラーの詳細
fields @timestamp, message, context.employeeId, context.error
| filter logger = "com.example.attendance.service.PayrollService"
| filter level = "ERROR"
| sort @timestamp desc
| limit 100
```

### 10.2. ログベースアラート

**CloudWatch Alarms設定**:

```json
{
  "AlarmName": "HighErrorLogCount",
  "MetricName": "ErrorLogCount",
  "Namespace": "AttendanceAPI",
  "Statistic": "Sum",
  "Period": 300,
  "EvaluationPeriods": 1,
  "Threshold": 10,
  "ComparisonOperator": "GreaterThanThreshold",
  "AlarmActions": [
    "arn:aws:sns:ap-northeast-1:123456789012:attendance-alerts"
  ]
}
```

---

## 11. ダッシュボード運用

### 11.1. 日次確認項目

| 時刻 | 確認項目 | 正常基準 |
|------|---------|---------|
| 09:00 | バッチ処理成功確認 | すべて成功 |
| 09:00 | エラーログ確認 | ERRORログ < 10件/日 |
| 12:00 | API レスポンス時間 | P95 < 500ms |
| 18:00 | DB接続プール使用率 | < 80% |
| 23:00 | ディスク使用率 | < 80% |

### 11.2. 週次確認項目

| 曜日 | 確認項目 | アクション |
|------|---------|-----------|
| 月曜 | 先週のインシデント振り返り | 再発防止策の検討 |
| 水曜 | パフォーマンストレンド分析 | スケーリング計画の見直し |
| 金曜 | セキュリティログレビュー | 異常アクセスの確認 |

---

## 12. まとめ

本監視戦略により、以下を実現する:

1. ✅ **リアルタイム監視**: Prometheusによる15秒間隔のメトリクス収集
2. ✅ **自動アラート**: Criticalアラートは即座にSlack/PagerDutyで通知
3. ✅ **可視化**: Grafanaダッシュボードでシステム全体を一望
4. ✅ **ビジネス監視**: 給与計算回数、バッチ成功率など業務指標も追跡
5. ✅ **迅速な問題解決**: アラートとログを組み合わせた効率的なトラブルシューティング

システムの健全性が継続的に監視され、問題の早期発見と迅速な対応が可能な体制が整っている。
