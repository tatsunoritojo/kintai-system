# Git運用ルール

## 目次
- [ブランチ戦略](#ブランチ戦略)
- [コミットメッセージ規約](#コミットメッセージ規約)
- [プルリクエストフロー](#プルリクエストフロー)
- [コードレビューガイドライン](#コードレビューガイドライン)
- [リリースフロー](#リリースフロー)
- [緊急対応フロー](#緊急対応フロー)
- [Git操作コマンド集](#git操作コマンド集)

---

## ブランチ戦略

### Git Flowを採用

```
main (本番環境)
  ├── develop (開発統合ブランチ)
  │   ├── feature/TICKET-001-add-login  (機能開発)
  │   ├── feature/TICKET-002-payroll-calculation
  │   └── bugfix/TICKET-010-fix-validation
  ├── release/v1.0.0 (リリース準備)
  └── hotfix/v1.0.1-fix-critical-bug (緊急修正)
```

### ブランチの種類と役割

| ブランチ種類 | 命名規則 | 派生元 | マージ先 | 用途 |
|------------|---------|--------|---------|------|
| **main** | `main` | - | - | 本番環境デプロイ用。常に安定版 |
| **develop** | `develop` | `main` | - | 開発統合ブランチ。次リリースの開発版 |
| **feature** | `feature/TICKET-XXX-description` | `develop` | `develop` | 新機能開発 |
| **bugfix** | `bugfix/TICKET-XXX-description` | `develop` | `develop` | 開発中のバグ修正 |
| **release** | `release/vX.Y.Z` | `develop` | `main`, `develop` | リリース準備 (QA、軽微な修正) |
| **hotfix** | `hotfix/vX.Y.Z-description` | `main` | `main`, `develop` | 本番環境の緊急バグ修正 |

---

## ブランチ運用詳細

### 1. feature ブランチ (機能開発)

**命名規則**:
```
feature/TICKET-XXX-short-description
```

**例**:
- `feature/TICKET-123-add-employee-registration`
- `feature/TICKET-456-integrate-google-calendar`

**作成方法**:
```bash
# developブランチから派生
git checkout develop
git pull origin develop
git checkout -b feature/TICKET-123-add-employee-registration
```

**マージ方法**:
```bash
# developにマージ (プルリクエスト経由)
# レビュー承認後、Squash and Mergeを使用
```

**ライフサイクル**:
1. `develop` から `feature` ブランチを作成
2. 機能開発 (コミットを細かく分ける)
3. プルリクエスト作成
4. コードレビュー
5. `develop` にマージ
6. ブランチ削除

---

### 2. bugfix ブランチ (開発中のバグ修正)

**命名規則**:
```
bugfix/TICKET-XXX-short-description
```

**例**:
- `bugfix/TICKET-789-fix-payroll-calculation`
- `bugfix/TICKET-012-fix-validation-error`

**運用**:
- `feature` ブランチと同じフロー
- 軽微なバグは直接 `develop` にコミット可 (レビュー必須)

---

### 3. release ブランチ (リリース準備)

**命名規則**:
```
release/vX.Y.Z
```

**例**:
- `release/v1.0.0`
- `release/v1.1.0`

**作成タイミング**:
- `develop` ブランチが次リリース機能を含んだ段階

**作成方法**:
```bash
git checkout develop
git pull origin develop
git checkout -b release/v1.0.0
```

**許可される変更**:
- バージョン番号の更新 (`pom.xml`, `application.yml`)
- ドキュメント更新 (CHANGELOG.md, README.md)
- QAで発見された軽微なバグ修正

**マージ方法**:
```bash
# mainにマージ (プルリクエスト経由)
git checkout main
git merge --no-ff release/v1.0.0
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin main --tags

# developにもマージ (バグ修正を反映)
git checkout develop
git merge --no-ff release/v1.0.0
git push origin develop

# releaseブランチ削除
git branch -d release/v1.0.0
git push origin --delete release/v1.0.0
```

---

### 4. hotfix ブランチ (緊急修正)

**命名規則**:
```
hotfix/vX.Y.Z-short-description
```

**例**:
- `hotfix/v1.0.1-fix-login-bug`
- `hotfix/v1.0.2-fix-critical-security-issue`

**作成タイミング**:
- 本番環境で重大なバグが発見された場合

**作成方法**:
```bash
git checkout main
git pull origin main
git checkout -b hotfix/v1.0.1-fix-login-bug
```

**マージ方法**:
```bash
# mainにマージ
git checkout main
git merge --no-ff hotfix/v1.0.1-fix-login-bug
git tag -a v1.0.1 -m "Hotfix: Fix login bug"
git push origin main --tags

# developにもマージ
git checkout develop
git merge --no-ff hotfix/v1.0.1-fix-login-bug
git push origin develop

# hotfixブランチ削除
git branch -d hotfix/v1.0.1-fix-login-bug
git push origin --delete hotfix/v1.0.1-fix-login-bug
```

---

## コミットメッセージ規約

### Conventional Commits形式を採用

**フォーマット**:
```
<type>(<scope>): <subject>

<body>

<footer>
```

### type (必須)

| Type | 用途 | 例 |
|------|------|-----|
| **feat** | 新機能追加 | `feat(employee): add employee registration API` |
| **fix** | バグ修正 | `fix(payroll): fix calculation error for overtime` |
| **docs** | ドキュメント変更 | `docs(readme): update setup instructions` |
| **style** | コードフォーマット (機能変更なし) | `style(controller): apply Google Java Style` |
| **refactor** | リファクタリング (機能変更なし) | `refactor(service): extract common logic to util` |
| **test** | テスト追加・修正 | `test(payroll): add unit tests for edge cases` |
| **chore** | ビルド・ツール設定変更 | `chore(pom): update Spring Boot to 3.2.1` |
| **perf** | パフォーマンス改善 | `perf(repository): add index to employee_number` |
| **ci** | CI/CD設定変更 | `ci(github): add code coverage report` |

### scope (任意)

変更対象のモジュールや機能を指定。

**例**:
- `employee`, `payroll`, `calendar`, `auth`, `database`, `config`

### subject (必須)

変更内容を **50文字以内** で簡潔に記述。

**ルール**:
- 動詞の原形で始める (add, fix, update, remove)
- 文末にピリオド不要
- 日本語または英語 (プロジェクトで統一)

**Good例**:
```
feat(payroll): add overtime calculation logic
fix(auth): fix JWT token expiration issue
docs(api): update API specification for v1.1
```

**Bad例**:
```
Added new feature  (過去形)
fixed bug.  (文末のピリオド)
update  (scope不明)
```

### body (任意)

変更理由や詳細を記述。72文字で改行。

**例**:
```
feat(payroll): add overtime calculation logic

- Implement hourly rate calculation for overtime (> 8 hours/day)
- Add 25% premium for weekday overtime
- Add 35% premium for weekend/holiday overtime
```

### footer (任意)

BREAKING CHANGEやIssue番号を記述。

**例**:
```
feat(api): change payroll API response format

BREAKING CHANGE: Response structure changed from flat to nested.

Closes #123
```

---

## コミット例

### 例1: 新機能追加

```
feat(employee): add employee registration API

- Add POST /api/v1/employees endpoint
- Implement validation for employee number uniqueness
- Add unit tests for EmployeeService.createEmployee()

Closes #45
```

### 例2: バグ修正

```
fix(payroll): fix calculation error for night shift premium

Fixed incorrect calculation when work hours span across midnight.
Now correctly applies 25% premium for 22:00-05:00 time range.

Closes #78
```

### 例3: リファクタリング

```
refactor(service): extract common date utility methods

- Move date range validation to DateUtils
- Extract business day calculation to separate method
- Reduce code duplication across services
```

### 例4: ドキュメント更新

```
docs(readme): update local development setup instructions

- Add PostgreSQL installation steps for Windows
- Update Docker Compose command examples
- Fix typo in environment variables section
```

---

## プルリクエストフロー

### 1. プルリクエスト作成前のチェックリスト

- [ ] ローカルでビルド成功 (`mvn clean package`)
- [ ] ユニットテスト全件合格 (`mvn test`)
- [ ] コードカバレッジ80%以上
- [ ] コードフォーマット適用 (Google Java Style)
- [ ] コミットメッセージがConventional Commits形式
- [ ] `develop` ブランチの最新版をマージ済み

```bash
# developの最新版を取り込む
git checkout develop
git pull origin develop
git checkout feature/TICKET-123-add-login
git merge develop
# コンフリクト解決
git push origin feature/TICKET-123-add-login
```

### 2. プルリクエストテンプレート

プルリクエスト作成時は以下のテンプレートを使用:

```markdown
## 概要
<!-- このPRで何を実装したかを簡潔に記述 -->

従業員登録API (POST /api/v1/employees) を実装しました。

## 関連Issue
<!-- Issueトラッキングシステムのチケット番号 -->

Closes #123

## 変更内容
<!-- 主な変更点をリスト形式で記述 -->

- [ ] EmployeeController.createEmployee() を追加
- [ ] EmployeeService.createEmployee() を追加
- [ ] 従業員番号の一意性バリデーション実装
- [ ] ユニットテスト追加 (EmployeeServiceTest)
- [ ] API仕様書更新

## テスト
<!-- どのようにテストしたかを記述 -->

### ユニットテスト
- EmployeeServiceTest: 10ケース追加
- カバレッジ: 85% (行カバレッジ)

### 手動テスト
- [ ] 正常系: 従業員登録成功
- [ ] 異常系: 従業員番号重複エラー
- [ ] 異常系: バリデーションエラー (必須項目不足)

## スクリーンショット (UIの場合)
<!-- 画面変更がある場合はスクリーンショット添付 -->

## レビュー観点
<!-- レビュアーに特に確認してほしい点 -->

- バリデーションロジックが適切か
- エラーハンドリングが適切か
- テストケースが十分か

## チェックリスト
<!-- 自己レビュー用チェックリスト -->

- [x] ビルド成功
- [x] テスト全件合格
- [x] コードカバレッジ80%以上
- [x] コードフォーマット適用
- [x] ドキュメント更新
- [x] マイグレーションスクリプト追加 (DBスキーマ変更時)
```

### 3. プルリクエストのマージ方法

#### Squash and Merge (推奨)

**使用場面**: 通常のfeature/bugfixブランチ

**メリット**:
- コミット履歴がクリーンになる
- 開発中の細かいコミットを1つにまとめられる

**GitHub操作**:
1. プルリクエスト画面で **Squash and merge** を選択
2. コミットメッセージを編集 (Conventional Commits形式)
3. **Confirm squash and merge**

#### Merge commit

**使用場面**: release/hotfixブランチ

**メリット**:
- ブランチの履歴が保持される
- リリース履歴が明確

---

## コードレビューガイドライン

### レビュアーの責務

#### 1. 機能的正確性
- [ ] 要件を満たしているか
- [ ] エッジケースを考慮しているか
- [ ] エラーハンドリングが適切か

#### 2. コード品質
- [ ] 命名規則が適切か
- [ ] 単一責任の原則を守っているか
- [ ] 重複コードがないか (DRY原則)
- [ ] マジックナンバーを定数化しているか

#### 3. テスト
- [ ] ユニットテストが書かれているか
- [ ] カバレッジ80%以上を達成しているか
- [ ] 正常系・異常系の両方をテストしているか

#### 4. セキュリティ
- [ ] SQLインジェクション対策が施されているか
- [ ] XSS対策が施されているか
- [ ] 認証・認可チェックが適切か
- [ ] ログに機密情報が含まれていないか

#### 5. パフォーマンス
- [ ] N+1問題が発生していないか
- [ ] 不要なデータベースアクセスがないか
- [ ] 適切なインデックスが設定されているか

#### 6. ドキュメント
- [ ] JavaDocが書かれているか
- [ ] API仕様書が更新されているか
- [ ] README/セットアップ手順が更新されているか (必要に応じて)

### レビューコメントの書き方

#### Prefix (推奨)

| Prefix | 意味 | 対応 |
|--------|------|------|
| **[MUST]** | 必須修正 | 修正必須。承認ブロック |
| **[SHOULD]** | 推奨修正 | できれば修正してほしい |
| **[NITS]** | 軽微な指摘 | typo、フォーマットなど |
| **[Q]** | 質問 | 理解のための質問 |
| **[IMO]** | 個人的意見 | 参考程度 |

#### Good例

```markdown
[MUST] SQLインジェクションの脆弱性があります

以下の行で文字列連結を使用しているため、SQLインジェクションのリスクがあります:
`String sql = "SELECT * FROM employees WHERE name = '" + name + "'";`

PreparedStatementまたはJPQLを使用してください:
`@Query("SELECT e FROM Employee e WHERE e.name = :name")`
```

```markdown
[SHOULD] メソッド名をより明確にできます

`process()` では何を処理するか不明です。
`calculateMonthlyPayroll()` のように具体的な名前に変更することをお勧めします。
```

```markdown
[NITS] typo修正

`calcurate` → `calculate`
```

#### Bad例

```markdown
❌ "ここ間違ってます"  (具体性がない)
❌ "この書き方ダメ"  (理由がない)
❌ "書き直して"  (代替案がない)
```

### レビュー承認基準

**承認できる条件**:
- [MUST]指摘がすべて解消されている
- ビルド成功、テスト全件合格
- コードカバレッジ80%以上

**承認できない条件**:
- [MUST]指摘が未解消
- テスト失敗
- セキュリティ上の問題がある

---

## リリースフロー

### 1. リリース計画

**バージョン番号ルール (Semantic Versioning)**:
```
vX.Y.Z
```

- **X (Major)**: 破壊的変更
- **Y (Minor)**: 新機能追加 (後方互換性あり)
- **Z (Patch)**: バグ修正

**例**:
- `v1.0.0` → 初回リリース
- `v1.1.0` → 新機能追加
- `v1.1.1` → バグ修正

### 2. リリース手順

#### Step 1: releaseブランチ作成

```bash
git checkout develop
git pull origin develop
git checkout -b release/v1.1.0
```

#### Step 2: バージョン番号更新

**pom.xml**:
```xml
<version>1.1.0</version>
```

**application.yml**:
```yaml
app:
  version: 1.1.0
```

```bash
git add pom.xml src/main/resources/application.yml
git commit -m "chore(release): bump version to 1.1.0"
```

#### Step 3: CHANGELOG.md更新

**CHANGELOG.md**:
```markdown
## [1.1.0] - 2024-01-15

### Added
- 従業員一括登録API (#45)
- Google Calendar同期機能 (#67)

### Changed
- 給与計算ロジックを改善 (#78)

### Fixed
- ログイン時のバリデーションエラー修正 (#89)
```

```bash
git add CHANGELOG.md
git commit -m "docs(changelog): update changelog for v1.1.0"
```

#### Step 4: QAテスト

- ステージング環境にデプロイ
- QAチームによるテスト実施
- バグ発見時は `release/v1.1.0` で修正

#### Step 5: mainにマージ

```bash
# プルリクエスト作成 (release/v1.1.0 → main)
# レビュー承認後、Merge commit でマージ

git checkout main
git pull origin main
git merge --no-ff release/v1.1.0
git tag -a v1.1.0 -m "Release version 1.1.0"
git push origin main --tags
```

#### Step 6: developにマージ

```bash
git checkout develop
git pull origin develop
git merge --no-ff release/v1.1.0
git push origin develop
```

#### Step 7: リリースブランチ削除

```bash
git branch -d release/v1.1.0
git push origin --delete release/v1.1.0
```

---

## 緊急対応フロー (Hotfix)

### 発生トリガー

- 本番環境で重大なバグ発見
- セキュリティ脆弱性発見
- データ整合性の問題

### 対応手順

#### Step 1: hotfixブランチ作成

```bash
git checkout main
git pull origin main
git checkout -b hotfix/v1.0.1-fix-login-bug
```

#### Step 2: バグ修正

```bash
# バグ修正実装
git add .
git commit -m "fix(auth): fix login validation bug"

# バージョン番号更新
git add pom.xml
git commit -m "chore(hotfix): bump version to 1.0.1"
```

#### Step 3: テスト

```bash
mvn clean test
mvn verify
```

#### Step 4: mainにマージ

```bash
git checkout main
git merge --no-ff hotfix/v1.0.1-fix-login-bug
git tag -a v1.0.1 -m "Hotfix: Fix login bug"
git push origin main --tags
```

#### Step 5: developにもマージ

```bash
git checkout develop
git merge --no-ff hotfix/v1.0.1-fix-login-bug
git push origin develop
```

#### Step 6: 本番デプロイ

```bash
# CI/CDパイプラインが自動でデプロイ
# または手動デプロイ
```

#### Step 7: hotfixブランチ削除

```bash
git branch -d hotfix/v1.0.1-fix-login-bug
git push origin --delete hotfix/v1.0.1-fix-login-bug
```

---

## Git操作コマンド集

### ブランチ操作

```bash
# ブランチ一覧表示
git branch

# リモートブランチ一覧表示
git branch -r

# ブランチ作成
git checkout -b feature/TICKET-123-add-login

# ブランチ切り替え
git checkout develop

# ブランチ削除 (ローカル)
git branch -d feature/TICKET-123-add-login

# ブランチ削除 (リモート)
git push origin --delete feature/TICKET-123-add-login
```

### コミット操作

```bash
# 変更ファイル確認
git status

# 変更内容確認
git diff

# ステージング
git add src/main/java/com/example/attendance/EmployeeService.java

# すべてステージング
git add .

# コミット
git commit -m "feat(employee): add employee registration API"

# 直前のコミットメッセージ修正
git commit --amend

# 直前のコミットに変更を追加 (メッセージ変更なし)
git add .
git commit --amend --no-edit
```

### リモート操作

```bash
# リモート確認
git remote -v

# プル (fetch + merge)
git pull origin develop

# プッシュ
git push origin feature/TICKET-123-add-login

# 強制プッシュ (履歴書き換え時、注意！)
git push origin feature/TICKET-123-add-login --force
```

### マージ操作

```bash
# developの最新を取り込む
git checkout feature/TICKET-123-add-login
git merge develop

# コンフリクト解消後
git add .
git commit -m "merge: resolve conflict with develop"

# マージ中止
git merge --abort
```

### ログ確認

```bash
# コミットログ表示
git log

# 1行表示
git log --oneline

# グラフ表示
git log --oneline --graph --all

# 特定ファイルの履歴
git log -- src/main/java/com/example/attendance/EmployeeService.java
```

### 特定コミットに戻す

```bash
# 特定コミットを確認
git log --oneline

# 作業ディレクトリを特定コミットに戻す (履歴は残る)
git revert <commit-hash>

# 特定コミットまで履歴を巻き戻す (危険！)
git reset --hard <commit-hash>
```

### スタッシュ (一時退避)

```bash
# 現在の変更を一時退避
git stash

# スタッシュ一覧表示
git stash list

# スタッシュを復元
git stash apply

# スタッシュを復元して削除
git stash pop

# スタッシュ削除
git stash drop
```

---

## トラブルシューティング

### 問題1: コンフリクト発生

```bash
# developの最新をマージしたらコンフリクト
git merge develop

# コンフリクトファイル確認
git status

# IDEでコンフリクト解消 (<<<<<<, ======, >>>>>> を手動修正)

# 解消後
git add .
git commit -m "merge: resolve conflict with develop"
```

### 問題2: 間違ったブランチにコミットしてしまった

```bash
# 現在のブランチ: develop (本来はfeatureブランチにコミットすべきだった)

# 直前のコミットを取り消す (変更はワーキングツリーに残る)
git reset --soft HEAD~1

# 正しいブランチに切り替え
git checkout feature/TICKET-123-add-login

# 再度コミット
git add .
git commit -m "feat(employee): add employee registration API"
```

### 問題3: プッシュ済みコミットメッセージを修正したい

```bash
# ❌ Bad: 履歴書き換え (push済みの場合は禁止)
git commit --amend
git push --force

# ✅ Good: 新しいコミットで修正
# コミットメッセージの修正は諦めて、次回から正しく書く
```

---

## 参考リンク

- [Git Flow](https://nvie.com/posts/a-successful-git-branching-model/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Semantic Versioning](https://semver.org/)
- [GitHub Flow](https://guides.github.com/introduction/flow/)
