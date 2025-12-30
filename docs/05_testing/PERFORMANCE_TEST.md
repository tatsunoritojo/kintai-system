# パフォーマンステスト計画書

- バージョン: 1.0
- 作成日: 2025年12月30日
- 最終更新日: 2025年12月30日
- 作成者: Project Manager

---

## 1. 概要

本ドキュメントは、勤怠管理システムのパフォーマンステスト計画を定義する。非機能要件（NFR-002: パフォーマンス）の検証、ボトルネックの特定、スケーラビリティの確認を目的とし、JMeter/Gatlingを用いた負荷テストシナリオを記載する。

---

## 2. パフォーマンス要件（NFR-002）

### 2.1. 目標値

| 指標 | 目標値 | 測定条件 |
|------|--------|---------|
| **レスポンス時間（平均）** | < 300ms | 通常負荷時（10同時ユーザー） |
| **レスポンス時間（P95）** | < 500ms | 通常負荷時 |
| **レスポンス時間（P99）** | < 1秒 | ピーク負荷時（100同時ユーザー） |
| **スループット** | > 100 req/s | ピーク負荷時 |
| **エラー率** | < 1% | すべての負荷条件 |
| **同時接続数** | 100ユーザー | ピーク時 |
| **CPU使用率** | < 70% | 通常負荷時 |
| **メモリ使用率** | < 80% | 通常負荷時 |
| **DB接続プール使用率** | < 80% | 通常負荷時 |

### 2.2. 許容値

| 指標 | 許容値 | 備考 |
|------|--------|------|
| **レスポンス時間（P95）** | < 1秒 | 目標値を超えた場合でも許容 |
| **エラー率** | < 5% | 一時的な高負荷時のみ |
| **CPU使用率** | < 90% | ピーク時のみ |

---

## 3. テスト環境

### 3.1. 環境構成

| 環境 | 用途 | 構成 |
|------|------|------|
| **Staging** | パフォーマンステスト実施環境 | 本番と同一構成 |
| **Production** | 本番前最終確認 | 実際の本番環境 |

**Staging環境スペック**:
- **アプリケーション**: AWS ECS Fargate, 2 vCPU, 4GB RAM × 2インスタンス
- **データベース**: AWS RDS PostgreSQL 15, db.t3.medium (2 vCPU, 4GB RAM)
- **ロードバランサー**: AWS ALB
- **キャッシュ**: Redis 7 (cache.t3.micro)

### 3.2. テストデータ

| テーブル | レコード数 | 備考 |
|---------|-----------|------|
| **employees** | 100件 | 実際の従業員数を想定 |
| **work_records** | 50,000件 | 1年分の勤務記録（100従業員 × 500件） |
| **students** | 500件 | 実際の生徒数を想定 |
| **hourly_wages** | 50件 | すべての勤務形態×生徒レベルの組み合わせ |

**テストデータ生成スクリプト**:

```sql
-- performance-test-data.sql

-- 100人の従業員
INSERT INTO employees (email, name, role, is_active, created_at, updated_at)
SELECT
    'employee' || id || '@example.com',
    'Employee ' || id,
    CASE WHEN id <= 10 THEN 'ADMIN' ELSE 'USER' END,
    true,
    NOW(),
    NOW()
FROM generate_series(1, 100) AS id;

-- 50,000件の勤務記録（1年分）
INSERT INTO work_records (
    employee_id,
    google_event_id,
    event_title,
    start_time,
    end_time,
    work_type_id,
    student_id,
    is_paid,
    synced_at,
    created_at,
    updated_at
)
SELECT
    (random() * 99 + 1)::INTEGER,
    'event_' || gen_id,
    '個別指導(生徒' || (random() * 499 + 1)::INTEGER || ')',
    NOW() - (random() * 365 || ' days')::INTERVAL,
    NOW() - (random() * 365 || ' days')::INTERVAL + '90 minutes'::INTERVAL,
    1,
    (random() * 499 + 1)::INTEGER,
    false,
    NOW(),
    NOW(),
    NOW()
FROM generate_series(1, 50000) AS gen_id;
```

