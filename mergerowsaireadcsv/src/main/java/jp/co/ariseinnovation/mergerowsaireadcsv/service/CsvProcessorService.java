package jp.co.ariseinnovation.mergerowsaireadcsv.service;

import jp.co.ariseinnovation.mergerowsaireadcsv.processor.DataProcessor;
import jp.co.ariseinnovation.mergerowsaireadcsv.transformer.CsvRecordTransformer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * CSV処理のメインサービスクラス
 * 
 * <p>このクラスは、OCRソフトウェアで読み取ったCSVファイルの処理を統括します。
 * 指定されたフォルダ内のCSVファイルを順次処理し、データの加工と出力を行います。</p>
 * 
 * <h3>処理の流れ</h3>
 * <ol>
 *   <li><b>CSVファイルの検索</b>: 指定フォルダ内の.csvファイルを検索</li>
 *   <li><b>ファイルの読み込み</b>: CSVファイルをレコード単位で読み込み</li>
 *   <li><b>form_idの抽出</b>: CSVからform_idを取得し、処理対象かを判定</li>
 *   <li><b>プロセッサーの選択</b>: form_idに対応するプロセッサーを選択</li>
 *   <li><b>データの加工</b>: ItemName置換とマージ処理を実行</li>
 *   <li><b>結果の出力</b>: 加工後のデータを元のCSVファイルに上書き</li>
 * </ol>
 * 
 * <h3>対応するform_id</h3>
 * <ul>
 *   <li>02_060_XX_XX: 有価証券（SecuritiesProcessor）</li>
 *   <li>02_050_XX_XX: 役員給与（ExecutiveCompensationProcessor）</li>
 * </ul>
 * 
 * @see DataProcessor
 * @see CsvRecordTransformer
 * @author Arise Innovation
 */
@Service
public class CsvProcessorService {

	private static final Logger logger = LoggerFactory.getLogger(CsvProcessorService.class);

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private CsvRecordTransformer recordTransformer;

	@Autowired
	private List<DataProcessor> dataProcessors;

	/**
	 * 指定フォルダ内のCSVファイルを処理
	 * 
	 * <p>フォルダ内の全CSVファイルを検索し、順次処理します。
	 * 処理対象外のファイル（form_idが未対応）はスキップされます。</p>
	 * 
	 * <h4>処理対象ファイル</h4>
	 * <ul>
	 *   <li>拡張子が.csv（大文字小文字不問）</li>
	 *   <li>通常ファイル（ディレクトリは除外）</li>
	 *   <li>ファイル名順にソートして処理</li>
	 * </ul>
	 * 
	 * @param folderPath 処理対象のフォルダパス
	 * @throws Exception ファイル読み込みエラーなど
	 */
	public void processCsvFiles(Path folderPath) throws Exception {
		// フォルダ内のファイルをリスト化
		try (var stream = Files.list(folderPath)) {
			// .csvファイルのみを抽出し、ソート
			var csvFiles = stream
					.filter(Files::isRegularFile)  // 通常ファイルのみ
					.filter(p -> p.toString().toLowerCase().endsWith(".csv"))  // .csv拡張子
					.sorted()  // ファイル名順にソート
					.toList();

			// CSVファイルが見つからない場合は警告を出力して終了
			if (csvFiles.isEmpty()) {
				logger.warn(messageSource.getMessage("warn.no.csv.files", new Object[] { folderPath },
						Locale.getDefault()));
				return;
			}

			logger.info(messageSource.getMessage("info.csv.files.found", new Object[] { csvFiles.size() },
					Locale.getDefault()));

			// 処理結果のカウンター
			int processedCount = 0;  // 処理成功したファイル数
			int skippedCount = 0;    // スキップしたファイル数

			// 各CSVファイルを順次処理
			for (Path csvFile : csvFiles) {
				if (processSingleCsvFile(csvFile)) {
					processedCount++;
				} else {
					skippedCount++;
				}
			}

			// 処理結果のサマリーを出力
			logger.info(messageSource.getMessage("info.processing.summary",
					new Object[] { processedCount, skippedCount }, Locale.getDefault()));
		}
	}

