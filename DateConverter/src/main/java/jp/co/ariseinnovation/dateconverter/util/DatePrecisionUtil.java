package jp.co.ariseinnovation.dateconverter.util;

import jp.co.ariseinnovation.dateconverter.enums.OutputFormat;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日付データの精度を判定するユーティリティクラス
 */
public class DatePrecisionUtil {

    private static final Logger logger = LoggerFactory.getLogger(DatePrecisionUtil.class);

    /**
     * プライベートコンストラクタ
     * ユーティリティクラスのためインスタンス化を禁止（DateParser等の他ユーティリティと同じ規約）
     */
    private DatePrecisionUtil() {
    }

    /**
     * 入力データの精度を判定する
     * 
     * @param src 入力データ
     * @param gengo マッチした元号情報（nullの場合はDateParserで解析）
     * @return true: 年月日精度, false: 年月精度
     */
    public static boolean hasDatePrecision(String src, GengoYearTable gengo) {
        if (StringUtils.isEmpty(src)) {
            return false;
        }
        
        if (gengo != null) {
            // 和暦の場合の精度判定
            return hasWarekiDatePrecision(src, gengo);
        } else {
            // その他の形式の場合はDateParserで判定
            return hasOtherFormatDatePrecision(src);
        }
    }
    
    /**
     * 和暦形式の精度を判定する
     * 
     * @param src 入力データ
     * @param gengo 元号情報
     * @return true: 年月日精度, false: 年月精度
     */
    private static boolean hasWarekiDatePrecision(String src, GengoYearTable gengo) {
        String yearMonthDayPart = src.substring(gengo.gengo.length());
        
        // 年月日を含むパターンをチェック
        String[] datePatterns = {
            // 年月日文字を含むパターン（例：10年5月15日）
            "(\\d{1,2})年(\\d{1,2})月(\\d{1,2})日",
            // 年月日区切り文字パターン（例：10.5.15、10-5-15、10/5/15、10,5,15）
            "(\\d{1,2})[.\\-/,]+(\\d{1,2})[.\\-/,]+(\\d{1,2})",
            // 年月（3-4桁）日拡張区切り文字パターン（例：105.30、1005.30）
            "(\\d{3,4})[.\\-/,]+(\\d{1,2})",
            // 年月日拡張区切り文字パターン（例：10.530、10.0530）
            "(\\d{1,2})[.\\-/,]+(\\d{3,4})",
            // 区切り文字なし6桁パターン（例：100515 → 10年05月15日）
            "(\\d{2})(\\d{2})(\\d{2})",
            // 区切り文字なし5桁パターン（例：10530 → 10年530）
            "(\\d{2})(\\d{3})"
        };
        
        for (String pattern : datePatterns) {
            if (yearMonthDayPart.matches(pattern)) {
                // さらに詳細な検証を行う
                if (isValidDatePattern(yearMonthDayPart, pattern)) {
                    return true;
                }
            }
        }
        
        return false; // 年月のみの精度
    }
    
    /**
     * 日付パターンが有効な年月日を表しているかを検証する
     * 
     * @param yearMonthDayPart 年月日部分
     * @param pattern マッチしたパターン
     * @return true: 有効な年月日, false: 年月のみまたは無効
     */
    private static boolean isValidDatePattern(String yearMonthDayPart, String pattern) {
        try {
            java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = compiledPattern.matcher(yearMonthDayPart);
            
            if (matcher.matches()) {
                // パターンに応じて日付の存在を確認
                if (pattern.contains("年") && pattern.contains("月") && pattern.contains("日")) {
                    // 年月日文字パターン
                    return matcher.groupCount() >= 3;
                } else if (pattern.contains("\\d{3,4}") || pattern.contains("\\d{2})(\\d{3}")) {
                    // 拡張パターンの場合、実際に日付部分があるかを確認
                    return hasValidDayInExtendedPattern(yearMonthDayPart, matcher);
                } else if (matcher.groupCount() >= 3) {
                    // 3つのグループがある場合は年月日
                    return true;
                }
            }
        } catch (Exception e) {
            logger.debug("Pattern validation failed for: " + yearMonthDayPart, e);
        }
        
        return false;
    }
    
