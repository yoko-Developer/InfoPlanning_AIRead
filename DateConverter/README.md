# DateConverter

和暦から西暦への日付変換プログラム

## 概要

このプログラムは、CSVファイル内の和暦表記を西暦YYYYMM形式に変換するSpring Bootアプリケーションです。
sample_pj1の日付変換機能をベースに、sample_pj2のSpring Boot構成とログ設定を採用して開発されています。

## 機能

- CSVファイルの和暦を西暦に変換
- 複数の和暦フォーマットに対応（昭和、平成、令和）
- 柔軟な日付フォーマット解析
- Spring Bootベースの構成
- 詳細なログ出力

## 対応フォーマット

### 和暦フォーマット
- `H10.5` → `199805` (平成10年5月)
- `S63.12` → `198812` (昭和63年12月)
- `R3.4` → `202104` (令和3年4月)
- `R5.4.30` → `20230430` (令和5年4月30日、年月日精度)
- `平成10年5月15日` → `19980515` (漢字の年月日表記)

#### 元号と年の間の区切り・空白に対応

元号と年の間には、区切り文字なし・空白・区切り文字（`. - / ,`）のいずれか1つを挟めます。
また、全角文字（全角数字・全角スペース・全角記号「．」「・」「／」等）は自動的に半角へ変換されます。

- `平 1・1・1` → `19890101` / `198901`（半角数字・半角スペース・全角中点）
- `平 １・１・１` → `19890101` / `198901`（全角数字・全角スペース・全角中点）
- `H.30/09/30` → `20180930` / `201809`（元号直後にドット、年月日はスラッシュ区切り）
- `R.3/9/30` → `20210930` / `202109`
- `H．３０／０９／３０` → `20180930` / `201809`（全角ドット・全角スラッシュ・全角数字）

### 西暦フォーマット
- `20231225` → `202312` (2023年12月25日 → 2023年12月)
- `202312末` → `202312` (2023年12月末日)
- `2023-04-30` / `2023.04.30` / `2023/04/30` / `2023,04,30` → `20230430` (西暦の区切り文字年月日形式)

### 出力形式（`--format`オプション）

`--format YYYYMMDD`（デフォルト）または `--format YYYYMM` で出力形式を指定できます。
入力データが年月精度しか持たない場合は、`YYYYMMDD`を指定しても`YYYYMM`形式で出力されます。
詳細は [OUTPUT_FORMAT_USAGE.md](OUTPUT_FORMAT_USAGE.md) を参照してください。

## 使用方法

### ビルド

**Maven Wrapper（`mvnw`）の使用を推奨します。** グローバルにインストールされた`mvn`はバージョンや設定が環境ごとに異なるため、
ビルド時にテストコンパイルが失敗する等の不整合が発生することがあります。`mvnw`はこのプロジェクトで動作確認済みのMavenバージョン
（3.9.11、`.mvn/wrapper/maven-wrapper.properties`で固定）を自動的にダウンロードして使用するため、環境に依存せず安定してビルドできます。

```bash
cd DateConverter

# Windows
.\mvnw.cmd clean package

# macOS/Linux
./mvnw clean package
```

グローバルの`mvn`でビルドが失敗する場合は、上記の`mvnw`（Wrapper）を使用してください。

### 実行

```bash
java -jar target/dateconverter-0.0.1-SNAPSHOT.jar --in input.csv --out output.csv
```

### オプション

- `--in <file>`: 入力CSVファイル（必須）
- `--out <file>`: 出力CSVファイル（必須）
- `--cols <list>`: 変換対象列名（カンマ区切り、デフォルト: 取得,取得日,使用,供用,供用日,事業供用開始日,契約）
- `--charset <charset>`: 文字コード（デフォルト: UTF-8）
- `--format <YYYYMMDD|YYYYMM>`: 出力形式（デフォルト: YYYYMMDD、詳細は[OUTPUT_FORMAT_USAGE.md](OUTPUT_FORMAT_USAGE.md)参照）
- `--extra-separators <chars>`: 追加の区切り文字（指定した文字を標準の区切り文字「.」として扱う。デフォルト: なし）
- `--required-content <str>`: 指定した文字列を含まないCSVファイルは変換せずそのままコピー（デフォルト: チェックなし）

