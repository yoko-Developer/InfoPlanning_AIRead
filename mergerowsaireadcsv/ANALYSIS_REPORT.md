# 有価証券CSV変換ロジック変更提案

## 問題の概要
OCRソフトウェアのVerUp前後で、有価証券CSVの出力形式が変更され、現在の変換ロジックではVerUp後のCSVで意図した通りのアウトプットが得られない。

### 背景
- **現在の処理**: 複数行に分割されたデータを1行にマージする処理
- **マージ対象の検出方法**: `期末現在高`というItemNameを持ち、かつValueが空白の行を検出
- **マージ処理**: 検出した行の次のGroupIDの行とデータを結合
- **問題**: VerUp後のCSVでは`期末現在高`行が常に空白のため、誤検出が発生

## VerUp前後の主な違い

### 1. ItemName構造の変化

#### VerUp前（02_060_75_01形式）:
```
ItemName: 区分, amount_1, 期末現在高, amount_3, 期中, 増(減)の, 明細, amount_7
```

#### VerUp後（02_060_75_01形式）:
```
ItemName: 区分, 種類銘柄, 数量_01, 期末現在高, 金額, 異動年月日, 数量_02, 異動事由, 
          期中増(減)の明細売却(買入)先の名称(氏名)金額, 売却(買入)先の所在地(住所), 摘要
```

### 2. データ配置の変化

| 項目 | VerUp前 | VerUp後 |
|------|---------|---------|
| 種類・銘柄情報 | `区分`行のValue | `種類銘柄`行のValue |
| 数量情報 | `amount_1`行のValue | `数量_01`行のValue |
| 金額情報 | `期末現在高`行のValue | `金額`行のValue |
| 期末現在高 | Valueに金額データあり | Valueは空 |

### 3. 具体例の比較

#### VerUp前（有価証券01_0.csv）:
```csv
"区分","0","detail","2","その他 沖縄県卸商業","100","null",...
"区分","0","detail","3","団地協同組合","100","null",...
"amount_1","0","detail","2","口","75","null",...
"amount_1","0","detail","3","2,434","100","null",...
"期末現在高","0","detail","3","24,340,000","100","null",...  ← 金額データがここに格納
```

**VerUp前の特徴**:
- `区分`行に種類と銘柄の情報が複数行に分かれて格納
- `amount_1`行に数量情報が格納
- `期末現在高`行に金額データ（24,340,000）が格納される
- **マージ対象**: `期末現在高`行でValueが空白の場合のみ

#### VerUp後（有価証券01_0.csv）:
```csv
"区分","0","detail","0","その他","100","null",...
"種類銘柄","0","detail","0","沖縄県卸商業 団地協同組合","100","null",...
"数量_01","0","detail","0","ロ 2,434","82","null",...
"期末現在高","0","detail","0","","100","null",...  ← 常に空白
"金額","0","detail","0","24,340,000","100","null",...  ← 金額データが新しい行に移動
```

**VerUp後の特徴**:
- `区分`行には分類のみ（"その他"）
- 新しい`種類銘柄`行に具体的な名称が統合されて格納
- `数量_01`行に数量情報が格納（名称変更）
- `期末現在高`行は**常に空白**（データなし）
- 新しい`金額`行に金額データ（24,340,000）が格納される
- **問題点**: `期末現在高`が常に空白のため、現在のロジックでは全行がマージ対象として誤検出される

### 4. form_idによる形式の違い

OCRソフトウェアは複数の帳票形式を認識し、form_idで区別しています。

#### 02_060_75_01形式（有価証券01 - 明細形式）:

**VerUp前の構造**:
```
ItemName: 区分, amount_1, 期末現在高, amount_3, 期中, 増(減)の, 明細, amount_7
         ↓      ↓         ↓
       種類/銘柄  数量    金額データ
```

**VerUp後の構造**:
```
ItemName: 区分, 種類銘柄, 数量_01, 期末現在高, 金額, 異動年月日, ...
         ↓      ↓        ↓       ↓(空)    ↓
       分類   種類/銘柄   数量    空白   金額データ
```

**変更点**:
- `区分`から`種類銘柄`へ情報が分離
- `amount_1`が`数量_01`に名称変更
- `期末現在高`のデータが`金額`行に移動

#### 02_060_02_01形式（有価証券02 - 一覧形式）:

