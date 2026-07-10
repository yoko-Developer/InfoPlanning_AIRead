package jp.co.ariseinnovation.dateconverter.service;

import jp.co.ariseinnovation.dateconverter.enums.OutputFormat;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CsvDateConverterServiceのテストクラス
 */
@SpringBootTest
class CsvDateConverterServiceTest {

    @Autowired
    private CsvDateConverterService service;

    /**
     * 全ての元号形式の変換テスト
     */
    @Test
    void testConvertToYearMonth_AllGengoFormats() throws Exception {
        // リフレクションを使用してprivateメソッドにアクセス
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // アルファベット略称のテスト
        assertEquals("199805", method.invoke(service, "H10.5"));   // 平成10年5月
        assertEquals("198812", method.invoke(service, "S63.12"));  // 昭和63年12月
        assertEquals("202104", method.invoke(service, "R3.4"));    // 令和3年4月
        assertEquals("202104", method.invoke(service, "L3.4"));    // 令和3年4月（L表記）

        // 漢字フル表記のテスト
        assertEquals("199805", method.invoke(service, "平成10.5"));   // 平成10年5月
        assertEquals("198812", method.invoke(service, "昭和63.12")); // 昭和63年12月
        assertEquals("202104", method.invoke(service, "令和3.4"));   // 令和3年4月

        // 漢字1文字表記のテスト
        assertEquals("199805", method.invoke(service, "平10.5"));   // 平成10年5月
        assertEquals("198812", method.invoke(service, "昭63.12"));  // 昭和63年12月
        assertEquals("202104", method.invoke(service, "令3.4"));    // 令和3年4月
    }

    /**
     * 様々な区切り文字の変換テスト
     */
    @Test
    void testConvertToYearMonth_VariousSeparators() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // ドット区切り
        assertEquals("199805", method.invoke(service, "H10.5"));

        // ハイフン区切り
        assertEquals("199805", method.invoke(service, "H10-5"));

        // スラッシュ区切り
        assertEquals("199805", method.invoke(service, "H10/5"));

        // カンマ区切り
        assertEquals("199805", method.invoke(service, "H10,5"));

        // 年月文字付き
        assertEquals("199805", method.invoke(service, "平成10年5月"));
        assertEquals("199805", method.invoke(service, "H10年5月"));
        assertEquals("198812", method.invoke(service, "昭和63年12月"));

        // 区切り文字なし（連続数字）
        assertEquals("199805", method.invoke(service, "H105"));    // H10年5月として解釈（3桁）
        assertEquals("198812", method.invoke(service, "S6312"));   // S63年12月として解釈（4桁）
        assertEquals("199805", method.invoke(service, "H1005"));   // H10年05月として解釈（4桁）
        assertEquals("198905", method.invoke(service, "H15"));     // H1年5月として解釈（2桁）

