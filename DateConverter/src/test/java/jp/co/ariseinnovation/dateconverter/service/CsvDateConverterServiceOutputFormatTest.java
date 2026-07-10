package jp.co.ariseinnovation.dateconverter.service;

import jp.co.ariseinnovation.dateconverter.enums.OutputFormat;
import jp.co.ariseinnovation.dateconverter.util.DateConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.StringWriter;
import java.io.StringReader;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CsvDateConverterServiceの出力形式指定機能のテストクラス
 */
@SpringBootTest
public class CsvDateConverterServiceOutputFormatTest {

    private CsvDateConverterService service = new CsvDateConverterService();

    @Test
    public void testGetDefaultTargetColumns() {
        // デフォルト対象列の取得テスト
        var defaultColumns = Arrays.asList(DateConstants.DEFAULT_TARGET_COLUMNS);
        assertNotNull(defaultColumns);
        assertTrue(defaultColumns.size() > 0);
        assertTrue(defaultColumns.contains("取得"));
    }

    @Test
    public void testOutputFormatEnum() {
        // OutputFormat列挙型のテスト
        assertEquals("YYYYMM", OutputFormat.YYYYMM.getFormat());
        assertEquals("YYYYMMDD", OutputFormat.YYYYMMDD.getFormat());
    }

    @Test
    public void testServiceInstantiation() {
        // サービスのインスタンス化テスト
        assertNotNull(service);
        assertTrue(service instanceof CsvDateConverterService);
    }

    @Test
    public void testMethodSignatures() {
        // メソッドシグネチャの存在確認
        try {
            // デフォルト形式のメソッド
            var method1 = CsvDateConverterService.class.getMethod(
                "convertCsvFile", 
                String.class, String.class, java.util.List.class, String.class
            );
            assertNotNull(method1);

            // 出力形式指定のメソッド
            var method2 = CsvDateConverterService.class.getMethod(
                "convertCsvFile", 
                String.class, String.class, java.util.List.class, String.class, OutputFormat.class
            );
            assertNotNull(method2);

        } catch (NoSuchMethodException e) {
            fail("Expected methods not found: " + e.getMessage());
        }
    }

    @Test
    public void testOutputFormatParameterHandling() {
        // 出力形式パラメータの処理テスト（実際のファイル処理なし）
        
        // 各出力形式が正常に受け入れられることを確認
        assertDoesNotThrow(() -> {
            OutputFormat.valueOf("YYYYMM");
            OutputFormat.valueOf("YYYYMMDD");
        });
        
        // 無効な値でエラーが発生することを確認
        assertThrows(IllegalArgumentException.class, () -> {
            OutputFormat.valueOf("INVALID");
        });
    }
}