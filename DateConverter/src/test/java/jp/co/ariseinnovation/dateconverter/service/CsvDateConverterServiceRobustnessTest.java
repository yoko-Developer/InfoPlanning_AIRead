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
 * CsvDateConverterServiceの堅牢性（CSV構造の異常系・ファイルI/Oエラー）を狙ったテストクラス
 *
 * 実装を追認するのではなく、「業務ツールとして最低限満たすべき性質」を独立した基準として
 * テストする。CSVの列数が不定な入力、ファイルが存在しない場合等、これまで一度も
 * 検証されていなかった領域を対象とする。
 */
@SpringBootTest
public class CsvDateConverterServiceRobustnessTest {

    private final CsvDateConverterService service = new CsvDateConverterService();

    @Test
    public void test列数が5未満の行はクラッシュせず通過する(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        // 5列目（日付データ列）が存在しない行
        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3",
            "取得日,データ1,データ2"
        );
        Files.write(inputFile, inputLines);

        int convertedCount = assertDoesNotThrow(() -> service.convertCsvFile(
            inputFile.toString(), outputFile.toString(),
            Arrays.asList("取得日"), "UTF-8", OutputFormat.YYYYMMDD
        ));

        assertEquals(0, convertedCount);
        List<String> outputLines = Files.readAllLines(outputFile);
        assertEquals(2, outputLines.size());
        assertTrue(outputLines.get(1).contains("データ1"));
    }

    @Test
    public void test列数が5を超える行では5列目のみ変換され他の列は保持される(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ,列6,列7",
            "取得日,データ1,データ2,データ3,H10.5.15,データ6,データ7"
        );
        Files.write(inputFile, inputLines);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(), outputFile.toString(),
            Arrays.asList("取得日"), "UTF-8", OutputFormat.YYYYMMDD
        );

        assertEquals(1, convertedCount);
        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("\"19980515\""));
        assertTrue(outputLines.get(1).contains("データ6"));
        assertTrue(outputLines.get(1).contains("データ7"));
    }

    @Test
    public void test空ファイルはクラッシュせず変換件数0で終わる(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        Files.write(inputFile, List.of());

        int convertedCount = assertDoesNotThrow(() -> service.convertCsvFile(
            inputFile.toString(), outputFile.toString(),
            Arrays.asList("取得日"), "UTF-8", OutputFormat.YYYYMMDD
        ));

        assertEquals(0, convertedCount);
    }

    @Test
    public void testヘッダーのみのファイルはクラッシュせず変換件数0で終わる(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        Files.write(inputFile, List.of("項目名,列2,列3,列4,日付データ"));

        int convertedCount = service.convertCsvFile(
            inputFile.toString(), outputFile.toString(),
            Arrays.asList("取得日"), "UTF-8", OutputFormat.YYYYMMDD
        );

        assertEquals(0, convertedCount);
    }

    @Test
    public void test非対象列に埋め込みカンマがあっても列ずれせず5列目が正しく変換される(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        // 列2にカンマを含む値をダブルクォートで囲んで埋め込む
        List<String> inputLines = Arrays.asList(
            "項目名,列2,列3,列4,日付データ",
            "取得日,\"データ,カンマ入り\",データ2,データ3,H10.5.15"
        );
        Files.write(inputFile, inputLines);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(), outputFile.toString(),
            Arrays.asList("取得日"), "UTF-8", OutputFormat.YYYYMMDD
        );

        assertEquals(1, convertedCount);
        List<String> outputLines = Files.readAllLines(outputFile);
        assertTrue(outputLines.get(1).contains("\"19980515\""),
                "埋め込みカンマにより列がずれ、日付変換が正しく行われない可能性がある。実際の出力: " + outputLines.get(1));
    }

    @Test
    public void test非対象列に埋め込み改行があっても列ずれせず5列目が正しく変換される(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String content = "項目名,列2,列3,列4,日付データ\r\n"
                + "取得日,\"データ\n改行入り\",データ2,データ3,H10.5.15\r\n";
        Files.writeString(inputFile, content);

        int convertedCount = service.convertCsvFile(
            inputFile.toString(), outputFile.toString(),
            Arrays.asList("取得日"), "UTF-8", OutputFormat.YYYYMMDD
        );

        assertEquals(1, convertedCount);
        List<String> outputContent = Files.readAllLines(outputFile);
        String joined = String.join("\n", outputContent);
        assertTrue(joined.contains("19980515"),
                "埋め込み改行により列がずれ、日付変換が正しく行われない可能性がある。実際の出力: " + joined);
    }

    @Test
    public void test存在しない入力ファイルは例外を投げる() {
        assertThrows(IOException.class, () -> service.convertCsvFile(
            "存在しないファイル_" + System.identityHashCode(this) + ".csv",
            "output.csv",
            Arrays.asList("取得日"), "UTF-8", OutputFormat.YYYYMMDD
        ));
    }

    @Test
    public void test不正な文字コード名は例外を投げる(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Path outputFile = tempDir.resolve("output.csv");
        Files.write(inputFile, List.of("項目名,列2,列3,列4,日付データ"));

        // 文字コード名が不正な形式の場合はIllegalCharsetNameException、
        // 形式は正しいが未知の文字コードの場合はUnsupportedCharsetExceptionとなる。
        // どちらもIllegalArgumentExceptionのサブクラスであるため、共通の親クラスで検証する。
        assertThrows(IllegalArgumentException.class, () -> service.convertCsvFile(
            inputFile.toString(), outputFile.toString(),
            Arrays.asList("取得日"), "存在しない文字コードXYZ", OutputFormat.YYYYMMDD
        ));
    }

    @Test
    public void test出力先ディレクトリが存在しない場合は例外を投げる(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("input.csv");
        Files.write(inputFile, List.of("項目名,列2,列3,列4,日付データ"));
        Path nonExistentDir = tempDir.resolve("no_such_dir").resolve("output.csv");

        assertThrows(IOException.class, () -> service.convertCsvFile(
            inputFile.toString(), nonExistentDir.toString(),
            Arrays.asList("取得日"), "UTF-8", OutputFormat.YYYYMMDD
        ));
    }
}
