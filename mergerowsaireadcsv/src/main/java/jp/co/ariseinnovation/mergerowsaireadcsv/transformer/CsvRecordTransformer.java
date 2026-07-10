package jp.co.ariseinnovation.mergerowsaireadcsv.transformer;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CSVレコードの変換処理を担当するクラス
 * 
 * <p>このクラスは、OCRで読み取ったCSVデータのItemName（項目名）を正規化する処理を提供します。
 * OCRの誤認識や分割により、ItemNameが複数行に分かれている場合に、それらを統合します。</p>
 * 
 * <h3>主な処理内容</h3>
 * <ol>
 *   <li><b>ItemName置換マップの作成</b>: GroupID=0のamount_X行のValueを使用して、新しいItemNameを生成</li>
 *   <li><b>重複ItemNameの検出</b>: 同じItemNameが複数回出現する場合を検出</li>
 *   <li><b>枝番の付与</b>: 重複するItemNameに連番（_100, _101, ...）を付与</li>
 *   <li><b>GroupID=0の削除</b>: 置換マップが存在する場合、GroupID=0の行を削除</li>
 *   <li><b>空白の削除</b>: ItemNameから空白文字を削除</li>
 * </ol>
 * 
 * <h3>処理例</h3>
 * <pre>
 * 【入力データ】
 * GroupID=0, ItemName="amount_0", Value="区分"
 * GroupID=0, ItemName="amount_1", Value="数量"
 * GroupID=1, ItemName="amount_0", Value="その他"
 * GroupID=1, ItemName="amount_1", Value="2,434"
 * 
 * 【処理】
 * 1. 置換マップ作成: amount_0 → 区分_0, amount_1 → 数量_1
 * 2. GroupID=0の行を削除
 * 3. GroupID=1以降のItemNameを置換
 * 
 * 【出力データ】
 * GroupID=1, ItemName="区分_0", Value="その他"
 * GroupID=1, ItemName="数量_1", Value="2,434"
 * </pre>
 * 
 * @see CsvProcessorService
 * @author Arise Innovation
 */
@Component
public class CsvRecordTransformer {

	private static final Logger logger = LoggerFactory.getLogger(CsvRecordTransformer.class);

	@Autowired
	private MessageSource messageSource;

