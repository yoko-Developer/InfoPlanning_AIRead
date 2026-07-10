package jp.co.ariseinnovation.dateconverter.util;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DateParserのテストクラス
 */
class DateParserTest {

    @Test
    void testParseWareki() throws Exception {
        // 和暦のテスト
        DateTime result = DateParser.Parse("H10.5");
        assertEquals(1998, result.getYear());
        assertEquals(5, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void testParseWarekiWithDay() throws Exception {
        // 和暦（日付付き）のテスト
        DateTime result = DateParser.Parse("R3.12.25");
        assertEquals(2021, result.getYear());
        assertEquals(12, result.getMonthOfYear());
        assertEquals(25, result.getDayOfMonth());
    }

    @Test
    void testParseYYYYMMDD() throws Exception {
        // 西暦8桁のテスト
        DateTime result = DateParser.Parse("20231225");
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthOfYear());
        assertEquals(25, result.getDayOfMonth());
    }

    @Test
    void testParseYYYYMM() throws Exception {
        // 西暦6桁（月末）のテスト
        DateTime result = DateParser.Parse("202312末");
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthOfYear());
        assertEquals(31, result.getDayOfMonth()); // 12月の末日
    }

    @Test
    void testParseInvalidFormat() {
        // 無効なフォーマットのテスト
        assertThrows(Exception.class, () -> {
            DateParser.Parse("invalid");
        });
    }

    @Test
    void testParseNull() {
        // nullのテスト
        assertThrows(IllegalArgumentException.class, () -> {
            DateParser.Parse(null);
        });
    }

    @Test
    void testYYYY_MM_DD_形式() throws Exception {
        // YYYY-MM-DD形式の基本テスト
        DateTime result = DateParser.Parse("2023-04-30");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void testYYYY_MM_DD_形式_1桁月日() throws Exception {
        // 1桁の月・日
        DateTime result = DateParser.Parse("2023-4-5");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(5, result.getDayOfMonth());
    }

    @Test
    void testYYYY_MM_DD_形式_2桁月日() throws Exception {
        // 2桁の月・日
        DateTime result = DateParser.Parse("2023-12-25");
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthOfYear());
        assertEquals(25, result.getDayOfMonth());
    }

