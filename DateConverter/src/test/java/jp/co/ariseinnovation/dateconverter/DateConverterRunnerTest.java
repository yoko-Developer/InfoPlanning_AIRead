package jp.co.ariseinnovation.dateconverter;

import jp.co.ariseinnovation.dateconverter.enums.OutputFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DateConverterRunnerのテストクラス
 */
public class DateConverterRunnerTest {

    @Test
    public void testArgumentsParsing_WithFormat() {
        // 出力形式を指定した場合のテスト
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv",
            "--format", "YYYYMM"
        };
        
        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);
        
        assertTrue(arguments.isValid());
        assertEquals("input.csv", arguments.input);
        assertEquals("output.csv", arguments.output);
        assertEquals(OutputFormat.YYYYMM, arguments.outputFormat);
    }

    @Test
    public void testArgumentsParsing_WithoutFormat() {
        // 出力形式を指定しない場合のテスト（デフォルト値）
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv"
        };
        
        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);
        
        assertTrue(arguments.isValid());
        assertEquals(OutputFormat.YYYYMMDD, arguments.outputFormat); // デフォルト値
    }

    @Test
    public void testArgumentsParsing_InvalidFormat() {
        // 無効な出力形式を指定した場合のテスト
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv",
            "--format", "INVALID"
        };
        
        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);
        
        assertTrue(arguments.isValid());
        assertEquals(OutputFormat.YYYYMMDD, arguments.outputFormat); // デフォルト値にフォールバック
    }

    @Test
    public void testArgumentsParsing_AllOptions() {
        // 全オプションを指定した場合のテスト
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv",
            "--cols", "取得,使用,供用",
            "--charset", "Shift_JIS",
            "--format", "YYYYMMDD"
        };
        
        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);
        
        assertTrue(arguments.isValid());
        assertEquals("input.csv", arguments.input);
        assertEquals("output.csv", arguments.output);
        assertEquals("Shift_JIS", arguments.charset);
        assertEquals(OutputFormat.YYYYMMDD, arguments.outputFormat);
        assertEquals(3, arguments.columns.size());
        assertTrue(arguments.columns.contains("取得"));
        assertTrue(arguments.columns.contains("使用"));
        assertTrue(arguments.columns.contains("供用"));
    }

    @Test
    public void testArgumentsParsing_CaseInsensitiveFormat() {
        // 大文字小文字を区別しない出力形式のテスト
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv",
            "--format", "yyyymm"
        };
        
        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);
        
        assertTrue(arguments.isValid());
        assertEquals(OutputFormat.YYYYMM, arguments.outputFormat);
    }

    @Test
    public void testArgumentsParsing_EmptyFormat() {
        // 空の出力形式を指定した場合のテスト
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv",
            "--format", ""
        };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertTrue(arguments.isValid());
        assertEquals(OutputFormat.YYYYMMDD, arguments.outputFormat); // デフォルト値
    }

    @Test
    public void testArgumentsParsing_Cols空白と空要素の除去() {
        // 前後の空白除去、空要素の除外を確認
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv",
            "--cols", "取得, ,使用,,契約 "
        };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertTrue(arguments.isValid());
        assertEquals(3, arguments.columns.size());
        assertTrue(arguments.columns.contains("取得"));
        assertTrue(arguments.columns.contains("使用"));
        assertTrue(arguments.columns.contains("契約"));
    }

    @Test
    public void testArgumentsParsing_奇数個の引数は無効() {
        // オプション名と値がペアにならない場合は解析されず無効な引数として扱われる
        String[] args = { "--in" };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertFalse(arguments.isValid());
        assertNull(arguments.input);
    }

    @Test
    public void testArgumentsParsing_未知のオプションは無視される() {
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv",
            "--unknown", "value"
        };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertTrue(arguments.isValid());
        assertEquals("input.csv", arguments.input);
        assertEquals("output.csv", arguments.output);
    }

    @Test
    public void testArgumentsParsing_RequiredContent指定() {
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv",
            "--required-content", "対象キーワード"
        };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertTrue(arguments.isValid());
        assertEquals("対象キーワード", arguments.requiredContent);
    }

    @Test
    public void testArgumentsParsing_RequiredContent未指定時はnull() {
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv"
        };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertNull(arguments.requiredContent);
    }

    @Test
    public void testArgumentsParsing_出力ファイル未指定は無効() {
        String[] args = { "--in", "input.csv" };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertFalse(arguments.isValid());
    }

    @Test
    public void testArgumentsParsing_入力ファイルが空文字は無効() {
        String[] args = { "--in", "", "--out", "output.csv" };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertFalse(arguments.isValid());
    }

    @Test
    public void testArgumentsParsing_出力ファイルが空文字は無効() {
        String[] args = { "--in", "input.csv", "--out", "" };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertFalse(arguments.isValid());
    }

    @Test
    public void testArgumentsParsing_ExtraSeparators指定() {
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv",
            "--extra-separators", "／~"
        };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertTrue(arguments.isValid());
        assertEquals("／~", arguments.extraSeparators);
    }

    @Test
    public void testArgumentsParsing_ExtraSeparators未指定時はデフォルト空文字() {
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv"
        };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertEquals("", arguments.extraSeparators);
    }

    @Test
    public void testArgumentsParsing_ExtraSeparators空文字指定() {
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv",
            "--extra-separators", ""
        };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertTrue(arguments.isValid());
        assertEquals("", arguments.extraSeparators);
    }

    @Test
    public void testArgumentsParsing_全オプションにExtraSeparatorsを含む() {
        String[] args = {
            "--in", "input.csv",
            "--out", "output.csv",
            "--cols", "取得,使用,供用",
            "--charset", "Shift_JIS",
            "--format", "YYYYMMDD",
            "--extra-separators", "／"
        };

        DateConverterRunner.Arguments arguments = DateConverterRunner.Arguments.parse(args);

        assertTrue(arguments.isValid());
        assertEquals("／", arguments.extraSeparators);
        assertEquals(OutputFormat.YYYYMMDD, arguments.outputFormat);
    }
}