	/**
	 * レコードを加工する
	 * 
	 * <p>このメソッドは、CSVレコードのリストを受け取り、以下の処理を行います：</p>
	 * <ol>
	 *   <li>ItemNameの出現回数をカウント（重複検出のため）</li>
	 *   <li>GroupID=0の行を削除（置換マップが存在する場合のみ）</li>
	 *   <li>各レコードをMap形式に変換し、ItemNameを正規化</li>
	 * </ol>
	 * 
	 * <h4>GroupID=0の削除条件</h4>
	 * <p>ItemName置換マップが存在する場合、GroupID=0の行は削除されます。
	 * これは、GroupID=0の行がItemName定義行であり、実際のデータはGroupID=1以降に
	 * 存在するためです。</p>
	 * 
	 * <h4>処理例</h4>
	 * <pre>
	 * 【入力】
	 * GroupID=0, ItemName="amount_0", Value="区分"      ← 削除対象
	 * GroupID=0, ItemName="amount_1", Value="数量"      ← 削除対象
	 * GroupID=1, ItemName="amount_0", Value="その他"    ← 保持（ItemName置換）
	 * GroupID=1, ItemName="amount_1", Value="2,434"     ← 保持（ItemName置換）
	 * 
	 * 【出力】
	 * GroupID=1, ItemName="区分_0", Value="その他"
	 * GroupID=1, ItemName="数量_1", Value="2,434"
	 * </pre>
	 * 
	 * @param records 処理対象のCSVレコードリスト
	 * @param itemNameReplacements ItemName置換マップ（Key: 置換前, Value: 置換後）
	 * @return 加工後のレコードリスト（各レコードはMapで表現）
	 */
	public List<Map<String, String>> applyTransformations(List<CSVRecord> records,
			Map<String, String> itemNameReplacements) {
		// 結果を格納するリスト
		List<Map<String, String>> transformedRecords = new ArrayList<>();
		
		// 削除されたレコード数のカウンター
		int deletedCount = 0;
		
		// 置換マップが存在するかどうか
		boolean hasReplacements = !itemNameReplacements.isEmpty();

		// ステップ1: ItemNameの出現回数をカウント（重複検出のため）
		Map<String, Integer> itemNameOccurrences = countItemNameOccurrences(records);
		
		// ItemNameごとの連番カウンター（枝番付与用）
		Map<String, Integer> itemNameCounters = new HashMap<>();

		// CSVのヘッダー情報を取得
		List<String> headers = records.isEmpty() ? List.of()
				: new ArrayList<>(records.get(0).getParser().getHeaderNames());

		// ステップ2: 各レコードを処理
		for (CSVRecord record : records) {
			String itemName = record.get("ItemName");
			String groupId = record.get("GrupID");

			// GroupID=0の行を削除（置換マップが存在する場合のみ）
			// GroupID=-1（ヘッダー情報）は削除しない
			if (hasReplacements && "0".equals(groupId) && !"-1".equals(groupId)) {
				deletedCount++;
				logger.debug(messageSource.getMessage("debug.record.deleted",
						new Object[] { itemName, groupId }, Locale.getDefault()));
				continue;  // このレコードはスキップ
			}

			// ステップ3: レコードをMap形式に変換し、ItemNameを正規化
			Map<String, String> newRecord = convertRecordToMap(record, headers, itemNameReplacements,
					itemNameOccurrences, itemNameCounters);
			transformedRecords.add(newRecord);
		}

		// 削除されたレコード数をログ出力
		logger.info(messageSource.getMessage("info.deleted.record.count",
				new Object[] { deletedCount }, Locale.getDefault()));
		
		return transformedRecords;
	}

	/**
	 * ItemNameの出現回数をカウント
	 * 
	 * <p>GroupID=0の行を対象に、各ItemNameが何回出現するかをカウントします。
	 * 同じItemNameが複数回出現する場合、後続の処理で枝番（連番）を付与するために使用します。</p>
	 * 
	 * <h4>カウント対象</h4>
	 * <ul>
	 *   <li>GroupID=0の行のみ</li>
	 *   <li>GroupID=-1（ヘッダー情報）は除外</li>
	 * </ul>
	 * 
	 * <h4>カウント例</h4>
	 * <pre>
	 * 【入力データ】
	 * GroupID=0, ItemName="区分"
	 * GroupID=0, ItemName="区分"
	 * GroupID=0, ItemName="区分"
	 * GroupID=0, ItemName="amount_1"
	 * GroupID=1, ItemName="区分"  ← GroupID=1なのでカウント対象外
	 * 
	 * 【カウント結果】
	 * "区分" → 3回
	 * "amount_1" → 1回
	 * </pre>
	 * 
	 * <h4>重複検出のログ出力</h4>
	 * <p>2回以上出現するItemNameが検出された場合、デバッグログに出力されます。</p>
	 * 
	 * @param records カウント対象のCSVレコードリスト
	 * @return ItemNameをキー、出現回数を値とするMap
	 */
	private Map<String, Integer> countItemNameOccurrences(List<CSVRecord> records) {
		Map<String, Integer> itemNameOccurrences = new HashMap<>();

		// GroupID=0の行を対象にItemNameをカウント
		for (CSVRecord record : records) {
			String itemName = record.get("ItemName");
			String groupId = record.get("GrupID");

			// GroupID=0の行のみカウント
			if ("0".equals(groupId)) {
				// 既存のカウントに+1、存在しない場合は0+1=1
				itemNameOccurrences.put(itemName, itemNameOccurrences.getOrDefault(itemName, 0) + 1);
			}
		}

		// 重複が検出された場合、デバッグログに出力
		itemNameOccurrences.forEach((itemName, count) -> {
			if (count > 1) {
				logger.debug(messageSource.getMessage("debug.duplicate.item.name.detected",
						new Object[] { itemName, count }, Locale.getDefault()));
			}
		});

		return itemNameOccurrences;
	}