**VerUp前の構造**:
```
ItemName: amount_0, amount_1, 期末現在高, amount_3, amount_4, ...
          ↓         ↓         ↓
        種類/銘柄    数量    金額データ
```

**VerUp後の構造**:
```
ItemName: 種 図嶺, 銘柄, 数量_01, 期末現在高金額月, 異動年月目, ...
          ↓       ↓      ↓       ↓
         種類    銘柄    数量   金額データ（統合列名）
```

**変更点**:
- `amount_0`が`種 図嶺`に名称変更（OCR誤認識の可能性あり）
- 新しい`銘柄`行が追加
- `期末現在高`が`期末現在高金額月`に名称変更（列名が統合）
- または`金額og`という列名も使用される

**重要**: form_idによって列名が異なるため、両方に対応する必要がある

## 現在のロジックの問題点

### SecuritiesProcessor.java の現在の実装
```java
@Override
protected String getItemNameKeyword() {
    return "期末現在高";  // ← このキーワードのみで判定
}
```

### AbstractMergeProcessor.java のマージ判定ロジック
```java
// マージ対象の検出条件
if (itemName != null && itemName.contains(getItemNameKeyword()) && StringUtils.isBlank(value)) {
    // ↑ ItemNameに"期末現在高"が含まれ、かつValueが空白の場合にマージ対象とする
    mergeSourceGroupIds.add(groupId);
}
```

### 問題の詳細

#### VerUp前での動作（正常）:
```
GroupID=2: 期末現在高, Value=""        ← 空白なのでマージ対象として検出 ✓
GroupID=3: 期末現在高, Value="24,340,000"  ← データありなので通常処理
→ GroupID=2とGroupID=3をマージ → 正しい結果
```

#### VerUp後での動作（異常）:
```
GroupID=0: 期末現在高, Value=""        ← 空白なのでマージ対象として検出
GroupID=1: 期末現在高, Value=""        ← 空白なのでマージ対象として検出
GroupID=2: 期末現在高, Value=""        ← 空白なのでマージ対象として検出
...
→ すべてのGroupIDがマージ対象として誤検出される ✗
→ 意図しないマージが発生し、データが破損
```

**根本原因**:
- VerUp後では`期末現在高`行が常に空白（データは`金額`行に移動）
- 現在のロジックは「`期末現在高`が空白 = マージが必要」と判定
- しかし実際には「`金額`行が空白 = マージが必要」が正しい判定条件
- キーワードが`期末現在高`のみのため、VerUp後のCSVに対応できない

## 推奨される変更内容

### 解決策1: form_idベースの判定ロジック追加

**概要**: form_idに応じて異なるキーワードを使用し、正確にマージ対象を検出する

**実装例**:
```java
@Override
protected String getItemNameKeyword() {
    // 基本的なキーワードを返す（後方互換性のため）
    return "期末現在高";
}

/**
 * マージ対象かどうかを判定する
 * 
 * @param record 判定対象のレコード
 * @return マージ対象の場合true
 */
@Override
protected boolean shouldMerge(Map<String, String> record) {
    String itemName = record.get("ItemName");
    String value = record.get("Value");
    String formId = getFormIdFromContext(); // コンテキストから取得
    
    // VerUp後の判定: form_idに応じて適切なItemNameをチェック
    if (formId.equals("02_060_75_01")) {
        // 有価証券01形式: "金額"行または"期末現在高"行が空白の場合
        return ("金額".equals(itemName) || "期末現在高".equals(itemName)) 
               && StringUtils.isBlank(value);
    } else if (formId.equals("02_060_02_01")) {
        // 有価証券02形式: "金額og"、"期末現在高金額月"、または"期末現在高"行が空白の場合
        return ("金額og".equals(itemName) 
                || "期末現在高金額月".equals(itemName) 
                || "期末現在高".equals(itemName)) 
               && StringUtils.isBlank(value);
    }
    
    // VerUp前の判定（従来通り）: "期末現在高"を含むItemNameが空白の場合
    return itemName != null && itemName.contains("期末現在高") 
           && StringUtils.isBlank(value);
}
```

**メリット**:
- form_idごとに最適な判定が可能
- VerUp前後の両方に対応
- 誤検出を防止

**デメリット**:
- form_idの取得方法を実装する必要がある
- 新しいform_idが追加された場合、コード修正が必要

