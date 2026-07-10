package jp.co.ariseinnovation.dateconverter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DateConverterRunnerの結合テストクラス
 * Springでの実配線（DIコンテナ経由）を通した実行結果を検証する
 */
@SpringBootTest
public class DateConverterRunnerIntegrationTest {

    @Autowired
    private DateConverterRunner runner;

    @Test
    public void test正常系_和暦から西暦への変換が実行される(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        Files.write(inputFile, List.of(
                "項目名,列2,列3,列4,日付データ",
                "取得日,データ1,データ2,データ3,R5.4.30"
        ));

        runner.run("--in", inputFile.toString(), "--out", outputFile.toString(),
                "--cols", "取得日", "--format", "YYYYMMDD");

        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("\"20230430\""));
    }

    @Test
    public void testRequiredContent_含む場合は変換が実行される(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        Files.write(inputFile, List.of(
                "項目名,列2,列3,列4,日付データ",
                "取得日,対象キーワードあり,データ2,データ3,R5.4.30"
        ));

        runner.run("--in", inputFile.toString(), "--out", outputFile.toString(),
                "--cols", "取得日", "--required-content", "対象キーワード");

        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("\"20230430\""));
    }

    @Test
    public void testRequiredContent_含まない場合は変換されずコピーされる(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        List<String> inputLines = List.of(
                "項目名,列2,列3,列4,日付データ",
                "取得日,データ1,データ2,データ3,R5.4.30"
        );
        Files.write(inputFile, inputLines);

        runner.run("--in", inputFile.toString(), "--out", outputFile.toString(),
                "--cols", "取得日", "--required-content", "存在しないキーワード");

        // 変換されず、入力ファイルがそのままコピーされる（和暦が残ったまま）
        List<String> outputLines = Files.readAllLines(outputFile);
        assertEquals(inputLines, outputLines);
        assertTrue(outputLines.get(1).contains("R5.4.30"));
    }

    @Test
    public void test引数なし実行は例外を投げない() {
        assertDoesNotThrow(() -> runner.run());
    }

    @Test
    public void test追加区切り文字オプションを指定した変換(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        Files.write(inputFile, List.of(
                "項目名,列2,列3,列4,日付データ",
                "取得日,データ1,データ2,データ3,R3~4~30" // チルダ区切りの和暦
        ));

        runner.run("--in", inputFile.toString(), "--out", outputFile.toString(),
                "--cols", "取得日", "--format", "YYYYMMDD", "--extra-separators", "~");

        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("\"20210430\""));
    }

    @Test
    public void test追加区切り文字オプション未指定時は未対応の区切り文字が変換されない(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        Files.write(inputFile, List.of(
                "項目名,列2,列3,列4,日付データ",
                "取得日,データ1,データ2,データ3,R3~4~30"
        ));

        runner.run("--in", inputFile.toString(), "--out", outputFile.toString(),
                "--cols", "取得日", "--format", "YYYYMMDD");

        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("R3~4~30")); // 未対応のため変換されず元の値のまま
    }

    /**
     * --cols未指定時（デフォルト対象列）でも、DateConstants.DEFAULT_TARGET_COLUMNSの
     * コメントが明示する意図（"使用" → 「使用開始日等」に一致すべき）通りに
     * 変換対象と判定されることを確認する。
     * isTargetColumnは前方一致（プレフィックスマッチ）で判定するよう修正済み。
     */
    @Test
    public void testデフォルト対象列は項目名の前方一致でも変換対象と判定される(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        Files.write(inputFile, List.of(
                "項目名,列2,列3,列4,日付データ",
                "使用開始日,データ1,データ2,データ3,H10.5.15" // DEFAULT_TARGET_COLUMNSの"使用"コメントが想定する項目名
        ));

        // --colsを指定せず、デフォルト対象列（DateConstants.DEFAULT_TARGET_COLUMNS）に委ねる
        runner.run("--in", inputFile.toString(), "--out", outputFile.toString(),
                "--format", "YYYYMMDD");

        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("\"19980515\""));
    }

    /**
     * DateConstants.DEFAULT_TARGET_COLUMNSの7エントリ全てについて、
     * --cols未指定（デフォルト対象列）でも前方一致により変換対象と判定されることを、
     * 実データ（各エントリより長い項目名）で網羅的に確認する。
     */
    @Test
    public void testデフォルト対象列7エントリ全てが前方一致で機能する(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        Files.write(inputFile, List.of(
                "項目名,列2,列3,列4,日付データ",
                "取得年月日,d,d,d,H1.1.1",     // "取得"に前方一致
                "取得日時,d,d,d,H2.2.2",       // "取得日"に前方一致
                "使用開始日,d,d,d,H3.3.3",     // "使用"に前方一致
                "供用開始日,d,d,d,H4.4.4",     // "供用"に前方一致
                "供用日程,d,d,d,H5.5.5",       // "供用日"（および"供用"）に前方一致
                "事業供用開始日時,d,d,d,H6.6.6", // "事業供用開始日"に前方一致
                "契約日,d,d,d,H7.7.7"          // "契約"に前方一致
        ));

        // --colsを指定せず、デフォルト対象列（DateConstants.DEFAULT_TARGET_COLUMNS）に委ねる
        runner.run("--in", inputFile.toString(), "--out", outputFile.toString(), "--format", "YYYYMMDD");

        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("\"19890101\""), "取得: " + outputLines.get(1));
        assertTrue(outputLines.get(2).contains("\"19900202\""), "取得日: " + outputLines.get(2));
        assertTrue(outputLines.get(3).contains("\"19910303\""), "使用: " + outputLines.get(3));
        assertTrue(outputLines.get(4).contains("\"19920404\""), "供用: " + outputLines.get(4));
        assertTrue(outputLines.get(5).contains("\"19930505\""), "供用日: " + outputLines.get(5));
        assertTrue(outputLines.get(6).contains("\"19940606\""), "事業供用開始日: " + outputLines.get(6));
        assertTrue(outputLines.get(7).contains("\"19950707\""), "契約: " + outputLines.get(7));
    }

    /**
     * 機能間の組み合わせテスト：--required-contentと--extra-separatorsを同時に指定した場合、
     * 必須コンテンツチェック（行全体のテキスト検索）が追加区切り文字の正規化と独立して
     * 正しく動作することを確認する。
     */
    @Test
    public void testRequiredContentとExtraSeparatorsの組み合わせ(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        Files.write(inputFile, List.of(
                "項目名,列2,列3,列4,日付データ",
                "取得日,対象キーワードあり,データ2,データ3,R3~4~30"
        ));

        runner.run("--in", inputFile.toString(), "--out", outputFile.toString(),
                "--cols", "取得日", "--required-content", "対象キーワード",
                "--extra-separators", "~");

        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("\"20210430\""));
    }
}
