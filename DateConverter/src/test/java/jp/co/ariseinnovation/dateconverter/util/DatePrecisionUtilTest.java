package jp.co.ariseinnovation.dateconverter.util;

import jp.co.ariseinnovation.dateconverter.enums.OutputFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DatePrecisionUtilの単体テストクラス
 */
public class DatePrecisionUtilTest {

    @Test
    public void testHasDatePrecision_和暦年月日() {
        // 和暦の年月日形式（日付精度あり）
        GengoYearTable gengo = GengoYearTable.HEISEI_H;
        
        assertTrue(DatePrecisionUtil.hasDatePrecision("H10.5.15", gengo));
        assertTrue(DatePrecisionUtil.hasDatePrecision("H10年5月15日", gengo));
        assertTrue(DatePrecisionUtil.hasDatePrecision("H100515", gengo));
    }

    @Test
    public void testHasDatePrecision_和暦年月() {
        // 和暦の年月形式（日付精度なし）
        GengoYearTable gengo = GengoYearTable.HEISEI_H;
        
        assertFalse(DatePrecisionUtil.hasDatePrecision("H10.5", gengo));
        assertFalse(DatePrecisionUtil.hasDatePrecision("H10年5月", gengo));
        assertFalse(DatePrecisionUtil.hasDatePrecision("H1005", gengo));
    }

    @Test
    public void testHasDatePrecision_その他の形式() {
        // DateParserで処理される形式
        // YYYYMMDD形式（8桁連結）は日の値に関わらず明示的に年月日精度
        assertTrue(DatePrecisionUtil.hasDatePrecision("20230401", null)); // 日が01でも年月日精度
        assertTrue(DatePrecisionUtil.hasDatePrecision("20230415", null)); // 日が1日以外も年月日精度
    }

    @Test
    public void testHasDatePrecision_全角区切り文字の和暦で日が1日の場合() {
        // 全角の元号・区切り文字（・等）はfindMatchingGengoに直接マッチせずDateParserの
        // フォールバック経由で処理されるため、日が1日でも「日が明示的に指定された年月日精度」
        // として正しく判定されることを確認する（日が1日の場合の年月精度との誤判定を防ぐ）
        assertTrue(DatePrecisionUtil.hasDatePrecision("平1・1・1", null));
        assertTrue(DatePrecisionUtil.hasDatePrecision("平１・１・１", null));
        assertTrue(DatePrecisionUtil.hasDatePrecision("H.30/09/30", null));
    }

    @Test
    public void testDetermineOutputFormat_年月精度() {
        // 入力データが年月精度の場合、YYYYMMDDを指定してもYYYYMMになる
        GengoYearTable gengo = GengoYearTable.HEISEI_H;
        
        OutputFormat result = DatePrecisionUtil.determineOutputFormat("H10.5", gengo, OutputFormat.YYYYMMDD);
        assertEquals(OutputFormat.YYYYMM, result);
        
        result = DatePrecisionUtil.determineOutputFormat("H10.5", gengo, OutputFormat.YYYYMM);
        assertEquals(OutputFormat.YYYYMM, result);
    }

    @Test
    public void testDetermineOutputFormat_年月日精度() {
        // 入力データが年月日精度の場合、指定された形式を使用
        GengoYearTable gengo = GengoYearTable.HEISEI_H;
        
        OutputFormat result = DatePrecisionUtil.determineOutputFormat("H10.5.15", gengo, OutputFormat.YYYYMMDD);
        assertEquals(OutputFormat.YYYYMMDD, result);
        
        result = DatePrecisionUtil.determineOutputFormat("H10.5.15", gengo, OutputFormat.YYYYMM);
        assertEquals(OutputFormat.YYYYMM, result);
    }

    @Test
    public void testDetermineOutputFormat_年月日精度_令和() {
        // 令和の年月日精度でYYYYMM形式を指定した場合
        GengoYearTable gengo = GengoYearTable.REIWA_R;
        
        OutputFormat result = DatePrecisionUtil.determineOutputFormat("R5.4.30", gengo, OutputFormat.YYYYMM);
        assertEquals(OutputFormat.YYYYMM, result);
        
        result = DatePrecisionUtil.determineOutputFormat("R5.4.30", gengo, OutputFormat.YYYYMMDD);
        assertEquals(OutputFormat.YYYYMMDD, result);
    }

