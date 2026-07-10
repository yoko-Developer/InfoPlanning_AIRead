package jp.co.ariseinnovation.mergerowsaireadcsv.processor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 有価証券（02_060）の処理を担当するクラス
 * 
 * <p>このクラスは、OCRソフトウェアで読み取った有価証券のCSVデータを処理します。
 * 複数行に分割されたデータを1行にマージする処理を行います。</p>
 * 
 * <h3>処理対象のform_id</h3>
 * <ul>
 *   <li>02_060_75_01: 有価証券明細形式</li>
 *   <li>02_060_02_01: 有価証券一覧形式</li>
 * </ul>
 * 
 * <h3>処理の流れ</h3>
 * <ol>
 *   <li><b>前処理（VerUp後対応）</b>: "期末現在高"行が空白で、次のグループが"金額"行の場合、値を移動</li>
 *   <li><b>マージ処理</b>: ItemNameが"期末現在高"を含み、かつValueが空白の行を検出してマージ</li>
 * </ol>
 * 
 * <h3>VerUp前後の違い</h3>
 * <ul>
 *   <li><b>VerUp前</b>: "期末現在高"行に金額データが格納</li>
 *   <li><b>VerUp後</b>: "期末現在高"行は空白、"金額"行に金額データが格納</li>
 * </ul>
 * 
 * <h3>前処理の詳細</h3>
 * <p>VerUp後のCSVに対応するため、以下の前処理を実行します：</p>
 * <ol>
 *   <li>ItemName.contains("期末現在高")でValueが空白の行を検出</li>
 *   <li>その行のグループの直後がItemName.startsWith("金額")の行のグループか確認</li>
 *   <li>Page, GID, GrupIDが一致する"金額"行のValueを"期末現在高"行にコピー</li>
 *   <li>"金額"行のValueをクリア</li>
 * </ol>
 * 
 * @see AbstractMergeProcessor
 * @author Arise Innovation
 */
@Component
public class SecuritiesProcessor extends AbstractMergeProcessor {

	private static final Logger logger = LoggerFactory.getLogger(SecuritiesProcessor.class);

	/**
	 * 有価証券の処理を実行（前処理 + マージ処理）
	 * 
	 * <p>このメソッドは、VerUp後のCSVに対応するため、以下の処理を実行します：</p>
	 * <ol>
	 *   <li>前処理1: "異動"で始まる行より後ろの"金額"行を"金額_101"に変更</li>
	 *   <li>前処理2: "期末現在高"行が空白で次のグループが"金額"行の場合、値を移動</li>
	 *   <li>マージ処理: 親クラスのマージ処理を実行</li>
	 * </ol>
	 * 
	 * @param records 処理対象のレコードリスト
	 * @return 処理後のレコードリスト
	 */
	@Override
	public List<Map<String, String>> process(List<Map<String, String>> records) {
		logger.info("有価証券の処理を開始します");
		
		// 前処理1: "期末現在高"と"金額"の値を移動（"金額"の名前変更前に実行）
		List<Map<String, String>> preprocessedRecords = preprocessAmountFields(records);
		
		// 前処理2: "異動"で始まる行より後ろの"金額"行を"金額_101"に変更
		List<Map<String, String>> renamedRecords = renameAmountFieldsAfterIdou(preprocessedRecords);
		
		// 親クラスのマージ処理を実行
		return super.process(renamedRecords);
	}

