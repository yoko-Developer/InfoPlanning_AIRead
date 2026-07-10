package jp.co.ariseinnovation.mergerowsaireadcsv.processor;

import org.springframework.stereotype.Component;

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
 * <h3>マージ処理の概要</h3>
 * <ol>
 *   <li>ItemNameが"期末現在高"を含み、かつValueが空白の行を検出</li>
 *   <li>検出した行（マージ元）と次のGroupIDの行（マージ先）を結合</li>
 *   <li>マージ元のValueとマージ先のValueを"\n"で連結</li>
 * </ol>
 * 
 * <h3>注意事項</h3>
 * <p>OCRソフトウェアのVerUp後、CSV形式が変更されています：</p>
 * <ul>
 *   <li>VerUp前: "期末現在高"行に金額データが格納</li>
 *   <li>VerUp後: "期末現在高"行は空白、"金額"行に金額データが格納</li>
 * </ul>
 * <p>現在のキーワード"期末現在高"では、VerUp後のCSVで誤検出が発生する可能性があります。</p>
 * 
 * @see AbstractMergeProcessor
 * @author Arise Innovation
 */
@Component
public class OldSecuritiesProcessor extends AbstractMergeProcessor {

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
		// return formId.startsWith("02_060");
		return formId.startsWith("XX_XXX");
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
