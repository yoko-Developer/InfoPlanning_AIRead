package jp.co.ariseinnovation.dateconverter.service;

import jp.co.ariseinnovation.dateconverter.enums.OutputFormat;
import jp.co.ariseinnovation.dateconverter.util.DateConstants;
import jp.co.ariseinnovation.dateconverter.util.DateParser;
import jp.co.ariseinnovation.dateconverter.util.DatePrecisionUtil;
import jp.co.ariseinnovation.dateconverter.util.GengoYearTable;
import jp.co.ariseinnovation.dateconverter.util.MessageUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * CSVファイルの和暦を西暦YYYYMM形式またはYYYYMMDD形式に変換するサービス
 * 
 * このサービスは以下の処理を行います：
 * 1. 入力CSVファイルを読み込み
 * 2. 指定された対象列の5列目（0ベースで4列目）の値を検査
 * 3. 和暦形式（H10.5、R5.4.30等）を西暦YYYYMM形式（199805等）またはYYYYMMDD形式（20230430等）に変換
 * 4. 変換結果を出力CSVファイルに書き込み
 * 
 * 対応する和暦形式：
 * - GengoYearTableに定義された全ての元号 + 様々な区切り文字での年月日表記
 *   元号: S、H、R、L、昭和、平成、令和、昭、平、令
 *   区切り文字: . - / , 年月日（例：R5.4.30、H10.5、昭和63-12、令和3/4、H10,5、平成10年5月15日）
 *   区切り文字なし: H100530（10年05月30日）、H1005（10年05月）、H105（10年5月）、H15（1年5月）
 *   スペース: 全角・半角スペースは前処理で削除される
 * - その他の日付形式はDateParserで処理
 */
@Service
public class CsvDateConverterService {

    /** ログ出力用 */
    private static final Logger logger = LoggerFactory.getLogger(CsvDateConverterService.class);

    /**
     * CSVファイルの和暦を西暦に変換（デフォルト出力形式：YYYYMMDD）
     * 
     * @param inputFile 入力ファイルパス
     * @param outputFile 出力ファイルパス
     * @param targetColumns 変換対象列名のリスト（1列目の項目名に前方一致する文字列）
     * @param charset 文字コード（UTF-8、Shift_JIS等）
     * @return 変換された行数
     * @throws Exception ファイル読み書きエラー、文字コードエラー等
     */
    public int convertCsvFile(String inputFile, String outputFile, List<String> targetColumns, String charset) throws Exception {
        return convertCsvFile(inputFile, outputFile, targetColumns, charset, OutputFormat.YYYYMMDD);
    }

    /**
     * CSVファイルの和暦を西暦に変換（追加区切り文字なし）
     *
     * @param inputFile 入力ファイルパス
     * @param outputFile 出力ファイルパス
     * @param targetColumns 変換対象列名のリスト（1列目の項目名に前方一致する文字列）
     * @param charset 文字コード（UTF-8、Shift_JIS等）
     * @param outputFormat 出力形式（YYYYMM または YYYYMMDD）
     * @return 変換された行数
     * @throws Exception ファイル読み書きエラー、文字コードエラー等
     */
    public int convertCsvFile(String inputFile, String outputFile, List<String> targetColumns, String charset, OutputFormat outputFormat) throws Exception {
        return convertCsvFile(inputFile, outputFile, targetColumns, charset, outputFormat, "");
    }