    @Test
    public void testDetermineOutputFormat_年月日精度_昭和() {
        // 昭和の年月日精度でYYYYMM形式を指定した場合
        GengoYearTable gengo = GengoYearTable.SHOWA;
        
        OutputFormat result = DatePrecisionUtil.determineOutputFormat("昭和63.12.31", gengo, OutputFormat.YYYYMM);
        assertEquals(OutputFormat.YYYYMM, result);
        
        result = DatePrecisionUtil.determineOutputFormat("昭和63.12.31", gengo, OutputFormat.YYYYMMDD);
        assertEquals(OutputFormat.YYYYMMDD, result);
    }

    @Test
    public void testDetermineOutputFormat_null入力() {
        // null入力の場合
        OutputFormat result = DatePrecisionUtil.determineOutputFormat(null, null, OutputFormat.YYYYMMDD);
        assertEquals(OutputFormat.YYYYMM, result); // 精度判定できない場合はYYYYMM

        result = DatePrecisionUtil.determineOutputFormat("", null, OutputFormat.YYYYMMDD);
        assertEquals(OutputFormat.YYYYMM, result); // 空文字の場合もYYYYMM
    }

    @Test
    public void testHasDatePrecision_拡張区切り文字パターン() {
        // 年月（3-4桁）+区切り文字+日、年+区切り文字+月日（3-4桁）のパターンも日付精度ありと判定される
        GengoYearTable gengo = GengoYearTable.HEISEI_H;

        assertTrue(DatePrecisionUtil.hasDatePrecision("H105.30", gengo));   // 年月(3桁)+区切り+日
        assertTrue(DatePrecisionUtil.hasDatePrecision("H1005.30", gengo)); // 年月(4桁)+区切り+日
        assertTrue(DatePrecisionUtil.hasDatePrecision("H10.530", gengo));  // 年+区切り+月日(3桁)
        assertTrue(DatePrecisionUtil.hasDatePrecision("H10.0530", gengo)); // 年+区切り+月日(4桁)
    }

    @Test
    public void testHasDatePrecision_5桁区切りなしパターン() {
        // 5桁の区切りなしパターン（年+月日）も日付精度ありと判定される
        GengoYearTable gengo = GengoYearTable.HEISEI_H;

        assertTrue(DatePrecisionUtil.hasDatePrecision("H10530", gengo)); // 10年5月30日として解釈可能
    }

    @Test
    public void testHasDatePrecision_パースできない文字列() {
        // パースできない文字列は例外を握りつぶしてfalse（年月精度扱い）を返す
        assertFalse(DatePrecisionUtil.hasDatePrecision("abc", null));
    }

    @Test
    public void testHasDatePrecision_YYYYMMDD形式は日の値に関わらず年月日精度() {
        // ハイフン区切り（YYYY-MM-DD）・8桁連結（YYYYMMDD）のいずれも、
        // 日の値（01であっても）に関わらず明示的に年月日精度と判定される
        assertTrue(DatePrecisionUtil.hasDatePrecision("2023-04-01", null)); // ハイフン区切り、日=01
        assertTrue(DatePrecisionUtil.hasDatePrecision("20230401", null));   // 8桁連結、日=01
        assertTrue(DatePrecisionUtil.hasDatePrecision("20230415", null));   // 8桁連結、日=15
    }

    @Test
    public void testDetermineOutputFormat_8桁YYYYMMDDは日の値に関わらず年月日精度() {
        // 20230401（日=01）でもYYYYMMDDを要求すればYYYYMMDDのまま出力される
        OutputFormat result = DatePrecisionUtil.determineOutputFormat("20230401", null, OutputFormat.YYYYMMDD);
        assertEquals(OutputFormat.YYYYMMDD, result);

        // ハイフン区切りも同様に日=01でも明示的に年月日精度として扱われる
        result = DatePrecisionUtil.determineOutputFormat("2023-04-01", null, OutputFormat.YYYYMMDD);
        assertEquals(OutputFormat.YYYYMMDD, result);
    }