        // 複数の区切り文字パターン（漢字表記）
        assertEquals("202104", method.invoke(service, "令和3-4"));
        assertEquals("202104", method.invoke(service, "令和3/4"));
        assertEquals("202104", method.invoke(service, "令和3,4"));   // カンマ区切り
        assertEquals("202104", method.invoke(service, "令和3年4月"));
    }

    /**
     * 無効な形式のテスト
     */
    @Test
    void testConvertToYearMonth_InvalidFormats() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // 無効な形式は元の文字列をそのまま返す
        assertEquals("", method.invoke(service, ""));
        assertEquals("invalid", method.invoke(service, "invalid"));
        assertEquals("H10", method.invoke(service, "H10"));        // 月なし
        assertEquals("H.5", method.invoke(service, "H.5"));        // 年なし
        assertEquals("X10.5", method.invoke(service, "X10.5"));    // 未対応元号
        assertEquals("H10.13", method.invoke(service, "H10.13"));  // 無効な月（13月）
        assertEquals("H10.0", method.invoke(service, "H10.0"));    // 無効な月（0月）
    }

    /**
     * null値のテスト
     */
    @Test
    void testConvertToYearMonth_NullValue() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        assertNull(method.invoke(service, (String) null));
    }

    /**
     * 境界値のテスト
     */
    @Test
    void testConvertToYearMonth_BoundaryValues() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // 1桁の年・月（様々な区切り文字）
        assertEquals("198901", method.invoke(service, "H1.1"));    // 平成1年1月
        assertEquals("198901", method.invoke(service, "H1-1"));    // 平成1年1月
        assertEquals("198901", method.invoke(service, "H1/1"));    // 平成1年1月
        assertEquals("198901", method.invoke(service, "H1,1"));    // 平成1年1月（カンマ区切り）
        assertEquals("198901", method.invoke(service, "H11"));     // H1年1月（2桁パターン）
        assertEquals("198901", method.invoke(service, "平成1年1月")); // 平成1年1月

        // 2桁の年・月（様々な区切り文字）
        assertEquals("201012", method.invoke(service, "H22.12"));  // 平成22年12月
        assertEquals("201012", method.invoke(service, "H22-12"));  // 平成22年12月
        assertEquals("201012", method.invoke(service, "H22/12"));  // 平成22年12月
        assertEquals("201012", method.invoke(service, "H22,12"));  // 平成22年12月（カンマ区切り）
        assertEquals("201012", method.invoke(service, "H2212"));   // H22年12月（4桁パターン）
        assertEquals("201012", method.invoke(service, "平成22年12月")); // 平成22年12月

        // 月の境界値
        assertEquals("198901", method.invoke(service, "H1.1"));    // 1月
        assertEquals("198912", method.invoke(service, "H1.12"));   // 12月
    }

    /**
     * スペース正規化のテスト
     */
    @Test
    void testSpaceNormalization() throws Exception {
        Method convertMethod = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        convertMethod.setAccessible(true);
        
        Method normalizeMethod = CsvDateConverterService.class.getDeclaredMethod("normalizeSpaces", String.class);
        normalizeMethod.setAccessible(true);

        // 半角スペースが削除されることを確認
        String normalized1 = (String) normalizeMethod.invoke(service, "H 10.5");
        assertEquals("199805", convertMethod.invoke(service, normalized1));   // 元号後のスペース
        
        String normalized2 = (String) normalizeMethod.invoke(service, "H10 .5");
        assertEquals("199805", convertMethod.invoke(service, normalized2));   // 区切り文字前のスペース
        
        String normalized3 = (String) normalizeMethod.invoke(service, "H10. 5");
        assertEquals("199805", convertMethod.invoke(service, normalized3));   // 区切り文字後のスペース
        
        String normalized4 = (String) normalizeMethod.invoke(service, "H 10 . 5");
        assertEquals("199805", convertMethod.invoke(service, normalized4)); // 複数箇所のスペース

        // 全角スペースが半角スペースに変換されてから削除されることを確認
        String normalized5 = (String) normalizeMethod.invoke(service, "H　10.5");
        assertEquals("199805", convertMethod.invoke(service, normalized5));   // 元号後の全角スペース
        
        String normalized6 = (String) normalizeMethod.invoke(service, "H10　.5");
        assertEquals("199805", convertMethod.invoke(service, normalized6));   // 区切り文字前の全角スペース
        
        String normalized7 = (String) normalizeMethod.invoke(service, "H10.　5");
        assertEquals("199805", convertMethod.invoke(service, normalized7));   // 区切り文字後の全角スペース
        
        String normalized8 = (String) normalizeMethod.invoke(service, "H　10　.　5");
        assertEquals("199805", convertMethod.invoke(service, normalized8)); // 複数箇所の全角スペース

        // 全角・半角スペース混在
        String normalized9 = (String) normalizeMethod.invoke(service, "H 10　.5");
        assertEquals("199805", convertMethod.invoke(service, normalized9));   // 半角・全角混在
        
        String normalized10 = (String) normalizeMethod.invoke(service, "H　10 . 5");
        assertEquals("199805", convertMethod.invoke(service, normalized10));  // 全角・半角混在

        // スペース削除により連続数字として解釈される場合
        String normalized11 = (String) normalizeMethod.invoke(service, "H 10 5");
        assertEquals("199805", convertMethod.invoke(service, normalized11));     // H 10 5 → H105 → H10年5月
        
        String normalized12 = (String) normalizeMethod.invoke(service, "H　10　5");
        assertEquals("199805", convertMethod.invoke(service, normalized12));   // 全角スペース版
    }

    /**
     * カンマ区切りの変換テスト
     */
    @Test
    void testConvertToYearMonth_CommaSeparator() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // アルファベット略称 + カンマ区切り
        assertEquals("199805", method.invoke(service, "H10,5"));     // 平成10年5月
        assertEquals("198812", method.invoke(service, "S63,12"));    // 昭和63年12月
        assertEquals("202104", method.invoke(service, "R3,4"));      // 令和3年4月
        assertEquals("202104", method.invoke(service, "L3,4"));      // 令和3年4月（L表記）

        // 漢字フル表記 + カンマ区切り
        assertEquals("199805", method.invoke(service, "平成10,5"));   // 平成10年5月
        assertEquals("198812", method.invoke(service, "昭和63,12")); // 昭和63年12月
        assertEquals("202104", method.invoke(service, "令和3,4"));   // 令和3年4月

        // 漢字1文字表記 + カンマ区切り
        assertEquals("199805", method.invoke(service, "平10,5"));    // 平成10年5月
        assertEquals("198812", method.invoke(service, "昭63,12"));   // 昭和63年12月
        assertEquals("202104", method.invoke(service, "令3,4"));     // 令和3年4月

        // 1桁の年・月
        assertEquals("198901", method.invoke(service, "H1,1"));      // 平成1年1月
        assertEquals("202612", method.invoke(service, "R8,12"));     // 令和8年12月
    }

    /**
     * 区切り文字なしの変換テスト
     */
    @Test
    void testConvertToYearMonth_NoSeparator() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // 4桁パターン（YYMM形式）
        assertEquals("199805", method.invoke(service, "H1005"));     // H10年05月
        assertEquals("198812", method.invoke(service, "S6312"));     // S63年12月
        assertEquals("202104", method.invoke(service, "R0304"));     // R03年04月
        assertEquals("202101", method.invoke(service, "L0301"));     // L03年01月

        // 3桁パターン（YYM形式）
        assertEquals("199805", method.invoke(service, "H105"));      // H10年5月
        assertEquals("198812", method.invoke(service, "S6312"));     // S63年12月（4桁として優先解釈）
        assertEquals("202104", method.invoke(service, "R034"));      // R03年4月
        assertEquals("202109", method.invoke(service, "L039"));      // L03年9月

        // 2桁パターン（YM形式）
        assertEquals("198905", method.invoke(service, "H15"));       // H1年5月
        assertEquals("193602", method.invoke(service, "S112"));      // S11年2月（3桁として優先解釈）
        assertEquals("201909", method.invoke(service, "R19"));       // R1年9月
        assertEquals("201901", method.invoke(service, "L11"));       // L1年1月

        // 漢字元号での区切り文字なしパターン
        assertEquals("199805", method.invoke(service, "平成1005"));   // 平成10年05月
        assertEquals("198812", method.invoke(service, "昭和6312"));   // 昭和63年12月
        assertEquals("202104", method.invoke(service, "令和0304"));   // 令和03年04月
        assertEquals("199805", method.invoke(service, "平105"));      // 平10年5月（3桁判定）
        assertEquals("192605", method.invoke(service, "昭15"));       // 昭1年5月
    }

    /**
     * 3桁数字の判定ロジックテスト
     */
    @Test
    void testConvertToYearMonth_3DigitLogic() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // (1) N2N3が13以上であれば、YYM（年年月）
        assertEquals("H1015", method.invoke(service, "H1015"));     // H10年15月 → 無効なので変換されない
        assertEquals("H1013", method.invoke(service, "H1013"));     // H10年13月 → 無効なので変換されない
        
        // 有効な月での13以上テスト（実際は13以上は無効月なので、YYMパターンで3桁目を月とする）
        assertEquals("199805", method.invoke(service, "H105"));      // H10年5月（YYMパターン）
        assertEquals("199812", method.invoke(service, "H1012"));     // H10年12月（YYMパターン）

        // (2) 元号+N1N2がシステム日付よりも未来であれば、YMM（年月月）
        // 現在が2024年の場合、令和50年（2068年）は未来なので、YMMパターンを試行
        // ただし、実際の判定は現在のシステム日付に依存するため、具体的な値は環境による
        
        // (3) (1)(2)以外なら、YYM（年年月）
        assertEquals("199805", method.invoke(service, "H105"));      // H10年5月（YYMパターン）
        assertEquals("198312", method.invoke(service, "S5812"));     // S58年12月（4桁パターン）
        assertEquals("202109", method.invoke(service, "R039"));      // R03年9月（YYMパターン）

        // 境界値テスト
        assertEquals("199801", method.invoke(service, "H101"));      // H10年1月（YYMパターン）
        assertEquals("199812", method.invoke(service, "H1012"));     // H10年12月（YYMパターン）
        
        // 無効な月の場合（変換されない）
        assertEquals("H1013", method.invoke(service, "H1013"));      // H10年13月 → 無効月、変換されない
        assertEquals("H1000", method.invoke(service, "H1000"));      // H10年00月 → 無効月、変換されない
    }

    /**
     * 5桁・6桁の区切りなしパターン（日付精度あり）の変換テスト
     * デフォルト出力形式（YYYYMMDD）で日付まで出力されることを確認
     */
    @Test
    void testConvertToYearMonth_5桁6桁パターンの日付精度() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        assertEquals("19980530", method.invoke(service, "H10530"));  // 5桁：10年5月30日
        assertEquals("19980515", method.invoke(service, "H100515")); // 6桁：10年05月15日
    }

    /**
     * 拡張区切り文字パターン（年月3-4桁+区切り+日、年+区切り+月日3-4桁）の変換テスト
     */
    @Test
    void testConvertToYearMonth_拡張区切り文字パターン() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        assertEquals("19980530", method.invoke(service, "H105.30"));   // 年月(3桁)+区切り+日
        assertEquals("19980530", method.invoke(service, "H1005.30")); // 年月(4桁)+区切り+日
        assertEquals("19980530", method.invoke(service, "H10.530"));  // 年+区切り+月日(3桁)
        assertEquals("19980530", method.invoke(service, "H10.0530")); // 年+区切り+月日(4桁)
    }

    /**
     * 漢字の年月日パターン（日付付き）の変換テスト
     */
    @Test
    void testConvertToYearMonth_漢字年月日パターン() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        assertEquals("19980515", method.invoke(service, "平成10年5月15日"));
        assertEquals("19980515", method.invoke(service, "H10年5月15日"));
    }

    /**
     * 全角元号・全角区切り文字はGengoYearTableの直接マッチに失敗し、
     * DateParserへのフォールバック経由で変換されることを確認する
     * （findMatchingGengoは半角の元号文字列・区切り文字クラスのみを対象とするため）
     */
    @Test
    void testConvertToYearMonth_DateParserフォールバック経由の変換() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        assertEquals("199805", method.invoke(service, "Ｈ１０．５")); // 全角元号・全角数字
        assertEquals("199805", method.invoke(service, "H10。5"));    // 全角句点区切り
        assertEquals("199805", method.invoke(service, "H10・5"));    // 全角中点区切り
    }

    /**
     * 3桁パターンの「未来判定」ロジック（元号+N1がシステム日付より未来ならYMM解釈）の動的検証
     * 実行日時に依存するため、現在の令和年から必ず未来になる値を動的に算出して検証する
     */
    @Test
    void test3桁パターン_未来判定ロジックYMM() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        int currentYear = LocalDate.now().getYear();
        int n1 = currentYear - 2017; // 令和(2018+n1) = currentYear+1 となり必ず未来
        Assumptions.assumeTrue(n1 >= 0 && n1 <= 9,
                "n1が1桁の範囲外のため、この実行環境では未来判定分岐を検証できません");

        String input = "R" + n1 + "06"; // n2n3=06(<13かつ有効な月)
        String expected = String.format("%d%02d", 2018 + n1, 6);

        assertEquals(expected, method.invoke(service, input));
    }

    /**
     * 出力形式（OutputFormat）と入力精度の組み合わせテスト（サービス層での2引数メソッド経由）
     */
    @Test
    void testConvertToYearMonth_出力形式と精度の組み合わせ() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod(
                "convertToYearMonth", String.class, OutputFormat.class);
        method.setAccessible(true);

        // 日付精度ありの入力：指定形式がそのまま使われる
        assertEquals("19980515", method.invoke(service, "H10.5.15", OutputFormat.YYYYMMDD));
        assertEquals("199805", method.invoke(service, "H10.5.15", OutputFormat.YYYYMM));

        // 年月精度のみの入力：YYYYMMDDを指定してもYYYYMMにダウングレードされる
        assertEquals("199805", method.invoke(service, "H10.5", OutputFormat.YYYYMMDD));
        assertEquals("199805", method.invoke(service, "H10.5", OutputFormat.YYYYMM));
    }

    /**
     * 8桁YYYYMMDD形式は日の値（01であっても）に関わらず年月日精度として扱われることを確認するテスト
     * （修正前は日が01の場合にgetDayOfMonth() != 1の判定で年月精度に誤ってダウングレードされていた）
     */
    @Test
    void testConvertToYearMonth_8桁YYYYMMDDは日の値に関わらず正しく変換される() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod(
                "convertToYearMonth", String.class, OutputFormat.class);
        method.setAccessible(true);

        assertEquals("20230401", method.invoke(service, "20230401", OutputFormat.YYYYMMDD));   // 日=01でも年月日精度
        assertEquals("20230415", method.invoke(service, "20230415", OutputFormat.YYYYMMDD));   // 日≠1も年月日精度
        assertEquals("20230401", method.invoke(service, "2023-04-01", OutputFormat.YYYYMMDD)); // ハイフン区切りも同様
        assertEquals("202304", method.invoke(service, "20230401", OutputFormat.YYYYMM));       // YYYYMM指定時はYYYYMM
    }

    /**
     * normalizeExtraSeparators単体の変換テスト
     */
    @Test
    void testNormalizeExtraSeparators_単一文字の変換() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod(
                "normalizeExtraSeparators", String.class, String.class);
        method.setAccessible(true);

        assertEquals("H10.5", method.invoke(service, "H10／5", "／"));   // 全角スラッシュ→ドット
        assertEquals("R3.4.30", method.invoke(service, "R3~4~30", "~")); // チルダ→ドット
    }

    @Test
    void testNormalizeExtraSeparators_複数文字の変換() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod(
                "normalizeExtraSeparators", String.class, String.class);
        method.setAccessible(true);

        // 複数の追加区切り文字が混在していても、指定した集合に含まれる文字は全てドットに変換される
        assertEquals("R3.4.30", method.invoke(service, "R3／4~30", "／~"));
        assertEquals("H10.5", method.invoke(service, "H10~5", "／~"));
    }

    @Test
    void testNormalizeExtraSeparators_集合に含まれない文字は変換されない() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod(
                "normalizeExtraSeparators", String.class, String.class);
        method.setAccessible(true);

        // "~"のみを追加区切り文字として指定した場合、"／"は変換されない
        assertEquals("H10／5", method.invoke(service, "H10／5", "~"));
    }

    @Test
    void testNormalizeExtraSeparators_空文字またはnullは無変換() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod(
                "normalizeExtraSeparators", String.class, String.class);
        method.setAccessible(true);

        assertEquals("H10／5", method.invoke(service, "H10／5", ""));
        assertEquals("H10／5", method.invoke(service, "H10／5", (String) null));
    }

    @Test
    void testNormalizeExtraSeparators_srcがnullの場合はnullを返す() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod(
                "normalizeExtraSeparators", String.class, String.class);
        method.setAccessible(true);

        assertNull(method.invoke(service, (String) null, "／"));
    }

    @Test
    void testNormalizeExtraSeparators_既存の区切り文字を指定しても影響なし() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod(
                "normalizeExtraSeparators", String.class, String.class);
        method.setAccessible(true);

        // 既に標準でサポートされている「-」を追加区切り文字に指定しても、
        // 変換先が同じ「.」なので結果は変わらない
        assertEquals("H10.5", method.invoke(service, "H10-5", "-"));
    }

    /**
     * 西暦形式は和暦と同様に複数の区切り文字（. - / ,）に対応するべきであり、
     * ハイフン区切り（YYYY-MM-DD）と同じ意図でドット・スラッシュ・カンマ区切りも
     * 変換されることを確認するテスト。
     */
    @Test
    void testConvertToYearMonth_西暦は複数の区切り文字に対応する() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        assertEquals("20230430", method.invoke(service, "2023.04.30"));
        assertEquals("202304", method.invoke(service, "2023.04"));
        assertEquals("20230430", method.invoke(service, "2023/04/30"));
        assertEquals("202304", method.invoke(service, "2023/04"));
        assertEquals("20230430", method.invoke(service, "2023,04,30"));
        assertEquals("202304", method.invoke(service, "2023,04"));
    }

    /**
     * --extra-separatorsは和暦だけでなく西暦形式にも同様に効くことを確認するテスト
     * （区切り文字を「.」に正規化する仕組みが西暦区切り文字パターンにも一般化されているため）
     */
    @Test
    void testConvertToYearMonth_追加区切り文字は西暦形式にも効く() throws Exception {
        Method normalizeMethod = CsvDateConverterService.class.getDeclaredMethod(
                "normalizeExtraSeparators", String.class, String.class);
        normalizeMethod.setAccessible(true);
        Method convertMethod = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        convertMethod.setAccessible(true);

        String normalized = (String) normalizeMethod.invoke(service, "2023／04／30", "／");
        assertEquals("20230430", convertMethod.invoke(service, normalized));
    }

    /**
     * 意図検証テスト：実在しないカレンダー日付（2月30日等）は、和暦の年月日形式であっても
     * 変換されるべきではない（無効な日付として拒否されるべき）。
     * 日は1〜31の範囲チェックのみで、月ごとの実際の日数（2月28/29日、4/6/9/11月30日）を
     * 検証していない疑いがあり、本テストで実際の挙動を確認する。
     */
    @Test
    void testConvertToYearMonth_実在しないカレンダー日付は変換されるべきではない() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // 平成10年（1998年、平年）の2月30日は存在しない → 変換されず元の文字列のまま返るべき
        assertEquals("H10.2.30", method.invoke(service, "H10.2.30"));
        // 平成11年（1999年、平年）の2月29日も存在しない → 変換されるべきではない
        assertEquals("H11.2.29", method.invoke(service, "H11.2.29"));
        // 令和6年（2024年、閏年）の2月29日は実在する → 正しく変換されるべき
        assertEquals("20240229", method.invoke(service, "R6.2.29"));
        // 4月31日は存在しない（4月は30日まで） → 変換されるべきではない
        assertEquals("H10.4.31", method.invoke(service, "H10.4.31"));
    }

    /**
     * カレンダー妥当性チェックが、明示的な区切り文字だけでなく、
     * 区切り文字なし6桁（YYMMDD）等の圧縮パターン経由でも同様に効くことを確認する。
     * （extractYearMonthDayのどの分岐を通っても、convertWarekiToYearMonthの検証を必ず通るため）
     */
    @Test
    void testConvertToYearMonth_圧縮パターンでもカレンダー妥当性チェックが効く() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // H100230 → 平成10年02月30日（区切りなし6桁）は実在しないため変換されるべきではない
        assertEquals("H100230", method.invoke(service, "H100230"));
        // H100228 → 平成10年02月28日（1998年、平年）は実在するため正しく変換されるべき
        assertEquals("19980228", method.invoke(service, "H100228"));
    }

    /**
     * 西暦の区切り文字拡張（ドット等）経由でも、カレンダー妥当性チェック
     * （DateParser内部のJoda DateTimeによる検証）が引き続き効くことを確認する。
     */
    @Test
    void testConvertToYearMonth_西暦ドット区切りでもカレンダー妥当性チェックが効く() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // 2023年2月30日は存在しない → 変換されるべきではない
        assertEquals("2023.02.30", method.invoke(service, "2023.02.30"));
        // 2023年2月28日は実在する → 正しく変換されるべき
        assertEquals("20230228", method.invoke(service, "2023.02.28"));
    }

    /**
     * うるう年の世紀ルール（100で割り切れるが400では割り切れない年は平年、
     * 400で割り切れる年は閏年）が正しく扱われることを確認する。
     * 令和・平成・昭和のいずれの元号年からも到達しない年のため、西暦形式で直接検証する。
     */
    @Test
    void testConvertToYearMonth_うるう年の世紀ルール() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // 2000年は400で割り切れるため閏年 → 2月29日は実在する
        assertEquals("20000229", method.invoke(service, "2000.02.29"));
        // 1900年は100で割り切れるが400では割り切れないため平年 → 2月29日は存在しない
        assertEquals("1900.02.29", method.invoke(service, "1900.02.29"));
        // 平成12年（2000年）でも同様に閏年として扱われることを確認
        assertEquals("20000229", method.invoke(service, "H12.2.29"));
    }

    /**
     * 年月精度パターンの検証：「末」「日」サフィックスの無い裸の6桁（202312）が、
     * ハイフン区切りの年月形式（2023-12）と同じ「年月精度」として扱われるかを確認する。
     *
     * 実際には、裸の6桁はYYYYMM_PATTERN（末?日?は両方オプション）にマッチし、
     * DateParserは月末日（12月なら31日）を補完した完全な日付として解析するため、
     * 「日が1日でない」＝日付精度ありと判定され、ハイフン区切り版とは異なる挙動になる。
     * これは意図的な「月末」変換の副作用であり、バグではなく仕様上の非対称性として記録する。
     */
    @Test
    void testConvertToYearMonth_裸の6桁とハイフン区切り年月の非対称性() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod(
                "convertToYearMonth", String.class, OutputFormat.class);
        method.setAccessible(true);

        // ハイフン区切り「2023-12」は年月精度（日は1日固定）と判定され、YYYYMMDD要求でもYYYYMMにダウングレードされる
        assertEquals("202312", method.invoke(service, "2023-12", OutputFormat.YYYYMMDD));

        // 裸の6桁「202312」は「末」の暗黙変換により月末日（31日）付きの日付精度ありと判定され、
        // YYYYMMDD要求時はダウングレードされず月末日が出力される
        assertEquals("20231231", method.invoke(service, "202312", OutputFormat.YYYYMMDD));

        // 「末」を明示した場合も同じ結果になることを確認（暗黙・明示で挙動が変わらないこと）
        assertEquals("20231231", method.invoke(service, "202312末", OutputFormat.YYYYMMDD));
    }

    /**
     * 西暦の区切り文字拡張（ドット等）と--extra-separatorsの組み合わせで、
     * 年月精度（日を伴わない）の入力が正しくダウングレードされることを確認する。
     */
    @Test
    void testConvertToYearMonth_西暦年月精度と追加区切り文字の組み合わせ() throws Exception {
        Method normalizeMethod = CsvDateConverterService.class.getDeclaredMethod(
                "normalizeExtraSeparators", String.class, String.class);
        normalizeMethod.setAccessible(true);
        Method convertMethod = CsvDateConverterService.class.getDeclaredMethod(
                "convertToYearMonth", String.class, OutputFormat.class);
        convertMethod.setAccessible(true);

        // 全角スラッシュを追加区切り文字とした西暦年月のみの入力
        String normalized = (String) normalizeMethod.invoke(service, "2023／04", "／");
        assertEquals("202304", convertMethod.invoke(service, normalized, OutputFormat.YYYYMMDD));
        assertEquals("202304", convertMethod.invoke(service, normalized, OutputFormat.YYYYMM));
    }

    /**
     * 漢字の年月パターンで「月」が省略された場合（H10年5、平成10年5等）でも
     * 正しく年月精度として変換されることを確認する。
     * PATTERN_YEAR_MONTH_KANJIの「月」は正規表現上オプション（月?）であるため。
     */
    @Test
    void testConvertToYearMonth_漢字年月パターンで月が省略された場合() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        assertEquals("199805", method.invoke(service, "H10年5"));
        assertEquals("199805", method.invoke(service, "平成10年5"));
    }

    /**
     * 圧縮パターン（区切り文字なし）の年月精度で、2引数メソッドに明示的にYYYYMMを
     * 要求した場合でも正しく変換されることを確認する（デフォルト経由のダウングレードに頼らない検証）。
     */
    @Test
    void testConvertToYearMonth_圧縮パターンでYYYYMMを明示要求() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod(
                "convertToYearMonth", String.class, OutputFormat.class);
        method.setAccessible(true);

        assertEquals("199805", method.invoke(service, "H105", OutputFormat.YYYYMM));
        assertEquals("199805", method.invoke(service, "H105", OutputFormat.YYYYMMDD)); // 年月精度のためダウングレード
        assertEquals("198905", method.invoke(service, "H15", OutputFormat.YYYYMM));
        assertEquals("198905", method.invoke(service, "H15", OutputFormat.YYYYMMDD)); // 年月精度のためダウングレード
    }

    /**
     * カバレッジ計測（JaCoCo）で未実行と判明した分岐を狙うテスト群：
     * 3桁月日（MMD/MDD）のあいまい解決で、MM+D解釈が無効でM+DD解釈が有効な場合。
     */
    @Test
    void testConvertToYearMonth_3桁月日でMMD解釈が無効ならMDD解釈が採用される() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // "131" → MM+D＝13月1日（無効月）→ M+DD＝1月31日（有効）で解釈される
        assertEquals("19980131", method.invoke(service, "H10.131"));
    }

    /**
     * 3桁月日で両方の解釈（MM+D・M+DD）とも無効な場合は変換されず元の値のまま返る。
     */
    @Test
    void testConvertToYearMonth_3桁月日で両解釈とも無効なら変換されない() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // "990" → MM+D＝99月0日（無効）、M+DD＝9月90日（無効）→ 変換不可
        assertEquals("H10.990", method.invoke(service, "H10.990"));
    }

    /**
     * 4桁月日（MMDD）が無効な月日の組み合わせの場合は変換されない。
     */
    @Test
    void testConvertToYearMonth_4桁月日が無効なら変換されない() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // "1332" → 13月32日（無効）
        assertEquals("H10.1332", method.invoke(service, "H10.1332"));
    }

    /**
     * 拡張年月パターン（年月3-4桁＋区切り＋日）で、月または日が無効な場合は変換されない。
     */
    @Test
    void testConvertToYearMonth_拡張年月パターンで無効な月日は変換されない() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // 4桁年月 "1013" → 10年13月（無効月）
        assertEquals("H1013.5", method.invoke(service, "H1013.5"));
        // 3桁年月 "105"（10年5月）は有効だが日=32が無効
        assertEquals("H105.32", method.invoke(service, "H105.32"));
    }

    /**
     * 3桁数字パターンの未実行分岐：3桁目（月）が0の場合は変換されない。
     */
    @Test
    void testConvertToYearMonth_3桁数字で月が0なら変換されない() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // "100" → n2n3=00（<13）→ 過去年 → YYM解釈でn3=0が無効月 → 変換不可
        assertEquals("H100", method.invoke(service, "H100"));
        // "990" → n2n3=90（≥13）→ YYM解釈でn3=0が無効月 → 変換不可
        assertEquals("H990", method.invoke(service, "H990"));
    }

    /**
     * 3桁月日の境界分岐：月が0（"001"）は両解釈とも無効、
     * MM+D解釈の日が0（"120"）はM+DD解釈（1月20日）にフォールバックされる。
     */
    @Test
    void testConvertToYearMonth_3桁月日の月0と日0の境界() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        // "001" → MM+D＝00月1日（無効）、M+DD＝0月01日（無効）→ 変換不可
        assertEquals("H10.001", method.invoke(service, "H10.001"));
        // "120" → MM+D＝12月0日（日が無効）→ M+DD＝1月20日（有効）で解釈される
        assertEquals("19980120", method.invoke(service, "H10.120"));
    }

    /**
     * 4桁月日の境界分岐：月は有効だが日が0または32の場合は変換されない。
     */
    @Test
    void testConvertToYearMonth_4桁月日の日の境界() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        assertEquals("H10.1200", method.invoke(service, "H10.1200")); // 12月0日（無効）
        assertEquals("H10.1232", method.invoke(service, "H10.1232")); // 12月32日（無効）
    }

    /**
     * 拡張年月パターン（年月3-4桁＋区切り＋日）の境界分岐：
     * 年月部分が解釈不能、月が0、日が0/32の場合はいずれも変換されない。
     */
    @Test
    void testConvertToYearMonth_拡張年月パターンの境界分岐() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        assertEquals("H990.5", method.invoke(service, "H990.5"));   // 3桁年月"990"が解釈不能
        assertEquals("H105.0", method.invoke(service, "H105.0"));   // 年月は有効だが日=0
        assertEquals("H1000.5", method.invoke(service, "H1000.5")); // 4桁年月：10年00月（無効）
        assertEquals("H1012.0", method.invoke(service, "H1012.0")); // 4桁年月は有効だが日=0
        assertEquals("H1012.32", method.invoke(service, "H1012.32")); // 4桁年月は有効だが日=32
    }

    /**
     * 3桁数字パターン：元号年が未来でも月部分（N2N3）が00なら変換されない。
     * （未来判定がtrue/falseのどちらでも、最終的にn3=0が無効月のため結果は不変＝実行日非依存）
     */
    @Test
    void testConvertToYearMonth_3桁数字で未来年でも月00は変換されない() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        assertEquals("R900", method.invoke(service, "R900"));
    }

    /**
     * 防御的分岐の契約固定テスト：normalizeSpacesはnull入力に対してnullを返し、
     * isTargetColumnはnull項目名に対してfalseを返す（NullPointerExceptionを投げない）。
     */
    @Test
    void test防御的null分岐の契約() throws Exception {
        Method normalizeMethod = CsvDateConverterService.class.getDeclaredMethod("normalizeSpaces", String.class);
        normalizeMethod.setAccessible(true);
        assertNull(normalizeMethod.invoke(service, (String) null));

        Method targetMethod = CsvDateConverterService.class.getDeclaredMethod(
                "isTargetColumn", String.class, java.util.List.class);
        targetMethod.setAccessible(true);
        assertEquals(Boolean.FALSE, targetMethod.invoke(service, null, java.util.List.of("取得")));
    }

    /**
     * 不正入力に対する堅牢性テスト：日付として解釈できない特殊文字（制御文字・絵文字等）を
     * 含む値でも、例外を投げてクラッシュすることなく元の文字列を返すべき。
     */
    @Test
    void testConvertToYearMonth_特殊文字を含む入力でも例外を投げない() {
        Method method = null;
        try {
            method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            fail(e);
        }
        Method finalMethod = method;

        // 絵文字混じりの値
        assertDoesNotThrow(() -> finalMethod.invoke(service, "R3.4.😀"));
        // 制御文字（NUL文字）を含む値
        assertDoesNotThrow(() -> finalMethod.invoke(service, "H10.5\u0000"));
        // 極端に長い文字列（パフォーマンス上のクラッシュがないことも兼ねて確認）
        assertDoesNotThrow(() -> finalMethod.invoke(service, "H".repeat(1000) + "10.5"));
        // サロゲートペア単体（不正なUTF-16シーケンス）
        assertDoesNotThrow(() -> finalMethod.invoke(service, "H10.5\uD800"));
    }

    /**
     * 月の妥当性チェック：明示的な区切り文字がある年月日形式でも、
     * 無効な月（0月・13月）は変換されるべきではない。
     */
    @Test
    void testConvertToYearMonth_区切り文字ありでも無効な月は変換されない() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        assertEquals("H10.0.15", method.invoke(service, "H10.0.15"));  // 0月は無効
        assertEquals("H10.13.15", method.invoke(service, "H10.13.15")); // 13月は無効
        assertEquals("H10.0", method.invoke(service, "H10.0"));         // 0月は無効（年月形式）
        assertEquals("H10.13", method.invoke(service, "H10.13"));       // 13月は無効（年月形式）
    }

    /**
     * 日の妥当性チェック：明示的な区切り文字がある年月日形式でも、
     * 無効な日（0日・32日）は変換されるべきではない。
     */
    @Test
    void testConvertToYearMonth_区切り文字ありでも無効な日は変換されない() throws Exception {
        Method method = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        method.setAccessible(true);

        assertEquals("H10.5.0", method.invoke(service, "H10.5.0"));   // 0日は無効
        assertEquals("H10.5.32", method.invoke(service, "H10.5.32")); // 32日は無効
    }

    /**
     * 追加区切り文字の正規化と実際の日付変換を組み合わせたテスト
     * （CsvDateConverterService内の処理順序: normalizeExtraSeparators → convertToYearMonth を模擬）
     */
    @Test
    void testNormalizeExtraSeparatorsと変換の組み合わせ() throws Exception {
        Method normalizeMethod = CsvDateConverterService.class.getDeclaredMethod(
                "normalizeExtraSeparators", String.class, String.class);
        normalizeMethod.setAccessible(true);
        Method convertMethod = CsvDateConverterService.class.getDeclaredMethod("convertToYearMonth", String.class);
        convertMethod.setAccessible(true);

        // 全角スラッシュを追加区切り文字として、年月のみの和暦を変換
        String normalized1 = (String) normalizeMethod.invoke(service, "H10／5", "／");
        assertEquals("199805", convertMethod.invoke(service, normalized1));

        // チルダを追加区切り文字として、年月日の和暦（令和）を変換
        String normalized2 = (String) normalizeMethod.invoke(service, "R3~4~30", "~");
        assertEquals("20210430", convertMethod.invoke(service, normalized2));

        // 追加区切り文字が指定されていない場合、全角スラッシュはそのまま残り変換されない
        String normalized3 = (String) normalizeMethod.invoke(service, "H10／5", "");
        assertEquals("H10／5", convertMethod.invoke(service, normalized3));
    }
}