---

## 4. テストツール

### 4.1. Apache JMeter

**選定理由**:
- GUIで視覚的にテストシナリオを作成可能
- 豊富なプラグインとレポート機能
- CI/CD統合が容易

**インストール**:

```bash
# Homebrewでインストール (macOS)
brew install jmeter

# またはDocker使用
docker pull justb4/jmeter:latest
```

### 4.2. Gatling

**選定理由**:
- Scalaベースで高パフォーマンス
- コードベースでシナリオ定義（バージョン管理しやすい）
- リアルタイムレポートが優秀

**インストール**:

```bash
# Mavenプロジェクトに追加
<dependency>
    <groupId>io.gatling.highcharts</groupId>
    <artifactId>gatling-maven-plugin</artifactId>
    <version>4.6.0</version>
</dependency>
```

---

## 5. テストシナリオ

### 5.1. シナリオ1: 通常負荷テスト（Baseline）

**目的**: 通常運用時のパフォーマンスを測定

**条件**:
- 同時ユーザー数: 10
- テスト時間: 30分
- ランプアップ: 2分

**シナリオ**:

| ステップ | アクション | 割合 | レスポンス目標 |
|---------|----------|------|-------------|
| 1 | ログイン | 5% | < 200ms |
| 2 | 従業員一覧取得 | 20% | < 300ms |
| 3 | 給与計算実行 | 50% | < 500ms |
| 4 | 勤務記録一覧取得 | 20% | < 400ms |
| 5 | ログアウト | 5% | < 100ms |

**JMeterスクリプト**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Baseline Load Test">
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">https://staging-api.example.com</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Users">
        <stringProp name="ThreadGroup.num_threads">10</stringProp>
        <stringProp name="ThreadGroup.ramp_time">120</stringProp>
        <stringProp name="ThreadGroup.duration">1800</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
      </ThreadGroup>
      <hashTree>
        <!-- HTTPリクエスト: ログイン -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Login">
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/auth/login</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
        </HTTPSamplerProxy>

        <!-- HTTPリクエスト: 給与計算 -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Calculate Payroll">
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/payrolls</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="employeeId" elementType="HTTPArgument">
                <stringProp name="Argument.value">${__Random(1,100)}</stringProp>
              </elementProp>
              <elementProp name="startDate" elementType="HTTPArgument">
                <stringProp name="Argument.value">2025-11-01</stringProp>
              </elementProp>
              <elementProp name="endDate" elementType="HTTPArgument">
                <stringProp name="Argument.value">2025-11-30</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
        </HTTPSamplerProxy>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

**実行コマンド**:

```bash
jmeter -n -t baseline-load-test.jmx \
    -l results/baseline-results.jtl \
    -e -o results/baseline-report
```

---

### 5.2. シナリオ2: ストレステスト（Peak Load）

**目的**: ピーク時の負荷に耐えられるか検証

**条件**:
- 同時ユーザー数: 100
- テスト時間: 15分
- ランプアップ: 5分

**期待結果**:
- P99レスポンス時間 < 1秒
- エラー率 < 1%
- システムダウンなし

**Gatlingスクリプト**:

```scala
package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class StressTestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("https://staging-api.example.com")
    .acceptHeader("application/json")
    .userAgentHeader("Gatling Performance Test")

  val scn = scenario("Stress Test Scenario")
    .exec(http("Login")
      .post("/api/v1/auth/login")
      .body(StringBody("""{"email":"user@example.com","password":"password"}"""))
      .check(status.is(200))
      .check(jsonPath("$.accessToken").saveAs("token"))
    )
    .pause(1)
    .repeat(10) {
      exec(http("Calculate Payroll")
        .get("/api/v1/payrolls")
        .queryParam("employeeId", "${__Random(1,100)}")
        .queryParam("startDate", "2025-11-01")
        .queryParam("endDate", "2025-11-30")
        .header("Authorization", "Bearer ${token}")
        .check(status.is(200))
        .check(responseTimeInMillis.lte(1000)) // P99 < 1秒
      )
      .pause(2)
    }

  setUp(
    scn.inject(
      rampUsers(100) during (5 minutes)
    ).protocols(httpProtocol)
  ).maxDuration(15 minutes)
    .assertions(
      global.responseTime.percentile(99).lt(1000),
      global.successfulRequests.percent.gt(99)
    )
}
```