	/**
	 * CSVRecordをMapに変換し、ItemNameを処理
	 * 
	 * <p>CSVRecordオブジェクトをMap&lt;String, String&gt;形式に変換します。
	 * 変換時に、ItemName列に対して特別な処理（置換、枝番付与、空白削除）を行います。</p>
	 * 
	 * <h4>処理内容</h4>
	 * <ol>
	 *   <li>CSVの各列をMapに格納</li>
	 *   <li>ItemName列の場合、{@link #processItemName}で正規化処理を実行</li>
	 *   <li>その他の列はそのまま格納</li>
	 * </ol>
	 * 
	 * <h4>変換例</h4>
	 * <pre>
	 * 【入力（CSVRecord）】
	 * ItemName="amount_0", Page="0", GrupID="1", Value="その他", ...
	 * 
	 * 【出力（Map）】
	 * {
	 *   "ItemName": "区分_0",  ← processItemNameで置換
	 *   "Page": "0",
	 *   "GrupID": "1",
	 *   "Value": "その他",
	 *   ...
	 * }
	 * </pre>
	 * 
	 * @param record 変換対象のCSVレコード
	 * @param headers CSVのヘッダー（列名のリスト）
	 * @param itemNameReplacements ItemName置換マップ
	 * @param itemNameOccurrences ItemNameの出現回数マップ
	 * @param itemNameCounters ItemNameごとの連番カウンター
	 * @return Map形式に変換されたレコード
	 */
	private Map<String, String> convertRecordToMap(CSVRecord record, List<String> headers,
			Map<String, String> itemNameReplacements, Map<String, Integer> itemNameOccurrences,
			Map<String, Integer> itemNameCounters) {
		// 結果を格納するMap
		Map<String, String> newRecord = new HashMap<>();
		
		// 元のItemNameを保持（枝番付与時に使用）
		String originalItemName = record.get("ItemName");
		String groupId = record.get("GrupID");

		// 各列をMapに格納
		for (String header : headers) {
			String value = record.get(header);

			// ItemName列の場合、特別な処理を実行
			if ("ItemName".equals(header)) {
				value = processItemName(value, originalItemName, groupId, itemNameReplacements,
						itemNameOccurrences, itemNameCounters);
			}

			// Mapに格納
			newRecord.put(header, value);
		}

		return newRecord;
	}