### 解決策2: 複数キーワード対応（推奨）

**概要**: 複数のキーワードをリストで管理し、いずれかに一致する場合にマージ対象とする

**実装例**:

AbstractMergeProcessorを拡張:
```java
/**
 * マージ対象を判定するItemNameのキーワードリストを取得
 * 複数のキーワードに対応することで、VerUp前後の両方のCSV形式に対応
 * 
 * @return ItemNameキーワードのリスト
 */
protected List<String> getItemNameKeywords() {
    // デフォルトは単一キーワード（後方互換性のため）
    return Arrays.asList(getItemNameKeyword());
}

/**
 * レコードをマージ処理する
 * 
 * @param records 処理対象のレコードリスト
 * @return マージ処理後のレコードリスト
 */
@Override
public List<Map<String, String>> process(List<Map<String, String>> records) {
    logger.info(messageSource.getMessage(getStartMessageKey(), null, Locale.getDefault()));

    // GroupIDごとにレコードをグループ化
    Map<String, List<Map<String, String>>> recordsByGroupId = new HashMap<>();
    // マージ元となるGroupIDのリスト
    List<String> mergeSourceGroupIds = new ArrayList<>();

    // 全レコードをスキャンして、マージ対象を検出
    for (Map<String, String> record : records) {
        String groupId = record.get("GrupID");
        // GroupIDが-1の場合はヘッダー情報なのでスキップ
        if (groupId == null || "-1".equals(groupId)) {
            continue;
        }

        // GroupIDごとにレコードを分類
        recordsByGroupId.computeIfAbsent(groupId, k -> new ArrayList<>()).add(record);

        String itemName = record.get("ItemName");
        String value = record.get("Value");
        
        // 複数キーワードのいずれかに一致し、かつValueが空白の場合、マージ対象として登録
        // 例: "期末現在高", "金額", "金額og", "期末現在高金額月" のいずれか
        boolean isTarget = getItemNameKeywords().stream()
            .anyMatch(keyword -> itemName != null && itemName.contains(keyword))
            && StringUtils.isBlank(value);
            
        if (isTarget) {
            // このGroupIDをマージ元として記録
            if (!mergeSourceGroupIds.contains(groupId)) {
                mergeSourceGroupIds.add(groupId);
                logger.debug("マージ対象GroupID検出: {} (ItemName: {})", groupId, itemName);
            }
        }
    }
    
    // マージ対象がない場合は元のレコードをそのまま返す
    if (mergeSourceGroupIds.isEmpty()) {
        logger.info(messageSource.getMessage(getNoTargetMessageKey(), null, Locale.getDefault()));
        return records;
    }

    // GroupIDを数値順にソート
    List<String> sortedGroupIds = sortGroupIds(recordsByGroupId.keySet());
    // マージ元GroupIDと次のGroupIDのマッピングを作成
    Map<String, String> mergeMapping = createMergeMapping(mergeSourceGroupIds, sortedGroupIds);
    // マッピングに基づいてレコードをマージ
    List<Map<String, String>> result = mergeRecords(records, mergeMapping);

    logger.info(messageSource.getMessage(getCompleteMessageKey(),
            new Object[] { mergeMapping.size() }, Locale.getDefault()));

    return result;
}
```

SecuritiesProcessorでキーワードリストをオーバーライド:
```java
/**
 * マージ対象を判定するItemNameのキーワードリストを取得
 * VerUp前後の両方のCSV形式に対応するため、複数のキーワードを返す
 * 
 * @return ItemNameキーワードのリスト
 */
@Override
protected List<String> getItemNameKeywords() {
    return Arrays.asList(
        "期末現在高",      // VerUp前の形式（02_060_75_01, 02_060_02_01）
        "金額",           // VerUp後の形式（02_060_75_01）
        "期末現在高金額月", // VerUp後の形式（02_060_02_01）
        "金額og"          // VerUp後の形式（02_060_02_01の別パターン）
    );
}
```

**メリット**:
- VerUp前後の両方に自動対応
- 新しいキーワードの追加が容易
- form_idを意識する必要がない
- コードがシンプルで保守しやすい

**デメリット**:
- 予期しないItemNameがキーワードに一致する可能性（部分一致のため）
- 完全一致にする場合は`equals()`を使用する必要がある

### 解決策3: バージョン検出による自動切り替え