    @Test
    void testYYYY_MM_形式() throws Exception {
        // YYYY-MM形式の基本テスト
        DateTime result = DateParser.Parse("2023-04");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth()); // 日は1日固定
    }

    @Test
    void testYYYY_MM_形式_1桁月() throws Exception {
        // 1桁の月
        DateTime result = DateParser.Parse("2023-4");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void testYYYY_MM_形式_2桁月() throws Exception {
        // 2桁の月
        DateTime result = DateParser.Parse("2023-12");
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void test全角ハイフンの変換() throws Exception {
        // 全角ハイフン（－）を半角ハイフン（-）に変換
        DateTime result = DateParser.Parse("２０２３－０４－３０");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void test全角長音記号の変換() throws Exception {
        // 全角長音記号（ー）を半角ハイフン（-）に変換
        DateTime result = DateParser.Parse("２０２３ー０４ー３０");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void testダッシュの変換() throws Exception {
        // EMダッシュ（—）を半角ハイフン（-）に変換
        DateTime result = DateParser.Parse("2023—04—30");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void test様々な年の形式() throws Exception {
        // 1900年代
        DateTime result1 = DateParser.Parse("1998-05-15");
        assertEquals(1998, result1.getYear());
        assertEquals(5, result1.getMonthOfYear());
        assertEquals(15, result1.getDayOfMonth());

        // 2000年代初期
        DateTime result2 = DateParser.Parse("2000-01-01");
        assertEquals(2000, result2.getYear());
        assertEquals(1, result2.getMonthOfYear());
        assertEquals(1, result2.getDayOfMonth());

        // 2020年代
        DateTime result3 = DateParser.Parse("2024-12-31");
        assertEquals(2024, result3.getYear());
        assertEquals(12, result3.getMonthOfYear());
        assertEquals(31, result3.getDayOfMonth());
    }

    @Test
    void test月の境界値() throws Exception {
        // 1月
        DateTime result1 = DateParser.Parse("2023-01-15");
        assertEquals(1, result1.getMonthOfYear());

        // 12月
        DateTime result2 = DateParser.Parse("2023-12-31");
        assertEquals(12, result2.getMonthOfYear());
    }

    @Test
    void test日の境界値() throws Exception {
        // 1日
        DateTime result1 = DateParser.Parse("2023-04-01");
        assertEquals(1, result1.getDayOfMonth());

        // 28日（2月）
        DateTime result2 = DateParser.Parse("2023-02-28");
        assertEquals(28, result2.getDayOfMonth());

        // 30日
        DateTime result3 = DateParser.Parse("2023-04-30");
        assertEquals(30, result3.getDayOfMonth());

        // 31日
        DateTime result4 = DateParser.Parse("2023-05-31");
        assertEquals(31, result4.getDayOfMonth());
    }

    @Test
    void test既存形式との競合なし() throws Exception {
        // YYYYMMDD形式は既存通り動作
        DateTime result1 = DateParser.Parse("20230430");
        assertEquals(2023, result1.getYear());
        assertEquals(4, result1.getMonthOfYear());
        assertEquals(30, result1.getDayOfMonth());

        // 和暦形式も既存通り動作
        DateTime result2 = DateParser.Parse("H10.5.15");
        assertEquals(1998, result2.getYear());
        assertEquals(5, result2.getMonthOfYear());
        assertEquals(15, result2.getDayOfMonth());
    }

    @Test
    void testYYYY_MM_形式との区別() throws Exception {
        // YYYY-MM形式
        DateTime result1 = DateParser.Parse("2023-04");
        assertEquals(2023, result1.getYear());
        assertEquals(4, result1.getMonthOfYear());
        assertEquals(1, result1.getDayOfMonth()); // 日は1日

        // YYYY-MM-DD形式
        DateTime result2 = DateParser.Parse("2023-04-15");
        assertEquals(2023, result2.getYear());
        assertEquals(4, result2.getMonthOfYear());
        assertEquals(15, result2.getDayOfMonth()); // 日は指定された値
    }

    @Test
    void testParseYYYYMMDD_無効な月() {
        // 13月は存在しないためエラー
        assertThrows(Exception.class, () -> DateParser.Parse("20231301"));
    }

    @Test
    void testParseYYYYMMDD_無効な日() {
        // 1月32日は存在しないためエラー
        assertThrows(Exception.class, () -> DateParser.Parse("20230132"));
    }

    @Test
    void testParseYYYYMM末_無効な月() {
        // 13月は存在しないためエラー
        assertThrows(Exception.class, () -> DateParser.Parse("202313末"));
    }

    @Test
    void testParseYYYYMM末日_日付含む表記() throws Exception {
        // 「末日」表記でも「末」と同様に扱われること
        DateTime result = DateParser.Parse("202304末日");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void test句点区切り文字の変換() throws Exception {
        // 全角句点（。）が半角ドットに変換されて解析されること
        DateTime result = DateParser.Parse("R3。12。25");
        assertEquals(2021, result.getYear());
        assertEquals(12, result.getMonthOfYear());
        assertEquals(25, result.getDayOfMonth());
    }

    @Test
    void test中点区切り文字の変換() throws Exception {
        // 全角中点（・）が半角ドットに変換されて解析されること
        DateTime result = DateParser.Parse("R3・12・25");
        assertEquals(2021, result.getYear());
        assertEquals(12, result.getMonthOfYear());
        assertEquals(25, result.getDayOfMonth());
    }

    @Test
    void test元年表記_年月形式() throws Exception {
        // 年月形式でも元年表記が使えること
        DateTime result = DateParser.Parse("H元.5");
        assertEquals(1989, result.getYear()); // 平成元年 = 1989年
        assertEquals(5, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void testフルワイド元号L() throws Exception {
        // 全角Ｌ表記（令和の別表記）が解析できること
        DateTime result = DateParser.Parse("Ｌ３．４");
        assertEquals(2021, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void testParseEmptyString() {
        // 空文字列はFormatExceptionとなること
        assertThrows(jp.co.ariseinnovation.dateconverter.exception.FormatException.class,
                () -> DateParser.Parse(""));
    }

    @Test
    void testParseInvalidFormat_例外の型() {
        // パース不能な文字列はFormatExceptionとなること
        assertThrows(jp.co.ariseinnovation.dateconverter.exception.FormatException.class,
                () -> DateParser.Parse("invalid"));
    }

    @Test
    void testParseNull_例外の型() {
        // nullはIllegalArgumentExceptionとなること
        assertThrows(IllegalArgumentException.class, () -> DateParser.Parse(null));
    }

    @Test
    void testYYYY_MM_DD_ドット区切り() throws Exception {
        // 西暦の区切り文字がドットでも和暦と同様に解析できること
        DateTime result = DateParser.Parse("2023.04.30");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void testYYYY_MM_DD_スラッシュ区切り() throws Exception {
        DateTime result = DateParser.Parse("2023/04/30");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void testYYYY_MM_DD_カンマ区切り() throws Exception {
        DateTime result = DateParser.Parse("2023,04,30");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void testYYYY_MM_ドット区切り() throws Exception {
        // 区切り文字がドットの年月形式は日が1日固定になること
        DateTime result = DateParser.Parse("2023.04");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void testYYYY_MM_スラッシュ区切り() throws Exception {
        DateTime result = DateParser.Parse("2023/04");
        assertEquals(2023, result.getYear());
        assertEquals(4, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }
}
