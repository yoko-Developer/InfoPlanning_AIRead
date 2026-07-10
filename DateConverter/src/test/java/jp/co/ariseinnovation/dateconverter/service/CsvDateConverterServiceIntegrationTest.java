package jp.co.ariseinnovation.dateconverter.service;

import jp.co.ariseinnovation.dateconverter.enums.OutputFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CsvDateConverterServiceの統合テストクラス
 * 実際のCSVファイルを使用して出力形式の動作を確認
 */
@SpringBootTest
public class CsvDateConverterServiceIntegrationTest {

    private CsvDateConverterService service = new CsvDateConverterService();

    @Test
    public void testOutputFormat_DatePrecisionInput_YYYYMMFormat(@TempDir Path tempDir) throws Exception {
        // 日付精度の入力データでYYYYMM形式を指定した場合のテスト
        
        // テスト用CSVファイルを作成
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");
        
        // 日付精度のデータを含むCSVを作成
        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,R5.4.30",  // 年月日精度
            "使用開始日,データ4,データ5,データ6,H10.5.15", // 年月日精度
            "供用開始日,データ7,データ8,データ9,昭和63.12.31" // 年月日精度
        );
        Files.write(inputFile, inputLines);
        
        // YYYYMM形式で変換実行
        int convertedCount = service.convertCsvFile(
            inputFile.toString(),
            outputFile.toString(),
            Arrays.asList("取得日", "使用開始日", "供用開始日"),
            "UTF-8",
            OutputFormat.YYYYMM
        );
        
        // 結果を検証
        assertEquals(3, convertedCount);
        
        List<String> outputLines = Files.readAllLines(outputFile);
        assertEquals(4, outputLines.size());
        
        // ヘッダー行は変更されない
        assertEquals("\"項目名\",\"列2\",\"列3\",\"列4\",\"日付データ\"", outputLines.get(0));
        