	/**
	 * 個別のCSVファイルを処理
	 * 
	 * <p>1つのCSVファイルを読み込み、加工処理を実行します。
	 * エラーが発生した場合はログに記録し、処理を継続します。</p>
	 * 
	 * @param csvFile 処理対象のCSVファイルパス
	 * @return 処理に成功した場合true、スキップまたはエラーの場合false
	 */
	private boolean processSingleCsvFile(Path csvFile) {
		logger.debug(messageSource.getMessage("info.checking.file", new Object[] { csvFile.getFileName() },
				Locale.getDefault()));

		try {
			// CSVファイルの加工処理を実行
			transformCsvFile(csvFile);
			return true;
		} catch (Exception e) {
			// エラーが発生してもログに記録して処理を継続
			logger.error(messageSource.getMessage("error.processing.file",
					new Object[] { csvFile.getFileName(), e.getMessage() }, Locale.getDefault()), e);
			return false;
		}
	}

	/**
	 * CSVファイルを読み込んで加工処理を実行
	 * 
	 * <p>このメソッドは、CSVファイルの加工処理の中核を担います。</p>
	 * 
	 * <h4>処理手順</h4>
	 * <ol>
	 *   <li>CSVファイルを読み込み、レコードリストに変換</li>
	 *   <li>form_idを抽出し、処理対象かを判定</li>
	 *   <li>form_idに対応するプロセッサーを検索</li>
	 *   <li>ItemName置換マップを作成</li>
	 *   <li>レコードにItemName置換を適用</li>
	 *   <li>プロセッサーでマージ処理を実行</li>
	 *   <li>加工後のデータを元のファイルに上書き</li>
	 * </ol>
	 * 
	 * @param csvFile 処理対象のCSVファイルパス
	 * @throws Exception ファイル読み込み・書き込みエラーなど
	 */
	private void transformCsvFile(Path csvFile) throws Exception {
		// ステップ1: CSVファイルを読み込み
		List<CSVRecord> records = new ArrayList<>();

		try (Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
				CSVParser csvParser = new CSVParser(reader,
						CSVFormat.DEFAULT.builder()
								.setHeader()  // 1行目をヘッダーとして扱う
								.setSkipHeaderRecord(true)  // ヘッダー行をスキップ
								.build())) {

			// 全レコードをリストに格納
			for (CSVRecord record : csvParser) {
				records.add(record);
			}
		}

		// ステップ2: form_idを抽出
		Optional<String> formIdOpt = extractFormId(records);
		if (formIdOpt.isEmpty()) {
			// form_idが見つからない場合は警告を出力してスキップ
			logger.warn(messageSource.getMessage("warn.form.id.not.found", new Object[] { csvFile.getFileName() },
					Locale.getDefault()));
			return;
		}

		String formId = formIdOpt.get();

		// ステップ3: form_idに対応するプロセッサーを検索
		DataProcessor processor = findProcessor(formId);
		if (processor == null) {
			// 対応するプロセッサーが見つからない場合はスキップ
			logger.debug(messageSource.getMessage("info.skipping.file",
					new Object[] { csvFile.getFileName(), formId }, Locale.getDefault()));
			return;
		}

		logger.info(messageSource.getMessage("info.processing.file",
				new Object[] { csvFile.getFileName(), formId }, Locale.getDefault()));

		// ステップ4: ItemName置換マップを作成
		// （OCRの誤認識などで分割されたItemNameを統合するため）
		Map<String, String> itemNameReplacements = recordTransformer.buildItemNameReplacements(records);
		logReplacements(csvFile, itemNameReplacements);

		// ステップ5: ヘッダー情報を取得
		List<String> headers = records.isEmpty() ? List.of()
				: new ArrayList<>(records.get(0).getParser().getHeaderNames());
		
		// ステップ6: ItemName置換を適用してレコードをMap形式に変換
		List<Map<String, String>> transformedRecords = recordTransformer.applyTransformations(records,
				itemNameReplacements);

		// ステップ7: プロセッサーでマージ処理を実行
		transformedRecords = processor.process(transformedRecords);

		// 処理前後のレコード数をログ出力
		logger.info(messageSource.getMessage("info.file.record.count",
				new Object[] { csvFile.getFileName(), records.size(), transformedRecords.size() },
				Locale.getDefault()));

		// ステップ8: 加工後のデータをCSVファイルに出力（元のファイルを上書き）
		writeTransformedCsv(csvFile, headers, transformedRecords);
	}