**実行コマンド**:

```bash
mvn gatling:test -Dgatling.simulationClass=simulations.StressTestSimulation
```

---

### 5.3. シナリオ3: スパイクテスト（Sudden Load）

**目的**: 急激な負荷増加に対する挙動を確認

**条件**:
- 初期ユーザー数: 10
- スパイク: 1分間で100ユーザーに増加
- スパイク後: 10ユーザーに戻す
- テスト時間: 10分

**期待結果**:
- スパイク時もシステムダウンしない
- 一時的なレスポンス時間悪化は許容
- スパイク終了後、正常なレスポンス時間に復帰

**JMeter設定**:

```xml
<ThreadGroup>
  <stringProp name="ThreadGroup.num_threads">100</stringProp>
  <stringProp name="ThreadGroup.ramp_time">60</stringProp>
  <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
    <boolProp name="LoopController.continue_forever">false</boolProp>
    <stringProp name="LoopController.loops">1</stringProp>
  </elementProp>
</ThreadGroup>
```

---

### 5.4. シナリオ4: 耐久テスト（Soak Test）

**目的**: 長時間運用時のメモリリーク、リソース枯渇を検出

**条件**:
- 同時ユーザー数: 20（定常）
- テスト時間: 24時間
- ランプアップ: 5分

**監視項目**:
- JVMヒープメモリ使用量の推移
- DB接続プール枯渇の有無
- レスポンス時間の経時変化
- エラー率の推移

**期待結果**:
- メモリ使用量が安定（増加し続けない）
- 24時間後もレスポンス時間が初期と同等
- エラー率 < 0.1%

**実行コマンド**:

```bash
jmeter -n -t soak-test.jmx \
    -l results/soak-results.jtl \
    -Jduration=86400 \
    -e -o results/soak-report
```

---

### 5.5. シナリオ5: スケーラビリティテスト

**目的**: 水平スケールの効果を検証

**条件**:
1. **1インスタンス**: 同時ユーザー数50、スループット測定
2. **2インスタンス**: 同時ユーザー数50、スループット測定
3. **4インスタンス**: 同時ユーザー数50、スループット測定

**期待結果**:
- インスタンス数に比例してスループット向上
- 2インスタンス: 約2倍のスループット
- 4インスタンス: 約4倍のスループット

**検証方法**:

```bash
# 1インスタンスでテスト
aws ecs update-service --desired-count 1
jmeter -n -t scalability-test.jmx -l results/1-instance.jtl

# 2インスタンスでテスト
aws ecs update-service --desired-count 2
jmeter -n -t scalability-test.jmx -l results/2-instances.jtl

# 4インスタンスでテスト
aws ecs update-service --desired-count 4
jmeter -n -t scalability-test.jmx -l results/4-instances.jtl
```

---

## 6. データベースパフォーマンステスト

### 6.1. クエリパフォーマンステスト

**目的**: スロークエリの検出、インデックスの効果確認

**テスト対象クエリ**:

| ID | クエリ | 期待実行時間 | インデックス |
|----|-------|------------|------------|
| **DB-001** | 従業員一覧取得（ページネーション） | < 50ms | idx_employees_email |
| **DB-002** | 勤務記録取得（従業員ID、期間指定） | < 100ms | idx_work_records_employee_time |
| **DB-003** | 給与計算用集計クエリ | < 200ms | idx_work_records_payment |
| **DB-004** | 時給マスタ検索（勤務形態、生徒レベル） | < 10ms | idx_hourly_wages_lookup |

