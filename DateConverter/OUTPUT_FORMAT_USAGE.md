# 出力形式指定機能の使用方法

## 概要

DateConverterアプリケーションで、コマンドライン引数から出力形式を指定できるようになりました。

## 出力形式オプション

### --format オプション

出力する日付の形式を指定します。

**指定可能な値:**
- `YYYYMMDD`: 年月日形式（例: 20230430）
- `YYYYMM`: 年月形式（例: 202304）

**デフォルト値:** `YYYYMMDD`

## 使用例

### 基本的な使用方法

```bash
# YYYYMMDD形式で出力（デフォルト）
java -jar dateconverter.jar --in input.csv --out output.csv

# YYYYMM形式で出力
java -jar dateconverter.jar --in input.csv --out output.csv --format YYYYMM

# YYYYMMDD形式で明示的に指定
java -jar dateconverter.jar --in input.csv --out output.csv --format YYYYMMDD
```

### 全オプションを指定した場合

```bash
java -jar dateconverter.jar \
  --in input.csv \
  --out output.csv \
  --cols 取得,使用,供用 \
  --charset UTF-8 \
  --format YYYYMM
```

## 動作仕様

### 入力データの精度による自動制御

出力形式を指定しても、入力データの精度によって実際の出力形式が決定されます：

1. **入力データが年月精度の場合**
   - `--format YYYYMMDD` を指定しても `YYYYMM` 形式で出力
   - 例: `H10.5` → `199805`（`19980501`にはならない）

2. **入力データが年月日精度の場合**
   - 指定された形式で出力
   - 例: `R5.4.30` + `--format YYYYMM` → `202304`
   - 例: `R5.4.30` + `--format YYYYMMDD` → `20230430`

### 変換例

| 入力データ | --format | 出力結果 | 説明 |
|-----------|----------|----------|------|
| H10.5 | YYYYMMDD | 199805 | 年月精度のため自動的にYYYYMM |
| H10.5 | YYYYMM | 199805 | 指定通り |
| R5.4.30 | YYYYMMDD | 20230430 | 指定通り |
| R5.4.30 | YYYYMM | 202304 | 指定通り |

## エラーハンドリング

### 無効な形式を指定した場合

```bash
java -jar dateconverter.jar --in input.csv --out output.csv --format INVALID
```

- 警告メッセージが表示されます
- デフォルト形式（YYYYMMDD）が使用されます
- 処理は正常に継続されます

### 大文字小文字の区別

形式指定は大文字小文字を区別しません：

```bash
# 以下はすべて同じ結果
--format YYYYMM
--format yyyymm
--format YyYyMm
```

## ログ出力

アプリケーション実行時に、指定された出力形式がログに出力されます：

```
2025-12-12 12:00:00 [main] INFO  DateConverterRunner - 出力形式: YYYYMM
```