	/**
	 * "異動"で始まる行より後ろの"金額"行を"金額_101"に変更
	 * 
	 * <p>CSVファイル内で、"異動"で始まるItemNameの行より後ろに出現する
	 * ItemName="金額"の行を、ItemName="金額_101"に変更します。</p>
	 * 
	 * <h3>処理の流れ</h3>
	 * <ol>
	 *   <li>CSVを順番に読み、"異動"で始まる行が出現したらフラグを立てる</li>
	 *   <li>フラグが立った後に出現するItemName="金額"の行を"金額_101"に変更</li>
	 * </ol>
	 * 
	 * <h3>対象となる"異動"で始まる行</h3>
	 * <ul>
	 *   <li>"異動年月日"</li>
	 *   <li>"異動事由"</li>
	 *   <li>その他"異動"で始まるItemName</li>
	 * </ul>
	 * 
	 * @param records 処理対象のレコードリスト（CSVファイルの行順を保持）
	 * @return 処理後のレコードリスト
	 */
	private List<Map<String, String>> renameAmountFieldsAfterIdou(List<Map<String, String>> records) {
		boolean afterIdou = false;
		int renameCount = 0;
		
		for (Map<String, String> record : records) {
			String itemName = record.get("ItemName");
			
			if (itemName == null) {
				continue;
			}
			
			// "異動"で始まる行が出現したらフラグを立てる
			if (itemName.startsWith("異動")) {
				afterIdou = true;
				continue;
			}
			
			// "異動"で始まる行より後ろで、ItemName="金額"の行を"金額_101"に変更
			if (afterIdou && "金額".equals(itemName)) {
				record.put("ItemName", "金額_101");
				renameCount++;
			}
		}
		
		if (renameCount > 0) {
			logger.info("「異動」で始まる行より後ろの「金額」行を「金額_101」に変更しました。変更件数: {}", renameCount);
		}
		
		return records;
	}