**実行計画確認**:

```sql
-- DB-002のEXPLAIN ANALYZE
EXPLAIN ANALYZE
SELECT *
FROM work_records
WHERE employee_id = 1
  AND start_time BETWEEN '2025-11-01' AND '2025-11-30'
  AND is_paid = false
ORDER BY start_time DESC;

-- 期待結果: Index Scan using idx_work_records_employee_time
```

### 6.2. 接続プールテスト

**目的**: HikariCP設定の妥当性確認

**設定**:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
```

**テストシナリオ**:
- 100同時リクエスト
- 各リクエストはDB接続を1秒保持

**期待結果**:
- 最大20接続で処理される
- タイムアウトエラーなし
- 接続待ち時間 < 100ms

**検証クエリ**:

```sql
-- 現在の接続数確認
SELECT count(*) AS active_connections
FROM pg_stat_activity
WHERE datname = 'attendance_staging'
  AND state = 'active';
```

---

## 7. 測定・分析

### 7.1. メトリクス収集

**Prometheusメトリクス**:

```promql
# P95レスポンス時間
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
)

# スループット
sum(rate(http_server_requests_seconds_count[5m]))

# エラー率
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
  /
sum(rate(http_server_requests_seconds_count[5m]))

# DB接続プール使用率
hikaricp_connections_active / hikaricp_connections_max * 100
```

### 7.2. JMeterレポート分析

**Summary Report**:
- Samples: 総リクエスト数
- Average: 平均レスポンス時間
- Min/Max: 最小/最大レスポンス時間
- 90th/95th/99th Percentile
- Error %: エラー率
- Throughput: スループット (req/s)

**Aggregate Graph**:
- エンドポイント別のレスポンス時間分布

**Response Time Graph**:
- 時系列でのレスポンス時間推移

### 7.3. Gatlingレポート分析

**Gatlingレポートの見方**:

```
================================================================================
---- Global Information --------------------------------------------------------
> request count                                       5000 (OK=4950   KO=50   )
> min response time                                     12 (OK=12     KO=5001 )
> max response time                                   1523 (OK=1523   KO=10000)
> mean response time                                   287 (OK=275    KO=7521 )
> std deviation                                        156 (OK=134    KO=1234 )
> response time 50th percentile                        245 (OK=242    KO=7500 )
> response time 75th percentile                        389 (OK=378    KO=8250 )
> response time 95th percentile                        512 (OK=498    KO=9500 )
> response time 99th percentile                        887 (OK=875    KO=9900 )
> mean requests/sec                                  83.333 (OK=82.5  KO=0.833)
================================================================================
```

**合格基準**:
- OK率 > 99%
- P95レスポンス時間 < 500ms
- スループット > 80 req/s

---

## 8. ボトルネック特定と改善

### 8.1. よくあるボトルネック

| ボトルネック | 症状 | 対策 |
|------------|------|------|
| **N+1クエリ** | 大量のSQLクエリ発行 | Eager Fetchingまたはバッチロード |
| **インデックス未使用** | フルテーブルスキャン | インデックス追加、EXPLAIN ANALYZE |
| **DB接続プール枯渇** | connection timeout | プールサイズ拡大、接続リーク調査 |
| **JVMヒープ不足** | OutOfMemoryError | ヒープサイズ拡大、メモリリーク修正 |
| **外部API遅延** | Google API呼び出しで遅延 | タイムアウト設定、キャッシュ導入 |

### 8.2. プロファイリングツール

**JProfiler / VisualVM**:

```bash
# JVM起動オプション
-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=9010 \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.authenticate=false
```

**pg_stat_statements（PostgreSQL）**:

```sql
-- 最も実行時間が長いクエリTop 10
SELECT
    calls,
    total_exec_time,
    mean_exec_time,
    query
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 10;
```

---

## 9. CI/CDでのパフォーマンステスト自動化

### 9.1. GitHub Actionsワークフロー

```yaml
# .github/workflows/performance-test.yml

