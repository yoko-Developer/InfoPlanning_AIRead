package jp.co.ariseinnovation.mergerowsaireadcsv.processor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * マージ処理の共通ロジックを提供する抽象クラス
 * 
 * <p>このクラスは、OCRで読み取ったCSVデータの複数行を1行にマージする処理を提供します。
 * 具体的な処理対象（有価証券、役員給与など）は、このクラスを継承して実装します。</p>
 * 
 * <h3>マージ処理の流れ</h3>
 * <ol>
 *   <li><b>マージ対象の検出</b>: 特定のItemNameを持ち、Valueが空白の行を検出</li>
 *   <li><b>GroupIDのソート</b>: 検出した行のGroupIDを数値順にソート</li>
 *   <li><b>マッピング作成</b>: マージ元GroupIDと次のGroupIDのマッピングを作成</li>
 *   <li><b>レコードのマージ</b>: マッピングに基づいてレコードを結合</li>
 * </ol>
 * 
 * <h3>マージ処理の具体例</h3>
 * <pre>
 * 【入力データ】
 * GroupID=0, ItemName="区分", Value="その他"
 * GroupID=0, ItemName="期末現在高", Value=""          ← 空白なのでマージ対象
 * GroupID=1, ItemName="区分", Value="沖縄県卸商業"
 * GroupID=1, ItemName="期末現在高", Value="24,340,000"
 * 
 * 【処理】
 * 1. GroupID=0の"期末現在高"が空白 → マージ元として検出
 * 2. GroupID=0とGroupID=1をマッピング
 * 3. GroupID=0のレコードを一時保存
 * 4. GroupID=1のレコードと結合
 * 
 * 【出力データ】
 * GroupID=1, ItemName="区分", Value="その他\n沖縄県卸商業"
 * GroupID=1, ItemName="期末現在高", Value="24,340,000"
 * </pre>
 * 
 * @see DataProcessor
 * @author Arise Innovation
 */
