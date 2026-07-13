package jp.co.ariseinnovation.dateconverter.util;

import jp.co.ariseinnovation.dateconverter.exception.FormatException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日付文字列解析ユーティリティクラス
 * 
 * DateTime.Parseの代替として以下の目的で作成：
 * - OS依存性を排除し、統一された動作を保証
 * - ブラックボックス化された仕様を明確化・制御
 * - 和暦・西暦の混在する日付形式に対応
 * 
 * 用語定義：
 * - year: 西暦年
 * - wareki: 和暦（昭和、平成、令和等）
 * - nen: 西暦か和暦か不明な年
 * 
 * 対応する日付形式：
 * 1. 和暦形式:
 *    - 年月: H10.5、昭和63.12、令和3.4等
 *    - 年月日: R3.12.25、H10.5.15、昭和63.12.31、平 1・1・1、H.30/09/30、R.3/9/30等
 *      （元号と年の間は区切り文字なし・空白・. - / , のいずれか1つを許容。
 *        年月日各項目間の区切り文字は西暦形式と同じ . - / , に対応）
 * 2. 西暦8桁: 20231225（YYYYMMDD）
 * 3. 西暦6桁末日: 202312末、202312末日（YYYYMM末）
 * 4. 西暦区切り文字年月日・年月: 2023-04-30、2023.04.30、2023/04/30、2023,04,30等
 *    （区切り文字は和暦形式と同じ . - / , に対応）
 */
public class DateParser {

    /** ログ出力用 */
    private static final Logger logger = LoggerFactory.getLogger(DateParser.class);

    /** 
     * 全角→半角変換マップ（効率化のため配列ベースに変更）
     * OCRで読み取った文字や手入力での全角文字を半角に統一するために使用
     */
    private static final String[][] CHAR_CONVERSION_PAIRS = {
        {"０","0"}, {"１","1"}, {"２","2"}, {"３","3"}, {"４","4"},
        {"５","5"}, {"６","6"}, {"７","7"}, {"８","8"}, {"９","9"},
        {"元","1"},  // 元年を1年として扱う
        {"Ｓ","S"}, {"Ｈ","H"}, {"Ｒ","R"}, {"Ｌ","L"},  // 全角年号を半角に変換
        {"．","."}, {"。","."}, {"・","."},  // 全角区切り文字を半角ドットに変換
        {"－","-"}, {"ー","-"}, {"—","-"}, {"―","-"},  // 全角ハイフンを半角ハイフンに変換
        {"／","/"},  // 全角スラッシュを半角スラッシュに変換
        {"　"," "}  // 全角スペースを半角スペースに変換
    };

    /**
     * 和暦の年号と年の間に許容する区切り文字（H.30、R．３等に対応するため任意で1つまで許容）
     * 年月日各項目間の区切り文字（. - / , ）は西暦形式と同様に複数個の連続も許容する
     */
    private static final String WAREKI_SEP = "[.\\-/,]";

    /** 和暦年月パターン（H10.5、昭和63.12、平成10.5、H.30等）
     *  元号と年の間には空白や区切り文字（. - / ,）を任意で1つ挟める */
    private static final Pattern WAREKI_YM_PATTERN = Pattern.compile(
            "^([HSRL]|昭和|平成|令和|昭|平|令)\\s*" + WAREKI_SEP + "?\\s*(\\d{1,2})" + WAREKI_SEP + "+(\\d{1,2})$");

    /** 和暦年月日パターン（R3.12.25、H10.5.15、昭和63.12.31、H.30/09/30、平 1・1・1等）
     *  元号と年の間には空白や区切り文字（. - / ,）を任意で1つ挟める */
    private static final Pattern WAREKI_YMD_PATTERN = Pattern.compile(
            "^([HSRL]|昭和|平成|令和|昭|平|令)\\s*" + WAREKI_SEP + "?\\s*(\\d{1,2})" + WAREKI_SEP + "+(\\d{1,2})" + WAREKI_SEP + "+(\\d{1,2})$");
    
    /** 西暦8桁パターン（20231225等） */
    private static final Pattern YYYYMMDD_PATTERN = Pattern.compile("^(\\d{4})(\\d{2})(\\d{2})$");

    /** 西暦の区切り文字（和暦と同様に複数の区切り文字に対応。DatePrecisionUtilからも参照される） */
    static final String YYYY_SEPARATORS = "[.\\-/,]";

    /** 西暦区切り文字年月日パターン（2023-04-30、2023.04.30、2023/04/30、2023,04,30等） */
    private static final Pattern YYYY_MM_DD_PATTERN = Pattern.compile(
            "^(\\d{4})" + YYYY_SEPARATORS + "+(\\d{1,2})" + YYYY_SEPARATORS + "+(\\d{1,2})$");