name: Performance Test

on:
  schedule:
    - cron: '0 2 * * 0'  # 毎週日曜 2:00
  workflow_dispatch:  # 手動実行も可能

jobs:
  performance-test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'

      - name: Install JMeter
        run: |
          wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.2.tgz
          tar -xzf apache-jmeter-5.6.2.tgz

      - name: Run Baseline Load Test
        run: |
          apache-jmeter-5.6.2/bin/jmeter -n \
            -t performance-tests/baseline-load-test.jmx \
            -l results/baseline-results.jtl \
            -e -o results/baseline-report

      - name: Analyze Results
        run: |
          # P95レスポンス時間が500ms以下であることを確認
          python scripts/check-performance.py results/baseline-results.jtl

      - name: Upload Report
        uses: actions/upload-artifact@v3
        with:
          name: performance-report
          path: results/baseline-report/

      - name: Notify Slack
        if: failure()
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "⚠️ Performance Test Failed",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "Performance test did not meet the criteria.\nCheck the report: ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}"
                  }
                }
              ]
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

### 9.2. 自動判定スクリプト

**scripts/check-performance.py**:

```python
#!/usr/bin/env python3
import csv
import sys

def analyze_jmeter_results(jtl_file):
    response_times = []
    errors = 0
    total = 0

    with open(jtl_file, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            total += 1
            elapsed = int(row['elapsed'])
            response_times.append(elapsed)
            if row['success'] != 'true':
                errors += 1

    # P95計算
    response_times.sort()
    p95_index = int(len(response_times) * 0.95)
    p95 = response_times[p95_index]

    # エラー率
    error_rate = (errors / total) * 100

    print(f"Total Requests: {total}")
    print(f"P95 Response Time: {p95}ms")
    print(f"Error Rate: {error_rate:.2f}%")

    # 合格判定
    if p95 > 500:
        print(f"❌ FAILED: P95 ({p95}ms) exceeds 500ms")
        sys.exit(1)

    if error_rate > 1:
        print(f"❌ FAILED: Error rate ({error_rate}%) exceeds 1%")
        sys.exit(1)

    print("✅ PASSED: All performance criteria met")

if __name__ == "__main__":
    analyze_jmeter_results(sys.argv[1])
```

---

## 10. テスト実施チェックリスト

### 10.1. 実施前チェックリスト

- [ ] Staging環境が本番と同一構成
- [ ] テストデータ投入完了（50,000件のwork_records）
- [ ] 監視ダッシュボード準備完了
- [ ] テストシナリオのドライラン成功
- [ ] ロールバック手順確認済み

### 10.2. 実施中チェックリスト

- [ ] CPU使用率をリアルタイム監視
- [ ] メモリ使用率をリアルタイム監視
- [ ] DB接続数をリアルタイム監視
- [ ] エラーログを随時確認
- [ ] レスポンス時間をリアルタイム監視

### 10.3. 実施後チェックリスト

- [ ] JMeter/Gatlingレポート生成
- [ ] 目標値との比較分析
- [ ] ボトルネック特定
- [ ] 改善提案書作成
- [ ] 結果をSlack #performance-testに報告

---

## 11. まとめ

本パフォーマンステスト計画書により、以下を実現する:

1. ✅ **NFR-002準拠**: P95 < 500msの目標を検証
2. ✅ **包括的なシナリオ**: 通常負荷、ストレス、スパイク、耐久テストを実施
3. ✅ **自動化**: CI/CDで定期的に実行、リグレッション防止
4. ✅ **ボトルネック特定**: プロファイリングツールで根本原因を分析
5. ✅ **スケーラビリティ検証**: 水平スケールの効果を測定

すべてのパフォーマンステストが合格することで、本番環境でも安定したレスポンス時間を提供できる。