        // 日付精度の入力でもYYYYMM形式で出力されることを確認
        assertTrue(outputLines.get(1).contains("\"202304\"")); // R5.4.30 → 202304
        assertTrue(outputLines.get(2).contains("\"199805\"")); // H10.5.15 → 199805
        assertTrue(outputLines.get(3).contains("\"198812\"")); // 昭和63.12.31 → 198812
    }

    @Test
    public void testOutputFormat_DatePrecisionInput_YYYYMMDDFormat(@TempDir Path tempDir) throws Exception {
        // 日付精度の入力データでYYYYMMDD形式を指定した場合のテスト
        
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");
        
        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,R5.4.30",
            "使用開始日,データ4,データ5,データ6,H10.5.15"
        );
        Files.write(inputFile, inputLines);
        
        // YYYYMMDD形式で変換実行
        int convertedCount = service.convertCsvFile(
            inputFile.toString(),
            outputFile.toString(),
            Arrays.asList("取得日", "使用開始日"),
            "UTF-8",
            OutputFormat.YYYYMMDD
        );
        
        // 結果を検証
        assertEquals(2, convertedCount);
        
        List<String> outputLines = Files.readAllLines(outputFile);
        
        // 日付精度の入力でYYYYMMDD形式で出力されることを確認
        assertTrue(outputLines.get(1).contains("\"20230430\"")); // R5.4.30 → 20230430
        assertTrue(outputLines.get(2).contains("\"19980515\"")); // H10.5.15 → 19980515
    }

    @Test
    public void testOutputFormat_MonthPrecisionInput_YYYYMMDDFormat(@TempDir Path tempDir) throws Exception {
        // 年月精度の入力データでYYYYMMDD形式を指定した場合のテスト
        // （入力精度により自動的にYYYYMM形式になることを確認）
        
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");
        
        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,R5.4",    // 年月精度
            "使用開始日,データ4,データ5,データ6,H10.5"  // 年月精度
        );
        Files.write(inputFile, inputLines);
        
        // YYYYMMDD形式を指定するが、入力精度により自動的にYYYYMM形式になる
        int convertedCount = service.convertCsvFile(
            inputFile.toString(),
            outputFile.toString(),
            Arrays.asList("取得日", "使用開始日"),
            "UTF-8",
            OutputFormat.YYYYMMDD
        );
        
        // 結果を検証
        assertEquals(2, convertedCount);
        
        List<String> outputLines = Files.readAllLines(outputFile);
        
        // 年月精度の入力では、YYYYMMDDを指定してもYYYYMM形式で出力されることを確認
        assertTrue(outputLines.get(1).contains("\"202304\"")); // R5.4 → 202304（20230401ではない）
        assertTrue(outputLines.get(2).contains("\"199805\"")); // H10.5 → 199805（19980501ではない）
    }

    @Test
    public void testOutputFormat_西暦年月精度でもYYYYMMにダウングレードされる(@TempDir Path tempDir) throws Exception {
        // 西暦の区切り文字拡張（ドット・スラッシュ）でも、和暦と同様に
        // 年月精度の場合はYYYYMMDDを指定してもYYYYMM形式にダウングレードされることをCSV経由で確認する

        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,2023.04",  // 西暦年月精度（ドット区切り）
            "使用開始日,データ4,データ5,データ6,2023/05" // 西暦年月精度（スラッシュ区切り）
        );
        Files.write(inputFile, inputLines);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(),
            outputFile.toString(),
            Arrays.asList("取得日", "使用開始日"),
            "UTF-8",
            OutputFormat.YYYYMMDD
        );

        assertEquals(2, convertedCount);

        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("\"202304\""), "実際の出力: " + outputLines.get(1));
        assertTrue(outputLines.get(2).contains("\"202305\""), "実際の出力: " + outputLines.get(2));
    }

    @Test
    public void testOutputFormat_MixedPrecisionInput(@TempDir Path tempDir) throws Exception {
        // 年月精度と年月日精度が混在する場合のテスト
        
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");
        
        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,R5.4",      // 年月精度
            "使用開始日,データ4,データ5,データ6,H10.5.15", // 年月日精度
            "供用開始日,データ7,データ8,データ9,昭和63.12"  // 年月精度
        );
        Files.write(inputFile, inputLines);
        
        // YYYYMM形式で変換実行
        int convertedCount = service.convertCsvFile(
            inputFile.toString(),
            outputFile.toString(),
            Arrays.asList("取得日", "使用開始日", "供用開始日"),
            "UTF-8",
            OutputFormat.YYYYMM
        );
        
        // 結果を検証
        assertEquals(3, convertedCount);
        
        List<String> outputLines = Files.readAllLines(outputFile);
        
        // すべてYYYYMM形式で出力されることを確認
        assertTrue(outputLines.get(1).contains("\"202304\"")); // R5.4 → 202304
        assertTrue(outputLines.get(2).contains("\"199805\"")); // H10.5.15 → 199805
        assertTrue(outputLines.get(3).contains("\"198812\"")); // 昭和63.12 → 198812
    }

    @Test
    public void testOutputFormat_DefaultBehavior(@TempDir Path tempDir) throws Exception {
        // デフォルト動作（出力形式指定なし）のテスト
        
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");
        
        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,R5.4.30",  // 年月日精度
            "使用開始日,データ4,データ5,データ6,H10.5"   // 年月精度
        );
        Files.write(inputFile, inputLines);
        
        // 出力形式を指定しない（デフォルト動作）
        int convertedCount = service.convertCsvFile(
            inputFile.toString(),
            outputFile.toString(),
            Arrays.asList("取得日", "使用開始日"),
            "UTF-8"
        );
        
        // 結果を検証
        assertEquals(2, convertedCount);
        
        List<String> outputLines = Files.readAllLines(outputFile);
        
        // デフォルト動作では入力精度に応じて出力形式が決まることを確認
        assertTrue(outputLines.get(1).contains("\"20230430\"")); // R5.4.30 → 20230430（年月日精度）
        assertTrue(outputLines.get(2).contains("\"199805\""));   // H10.5 → 199805（年月精度）
    }

    @Test
    public void test追加区切り文字_単一文字を指定した変換(@TempDir Path tempDir) throws Exception {
        // 全角スラッシュを追加区切り文字として指定した場合のテスト

        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,H10／5"      // 全角スラッシュ区切りの和暦（年月精度）
        );
        Files.write(inputFile, inputLines);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(),
            outputFile.toString(),
            Arrays.asList("取得日"),
            "UTF-8",
            OutputFormat.YYYYMMDD,
            "／"
        );

        assertEquals(1, convertedCount);

        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("\"199805\"")); // H10／5 → 199805
    }

    @Test
    public void test追加区切り文字_複数文字を指定した変換(@TempDir Path tempDir) throws Exception {
        // 複数の追加区切り文字（全角スラッシュ・チルダ）を同時に指定した場合のテスト

        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,R3~4~30",   // チルダ区切りの和暦（年月日精度）
            "使用開始日,データ4,データ5,データ6,H10／5"   // 全角スラッシュ区切りの和暦（年月精度）
        );
        Files.write(inputFile, inputLines);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(),
            outputFile.toString(),
            Arrays.asList("取得日", "使用開始日"),
            "UTF-8",
            OutputFormat.YYYYMMDD,
            "／~"
        );

        assertEquals(2, convertedCount);

        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("\"20210430\"")); // R3~4~30 → 20210430
        assertTrue(outputLines.get(2).contains("\"199805\""));   // H10／5 → 199805（年月精度のためYYYYMM）
    }

    @Test
    public void test追加区切り文字を指定しない場合は未対応の区切り文字は変換されない(@TempDir Path tempDir) throws Exception {
        // 追加区切り文字を指定しない場合、全角スラッシュ等は従来通り変換されないことを確認

        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,H10／5"
        );
        Files.write(inputFile, inputLines);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(),
            outputFile.toString(),
            Arrays.asList("取得日"),
            "UTF-8",
            OutputFormat.YYYYMMDD
        );

        assertEquals(0, convertedCount); // 変換されない

        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("H10／5")); // 元の値のまま
    }

    @Test
    public void test追加区切り文字指定時に変換失敗した値は書き換え前の内容を維持する(@TempDir Path tempDir) throws Exception {
        // 対象列に日付として解釈できない値が入っており、かつその値が偶然
        // 追加区切り文字を含んでいる場合でも、値自体が書き換えられないことを確認する
        // （追加区切り文字の正規化は変換成功時のみ反映されるべきで、失敗時に副作用を残してはならない）

        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,備考~欄" // 日付として解釈できない値（チルダを含む）
        );
        Files.write(inputFile, inputLines);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(),
            outputFile.toString(),
            Arrays.asList("取得日"),
            "UTF-8",
            OutputFormat.YYYYMMDD,
            "~" // チルダを追加区切り文字として指定
        );

        assertEquals(0, convertedCount); // 変換は発生しない

        List<String> outputLines = Files.readAllLines(outputFile);
        // 変換に失敗した値は、追加区切り文字による書き換え前の内容のまま出力される
        assertTrue(outputLines.get(1).contains("備考~欄"));
        assertFalse(outputLines.get(1).contains("備考.欄"));
    }

    /**
     * 一般的な不変条件テスト：日付として変換できない値は、
     * 追加区切り文字オプションの有無に関わらず、元の値と完全に一致したまま出力されなければならない。
     * normalizeSpaces（全角・半角スペース除去）による副作用も対象に含める。
     * （このテストは--extra-separators追加前から存在した潜在バグも検出する）
     */
    @Test
    public void test変換失敗時は原本の値を一切変更せず出力する(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        // 内部にスペースを含む、日付として解釈できない値
        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,取得日 未定"
        );
        Files.write(inputFile, inputLines);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(),
            outputFile.toString(),
            Arrays.asList("取得日"),
            "UTF-8",
            OutputFormat.YYYYMMDD,
            "" // 追加区切り文字なし（--extra-separators機能とは無関係の既存バグを検出する）
        );

        assertEquals(0, convertedCount);

        List<String> outputLines = Files.readAllLines(outputFile);
        // 内部のスペースが normalizeSpaces によって削除されず、元の値のまま出力されること
        assertTrue(outputLines.get(1).contains("取得日 未定"),
                "変換に失敗した値の内部スペースが削除されてはならない。実際の出力: " + outputLines.get(1));
    }

    /**
     * isTargetColumnの前方一致判定は、項目名の先頭・末尾だけでなく
     * 文字列途中に紛れ込んだブランク（全角・半角スペース）も除去した上で判定することを確認する。
     */
    @Test
    public void test項目名の途中にあるブランクも除去してから前方一致判定される(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "使 用 開始日,データ1,データ2,データ3,H10.5.15", // 半角スペースが項目名の途中に混入
            "使用　開始日,データ4,データ5,データ6,H10.6.15"  // 全角スペースが項目名の途中に混入
        );
        Files.write(inputFile, inputLines);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(), outputFile.toString(),
            Arrays.asList("使用"), "UTF-8", OutputFormat.YYYYMMDD
        );

        assertEquals(2, convertedCount,
                "項目名途中のブランクが除去されず前方一致に失敗している可能性がある");

        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("\"19980515\""));
        assertTrue(outputLines.get(2).contains("\"19980615\""));
    }

    /**
     * 前方一致化の副作用テスト：対象列名と無関係な意図の項目名（例：「使用停止日」）が
     * 前方一致により偶然「使用」にマッチしてしまっても、5列目が日付として解釈できない限り
     * データが変更されず安全に元の値のまま出力されることを確認する。
     * （前方一致による誤マッチ自体は起こり得るが、変換失敗時は原本を維持する不変条件により
     * 実害はないことを保証する）
     */
    @Test
    public void test前方一致による意図しないマッチでも非日付値は安全に素通りする(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "使用停止日,データ1,データ2,データ3,対象外の値なので変換されない"
        );
        Files.write(inputFile, inputLines);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(), outputFile.toString(),
            Arrays.asList("使用"), "UTF-8", OutputFormat.YYYYMMDD
        );

        assertEquals(0, convertedCount);
        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("対象外の値なので変換されない"),
                "前方一致で誤マッチした行の非日付値が書き換えられてはならない。実際の出力: " + outputLines.get(1));
    }

    /**
     * --charsetは今まで引数パースしかテストされておらず、実際にUTF-8以外の文字コード
     * （Shift_JIS）で書かれたCSVファイルを正しく読み書き・変換できるかは未検証だった。
     * 実際にShift_JISでファイルを書き込み、変換結果もShift_JISとして正しく読めることを確認する。
     */
    @Test
    public void testShiftJIS文字コードで実際に読み書き変換できる(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,H10.5.15"
        );
        Files.write(inputFile, inputLines, java.nio.charset.Charset.forName("Shift_JIS"));

        int convertedCount = service.convertCsvFile(
            inputFile.toString(), outputFile.toString(),
            Arrays.asList("取得日"), "Shift_JIS", OutputFormat.YYYYMMDD
        );

        assertEquals(1, convertedCount);

        List<String> outputLines = Files.readAllLines(outputFile, java.nio.charset.Charset.forName("Shift_JIS"));
        assertTrue(outputLines.get(0).contains("項目名"), "ヘッダーの日本語がShift_JISとして正しく読めていない");
        assertTrue(outputLines.get(1).contains("\"19980515\""));
    }

    /**
     * 対象行の5列目が空欄（空文字）の場合、変換対象外としてそのまま出力されることを確認する。
     * （カバレッジ計測で「value.trim().isEmpty()がtrueになる分岐」が未実行と判明したため追加）
     */
    @Test
    public void test対象行の5列目が空欄なら変換されずそのまま出力される(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,データ1,データ2,データ3,"
        );
        Files.write(inputFile, inputLines);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(), outputFile.toString(),
            Arrays.asList("取得日"), "UTF-8", OutputFormat.YYYYMMDD
        );

        assertEquals(0, convertedCount);
        List<String> outputLines = Files.readAllLines(outputFile);
        assertEquals(2, outputLines.size());
        assertTrue(outputLines.get(1).contains("データ3"));
    }

    /**
     * 1000行を超えるファイルで、進捗ログ分岐（PROGRESS_INTERVALごとのログ出力）を通過しつつ
     * 全行が欠落なく正しく変換されることを確認する。
     * （進捗ログの分岐はカバレッジ計測で未実行と判明。またこの経路はcsv.progressメッセージの
     * 整形を実際に実行するため、数値の桁区切りバグのような整形不具合の再発検知も兼ねる）
     */
    @Test
    public void test1000行超のファイルでも全行が欠落なく変換される(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        java.util.List<String> inputLines = new java.util.ArrayList<>();
        inputLines.add("項目名,列2,列3,列4,日付データ");
        for (int i = 0; i < 1500; i++) {
            inputLines.add("取得日,データ,データ,データ,H10.5.15");
        }
        Files.write(inputFile, inputLines);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(), outputFile.toString(),
            Arrays.asList("取得日"), "UTF-8", OutputFormat.YYYYMMDD
        );

        assertEquals(1500, convertedCount);
        List<String> outputLines = Files.readAllLines(outputFile);
        assertEquals(1501, outputLines.size());
        assertTrue(outputLines.get(1500).contains("\"19980515\""));
    }
}