	/**
	 * CSVファイルからform_idを抽出
	 * 
	 * <p>CSVレコードの中から、ItemNameが"form_id"の行を検索し、
	 * そのValueを取得します。</p>
	 * 
	 * <h4>form_idレコードの例</h4>
	 * <pre>
	 * ItemName="form_id", Value="02_060_75_01"
	 * </pre>
	 * 
	 * @param records CSVレコードのリスト
	 * @return form_idが見つかった場合はOptional.of(formId)、見つからない場合はOptional.empty()
	 */
	private Optional<String> extractFormId(List<CSVRecord> records) {
		for (CSVRecord record : records) {
			String itemName = record.get("ItemName");
			
			// ItemNameが"form_id"の行を検索
			if ("form_id".equals(itemName)) {
				String formId = record.get("Value");
				
				// Valueが空でない場合、form_idとして返す
				if (formId != null && !formId.trim().isEmpty()) {
					return Optional.of(formId.trim());
				}
			}
		}
		
		// form_idが見つからない場合
		return Optional.empty();
	}

	/**
	 * form_idに対応するプロセッサーを検索
	 * 
	 * <p>登録されているプロセッサーの中から、指定されたform_idを処理できる
	 * プロセッサーを検索します。</p>
	 * 
	 * <h4>プロセッサーの選択例</h4>
	 * <ul>
	 *   <li>form_id="02_060_75_01" → SecuritiesProcessor</li>
	 *   <li>form_id="02_050_XX_XX" → ExecutiveCompensationProcessor</li>
	 *   <li>form_id="99_999_XX_XX" → null（未対応）</li>
	 * </ul>
	 * 
	 * @param formId 検索対象のform_id
	 * @return 対応するプロセッサー、見つからない場合はnull
	 */
	private DataProcessor findProcessor(String formId) {
		for (DataProcessor processor : dataProcessors) {
			// 各プロセッサーに処理対象かを問い合わせ
			if (processor.isTargetFormId(formId)) {
				return processor;
			}
		}
		
		// 対応するプロセッサーが見つからない
		return null;
	}

	/**
	 * 置き換えマップの内容をログ出力
	 * 
	 * <p>ItemName置換マップの内容をログに出力します。
	 * デバッグ時に、どのItemNameがどのように置換されたかを確認できます。</p>
	 * 
	 * @param csvFile 処理対象のCSVファイル（ログ出力用）
	 * @param replacements ItemName置換マップ（Key: 置換前, Value: 置換後）
	 */
	private void logReplacements(Path csvFile, Map<String, String> replacements) {
		if (replacements.isEmpty()) {
			// 置換対象がない場合
			logger.info(messageSource.getMessage("info.no.item.name.replacements",
					new Object[] { csvFile.getFileName() }, Locale.getDefault()));
		} else {
			// 置換対象がある場合、件数と詳細を出力
			logger.info(messageSource.getMessage("info.item.name.replacements.found",
					new Object[] { csvFile.getFileName(), replacements.size() }, Locale.getDefault()));
			
			// 各置換の詳細をデバッグログに出力
			replacements.forEach((oldName, newName) -> {
				logger.debug(messageSource.getMessage("debug.replacement.detail",
						new Object[] { oldName, newName }, Locale.getDefault()));
			});
		}
	}

	/**
	 * 加工したレコードをCSVファイルに出力
	 * 
	 * <p>加工後のレコードを元のCSVファイルに上書き保存します。
	 * 全フィールドをダブルクォートで囲んで出力します。</p>
	 * 
	 * <h4>出力形式</h4>
	 * <ul>
	 *   <li>文字コード: UTF-8</li>
	 *   <li>クォートモード: ALL（全フィールドをダブルクォートで囲む）</li>
	 *   <li>ヘッダー: 元のCSVファイルのヘッダーを使用</li>
	 * </ul>
	 * 
	 * @param originalFile 出力先のCSVファイルパス（元のファイルを上書き）
	 * @param headers CSVのヘッダー行（列名のリスト）
	 * @param records 出力するレコードのリスト（各レコードはMapで表現）
	 * @throws Exception ファイル書き込みエラーなど
	 */
	private void writeTransformedCsv(Path originalFile, List<String> headers, List<Map<String, String>> records)
			throws Exception {
		try (BufferedWriter writer = Files.newBufferedWriter(originalFile, StandardCharsets.UTF_8);
				CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
						.setHeader(headers.toArray(new String[0]))  // ヘッダー行を設定
						.setQuoteMode(org.apache.commons.csv.QuoteMode.ALL)  // 全フィールドをクォート
						.build())) {

			// 各レコードを出力
			for (Map<String, String> record : records) {
				List<String> values = new ArrayList<>();
				
				// ヘッダーの順序に従って値を取得
				for (String header : headers) {
					values.add(record.get(header));
				}
				
				// 1行分のレコードを出力
				csvPrinter.printRecord(values);
			}
		}

		logger.info(messageSource.getMessage("info.output.file",
				new Object[] { originalFile.getFileName() }, Locale.getDefault()));
	}
}
