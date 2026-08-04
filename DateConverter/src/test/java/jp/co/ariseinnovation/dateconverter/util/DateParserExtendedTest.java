package jp.co.ariseinnovation.dateconverter.util;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DateParserの拡張テストクラス（年月日形式の和暦サポート）
 */
class DateParserExtendedTest {

    @Test
    void testParseWarekiYMD_令和() throws Exception {
        // 令和の年月日形式
        DateTime result = DateParser.Parse("R3.12.25");
        assertEquals(2021, result.getYear());
        assertEquals(12, result.getMonthOfYear());
        assertEquals(25, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYMD_平成() throws Exception {
        // 平成の年月日形式
        DateTime result = DateParser.Parse("H10.5.15");
        assertEquals(1998, result.getYear());
        assertEquals(5, result.getMonthOfYear());
        assertEquals(15, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYMD_昭和() throws Exception {
        // 昭和の年月日形式
        DateTime result = DateParser.Parse("昭和63.12.31");
        assertEquals(1988, result.getYear());
        assertEquals(12, result.getMonthOfYear());
        assertEquals(31, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYM_既存機能確認() throws Exception {
        // 既存の年月形式が正常に動作することを確認
        DateTime result = DateParser.Parse("H10.5");
        assertEquals(1998, result.getYear());
        assertEquals(5, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth()); // 年月形式は1日固定
    }

    @Test
    void testParseWarekiYMD_1桁月日() throws Exception {
        // 1桁の月日でもパースできることを確認
        DateTime result = DateParser.Parse("R5.4.3");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(3, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYMD_2桁年() throws Exception {
        // 2桁の年でもパースできることを確認
        DateTime result = DateParser.Parse("R10.1.1");
        assertEquals(2028, result.getYear()); // 令和10年 = 2028年
        assertEquals(1, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYMD_全角数字() throws Exception {
        // 全角数字の変換確認
        DateTime result = DateParser.Parse("Ｒ３．１２．２５");
        assertEquals(2021, result.getYear());
        assertEquals(12, result.getMonthOfYear());
        assertEquals(25, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYMD_元年() throws Exception {
        // 元年表記の確認
        DateTime result = DateParser.Parse("R元.5.1");
        assertEquals(2019, result.getYear()); // 令和元年 = 2019年
        assertEquals(5, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYMD_無効な年号() {
        // 無効な年号でエラーが発生することを確認
        assertThrows(Exception.class, () -> {
            DateParser.Parse("X3.12.25");
        });
    }

    @Test
    void testParseWarekiYMD_無効な日付() {
        // 無効な日付（13月）でエラーが発生することを確認
        assertThrows(Exception.class, () -> {
            DateParser.Parse("R3.13.25");
        });
    }

    @Test
    void testParseWarekiYMD_元号と年の間に半角スペース() throws Exception {
        // 平 1・1・1 → 平成1年1月1日
        DateTime result = DateParser.Parse("平 1・1・1");
        assertEquals(1989, result.getYear());
        assertEquals(1, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYMD_全角数字と全角スペース() throws Exception {
        // 平 １・１・１ → 平成1年1月1日（全角数字・全角スペース混在）
        DateTime result = DateParser.Parse("平 １・１・１");
        assertEquals(1989, result.getYear());
        assertEquals(1, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYMD_元号直後のドットとスラッシュ区切り() throws Exception {
        // H.30/09/30 → 平成30年9月30日
        DateTime result = DateParser.Parse("H.30/09/30");
        assertEquals(2018, result.getYear());
        assertEquals(9, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYMD_令和のドットとスラッシュ区切り() throws Exception {
        // R.3/9/30 → 令和3年9月30日
        DateTime result = DateParser.Parse("R.3/9/30");
        assertEquals(2021, result.getYear());
        assertEquals(9, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYMD_全角ドット全角スラッシュ全角数字() throws Exception {
        // H．３０／０９／３０ → 平成30年9月30日（全角ドット・全角スラッシュ・全角数字）
        DateTime result = DateParser.Parse("H．３０／０９／３０");
        assertEquals(2018, result.getYear());
        assertEquals(9, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYMD_日付の後ろに全角文字が続く場合() throws Exception {
        // 平30・2・28新品取得 → 平成30年2月28日（日付の後ろの全角文字列は切り捨てる）
        DateTime result = DateParser.Parse("平30・2・28新品取得");
        assertEquals(2018, result.getYear());
        assertEquals(2, result.getMonthOfYear());
        assertEquals(28, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYM_日付の後ろに全角文字が続く場合() throws Exception {
        // H30.2新品 → 平成30年2月（年月のみ、日は1日固定）
        DateTime result = DateParser.Parse("H30.2新品");
        assertEquals(2018, result.getYear());
        assertEquals(2, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void testParseYYYYMMDD_日付の後ろに全角文字が続く場合() throws Exception {
        // 20230430備考欄 → 2023年4月30日
        DateTime result = DateParser.Parse("20230430備考欄");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void testParseYYYY_MM_DD_日付の後ろに全角文字が続く場合() throws Exception {
        // 2023-04-30備考 → 2023年4月30日
        DateTime result = DateParser.Parse("2023-04-30備考");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiYMD_日付の後ろに半角文字が続く場合は非対応() {
        // 半角英数字が続く場合は日付として認識しない（非日付データの誤変換を避けるため）
        assertThrows(Exception.class, () -> {
            DateParser.Parse("H30.2.28ABC");
        });
    }
}