	/**
	 * ItemNameを処理（置き換え、枝番付与、空白削除）
	 * 
	 * <p>ItemNameに対して以下の処理を順次実行します：</p>
	 * <ol>
	 *   <li><b>置換処理</b>: 置換マップに存在する場合、新しいItemNameに置換</li>
	 *   <li><b>枝番付与（GroupID=0）</b>: 重複するItemNameに連番を付与</li>
	 *   <li><b>枝番付与（GroupID≠0）</b>: GroupID=0で付与された枝番を引き継ぐ</li>
	 *   <li><b>空白削除</b>: ItemNameから全ての空白文字を削除</li>
	 * </ol>
	 * 
	 * <h4>処理1: 置換処理</h4>
	 * <p>置換マップに存在するItemNameは、新しいItemNameに置換されます。</p>
	 * <pre>
	 * 置換マップ: {"amount_0": "区分_0", "amount_1": "数量_1"}
	 * ItemName="amount_0" → "区分_0"
	 * </pre>
	 * 
	 * <h4>処理2: 枝番付与（GroupID=0の場合）</h4>
	 * <p>同じItemNameが複数回出現する場合、連番（_100, _101, ...）を付与します。</p>
	 * <pre>
	 * 【入力】
	 * GroupID=0, ItemName="区分" (1回目)
	 * GroupID=0, ItemName="区分" (2回目)
	 * GroupID=0, ItemName="区分" (3回目)
	 * 
	 * 【出力】
	 * GroupID=0, ItemName="区分_100"
	 * GroupID=0, ItemName="区分_101"
	 * GroupID=0, ItemName="区分_102"
	 * </pre>
	 * 
	 * <h4>処理3: 枝番付与（GroupID≠0の場合）</h4>
	 * <p>GroupID=0で付与された枝番を、GroupID=1以降の行にも適用します。</p>
	 * <pre>
	 * 【GroupID=0で付与された枝番】
	 * GroupID=0, ItemName="区分_100"
	 * GroupID=0, ItemName="区分_101"
	 * 
	 * 【GroupID=1以降に適用】
	 * GroupID=1, ItemName="区分" → "区分_100"
	 * GroupID=1, ItemName="区分" → "区分_101"
	 * </pre>
	 * 
	 * <h4>処理4: 空白削除</h4>
	 * <p>ItemNameから全ての空白文字（スペース、タブ、改行など）を削除します。</p>
	 * <pre>
	 * ItemName="区分 _0" → "区分_0"
	 * ItemName="数 量_1" → "数量_1"
	 * </pre>
	 * 
	 * @param itemName 処理対象のItemName
	 * @param originalItemName 元のItemName（枝番付与前）
	 * @param groupId レコードのGroupID
	 * @param itemNameReplacements ItemName置換マップ
	 * @param itemNameOccurrences ItemNameの出現回数マップ
	 * @param itemNameCounters ItemNameごとの連番カウンター
	 * @return 処理後のItemName
	 */
	private String processItemName(String itemName, String originalItemName, String groupId,
			Map<String, String> itemNameReplacements, Map<String, Integer> itemNameOccurrences,
			Map<String, Integer> itemNameCounters) {

		// 処理1: 置換処理
		// 置換マップに存在する場合、新しいItemNameに置換
		if (itemNameReplacements.containsKey(itemName)) {
			String oldValue = itemName;
			itemName = itemNameReplacements.get(itemName);
			logger.debug(messageSource.getMessage("debug.item.name.replaced",
					new Object[] { oldValue, itemName }, Locale.getDefault()));
		}
		// 処理2: 枝番付与（GroupID=0の場合）
		// 同じItemNameが複数回出現する場合、連番を付与
		else if (itemNameOccurrences.getOrDefault(itemName, 0) > 1 && "0".equals(groupId)) {
			// 連番を取得（初回は100から開始）
			int occurrence = itemNameCounters.getOrDefault(itemName, 100);
			String newItemName = itemName + "_" + occurrence;
			
			logger.debug(messageSource.getMessage("debug.item.name.branch.added",
					new Object[] { itemName, newItemName, occurrence }, Locale.getDefault()));
			
			itemName = newItemName;
			
			// 次回のために連番をインクリメント
			itemNameCounters.put(originalItemName, occurrence + 1);
		}
		// 処理3: 枝番付与（GroupID≠0の場合）
		// GroupID=0で付与された枝番を引き継ぐ
		else if (itemNameOccurrences.getOrDefault(originalItemName, 0) > 1 && !"-1".equals(groupId)) {
			// GroupID=0で付与された連番を取得（逆順に適用）
			int occurrence = itemNameCounters.getOrDefault(originalItemName, 0) - 1;
			
			// 連番が有効な範囲内の場合のみ適用
			if (occurrence >= 0) {
				itemName = originalItemName + "_" + occurrence;
			}
		}

		// 処理4: 空白削除
		// ItemNameから全ての空白文字を削除
		return itemName != null ? itemName.replaceAll("\\s+", "") : null;
	}

