package jp.co.ariseinnovation.mergerowsaireadcsv.processor;

import java.util.List;
import java.util.Map;

/**
 * データ処理のインターフェース
 * 
 * <p>このインターフェースは、OCRで読み取ったCSVデータの処理を定義します。
 * 各帳票タイプ（有価証券、役員給与など）に対応するプロセッサーは、
 * このインターフェースを実装します。</p>
 * 
 * <h3>実装クラス</h3>
 * <ul>
 *   <li>{@link SecuritiesProcessor} - 有価証券（02_060）の処理</li>
 *   <li>{@link ExecutiveCompensationProcessor} - 役員給与（02_050）の処理</li>
 * </ul>
 * 
 * <h3>処理の流れ</h3>
 * <ol>
 *   <li>{@link #isTargetFormId(String)} でform_idが処理対象かチェック</li>
 *   <li>処理対象の場合、{@link #process(List)} でデータを加工</li>
 *   <li>加工後のデータをCSVファイルに出力</li>
 * </ol>
 * 
 * @author Arise Innovation
 */
public interface DataProcessor {

	/**
	 * 指定されたform_idが処理対象かどうかをチェック
	 * 
	 * <p>CSVファイルに含まれるform_idを確認し、このプロセッサーで処理すべきかを判定します。</p>
	 * 
	 * <h4>form_idの例</h4>
	 * <ul>
	 *   <li>02_060_75_01: 有価証券明細形式</li>
	 *   <li>02_060_02_01: 有価証券一覧形式</li>
	 *   <li>02_050_XX_XX: 役員給与</li>
	 * </ul>
	 * 
	 * @param formId 判定対象のform_id（CSVファイルから抽出）
	 * @return 処理対象の場合true、それ以外はfalse
	 */
	boolean isTargetFormId(String formId);

	/**
	 * データの個別処理を実行
	 * 
	 * <p>CSVファイルから読み込んだレコードを加工します。
	 * 各レコードはMapで表現され、キーは列名（ItemName, Value, GrupIDなど）、
	 * 値はその列の値です。</p>
	 * 
	 * <h4>レコードの構造例</h4>
	 * <pre>
	 * {
	 *   "ItemName": "期末現在高",
	 *   "Page": "0",
	 *   "GID": "detail",
	 *   "GrupID": "3",
	 *   "Value": "24,340,000",
	 *   "Conf": "100",
	 *   ...
	 * }
	 * </pre>
	 * 
	 * @param records 処理対象のレコードリスト（CSVの各行）
	 * @return 加工後のレコードリスト
	 */
	List<Map<String, String>> process(List<Map<String, String>> records);

	/**
	 * 処理タイプの名前を取得
	 * 
	 * <p>ログ出力やデバッグ時の識別に使用されます。</p>
	 * 
	 * @return プロセッサー名（例: "Securities", "ExecutiveCompensation"）
	 */
	String getProcessorName();
}