	/**
	 * "期末現在高"と"金額"フィールドの前処理
	 * 
	 * <p>VerUp後のCSVでは、"期末現在高"行が空白で、"金額"行にデータが格納されています。
	 * この前処理で、"金額"行の値を"期末現在高"行に移動し、値を移動した"金額"行を削除します。</p>
	 * 
	 * <h3>CSVの構造</h3>
	 * <p>実際のCSVでは、同じItemNameのレコードがGrupID順にまとまって並んでいます：</p>
	 * <pre>
	 * "期末現在高","0","detail","0",""
	 * "期末現在高","0","detail","1",""
	 * ...
	 * "期末現在高","0","detail","10",""
	 * "金額","0","detail","0","24,340,000"
	 * "金額","0","detail","1","24,340,000"
	 * ...
	 * </pre>
	 * 
	 * <h3>処理条件</h3>
	 * <ol>
	 *   <li>ItemName.contains("期末現在高")の行でValueが空白</li>
	 *   <li>同じPage, GID, GrupIDを持つItemName.startsWith("金額")の行が存在</li>
	 * </ol>
	 * 
	 * <h3>処理内容</h3>
	 * <ul>
	 *   <li>"金額"行のValueを"期末現在高"行のValueにコピー</li>
	 *   <li>値を移動した"金額"行を出力ファイルから削除</li>
	 * </ul>
	 * 
	 * <h3>処理方法</h3>
	 * <p>Page, GID, GrupIDの複合キーでインデックスを作成し、効率的にマッチングします。
	 * 値を移動した"金額"行は削除対象としてマークし、最後にフィルタリングします。</p>
	 * 
	 * @param records 処理対象のレコードリスト（CSVファイルの行順を保持）
	 * @return 前処理後のレコードリスト（削除対象の行を除く）
	 */
	private List<Map<String, String>> preprocessAmountFields(List<Map<String, String>> records) {
		// "期末現在高"行が存在するかチェック
		boolean hasKimatsuGenzaidaka = records.stream()
				.anyMatch(r -> {
					String itemName = r.get("ItemName");
					String grupId = r.get("GrupID");
					return itemName != null && itemName.contains("期末現在高") 
							&& grupId != null && !"-1".equals(grupId);
				});
		
		if (!hasKimatsuGenzaidaka) {
			logger.debug("「期末現在高」行が見つかりませんでした。");
			return records;
		}
		
		// "金額"行が存在するかチェック
		boolean hasKingaku = records.stream()
				.anyMatch(r -> {
					String itemName = r.get("ItemName");
					String grupId = r.get("GrupID");
					return itemName != null && itemName.startsWith("金額") 
							&& grupId != null && !"-1".equals(grupId);
				});
		
		if (!hasKingaku) {
			logger.debug("「金額」行が見つかりませんでした。");
			return records;
		}
		
		logger.info("「期末現在高」と「金額」の両方が存在します。値の移動処理を開始します。");
		
		// デバッグ: どの"金額"行が見つかったか確認
		long kingakuCount = records.stream()
				.filter(r -> {
					String itemName = r.get("ItemName");
					String grupId = r.get("GrupID");
					return itemName != null && itemName.startsWith("金額") 
							&& grupId != null && !"-1".equals(grupId);
				})
				.count();
		
		if (kingakuCount > 0) {
			logger.debug("見つかった金額行の総数: {}", kingakuCount);
		}
		
		// Page, GID, GrupIDの複合キーで"金額"行をインデックス化（すべての"金額"行）
		// 注意: 優先順位: "金額og" > "金額_01" > "金額_02" > ... > "金額"
		// 値がある行を優先し、値がない行は無視する
		Map<String, Map<String, String>> kingakuIndex = new HashMap<>();
		for (Map<String, String> record : records) {
			String itemName = record.get("ItemName");
			String grupId = record.get("GrupID");
			
			if (itemName != null && itemName.startsWith("金額") 
					&& grupId != null && !"-1".equals(grupId)) {
				String page = record.get("Page");
				String gid = record.get("GID");
				String key = page + "_" + gid + "_" + grupId;
				String value = record.get("Value");
				
				// 既に同じキーのレコードが存在する場合、優先順位をチェック
				Map<String, String> existingRecord = kingakuIndex.get(key);
				if (existingRecord != null) {
					String existingItemName = existingRecord.get("ItemName");
					String existingValue = existingRecord.get("Value");
					
					// 既存レコードに値があり、新規レコードに値がない場合は上書きしない
					if (StringUtils.isNotBlank(existingValue) && StringUtils.isBlank(value)) {
						logger.debug("金額行をスキップ（既存に値あり、新規に値なし）: key={}, existing={}, new={}", 
								key, existingItemName, itemName);
						continue;
					}
					
					// 既存が"金額og"の場合は常に優先（上書きしない）
					if ("金額og".equals(existingItemName)) {
						logger.debug("金額行をスキップ（既存が金額og）: key={}, existing={}, new={}", 
								key, existingItemName, itemName);
						continue;
					}
					
					// 既存と新規の両方が"金額_XX"形式の場合、番号が小さい方を優先
					if (existingItemName.matches("金額_\\d+") && itemName.matches("金額_\\d+")) {
						try {
							int existingNum = Integer.parseInt(existingItemName.substring(3));
							int newNum = Integer.parseInt(itemName.substring(3));
							if (existingNum < newNum) {
								logger.debug("金額行をスキップ（既存の番号が小さい）: key={}, existing={} ({}), new={} ({})", 
										key, existingItemName, existingNum, itemName, newNum);
								continue;
							}
						} catch (NumberFormatException e) {
							// 番号の解析に失敗した場合は警告を出力して続行
							logger.warn("金額行の番号解析に失敗: existing={}, new={}", existingItemName, itemName);
						}
					}
					
					// 既存が"金額_XX"で新規が"金額"（番号なし）の場合は上書きしない
					if (existingItemName.matches("金額_\\d+") && "金額".equals(itemName)) {
						logger.debug("金額行をスキップ（既存が番号付き、新規が番号なし）: key={}, existing={}, new={}", 
								key, existingItemName, itemName);
						continue;
					}
				}
				
				kingakuIndex.put(key, record);
				logger.debug("金額行をインデックスに追加: key={}, ItemName={}", key, itemName);
			}
		}
		
		logger.debug("金額行のインデックス作成完了。総数: {}", kingakuIndex.size());
		
		// 削除対象の"金額"行を追跡するSet
		Set<Map<String, String>> recordsToRemove = new HashSet<>();
		
		// "期末現在高"行を走査し、対応する"金額"行から値を移動し、すべての対応する"金額"行を削除対象としてマーク
		int movedCount = 0;
		for (Map<String, String> record : records) {
			String itemName = record.get("ItemName");
			String grupId = record.get("GrupID");
			
			if (itemName == null || !itemName.contains("期末現在高") 
					|| grupId == null || "-1".equals(grupId)) {
				continue;
			}
			
			String value = record.get("Value");
			
			// 対応する"金額"行を検索
			String page = record.get("Page");
			String gid = record.get("GID");
			String key = page + "_" + gid + "_" + grupId;
			
			logger.info("期末現在高行を処理中: key={}, Value={}", key, value);
			
			Map<String, String> kingakuRecord = kingakuIndex.get(key);
			if (kingakuRecord != null) {
				logger.info("対応する金額行が見つかりました: ItemName={}, Value={}", 
						kingakuRecord.get("ItemName"), kingakuRecord.get("Value"));
				
				// "期末現在高"行が空白の場合のみ値を移動
				if (StringUtils.isBlank(value)) {
					String kingakuValue = kingakuRecord.get("Value");
					
					if (StringUtils.isNotBlank(kingakuValue)) {
						// 値を移動
						record.put("Value", kingakuValue);
						movedCount++;
						
						logger.info("値を移動しました: Page={}, GID={}, GrupID={}, Value={}",
								page, gid, grupId, kingakuValue);
					}
				}
				
				// 対応する"期末現在高"行が存在する場合、"金額"行を削除対象としてマーク（値の有無に関わらず）
				recordsToRemove.add(kingakuRecord);
			} else {
				logger.warn("対応する金額行が見つかりませんでした: key={}", key);
			}
		}
		
		if (movedCount > 0) {
			logger.info("「期末現在高」への値の移動が完了しました。移動件数: {}", movedCount);
		}
		
		if (!recordsToRemove.isEmpty()) {
			// 削除対象の"金額"行を除外してリストを返す
			List<Map<String, String>> filteredRecords = records.stream()
					.filter(r -> !recordsToRemove.contains(r))
					.collect(Collectors.toList());
			
			int removedCount = records.size() - filteredRecords.size();
			logger.info("対応する「期末現在高」行が存在する「金額」行を削除しました。削除件数: {}", removedCount);
			
			return filteredRecords;
		}
		
		return records;
	}