### 実行例

```bash
# 基本的な使用
java -jar dateconverter.jar --in input.csv --out output.csv

# 対象列を指定
java -jar dateconverter.jar --in input.csv --out output.csv --cols 取得,使用,供用

# 文字コードを指定
java -jar dateconverter.jar --in input.csv --out output.csv --charset Shift_JIS

# 出力形式・追加区切り文字を指定
java -jar dateconverter.jar --in input.csv --out output.csv --format YYYYMM --extra-separators "~"
```

## プロジェクト構成

```
DateConverter/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── jp/co/ariseinnovation/dateconverter/
│   │   │       ├── DateConverterApplication.java      # メインクラス
│   │   │       ├── DateConverterRunner.java           # コマンドライン実行
│   │   │       ├── enums/
│   │   │       │   └── OutputFormat.java              # 出力形式（YYYYMMDD/YYYYMM）
│   │   │       ├── exception/
│   │   │       │   └── FormatException.java           # 例外クラス
│   │   │       ├── service/
│   │   │       │   └── CsvDateConverterService.java   # CSV変換サービス
│   │   │       └── util/
│   │   │           ├── DateConstants.java             # 定数（和暦基準年等）
│   │   │           ├── DateParser.java                # 日付パーサー
│   │   │           ├── DatePrecisionUtil.java         # 日付精度判定
│   │   │           ├── GengoYearTable.java            # 和暦テーブル
│   │   │           ├── MessageUtil.java               # メッセージ（i18n）
│   │   │           └── NumberToken.java               # 数値トークン
│   │   └── resources/
│   │       ├── application.properties                 # アプリケーション設定
│   │       ├── messages.properties                     # メッセージソース
│   │       └── logback-spring.xml                     # ログ設定
│   └── test/
│       └── java/
│           └── jp/co/ariseinnovation/dateconverter/    # 上記各クラスに対応するテスト一式
├── pom.xml                                            # Maven設定
├── .mvn/wrapper/                                      # Maven Wrapper（mvnw）
├── .gitignore
├── OUTPUT_FORMAT_USAGE.md                             # --formatオプションの詳細
└── README.md
```

## 依存関係

- Spring Boot 4.0.0
- Joda Time 2.13.0
- Apache Commons CSV 1.12.0
- Apache Commons Lang3 3.17.0
- JUnit 5 (テスト用)

## ログ設定

- ログファイル: `logs/application.log`
- 日次ローテーション
- 30日間保持
- コンソールとファイルの両方に出力

## テスト

```bash
# Windows
.\mvnw.cmd test

# macOS/Linux
./mvnw test
```

## 開発者向け情報

### 元プロジェクトとの関係

- **sample_pj1**: 日付変換ロジックのベース
- **sample_pj2**: Spring Boot構成とログ設定のベース

### 主な変更点

1. パッケージ構成をsample_pj2に合わせて変更
2. ログ設定をLogbackに変更（Log4jからの移行）
3. Spring Bootのサービス層として再構成
4. Apache Commons CSVを使用したCSV処理の改善
5. 和暦の元号と年の間の区切り・空白（`平 1・1・1`、`H.30/09/30`等）に対応
6. 全角文字（全角スラッシュ「／」・全角スペース「　」等）の半角変換を追加
7. 和暦の年月日精度判定を修正（日が1日の場合でも「年月のみ」と誤判定しないよう構造的に判定）
8. `pom.xml`に文字コード（`UTF-8`）を明示し、実行環境の既定コードページに依存したビルド失敗を防止