    /** 西暦区切り文字年月パターン（2023-04、2023.04、2023/04、2023,04等） */
    private static final Pattern YYYY_MM_PATTERN = Pattern.compile(
            "^(\\d{4})" + YYYY_SEPARATORS + "+(\\d{1,2})$");
    
    /** 西暦6桁末日パターン（202312末、202312末日等） */
    private static final Pattern YYYYMM_PATTERN = Pattern.compile("^(\\d{4})(\\d{2})末?日?$");

    /**
     * プライベートコンストラクタ
     * ユーティリティクラスのためインスタンス化を禁止
     */
    private DateParser() {
    }

    /**
     * 日付文字列をパースしてDateTimeオブジェクトを返す（基準日なし）
     * 
     * @param src パース対象の日付文字列
     * @return パース結果のDateTimeオブジェクト
     * @throws Exception パースに失敗した場合
     */
    public static DateTime Parse(String src) throws Exception {
        return Parse(src, null);
    }
    
    /**
     * 日付文字列をパースしてDateTimeオブジェクトを返す
     * 
     * 処理の流れ：
     * 1. 入力文字列の妥当性チェック
     * 2. 内部パース処理の実行
     * 3. 結果の妥当性チェックと例外処理
     * 
     * @param src パース対象の日付文字列
     * @param basisDate パース時の基準日（現在は未使用、将来の拡張用）
     * @return パース結果のDateTimeオブジェクト
     * @throws Exception パースに失敗した場合（IllegalArgumentException、FormatException）
     */
    public static DateTime Parse(String src, DateTime basisDate) throws Exception {
        if (src == null) {
            throw new IllegalArgumentException("failed to parse dateString");
        }
        
        // 内部パース処理を実行
        DateTime parsed = parseInternal(src);
        if (parsed == null) {
            throw new FormatException(src + " is not parsable");
        }
        return parsed;
    }

    /**
     * 和暦の年月日形式（元号+年+月+日の3項目）に構造的にマッチするかを判定する
     *
     * hasOtherFormatDatePrecisionの「日が1日の場合は年月精度」という簡易ヒューリスティックは、
     * 「年月のみで日が1日固定」と「年月日が明示的に指定されていて日がたまたま1日」を
     * 区別できない。本メソッドは正規化後の文字列が年月日3項目パターンに構造的にマッチするか
     * のみで判定するため、日の値（1日かどうか）に関わらず正確に判定できる。
     *
     * @param src 判定対象の文字列
     * @return 和暦の年月日形式に構造的にマッチする場合true
     */
    public static boolean isWarekiWithExplicitDay(String src) {
        if (StringUtils.isEmpty(src)) {
            return false;
        }
        return WAREKI_YMD_PATTERN.matcher(normalizeString(src)).matches();
    }

    /** パターンとパーサーのペア */
    private static final PatternParser[] PATTERN_PARSERS = {
        new PatternParser(WAREKI_YMD_PATTERN, DateParser::parseWarekiYMD),
        new PatternParser(WAREKI_YM_PATTERN, DateParser::parseWarekiYM),
        new PatternParser(YYYY_MM_DD_PATTERN, DateParser::parseYYYY_MM_DD),
        new PatternParser(YYYY_MM_PATTERN, DateParser::parseYYYY_MM),
        new PatternParser(YYYYMMDD_PATTERN, DateParser::parseYYYYMMDD),
        new PatternParser(YYYYMM_PATTERN, DateParser::parseYYYYMM)
    };

    /**
     * 内部パース処理（最適化版）
     * 
     * @param src パース対象の日付文字列
     * @return パース結果のDateTimeオブジェクト（パースできない場合はnull）
     */
    private static DateTime parseInternal(String src) {
        if (StringUtils.isEmpty(src)) {
            return null;
        }
        
        String normalized = normalizeString(src);
        
        try {
            // パターンを順次試行
            for (PatternParser patternParser : PATTERN_PARSERS) {
                Matcher matcher = patternParser.pattern.matcher(normalized);
                if (matcher.matches()) {
                    return patternParser.parser.apply(matcher);
                }
            }
            return null;
        } catch (Exception e) {
            logger.debug(MessageUtil.getMessage("date.parse.failed", src, e.getMessage()));
            return null;
        }
    }

    /**
     * パターンとパーサーのペアを保持するクラス
     */
    private static class PatternParser {
        final Pattern pattern;
        final java.util.function.Function<Matcher, DateTime> parser;

        PatternParser(Pattern pattern, java.util.function.Function<Matcher, DateTime> parser) {
            this.pattern = pattern;
            this.parser = parser;
        }
    }
    