	/**
	 * 指定されたform_idが処理対象かどうかを判定
	 * 
	 * <p>有価証券のform_idは"02_060"で始まるため、前方一致で判定します。</p>
	 * 
	 * @param formId 判定対象のform_id（例: "02_060_75_01", "02_060_02_01"）
	 * @return 処理対象の場合true、それ以外はfalse
	 */
	@Override
	public boolean isTargetFormId(String formId) {
		return formId.startsWith("02_060");
	}

	/**
	 * マージ対象を判定するItemNameのキーワードを取得
	 * 
	 * <p>このキーワードを含むItemNameで、かつValueが空白の行がマージ対象として検出されます。</p>
	 * 
	 * <h4>VerUp前のCSV（正常動作）</h4>
	 * <pre>
	 * ItemName="期末現在高", Value=""          ← マージ対象として検出
	 * ItemName="期末現在高", Value="24,340,000" ← データありなので通常処理
	 * </pre>
	 * 
	 * <h4>VerUp後のCSV（問題あり）</h4>
	 * <pre>
	 * ItemName="期末現在高", Value="" ← 常に空白のため、全行が誤検出される
	 * ItemName="金額", Value="24,340,000" ← 実際の金額データはこちらに移動
	 * </pre>
	 * 
	 * @return マージ対象判定用のキーワード（"期末現在高"）
	 */
	@Override
	protected String getItemNameKeyword() {
		return "期末現在高";
	}

	/**
	 * 処理開始時のログメッセージキーを取得
	 * 
	 * @return メッセージプロパティのキー
	 */
	@Override
	protected String getStartMessageKey() {
		return "info.securities.processing.start";
	}

	/**
	 * マージ対象が見つからなかった場合のログメッセージキーを取得
	 * 
	 * @return メッセージプロパティのキー
	 */
	@Override
	protected String getNoTargetMessageKey() {
		return "info.securities.no.merge.target";
	}

	/**
	 * 処理完了時のログメッセージキーを取得
	 * 
	 * @return メッセージプロパティのキー
	 */
	@Override
	protected String getCompleteMessageKey() {
		return "info.securities.merge.complete";
	}

	/**
	 * プロセッサーの名前を取得
	 * 
	 * <p>ログ出力やデバッグ時の識別に使用されます。</p>
	 * 
	 * @return プロセッサー名（"Securities"）
	 */
	@Override
	public String getProcessorName() {
		return "Securities";
	}
}
