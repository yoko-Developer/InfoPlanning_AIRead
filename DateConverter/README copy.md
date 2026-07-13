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

### 西暦フォーマット
- `20231225` → `202312` (2023年12月25日 → 2023年12月)
- `202312末` → `202312` (2023年12月末日)

## 使用方法

### ビルド

```bash
cd DateConverter
mvn clean package
```

### 実行

```bash
java -jar target/dateconverter-0.0.1-SNAPSHOT.jar --in input.csv --out output.csv
```

### オプション

- `--in <file>`: 入力CSVファイル（必須）
- `--out <file>`: 出力CSVファイル（必須）
- `--cols <list>`: 変換対象列名（カンマ区切り、デフォルト: 取得,使用,供用,供用日,契約）
- `--charset <charset>`: 文字コード（デフォルト: UTF-8）

### 実行例

```bash
# 基本的な使用
java -jar dateconverter.jar --in input.csv --out output.csv

# 対象列を指定
java -jar dateconverter.jar --in input.csv --out output.csv --cols 取得,使用,供用

# 文字コードを指定
java -jar dateconverter.jar --in input.csv --out output.csv --charset Shift_JIS
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
│   │   │       ├── exception/
│   │   │       │   └── FormatException.java           # 例外クラス
│   │   │       ├── service/
│   │   │       │   └── CsvDateConverterService.java   # CSV変換サービス
│   │   │       └── util/
│   │   │           ├── DateConverterUtil.java         # ユーティリティ
│   │   │           ├── DateParser.java                # 日付パーサー
│   │   │           ├── GengoYearTable.java            # 和暦テーブル
│   │   │           └── NumberToken.java               # 数値トークン
│   │   └── resources/
│   │       ├── application.properties                 # アプリケーション設定
│   │       └── logback-spring.xml                     # ログ設定
│   └── test/
│       └── java/
│           └── jp/co/ariseinnovation/dateconverter/
│               ├── DateConverterApplicationTests.java
│               └── util/
│                   └── DateParserTest.java            # 日付パーサーテスト
├── pom.xml                                            # Maven設定
├── .gitignore
└── README.md
```

## 依存関係

- Spring Boot 4.0.0
- Joda Time 2.12.5
- Apache Commons CSV 1.11.0
- Apache Commons Lang3 3.9
- JUnit 5 (テスト用)

## ログ設定

- ログファイル: `logs/application.log`
- 日次ローテーション
- 30日間保持
- コンソールとファイルの両方に出力

## テスト

```bash
mvn test
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