public abstract class AbstractMergeProcessor implements DataProcessor {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMergeProcessor.class);

	@Autowired
	protected MessageSource messageSource;

	/**
	 * マージ対象を判定するItemNameのキーワードを取得
	 * 
	 * <p>このキーワードを含むItemNameで、かつValueが空白の行がマージ対象として検出されます。</p>
	 * 
	 * @return マージ対象判定用のキーワード（例: "期末現在高"）
	 */
	protected abstract String getItemNameKeyword();

	/**
	 * 処理開始メッセージのキーを取得
	 * 
	 * @return メッセージプロパティのキー
	 */
	protected abstract String getStartMessageKey();

	/**
	 * マージ対象なしメッセージのキーを取得
	 * 
	 * @return メッセージプロパティのキー
	 */
	protected abstract String getNoTargetMessageKey();

	/**
	 * 処理完了メッセージのキーを取得
	 * 
	 * @return メッセージプロパティのキー
	 */
	protected abstract String getCompleteMessageKey();

	/**
	 * レコードのマージ処理を実行
	 * 
	 * <p>このメソッドは以下の手順でマージ処理を行います：</p>
	 * <ol>
	 *   <li>全レコードをスキャンし、マージ対象のGroupIDを検出</li>
	 *   <li>GroupIDを数値順にソート</li>
	 *   <li>マージ元GroupIDと次のGroupIDのマッピングを作成</li>
	 *   <li>マッピングに基づいてレコードを結合</li>
	 * </ol>
	 * 
	 * @param records 処理対象のレコードリスト（CSVの各行をMapで表現）
	 * @return マージ処理後のレコードリスト
	 */
	@Override
	public List<Map<String, String>> process(List<Map<String, String>> records) {
		logger.info(messageSource.getMessage(getStartMessageKey(), null, Locale.getDefault()));

		// GroupIDごとにレコードをグループ化するためのMap
		// Key: GroupID, Value: そのGroupIDに属するレコードのリスト
		Map<String, List<Map<String, String>>> recordsByGroupId = new HashMap<>();
		
		// マージ元となるGroupIDのリスト
		// （ItemNameがキーワードを含み、かつValueが空白の行のGroupID）
		List<String> mergeSourceGroupIds = new ArrayList<>();

		// ステップ1: 全レコードをスキャンし、マージ対象を検出
		for (Map<String, String> record : records) {
			String groupId = record.get("GrupID");
			
			// GroupIDが-1の場合はヘッダー情報なのでスキップ
			if (groupId == null || "-1".equals(groupId)) {
				continue;
			}

			// GroupIDごとにレコードを分類
			recordsByGroupId.computeIfAbsent(groupId, k -> new ArrayList<>()).add(record);

			// マージ対象の判定
			String itemName = record.get("ItemName");
			String value = record.get("Value");
			
			// ItemNameがキーワードを含み、かつValueが空白の場合、マージ対象として登録
			if (itemName != null && itemName.contains(getItemNameKeyword()) && StringUtils.isBlank(value)) {
				if (!mergeSourceGroupIds.contains(groupId)) {
					mergeSourceGroupIds.add(groupId);
					logger.debug(messageSource.getMessage("debug.merge.target.group.detected",
							new Object[] { groupId }, Locale.getDefault()));
				}
			}
		}

		// マージ対象が見つからない場合は、元のレコードをそのまま返す
		if (mergeSourceGroupIds.isEmpty()) {
			logger.info(messageSource.getMessage(getNoTargetMessageKey(), null, Locale.getDefault()));
			return records;
		}

		// ステップ2: GroupIDを数値順にソート
		List<String> sortedGroupIds = sortGroupIds(recordsByGroupId.keySet());
		
		// ステップ3: マージ元GroupIDと次のGroupIDのマッピングを作成
		Map<String, String> mergeMapping = createMergeMapping(mergeSourceGroupIds, sortedGroupIds);
		
		// ステップ4: マッピングに基づいてレコードをマージ
		List<Map<String, String>> result = mergeRecords(records, mergeMapping);

		logger.info(messageSource.getMessage(getCompleteMessageKey(),
				new Object[] { mergeMapping.size() }, Locale.getDefault()));

		return result;
	}

	/**
	 * GroupIDを数値でソート
	 * 
	 * <p>GroupIDは文字列として格納されていますが、数値として扱う必要があります。
	 * 例: "0", "1", "2", "10" → 数値順にソート → "0", "1", "2", "10"</p>
	 * 
	 * <p>数値変換に失敗した場合は、文字列として辞書順にソートします。</p>
	 * 
	 * @param groupIds ソート対象のGroupIDのセット
	 * @return ソート済みのGroupIDリスト
	 */
	private List<String> sortGroupIds(java.util.Set<String> groupIds) {
		List<String> sortedGroupIds = new ArrayList<>(groupIds);
		sortedGroupIds.sort((g1, g2) -> {
			try {
				// 数値として比較
				return Integer.compare(Integer.parseInt(g1), Integer.parseInt(g2));
			} catch (NumberFormatException e) {
				// 数値変換に失敗した場合は文字列として比較
				return g1.compareTo(g2);
			}
		});
		return sortedGroupIds;
	}

	/**
	 * マージ対象GroupIDとその次のGroupIDのマッピングを作成
	 * 
	 * <p>マージ元のGroupIDと、その次のGroupID（マージ先）のマッピングを作成します。</p>
	 * 
	 * <h4>マッピング例</h4>
	 * <pre>
	 * ソート済みGroupID: [0, 1, 2, 3, 4]
	 * マージ元GroupID: [0, 2]
	 * 
	 * 作成されるマッピング:
	 * 0 → 1 (GroupID=0のレコードをGroupID=1にマージ)
	 * 2 → 3 (GroupID=2のレコードをGroupID=3にマージ)
	 * </pre>
	 * 
	 * @param sourceGroupIds マージ元のGroupIDリスト
	 * @param sortedGroupIds ソート済みの全GroupIDリスト
	 * @return マージ元GroupIDをキー、マージ先GroupIDを値とするMap
	 */
	private Map<String, String> createMergeMapping(List<String> sourceGroupIds, List<String> sortedGroupIds) {
		Map<String, String> mergeMapping = new HashMap<>();
		
		for (String sourceGroupId : sourceGroupIds) {
			// ソート済みリストでの位置を取得
			int index = sortedGroupIds.indexOf(sourceGroupId);
			
			// 次のGroupIDが存在する場合のみマッピングを作成
			// （最後のGroupIDの場合はマージ先がないのでスキップ）
			if (index >= 0 && index < sortedGroupIds.size() - 1) {
				String targetGroupId = sortedGroupIds.get(index + 1);
				mergeMapping.put(sourceGroupId, targetGroupId);
				logger.debug(messageSource.getMessage("debug.merge.mapping",
						new Object[] { sourceGroupId, targetGroupId }, Locale.getDefault()));
			}
		}
		
		return mergeMapping;
	}

	/**
	 * マージマッピングに基づいてレコードをマージ
	 * 
	 * <p>このメソッドは、作成されたマッピングに基づいて実際のマージ処理を行います。</p>
	 * 
	 * <h4>処理の流れ</h4>
	 * <ol>
	 *   <li>マージ元のレコード（Valueが空白）を一時保存</li>
	 *   <li>マージ先のレコード（Valueにデータあり）を処理する際、一時保存したレコードと結合</li>
	 *   <li>結合したレコードを結果リストに追加</li>
	 * </ol>
	 * 
	 * <h4>マージ例</h4>
	 * <pre>
	 * マッピング: 0 → 1
	 * 
	 * 【入力】
	 * GroupID=0, ItemName="区分", Value="その他"
	 * GroupID=1, ItemName="区分", Value="沖縄県卸商業"
	 * 
	 * 【処理】
	 * 1. GroupID=0のレコードを一時保存（Key="区分_0_1"）
	 * 2. GroupID=1のレコードを処理
	 * 3. 一時保存から"区分_0_1"を取得
	 * 4. Value="その他" + "\n" + "沖縄県卸商業" = "その他\n沖縄県卸商業"
	 * 
	 * 【出力】
	 * GroupID=1, ItemName="区分", Value="その他\n沖縄県卸商業"
	 * </pre>
	 * 
	 * @param records 処理対象のレコードリスト
	 * @param mergeMapping マージ元GroupIDとマージ先GroupIDのマッピング
	 * @return マージ処理後のレコードリスト
	 */
	private List<Map<String, String>> mergeRecords(List<Map<String, String>> records,
			Map<String, String> mergeMapping) {
		// 結果を格納するリスト
		List<Map<String, String>> result = new ArrayList<>();
		
		// マージ元のレコードを一時保存するMap
		// Key: "ItemName_ソースGroupID_ターゲットGroupID", Value: マージ元のレコード
		Map<String, Map<String, String>> mergedRecords = new HashMap<>();

		for (Map<String, String> record : records) {
			String groupId = record.get("GrupID");
			String itemName = record.get("ItemName");

			// GroupIDが-1の場合はヘッダー情報なので、そのまま結果に追加
			if ("-1".equals(groupId)) {
				result.add(record);
				continue;
			}

			// このGroupIDがマージ元の場合
			if (mergeMapping.containsKey(groupId)) {
				// マージ先のGroupIDを取得
				String targetGroupId = mergeMapping.get(groupId);
				
				// 一時保存用のキーを作成（ItemName + "_" + ソースGroupID + "_" + ターゲットGroupID）
				String key = itemName + "_" + groupId + "_" + targetGroupId;
				
				// レコードを一時保存（後でマージ先のレコードと結合するため）
				mergedRecords.put(key, new HashMap<>(record));
				
				logger.debug(messageSource.getMessage("debug.merge.source.record.saved",
						new Object[] { itemName, groupId }, Locale.getDefault()));
				
				// マージ元のレコードは結果に追加せず、次のレコードへ
				continue;
			}

			// このGroupIDがマージ先の場合
			if (mergeMapping.containsValue(groupId)) {
				// 一時保存からマージ元のレコードを取得
				// マージ元のGroupIDを逆引きで取得
				String sourceGroupId = null;
				for (Map.Entry<String, String> entry : mergeMapping.entrySet()) {
					if (entry.getValue().equals(groupId)) {
						sourceGroupId = entry.getKey();
						break;
					}
				}
				
				String key = itemName + "_" + sourceGroupId + "_" + groupId;
				Map<String, String> sourceRecord = mergedRecords.get(key);

				if (sourceRecord != null) {
					// マージ元とマージ先のValueを結合
					String mergedValue = mergeValues(sourceRecord.get("Value"), record.get("Value"));
					
					// マージ先のレコードをコピーし、Valueを結合後の値に置き換え
					Map<String, String> mergedRecord = new HashMap<>(record);
					mergedRecord.put("Value", mergedValue);
					result.add(mergedRecord);

					logger.debug(messageSource.getMessage("debug.record.merged",
							new Object[] { itemName, groupId, sourceRecord.get("Value"), record.get("Value"),
									mergedValue },
							Locale.getDefault()));
				} else {
					// マージ元のレコードが見つからない場合は、そのまま追加
					result.add(record);
				}
			} else {
				// マージ対象でない通常のレコードは、そのまま追加
				result.add(record);
			}
		}

		return result;
	}

	/**
	 * 2つのValueを結合
	 * 
	 * <p>マージ元とマージ先のValueを"\n"（改行）で連結します。</p>
	 * 
	 * <h4>結合ルール</h4>
	 * <ul>
	 *   <li>両方に値がある場合: value1 + "\n" + value2</li>
	 *   <li>value1のみある場合: value1</li>
	 *   <li>value2のみある場合: value2</li>
	 *   <li>両方とも空の場合: ""（空文字列）</li>
	 * </ul>
	 * 
	 * <h4>結合例</h4>
	 * <pre>
	 * value1="その他", value2="沖縄県卸商業" → "その他\n沖縄県卸商業"
	 * value1="", value2="24,340,000" → "24,340,000"
	 * value1="出資金", value2="" → "出資金"
	 * </pre>
	 * 
	 * @param value1 マージ元のValue
	 * @param value2 マージ先のValue
	 * @return 結合後のValue
	 */
	private String mergeValues(String value1, String value2) {
		boolean hasValue1 = StringUtils.isNotBlank(value1);
		boolean hasValue2 = StringUtils.isNotBlank(value2);

		if (hasValue1 && hasValue2) {
			// 両方に値がある場合は改行で連結
			return value1 + "\\n" + value2;
		} else if (hasValue1) {
			// value1のみある場合
			return value1;
		} else if (hasValue2) {
			// value2のみある場合
			return value2;
		} else {
			// 両方とも空の場合
			return "";
		}
	}
}