    /**
     * 拡張パターンで有効な日付部分があるかを確認
     * 
     * @param yearMonthDayPart 年月日部分
     * @param matcher マッチャー
     * @return true: 有効な日付部分がある
     */
    private static boolean hasValidDayInExtendedPattern(String yearMonthDayPart, java.util.regex.Matcher matcher) {
        try {
            if (matcher.groupCount() >= 2) {
                String group1 = matcher.group(1);
                String group2 = matcher.group(2);
                
                // 3-4桁の部分に日付が含まれているかを確認
                if (group1.length() >= 3) {
                    // group1が3-4桁の場合、日付部分を抽出して検証
                    return extractAndValidateDay(group1, group2);
                } else if (group2.length() >= 3) {
                    // group2が3-4桁の場合、日付部分を抽出して検証
                    return extractAndValidateDay(group2, group1);
                }
            }
        } catch (Exception e) {
            logger.debug("Extended pattern validation failed", e);
        }
        
        return false;
    }
    
    /**
     * 拡張パターンから日付を抽出して検証
     * 
     * @param extendedPart 3-4桁の部分
     * @param otherPart その他の部分
     * @return true: 有効な日付がある
     */
    private static boolean extractAndValidateDay(String extendedPart, String otherPart) {
        if (extendedPart.length() == 3) {
            // 3桁の場合: MMD または MDD
            int digit1 = Integer.parseInt(extendedPart.substring(0, 1));
            int digit2 = Integer.parseInt(extendedPart.substring(1, 2));
            int digit3 = Integer.parseInt(extendedPart.substring(2, 3));
            
            // MM + D パターンをチェック
            int month1 = digit1 * 10 + digit2;
            int day1 = digit3;
            if (month1 >= 1 && month1 <= 12 && day1 >= 1 && day1 <= 31) {
                return true;
            }
            
            // M + DD パターンをチェック
            int month2 = digit1;
            int day2 = digit2 * 10 + digit3;
            if (month2 >= 1 && month2 <= 12 && day2 >= 1 && day2 <= 31) {
                return true;
            }
        } else if (extendedPart.length() == 4) {
            // 4桁の場合: MMDD
            int month = Integer.parseInt(extendedPart.substring(0, 2));
            int day = Integer.parseInt(extendedPart.substring(2, 4));
            if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * その他の形式（DateParserで処理される形式）の精度を判定する
     * 
     * @param src 入力データ
     * @return true: 年月日精度, false: 年月精度
     */
    private static boolean hasOtherFormatDatePrecision(String src) {
        try {
            // 西暦区切り文字年月日形式（2023-04-30、2023.04.30等）は明示的に年月日精度
            if (src.matches("^\\d{4}" + DateParser.YYYY_SEPARATORS + "+\\d{1,2}" + DateParser.YYYY_SEPARATORS + "+\\d{1,2}$")) {
                return true;
            }

            // 西暦区切り文字年月形式（2023-04、2023.04等）は明示的に年月精度
            if (src.matches("^\\d{4}" + DateParser.YYYY_SEPARATORS + "+\\d{1,2}$")) {
                return false;
            }

            DateTime parsedDate = DateParser.Parse(src);

            // YYYYMMDD形式（8桁連結）は日の値に関わらず明示的に年月日精度
            // （日が01の場合、getDayOfMonth()だけでは「年月精度で1日固定」と区別できないため）
            if (src.matches("^\\d{8}$")) {
                return true;
            }

            // 日が1日の場合は年月精度、それ以外は年月日精度と判定
            return parsedDate.getDayOfMonth() != 1;
        } catch (Exception e) {
            logger.debug("Failed to parse date for precision check: " + src, e);
            return false; // パースできない場合は年月精度として扱う
        }
    }
    
    /**
     * 入力データの精度と指定された出力形式に基づいて、実際の出力形式を決定する
     * 
     * @param src 入力データ
     * @param gengo マッチした元号情報
     * @param requestedFormat 要求された出力形式
     * @return 実際に使用する出力形式
     */
    public static OutputFormat determineOutputFormat(String src, GengoYearTable gengo, OutputFormat requestedFormat) {
        // 入力データの精度が年月の場合は、YYYYMMDDが指定されていてもYYYYMMとする
        return hasDatePrecision(src, gengo) ? requestedFormat : OutputFormat.YYYYMM;
    }
}