    /**
     * 回帰テスト：西暦の区切り文字をドット・スラッシュ・カンマに拡張した際、
     * ハイフン区切りで一度修正した「日=01を年月精度と誤判定する」バグが
     * 新しい区切り文字ごとに再発していないことを確認する。
     */
    @Test
    public void testHasDatePrecision_西暦区切り文字拡張後も日1日の誤判定が起きない() {
        assertTrue(DatePrecisionUtil.hasDatePrecision("2023.04.01", null)); // ドット区切り、日=01
        assertTrue(DatePrecisionUtil.hasDatePrecision("2023/04/01", null)); // スラッシュ区切り、日=01
        assertTrue(DatePrecisionUtil.hasDatePrecision("2023,04,01", null)); // カンマ区切り、日=01

        assertFalse(DatePrecisionUtil.hasDatePrecision("2023.04", null)); // ドット区切り年月のみ
        assertFalse(DatePrecisionUtil.hasDatePrecision("2023/04", null)); // スラッシュ区切り年月のみ
    }

    /**
     * カバレッジ計測で未実行と判明した分岐を狙うテスト：
     * 拡張パターン（年＋区切り＋月日3-4桁）で月日の解釈が無効な場合は日付精度なしと判定される。
     */
    @Test
    public void testHasDatePrecision_拡張パターンで月日解釈が無効なら日付精度なし() {
        GengoYearTable gengo = GengoYearTable.HEISEI_H;

        // "990" → MM+D＝99月0日（無効）、M+DD＝9月90日（無効）→ 日付精度なし
        assertFalse(DatePrecisionUtil.hasDatePrecision("H10.990", gengo));
        // "1332" → 13月32日（無効）→ 日付精度なし
        assertFalse(DatePrecisionUtil.hasDatePrecision("H10.1332", gengo));
        // "131" → MM+D＝13月1日（無効）だがM+DD＝1月31日（有効）→ 日付精度あり
        assertTrue(DatePrecisionUtil.hasDatePrecision("H10.131", gengo));
    }

    /**
     * 精度判定側の境界分岐：月0・日0・日32・年月部分解釈不能のケースで
     * 誤って日付精度ありと判定されないことを確認する（変換側の境界テストと対）。
     */
    @Test
    public void testHasDatePrecision_境界値の月日は日付精度なしと判定される() {
        GengoYearTable gengo = GengoYearTable.HEISEI_H;

        assertFalse(DatePrecisionUtil.hasDatePrecision("H10.001", gengo));  // 月0（両解釈無効）
        assertTrue(DatePrecisionUtil.hasDatePrecision("H10.120", gengo));   // MM+D無効→M+DD有効
        assertFalse(DatePrecisionUtil.hasDatePrecision("H10.1200", gengo)); // 12月0日
        assertFalse(DatePrecisionUtil.hasDatePrecision("H10.1232", gengo)); // 12月32日
        assertFalse(DatePrecisionUtil.hasDatePrecision("H990.5", gengo));   // 年月部分が解釈不能
    }

    /**
     * ユーティリティクラスとしてインスタンス化されない設計であることを確認する
     * （privateコンストラクタの存在確認）。
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        java.lang.reflect.Constructor<DatePrecisionUtil> constructor =
                DatePrecisionUtil.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    @Test
    public void testDetermineOutputFormat_西暦区切り文字拡張後も日1日の誤判定が起きない() {
        assertEquals(OutputFormat.YYYYMMDD,
                DatePrecisionUtil.determineOutputFormat("2023.04.01", null, OutputFormat.YYYYMMDD));
        assertEquals(OutputFormat.YYYYMMDD,
                DatePrecisionUtil.determineOutputFormat("2023/04/01", null, OutputFormat.YYYYMMDD));
        assertEquals(OutputFormat.YYYYMM,
                DatePrecisionUtil.determineOutputFormat("2023.04", null, OutputFormat.YYYYMMDD));
    }
}