**概要**: CSVのメタデータからバージョンを自動検出し、適切なキーワードを選択

**実装例**:
```java
/**
 * CSVがVerUp後の形式かどうかを判定
 * 
 * 判定基準:
 * 1. processDateが2026年以降の場合 → VerUp後
 * 2. Image_jshfilenameに"AIRead_ETL"が含まれる場合 → VerUp後
 * 3. 上記以外 → VerUp前
 * 
 * @param records 判定対象のレコードリスト
 * @return VerUp後の場合true、VerUp前の場合false
 */
private boolean isVerUpAfter(List<Map<String, String>> records) {
    for (Map<String, String> record : records) {
        String processDate = record.get("processDate");
        String imagePath = record.get("Image_jshfilename");
        
        // processDateが2026年以降の場合、VerUp後と判定
        if (processDate != null && processDate.startsWith("2026")) {
            logger.debug("VerUp後のCSVを検出（processDate: {}）", processDate);
            return true;
        }
        
        // Image_jshfilenameに"AIRead_ETL"が含まれる場合、VerUp後と判定
        // VerUp前: "C:\evaluation_test\nagashin_kanjo\..."
        // VerUp後: "C:\AIRead_ETL\ocrtemp\..."
        if (imagePath != null && imagePath.contains("AIRead_ETL")) {
            logger.debug("VerUp後のCSVを検出（Image_jshfilename: {}）", imagePath);
            return true;
        }
    }
    
    // 判定基準に該当しない場合はVerUp前と判定
    logger.debug("VerUp前のCSVと判定");
    return false;
}

/**
 * マージ対象を判定するItemNameのキーワードを取得
 * バージョンに応じて適切なキーワードを返す
 * 
 * @return ItemNameキーワード
 */
@Override
protected String getItemNameKeyword() {
    // バージョン判定を行い、適切なキーワードを返す
    if (isVerUpAfter(currentRecords)) {
        // VerUp後は"金額"をキーワードに使用
        // （"期末現在高"は常に空白のため使用不可）
        logger.debug("VerUp後のキーワードを使用: 金額");
        return "金額";
    }
    
    // VerUp前は従来通り"期末現在高"を使用
    logger.debug("VerUp前のキーワードを使用: 期末現在高");
    return "期末現在高";
}
```

**メリット**:
- 自動的にバージョンを判定
- ユーザーがバージョンを意識する必要がない
- 判定基準が明確（processDateとファイルパス）

**デメリット**:
- 判定基準が変更された場合、コード修正が必要
- 複数のform_idに対応する場合、さらに複雑になる
- currentRecordsをクラス変数として保持する必要がある

## 推奨実装アプローチ

**最も堅牢な方法**: 解決策2と3の組み合わせ

1. **複数キーワード対応**で両バージョンに対応
2. **バージョン自動検出**で適切なキーワードを選択
3. **form_id別の処理**で細かい差異に対応

### 実装手順

1. AbstractMergeProcessorに`getItemNameKeywords()`メソッドを追加
2. SecuritiesProcessorで複数キーワードを返すようにオーバーライド
3. バージョン検出ロジックを追加
4. マージ判定ロジックを複数キーワード対応に変更
5. 単体テストでVerUp前後のCSVを両方テスト

## テスト観点

### VerUp前のCSV（sample_before）
- ✓ `期末現在高`行のValueが空白の行を正しく検出
- ✓ 次のGroupIDとマージされる
- ✓ 既存の動作が維持される

### VerUp後のCSV（sample_after）
- ✓ `金額`行のValueが空白の行を正しく検出
- ✓ 次のGroupIDとマージされる
- ✓ VerUp前と同等の出力結果が得られる

### 混在ケース
- ✓ VerUp前後のCSVが混在しても正しく処理される
- ✓ form_idの違いに応じて適切に処理される

## まとめ

VerUp後のCSVでは、データ構造が変更され、金額情報が`期末現在高`行から`金額`行に移動しています。現在のロジックは`期末現在高`のみをキーワードとしているため、VerUp後のCSVでは正しく動作しません。

**必要な変更**:
1. 複数のItemNameキーワードに対応（`期末現在高`, `金額`, `期末現在高金額月`, `金額og`）
2. バージョン検出ロジックの追加
3. form_id別の処理分岐

これにより、VerUp前後のCSVの両方で同等の出力結果を得ることができます。
