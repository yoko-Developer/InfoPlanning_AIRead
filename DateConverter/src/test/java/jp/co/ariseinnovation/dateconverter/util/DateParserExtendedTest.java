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
}