    /**
     * CSVファイルの和暦を西暦に変換
     *
     * 処理の流れ：
     * 1. 入力ファイルを指定された文字コードで読み込み
     * 2. 各行を順次処理し、対象列の5列目の値を変換
     * 3. 変換結果を出力ファイルに書き込み
     * 4. 進捗を定期的にログ出力
     *
     * @param inputFile 入力ファイルパス
     * @param outputFile 出力ファイルパス
     * @param targetColumns 変換対象列名のリスト（1列目の項目名に前方一致する文字列）
     * @param charset 文字コード（UTF-8、Shift_JIS等）
     * @param outputFormat 出力形式（YYYYMM または YYYYMMDD）
     * @param extraSeparators 追加の区切り文字（この中に含まれる各文字を「.」として扱う。null・空文字は追加なし）
     * @return 変換された行数
     * @throws Exception ファイル読み書きエラー、文字コードエラー等
     */
    public int convertCsvFile(String inputFile, String outputFile, List<String> targetColumns, String charset,
            OutputFormat outputFormat, String extraSeparators) throws Exception {
        logger.info(MessageUtil.getMessage("csv.start"));
        logger.debug(MessageUtil.getMessage("config.details", inputFile, outputFile, targetColumns, charset, outputFormat.getFormat()));
        logger.debug("Extra separators: {}", extraSeparators);

        // 文字コードオブジェクトを作成
        Charset cs = Charset.forName(charset);
        int convertedCount = 0; // 変換された行数をカウント

        // try-with-resources文でファイルリソースを自動管理
        try (Reader reader = new InputStreamReader(new FileInputStream(inputFile), cs);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
             Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), cs);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setQuoteMode(org.apache.commons.csv.QuoteMode.ALL).build())) {

            int lineCount = 0; // 処理行数をカウント
            
            // CSVの各行を順次処理
            for (CSVRecord record : parser) {
                lineCount++;
                
                // 変換後のレコードを格納する配列
                String[] convertedRecord = new String[record.size()];
                boolean lineConverted = false; // この行で変換が行われたかのフラグ
                
                // 1列目の項目名を取得（対象列判定に使用）
                String itemName = record.size() > 0 ? record.get(0) : null;
                boolean isTargetItem = isTargetColumn(itemName, targetColumns);
                
                // 各列を順次処理
                for (int i = 0; i < record.size(); i++) {
                    String value = record.get(i);
                    
                    // 5列目（0ベースで4）かつ対象項目の場合のみ変換処理を実行
                    if (i == 4 && isTargetItem && value != null && !value.trim().isEmpty()) {
                        String trimmedValue = value.trim();
                        // 全角スペースを半角スペースに変換してから半角スペースを削除
                        String normalizedValue = normalizeSpaces(trimmedValue);
                        // 追加区切り文字を標準の区切り文字「.」に変換（変換判定用の一時的な値）
                        String separatorNormalizedValue = normalizeExtraSeparators(normalizedValue, extraSeparators);
                        String convertedValue = convertToYearMonth(separatorNormalizedValue, outputFormat);

                        if (!separatorNormalizedValue.equals(convertedValue)) {
                            // 変換に成功した場合のみ変換後の値を採用
                            logger.debug(MessageUtil.getMessage("date.convert.success", itemName, normalizedValue, convertedValue));
                            lineConverted = true;
                            convertedRecord[i] = convertedValue;
                        } else {
                            // 変換されなかった場合は、正規化処理を一切通していない元の値をそのまま維持する
                            // （スペース除去・追加区切り文字変換は「変換に成功した場合の入力整形」であり、
                            // 変換できなかった非日付データにまで適用して値を書き換えてはならないため）
                            convertedRecord[i] = value;
                        }
                    } else {
                        // 変換対象外の列はそのまま設定
                        convertedRecord[i] = value;
                    }
                }
                
                // 変換が行われた行数をカウント
                if (lineConverted) {
                    convertedCount++;
                }
                
                // 変換後のレコードを出力ファイルに書き込み
                printer.printRecord((Object[]) convertedRecord);
                
                // 定期的に進捗をログ出力（デフォルト1000行ごと）
                if (lineCount % DateConstants.PROGRESS_INTERVAL == 0) {
                    logger.info(MessageUtil.getMessage("csv.progress", lineCount));
                }
            }
            
            // 処理完了ログを出力
            logger.info(MessageUtil.getMessage("csv.complete", lineCount, convertedCount));
            
        } catch (Exception e) {
            // エラー発生時はログ出力して例外を再スロー
            logger.error(MessageUtil.getMessage("csv.error", e.getMessage()), e);
            throw e;
        }
        
        return convertedCount;
    }
    
    /** 全角スペース文字 */
    private static final char FULL_WIDTH_SPACE = '　';
    /** 半角スペース文字 */
    private static final char HALF_WIDTH_SPACE = ' ';
    
    /**
     * 文字列中のスペースを正規化（最適化版）
     * 
     * 処理内容：
     * 1. 全角スペース（　）を半角スペース（ ）に変換
     * 2. 全ての半角スペースを削除
     * 
     * @param src 正規化対象の文字列
     * @return スペースが正規化された文字列
     */
    private String normalizeSpaces(String src) {
        if (src == null) {
            return null;
        }
        
        // 効率化：StringBuilderを使用して一度のループで処理
        StringBuilder result = new StringBuilder(src.length());
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            // 全角・半角スペース以外の文字のみを追加
            if (c != FULL_WIDTH_SPACE && c != HALF_WIDTH_SPACE) {
                result.append(c);
            }
        }
        return result.toString();
    }

    /** 追加区切り文字の変換先（標準の区切り文字） */
    private static final char STANDARD_SEPARATOR = '.';

    /**
     * 追加の区切り文字を標準の区切り文字「.」に変換する
     *
     * 「.」は元号の年月日パターン（GengoYearTableマッチング）・DateParserの和暦パターンの
     * 両方で既にサポートされている区切り文字であるため、追加区切り文字をここで「.」に
     * 正規化するだけで、既存の正規表現定義（YEAR_MONTH_DAY_SEPARATORS等）を変更せずに
     * 新しい区切り文字（全角スラッシュ「／」、チルダ「~」等）に対応できる。
     *
     * @param src 正規化対象の文字列
     * @param extraSeparators 追加の区切り文字として扱う文字の集合（1文字ずつ判定。null・空文字は変換なし）
     * @return 追加区切り文字が標準の区切り文字に変換された文字列
     */
    private String normalizeExtraSeparators(String src, String extraSeparators) {
        if (src == null || StringUtils.isEmpty(extraSeparators)) {
            return src;
        }

        StringBuilder result = new StringBuilder(src.length());
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            result.append(extraSeparators.indexOf(c) >= 0 ? STANDARD_SEPARATOR : c);
        }
        return result.toString();
    }

    /**
     * 指定された項目名が変換対象列かどうかを判定
     *
     * 判定方法：
     * - 項目名がnullの場合はfalse
     * - 比較前に項目名から全角・半角スペースを除去する
     * - 対象列リストのいずれかの文字列が、スペース除去後の項目名の前方一致（プレフィックス）である場合true
     *   （例：対象列"使用" は項目名"使用開始日"に前方一致する）
     *
     * @param itemName 判定対象の項目名（CSVの1列目の値）
     * @param targetColumns 変換対象列名のリスト
     * @return 対象列の場合true、それ以外はfalse
     */
    private boolean isTargetColumn(String itemName, List<String> targetColumns) {
        if (itemName == null) {
            return false;
        }
        String normalizedItemName = normalizeSpaces(itemName);
        // 対象列名のいずれかが項目名に前方一致するかチェック
        return targetColumns.stream().anyMatch(normalizedItemName::startsWith);
    }

    /**
     * 和暦を西暦YYYYMM形式またはYYYYMMDD形式に変換（デフォルト出力形式：YYYYMMDD）
     * 
     * @param src 変換対象文字列（既にスペースが正規化済み）
     * @return 変換後文字列（変換できない場合は元の文字列をそのまま返す）
     */
    @SuppressWarnings("unused")
    private String convertToYearMonth(String src) {
        return convertToYearMonth(src, OutputFormat.YYYYMMDD);
    }

    /**
     * 和暦を西暦YYYYMM形式またはYYYYMMDD形式に変換
     * 
     * 変換パターン：
     * 1. GengoYearTableに定義された和暦形式（様々な区切り文字に対応）
     *    - 元号: S、H、R、L、昭和、平成、令和、昭、平、令
     *    - 区切り文字: . - / , 年月日
     *    - 年月日: R5.4.30、平成10年5月15日 → YYYYMMDD形式
     *    - 年月: H10.5、平成10年5月 → YYYYMM形式
     *    - 区切り文字なし: 6桁（YYMMDD）、4桁（YYMM）、3桁（YYMまたはYMM判定）、2桁（YM）
     *    - 3桁判定: N2N3≥13→YYM、元号+N1N2が未来→YMM、その他→YYM
     *    - 例: R5.4.30、H10.5、昭和63-12、令和3/4、H10,5、平成10年5月15日、H100530、H1005、H105、H15等
     *    - スペース: 前処理で全角・半角スペースは削除される
     * 2. その他の形式はDateParserに委譲
     * 
     * 入力データの精度による出力制御：
     * - 入力データの精度が年月日の場合：指定された出力形式（YYYYMM または YYYYMMDD）を使用
     * - 入力データの精度が年月の場合：YYYYMMDDが指定されていても YYYYMM 形式で出力
     * 
     * @param src 変換対象文字列（既にスペースが正規化済み）
     * @param outputFormat 要求された出力形式
     * @return 変換後文字列（変換できない場合は元の文字列をそのまま返す）
     */
    private String convertToYearMonth(String src, OutputFormat outputFormat) {
        // null または空文字列の場合はそのまま返す
        if (StringUtils.isEmpty(src)) {
            return src;
        }

        // GengoYearTableに定義された元号での変換を試行
        GengoYearTable matchedGengo = findMatchingGengo(src);
        if (matchedGengo != null) {
            return convertWarekiToYearMonth(src, matchedGengo, outputFormat);
        } else {
            // GengoYearTableでマッチしない場合はDateParserで処理を試行
            try {
                DateTime parsedDate = DateParser.Parse(src);
                
                // 入力データの精度と要求された出力形式に基づいて実際の出力形式を決定
                OutputFormat actualFormat = DatePrecisionUtil.determineOutputFormat(src, null, outputFormat);
                
                if (actualFormat == OutputFormat.YYYYMM) {
                    return parsedDate.toString("yyyyMM");
                } else {
                    return parsedDate.toString("yyyyMMdd");
                }
            } catch (Exception e) {
                // DateParserでも変換できない場合はデバッグログを出力して元の値を返す
                logger.debug(MessageUtil.getMessage("date.convert.parse.failed", src, e.getMessage()));
                return src;
            }
        }
    }

    /** 
     * 和暦の年月日区切り文字として使用される可能性のある文字
     * ドット、ハイフン、スラッシュ、カンマ、年月日文字を含む（スペースは除外）
     */
    private static final String YEAR_MONTH_DAY_SEPARATORS = "[.\\-/,年月日]";
    
    /** 
     * コンパイル済み正規表現パターン（パフォーマンス向上のため事前コンパイル）
     */
    private static final java.util.regex.Pattern PATTERN_3_DIGITS = java.util.regex.Pattern.compile("^\\d{3}$");
    private static final java.util.regex.Pattern PATTERN_YEAR_MONTH_DAY_KANJI = java.util.regex.Pattern.compile("(\\d{1,2})年(\\d{1,2})月(\\d{1,2})日?");
    private static final java.util.regex.Pattern PATTERN_YEAR_MONTH_KANJI = java.util.regex.Pattern.compile("(\\d{1,2})年(\\d{1,2})月?");
    private static final java.util.regex.Pattern PATTERN_YEAR_MONTH_DAY_SEPARATOR = java.util.regex.Pattern.compile("(\\d{1,2})" + YEAR_MONTH_DAY_SEPARATORS + "+(\\d{1,2})" + YEAR_MONTH_DAY_SEPARATORS + "+(\\d{1,2})");
    private static final java.util.regex.Pattern PATTERN_YEAR_MONTH_SEPARATOR = java.util.regex.Pattern.compile("(\\d{1,2})" + YEAR_MONTH_DAY_SEPARATORS + "+(\\d{1,2})");
    private static final java.util.regex.Pattern PATTERN_YEAR_MONTH_DAY_SEPARATOR_EXTENDED = java.util.regex.Pattern.compile("(\\d{1,2})" + YEAR_MONTH_DAY_SEPARATORS + "+(\\d{3,4})");
    private static final java.util.regex.Pattern PATTERN_YEAR_MONTH_EXTENDED_SEPARATOR = java.util.regex.Pattern.compile("(\\d{3,4})" + YEAR_MONTH_DAY_SEPARATORS + "+(\\d{1,2})");
    private static final java.util.regex.Pattern PATTERN_6_DIGITS = java.util.regex.Pattern.compile("(\\d{2})(\\d{2})(\\d{2})");
    private static final java.util.regex.Pattern PATTERN_5_DIGITS = java.util.regex.Pattern.compile("(\\d{2})(\\d{3})");
    private static final java.util.regex.Pattern PATTERN_4_DIGITS = java.util.regex.Pattern.compile("(\\d{2})(\\d{2})");
    private static final java.util.regex.Pattern PATTERN_2_DIGITS = java.util.regex.Pattern.compile("(\\d{1})(\\d{1})");

    /**
     * 入力文字列にマッチする元号をGengoYearTableから検索（最適化版）
     * 
     * 検索パターン：
     * - 元号 + 数字 + 区切り文字 + 数字 + 区切り文字 + 数字 の形式（年月日）
     * - 元号 + 数字 + 区切り文字 + 数字 の形式（年月）
     * - 対応する区切り文字: . - / , 年 月 日
     * - 例：R5.4.30、H10.5、昭和63-12、令和3/4、H10,5、平成10年5月15日等
     * - GengoYearTable.tableは既に長い順にソート済みのため、ソート処理を削減
     * - 注意: スペースは前処理で削除されるため、ここでは考慮しない
     * 
     * @param src 検索対象の文字列
     * @return マッチした元号情報と区切り文字情報、見つからない場合はnull
     */
    private GengoYearTable findMatchingGengo(String src) {
        // 効率化：GengoYearTable.tableは既に長い順にソート済みのため、
        // ソート処理を削除し、直接検索を実行
        return GengoYearTable.table.stream()
            .filter(gengo -> isMatchingGengoPattern(src, gengo.gengo))
            .findFirst()
            .orElse(null);
    }

    /**
     * 指定された元号パターンが文字列にマッチするかチェック
     * 
     * @param src チェック対象の文字列
     * @param gengoStr 元号文字列
     * @return マッチする場合true
     */
    private boolean isMatchingGengoPattern(String src, String gengoStr) {
        // 元号文字列をエスケープして動的パターンを構築
        String quotedGengo = java.util.regex.Pattern.quote(gengoStr);
        
        // 複数の区切り文字パターンを試行（効率化のため配列で管理）
        String[] patternStrings = {
            // 年月日区切り文字パターン（例：R5.4.30、平成10年5月15日）
            "^" + quotedGengo + "(\\d{1,2})" + YEAR_MONTH_DAY_SEPARATORS + "+(\\d{1,2})" + YEAR_MONTH_DAY_SEPARATORS + "+(\\d{1,2})$",
            // 年月日漢字パターン（例：平成10年5月15日）
            "^" + quotedGengo + "(\\d{1,2})年(\\d{1,2})月(\\d{1,2})日?$",
            // 年月（3-4桁）日拡張区切り文字パターン（例：H105.30、H1005.30）
            "^" + quotedGengo + "(\\d{3,4})" + YEAR_MONTH_DAY_SEPARATORS + "+(\\d{1,2})$",
            // 年月日拡張区切り文字パターン（例：H10.530、H10.0530）
            "^" + quotedGengo + "(\\d{1,2})" + YEAR_MONTH_DAY_SEPARATORS + "+(\\d{3,4})$",
            // 基本的な年月区切り文字パターン（スペースは除外）
            "^" + quotedGengo + "(\\d{1,2})" + YEAR_MONTH_DAY_SEPARATORS + "+(\\d{1,2})$",
            // 年月文字を含むパターン（例：平成10年5月）
            "^" + quotedGengo + "(\\d{1,2})年(\\d{1,2})月?$",
            // 区切り文字なし6桁パターン（例：H100530 → H10年05月30日として解釈）
            "^" + quotedGengo + "(\\d{2})(\\d{2})(\\d{2})$",
            // 区切り文字なし5桁パターン（例：H10530 → H10年530として解釈）
            "^" + quotedGengo + "(\\d{2})(\\d{3})$",
            // 区切り文字なし4桁パターン（例：H1005 → H10年05月として解釈）
            "^" + quotedGengo + "(\\d{2})(\\d{2})$",
            // 区切り文字なし3桁パターン（例：H105 → 特別判定ロジックで処理）
            "^" + quotedGengo + "(\\d{3})$",
            // 区切り文字なし2桁パターン（例：H15 → H1年5月として解釈）
            "^" + quotedGengo + "(\\d{1})(\\d{1})$"
        };
        
        // 効率化：一度のループでパターンマッチングを実行
        for (String patternString : patternStrings) {
            if (src.matches(patternString)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 和暦文字列を西暦YYYYMM形式またはYYYYMMDD形式に変換（デフォルト出力形式：YYYYMMDD）
     * 
     * @param src 変換対象の和暦文字列
     * @param gengo マッチした元号情報
     * @return 変換後の西暦YYYYMM形式またはYYYYMMDD形式文字列
     */
    @SuppressWarnings("unused")
    private String convertWarekiToYearMonth(String src, GengoYearTable gengo) {
        return convertWarekiToYearMonth(src, gengo, OutputFormat.YYYYMMDD);
    }

    /**
     * 和暦文字列を西暦YYYYMM形式またはYYYYMMDD形式に変換
     * 
     * 様々な区切り文字に対応：
     * - 年月日: R5.4.30、平成10年5月15日 → YYYYMMDD形式
     * - 年月: H10.5、平成10年5月 → YYYYMM形式
     * - ドット: H10.5
     * - ハイフン: H10-5
     * - スラッシュ: H10/5
     * - カンマ: H10,5
     * - 年月日文字: 平成10年5月15日
     * - 区切りなし6桁: H100530（10年05月30日として解釈）
     * - 区切りなし4桁: H1005（10年05月として解釈）
     * - 区切りなし3桁: H105（特別判定ロジックでYYMまたはYMMを判定）
     * - 区切りなし2桁: H15（1年5月として解釈）
     * - スペース: 前処理で削除されるため、H10 5 → H105として処理
     * 
     * 入力データの精度による出力制御：
     * - 入力データの精度が年月日の場合：指定された出力形式を使用
     * - 入力データの精度が年月の場合：YYYYMMDDが指定されていても YYYYMM 形式で出力
     * 
     * @param src 変換対象の和暦文字列
     * @param gengo マッチした元号情報
     * @param outputFormat 要求された出力形式
     * @return 変換後の西暦YYYYMM形式またはYYYYMMDD形式文字列
     */
    private String convertWarekiToYearMonth(String src, GengoYearTable gengo, OutputFormat outputFormat) {
        try {
            // 元号部分を除去して年月日部分を抽出
            String yearMonthDayPart = src.substring(gengo.gengo.length());
            
            // 年月日を抽出するための複数パターンを試行
            int[] yearMonthDay = extractYearMonthDay(yearMonthDayPart, gengo);
            if (yearMonthDay == null) {
                logger.warn(MessageUtil.getMessage("date.convert.unsupported.era", gengo.gengo, src));
                return src;
            }
            
            int year = yearMonthDay[0];
            int month = yearMonthDay[1];
            int day = yearMonthDay.length > 2 ? yearMonthDay[2] : -1; // 日が指定されていない場合は-1

            // 西暦年を計算（和暦年 + 基準年）
            int seirekiYear = year + gengo.yearAdd;

            // 日が指定されている場合、実在するカレンダー日付かを検証する
            // （1〜31の範囲チェックだけでは2月30日等の実在しない日付を検出できないため）
            if (day > 0 && !java.time.YearMonth.of(seirekiYear, month).isValidDay(day)) {
                logger.warn(MessageUtil.getMessage("date.convert.invalid.date", src, seirekiYear, month, day));
                return src;
            }

            // 入力データの精度と要求された出力形式に基づいて実際の出力形式を決定
            OutputFormat actualFormat = DatePrecisionUtil.determineOutputFormat(src, gengo, outputFormat);
            
            if (actualFormat == OutputFormat.YYYYMM) {
                // YYYYMM形式で出力
                return String.format("%d%02d", seirekiYear, month);
            } else {
                // YYYYMMDD形式で出力（日が指定されていない場合は01を使用）
                int outputDay = (day > 0) ? day : 1;
                return String.format("%d%02d%02d", seirekiYear, month, outputDay);
            }
            
        } catch (Exception e) {
            // 数値変換エラー等が発生した場合は警告ログを出力して元の文字列を返す
            logger.warn(MessageUtil.getMessage("date.convert.unsupported.era", gengo.gengo, src));
            return src;
        }
    }

    /**
     * 年月日部分から年、月、日の数値を抽出
     * 
     * @param yearMonthDayPart 年月日部分の文字列
     * @param gengo 元号情報（3桁数字の判定に使用）
     * @return [年, 月]または[年, 月, 日]の配列、抽出できない場合はnull
     */
    private int[] extractYearMonthDay(String yearMonthDayPart, GengoYearTable gengo) {
        // 3桁数字の特別処理（事前コンパイル済みパターンを使用）
        if (PATTERN_3_DIGITS.matcher(yearMonthDayPart).matches()) {
            return extract3DigitYearMonth(yearMonthDayPart, gengo);
        }
        
        // 事前コンパイル済みパターンを順次試行（パフォーマンス向上）
        java.util.regex.Pattern[] patterns = {
            PATTERN_YEAR_MONTH_DAY_KANJI,           // 年月日文字を含むパターン（例：10年5月15日）
            PATTERN_YEAR_MONTH_DAY_SEPARATOR,       // 年月日区切り文字パターン（例：10.5.15、10-5-15、10/5/15、10,5,15）
            PATTERN_YEAR_MONTH_KANJI,               // 年月文字を含むパターン（例：10年5月）
            PATTERN_YEAR_MONTH_EXTENDED_SEPARATOR,  // 年月（3-4桁）日拡張区切り文字パターン（例：105.30、1005.30）
            PATTERN_YEAR_MONTH_DAY_SEPARATOR_EXTENDED, // 年月日拡張区切り文字パターン（例：10.530、10.0530）
            PATTERN_YEAR_MONTH_SEPARATOR,           // 年月区切り文字パターン（例：10.5、10-5、10/5、10,5）
            PATTERN_6_DIGITS,                       // 区切り文字なし6桁パターン（例：100515 → 10年05月15日）
            PATTERN_5_DIGITS,                       // 区切り文字なし5桁パターン（例：10530 → 10年530）
            PATTERN_4_DIGITS,                       // 区切り文字なし4桁パターン（例：1005 → 10年05月）
            PATTERN_2_DIGITS                        // 区切り文字なし2桁パターン（例：15 → 1年5月）
        };
        
        for (java.util.regex.Pattern pattern : patterns) {
            java.util.regex.Matcher matcher = pattern.matcher(yearMonthDayPart);
            if (matcher.matches()) {
                try {
                    int year = Integer.parseInt(matcher.group(1));
                    int month = Integer.parseInt(matcher.group(2));
                    
                    // 拡張パターン（3桁・4桁の年月部分または月日部分）の処理
                    if (pattern == PATTERN_YEAR_MONTH_EXTENDED_SEPARATOR) {
                        // 3-4桁年月 + 区切り文字 + 1-2桁日パターン（例：105.30、1005.30）
                        String yearMonthStr = matcher.group(1);
                        int day = Integer.parseInt(matcher.group(2));
                        return extractYearMonthFromExtended(yearMonthStr, day, gengo);
                    } else if (pattern == PATTERN_YEAR_MONTH_DAY_SEPARATOR_EXTENDED || pattern == PATTERN_5_DIGITS) {
                        // 1-2桁年 + 区切り文字 + 3-4桁月日パターン（例：10.530、10.0530）
                        String monthDayStr = matcher.group(2);
                        return extractMonthDayFromExtended(year, monthDayStr);
                    }
                    
                    // 月の妥当性チェック（1-12の範囲）
                    if (month >= 1 && month <= 12) {
                        // 日が存在する場合（3つ目のグループがある場合）
                        if (matcher.groupCount() >= 3 && matcher.group(3) != null) {
                            int day = Integer.parseInt(matcher.group(3));
                            // 日の妥当性チェック（1-31の範囲）
                            if (day >= 1 && day <= 31) {
                                return new int[]{year, month, day};
                            }
                        } else {
                            // 年月のみの場合
                            return new int[]{year, month};
                        }
                    }
                } catch (NumberFormatException e) {
                    // 数値変換エラーの場合は次のパターンを試行
                    continue;
                }
            }
        }
        
        return null; // どのパターンにもマッチしない場合
    }

    /**
     * 拡張パターン（3桁・4桁）から月日を抽出
     * 
     * 3桁の場合: MMD または MDD
     * 4桁の場合: MMDD
     * 
     * @param year 年
     * @param monthDayStr 月日部分の文字列（3桁または4桁）
     * @return [年, 月, 日]の配列、抽出できない場合はnull
     */
    private int[] extractMonthDayFromExtended(int year, String monthDayStr) {
        if (monthDayStr.length() == 3) {
            // 3桁の場合: MMD または MDD を判定
            int firstDigit = Integer.parseInt(monthDayStr.substring(0, 1));
            int secondDigit = Integer.parseInt(monthDayStr.substring(1, 2));
            int thirdDigit = Integer.parseInt(monthDayStr.substring(2, 3));
            
            // MM + D パターンを試行
            int month1 = firstDigit * 10 + secondDigit;
            int day1 = thirdDigit;
            if (month1 >= 1 && month1 <= 12 && day1 >= 1 && day1 <= 31) {
                return new int[]{year, month1, day1};
            }
            
            // M + DD パターンを試行
            int month2 = firstDigit;
            int day2 = secondDigit * 10 + thirdDigit;
            if (month2 >= 1 && month2 <= 12 && day2 >= 1 && day2 <= 31) {
                return new int[]{year, month2, day2};
            }
            
        } else if (monthDayStr.length() == 4) {
            // 4桁の場合: MMDD
            int month = Integer.parseInt(monthDayStr.substring(0, 2));
            int day = Integer.parseInt(monthDayStr.substring(2, 4));
            if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                return new int[]{year, month, day};
            }
        }
        
        return null;
    }

    /**
     * 拡張パターン（3桁・4桁）から年月を抽出
     * 
     * 3桁の場合: YYM または YMM（3桁ロジックを適用）
     * 4桁の場合: YYMM
     * 
     * @param yearMonthStr 年月部分の文字列（3桁または4桁）
     * @param day 日
     * @param gengo 元号情報（3桁数字の判定に使用）
     * @return [年, 月, 日]の配列、抽出できない場合はnull
     */
    private int[] extractYearMonthFromExtended(String yearMonthStr, int day, GengoYearTable gengo) {
        if (yearMonthStr.length() == 3) {
            // 3桁の場合: 既存の3桁ロジックを使用
            int[] yearMonth = extract3DigitYearMonth(yearMonthStr, gengo);
            if (yearMonth != null && day >= 1 && day <= 31) {
                return new int[]{yearMonth[0], yearMonth[1], day};
            }
        } else if (yearMonthStr.length() == 4) {
            // 4桁の場合: YYMM
            int year = Integer.parseInt(yearMonthStr.substring(0, 2));
            int month = Integer.parseInt(yearMonthStr.substring(2, 4));
            if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                return new int[]{year, month, day};
            }
        }
        
        return null;
    }

    /**
     * 3桁数字の年月を判定して抽出
     * 
     * 判定ロジック：
     * (1) N2N3が13以上であれば、YYM（年年月）
     * (2) 元号+N1N2がシステム日付よりも未来であれば、YMM（年月月）
     * (3) (1)(2)以外なら、YYM（年年月）
     * 
     * @param digits3 3桁の数字文字列
     * @param gengo 元号情報
     * @return [年, 月]の配列、抽出できない場合はnull
     */
    private int[] extract3DigitYearMonth(String digits3, GengoYearTable gengo) {
        if (digits3.length() != 3) {
            return null;
        }
        
        int n1 = Integer.parseInt(digits3.substring(0, 1));  // 1桁目
        int n2 = Integer.parseInt(digits3.substring(1, 2));  // 2桁目
        int n3 = Integer.parseInt(digits3.substring(2, 3));  // 3桁目
        
        int n2n3 = n2 * 10 + n3;  // 2桁目と3桁目を結合
        int n1n2 = n1 * 10 + n2;  // 1桁目と2桁目を結合
        
        // (1) N2N3が13以上であれば、YYM（年年月）
        if (n2n3 >= 13) {
            // N2N3は月として無効なので、YYMとして解釈
            if (n3 >= 1 && n3 <= 12) {  // 3桁目が有効な月かチェック
                return new int[]{n1n2, n3};
            }
            return null;
        }
        
        // (2) 元号+N1N2がシステム日付よりも未来であれば、YMM（年月月）
        if (isFutureYear(n1, gengo)) {
            // YMM（年月月）として解釈
            if (n2n3 >= 1 && n2n3 <= 12) {  // N2N3が有効な月かチェック
                return new int[]{n1, n2n3};
            }
        }
        
        // (3) (1)(2)以外なら、YYM（年年月）
        if (n3 >= 1 && n3 <= 12) {  // 3桁目が有効な月かチェック
            return new int[]{n1n2, n3};
        }
        
        return null;
    }

    /**
     * 指定された年が現在のシステム日付よりも未来かどうかを判定
     * 
     * @param warekiYear 和暦年
     * @param gengo 元号情報
     * @return 未来の年の場合true
     */
    private boolean isFutureYear(int warekiYear, GengoYearTable gengo) {
        try {
            // 和暦年を西暦年に変換
            int seirekiYear = warekiYear + gengo.yearAdd;
            
            // 現在の西暦年を取得
            int currentYear = java.time.LocalDate.now().getYear();
            
            // 西暦年が現在年よりも未来かどうかを判定
            return seirekiYear > currentYear;
        } catch (Exception e) {
            // エラーが発生した場合は未来ではないと判定
            return false;
        }
    }


}