    /**
     * 和暦年月日形式の文字列をパース
     * 
     * @param matcher 和暦年月日パターンにマッチしたMatcherオブジェクト
     * @return パース結果のDateTimeオブジェクト（年号が未対応の場合はnull）
     */
    private static DateTime parseWarekiYMD(Matcher matcher) {
        try {
            String gengoChar = matcher.group(1);
            int year = Integer.parseInt(matcher.group(2));
            int month = Integer.parseInt(matcher.group(3));
            int day = Integer.parseInt(matcher.group(4));
            
            return parseWarekiCommon(gengoChar, year, month, day);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 和暦年月形式の文字列をパース
     * 
     * @param matcher 和暦年月パターンにマッチしたMatcherオブジェクト
     * @return パース結果のDateTimeオブジェクト（年号が未対応の場合はnull）
     */
    private static DateTime parseWarekiYM(Matcher matcher) {
        try {
            String gengoChar = matcher.group(1);
            int year = Integer.parseInt(matcher.group(2));
            int month = Integer.parseInt(matcher.group(3));
            
            return parseWarekiCommon(gengoChar, year, month, 1); // 日は1日固定
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 和暦の共通パース処理
     * 
     * @param gengoChar 年号文字（H、昭和等）
     * @param year 和暦年
     * @param month 月
     * @param day 日
     * @return パース結果のDateTimeオブジェクト（年号が未対応の場合はnull）
     */
    private static DateTime parseWarekiCommon(String gengoChar, int year, int month, int day) {
        GengoYearTable gengo = findGengoByChar(gengoChar);
        if (gengo == null) {
            return null; // 未対応の年号
        }
        
        int seireki = year + gengo.yearAdd;
        return new DateTime(seireki, month, day, 0, 0);
    }

    /**
     * 年号文字から対応するGengoYearTableを検索
     * 
     * @param gengoChar 年号文字（H、昭和等）
     * @return 対応するGengoYearTable（見つからない場合はnull）
     */
    private static GengoYearTable findGengoByChar(String gengoChar) {
        return GengoYearTable.table.stream()
            .filter(g -> g.gengo.equals(gengoChar))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * YYYY-MM-DD形式の文字列をパース
     * 
     * @param matcher YYYY-MM-DDパターンにマッチしたMatcherオブジェクト
     * @return パース結果のDateTimeオブジェクト
     */
    private static DateTime parseYYYY_MM_DD(Matcher matcher) {
        try {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            return new DateTime(year, month, day, 0, 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * YYYY-MM形式の文字列をパース
     * 
     * @param matcher YYYY-MMパターンにマッチしたMatcherオブジェクト
     * @return パース結果のDateTimeオブジェクト（1日固定）
     */
    private static DateTime parseYYYY_MM(Matcher matcher) {
        try {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            return new DateTime(year, month, 1, 0, 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * YYYYMMDD形式の文字列をパース
     * 
     * @param matcher YYYYMMDDパターンにマッチしたMatcherオブジェクト
     * @return パース結果のDateTimeオブジェクト
     */
    private static DateTime parseYYYYMMDD(Matcher matcher) {
        try {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            return new DateTime(year, month, day, 0, 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * YYYYMM末形式の文字列をパース
     * 
     * @param matcher YYYYMM末パターンにマッチしたMatcherオブジェクト
     * @return パース結果のDateTimeオブジェクト（その月の末日）
     */
    private static DateTime parseYYYYMM(Matcher matcher) {
        try {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            
            YearMonth yearMonth = YearMonth.of(year, month);
            int lastDay = yearMonth.lengthOfMonth();
            
            return new DateTime(year, month, lastDay, 0, 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 文字列の正規化処理（最適化版）
     * 
     * 処理内容：
     * 1. 前後の空白文字を除去
     * 2. 全角文字を半角文字に変換
     * 
     * @param src 正規化対象の文字列
     * @return 正規化後の文字列
     */
    private static String normalizeString(String src) {
        String trimmed = src.trim();
        
        // 変換が必要な文字が含まれているかチェック
        if (!needsNormalization(trimmed)) {
            return trimmed;
        }
        
        // StringBuilder を使用して効率的に変換
        StringBuilder result = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            String converted = convertChar(c);
            result.append(converted != null ? converted : c);
        }
        
        return result.toString();
    }

    /**
     * 正規化が必要かどうかをチェック
     * 
     * @param str チェック対象の文字列
     * @return 正規化が必要な場合true
     */
    private static boolean needsNormalization(String str) {
        for (String[] pair : CHAR_CONVERSION_PAIRS) {
            if (str.contains(pair[0])) {
                return true;
            }
        }
        return false;
    }

    /**
     * 単一文字の変換
     * 
     * @param c 変換対象の文字
     * @return 変換後の文字列（変換不要の場合はnull）
     */
    private static String convertChar(char c) {
        String charStr = String.valueOf(c);
        for (String[] pair : CHAR_CONVERSION_PAIRS) {
            if (pair[0].equals(charStr)) {
                return pair[1];
            }
        }
        return null;
    }


}