	/**
	 * ItemNameの置き換えマップを作成
	 * 
	 * <p>このメソッドは、GroupID=0の"amount_X"形式のItemNameを、
	 * そのValueを使用した新しいItemNameに置き換えるマップを作成します。</p>
	 * 
	 * <h4>処理の目的</h4>
	 * <p>OCRで読み取ったCSVでは、ItemNameが"amount_0", "amount_1"などの
	 * 汎用的な名前になっている場合があります。GroupID=0の行には、
	 * これらのItemNameに対応する実際の項目名（"区分", "数量"など）が
	 * Valueとして格納されています。このメソッドは、それらを使用して
	 * 意味のあるItemNameに置き換えるマップを作成します。</p>
	 * 
	 * <h4>処理手順</h4>
	 * <ol>
	 *   <li>GroupID=0の行を検索</li>
	 *   <li>ItemNameが"amount_"で始まり、Valueが空白でない行を抽出</li>
	 *   <li>Valueから空白を削除</li>
	 *   <li>新しいItemName = Value + "_" + 数字（amount_の後の数字）を生成</li>
	 *   <li>置換マップに追加</li>
	 * </ol>
	 * 
	 * <h4>処理例</h4>
	 * <pre>
	 * 【入力データ】
	 * GroupID=0, ItemName="amount_0", Value="区分"
	 * GroupID=0, ItemName="amount_1", Value="数量"
	 * GroupID=0, ItemName="amount_2", Value=""        ← Valueが空白なので除外
	 * GroupID=0, ItemName="formid", Value="02_060"    ← "amount_"で始まらないので除外
	 * GroupID=1, ItemName="amount_0", Value="その他"  ← GroupID=1なので除外
	 * 
	 * 【作成される置換マップ】
	 * {
	 *   "amount_0": "区分_0",
	 *   "amount_1": "数量_1"
	 * }
	 * 
	 * 【適用後】
	 * GroupID=1, ItemName="amount_0" → "区分_0"
	 * GroupID=1, ItemName="amount_1" → "数量_1"
	 * </pre>
	 * 
	 * <h4>空白削除の理由</h4>
	 * <p>OCRの誤認識により、Valueに余分な空白が含まれる場合があります。
	 * 例: "区分 " → "区分"、"数 量" → "数量"</p>
	 * 
	 * @param records 処理対象のCSVレコードリスト
	 * @return ItemName置換マップ（Key: 置換前のItemName, Value: 置換後のItemName）
	 */
	public Map<String, String> buildItemNameReplacements(List<CSVRecord> records) {
		// 置換マップを格納するMap
		Map<String, String> replacements = new HashMap<>();
		
		// GroupID=0の"amount_X"形式のItemNameとそのValueを一時保存
		Map<String, String> groupZeroValues = new HashMap<>();

		// ステップ1: GroupID=0の"amount_X"行を検索し、Valueを取得
		for (CSVRecord record : records) {
			String itemName = record.get("ItemName");
			String groupId = record.get("GrupID");
			String value = record.get("Value");

			// GroupID=-1（ヘッダー情報）はスキップ
			if ("-1".equals(groupId)) {
				continue;
			}

			// GroupID=0で、ItemNameが"amount_"で始まり、Valueが空白でない場合
			if ("0".equals(groupId) && itemName.startsWith("amount_") && StringUtils.isNotBlank(value)) {
				// Valueから空白を削除
				String trimmedValue = value.replaceAll("\\s+", "");
				groupZeroValues.put(itemName, trimmedValue);
			}
		}

		// ステップ2: 置換マップを作成
		for (Map.Entry<String, String> entry : groupZeroValues.entrySet()) {
			String itemName = entry.getKey();  // 例: "amount_0"
			String value = entry.getValue();    // 例: "区分"
			
			// "amount_"の後の数字を取得（例: "amount_0" → "0"）
			String suffix = itemName.substring("amount_".length());
			
			// 新しいItemNameを生成（例: "区分_0"）
			String newItemName = value + "_" + suffix;

			// 置換マップに追加
			replacements.put(itemName, newItemName);
			
			logger.debug(messageSource.getMessage("debug.replacement.map.added",
					new Object[] { itemName, newItemName }, Locale.getDefault()));
		}

		return replacements;
	}
}
