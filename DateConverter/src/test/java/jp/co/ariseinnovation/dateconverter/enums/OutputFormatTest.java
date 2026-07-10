package jp.co.ariseinnovation.dateconverter.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * OutputFormatの単体テストクラス
 */
public class OutputFormatTest {

    @Test
    public void testYYYYMM() {
        assertEquals("YYYYMM", OutputFormat.YYYYMM.getFormat());
    }

    @Test
    public void testYYYYMMDD() {
        assertEquals("YYYYMMDD", OutputFormat.YYYYMMDD.getFormat());
    }

    @Test
    public void testValues() {
        OutputFormat[] values = OutputFormat.values();
        assertEquals(2, values.length);
        assertEquals(OutputFormat.YYYYMM, values[0]);
        assertEquals(OutputFormat.YYYYMMDD, values[1]);
    }

    @Test
    public void testValueOf() {
        assertEquals(OutputFormat.YYYYMM, OutputFormat.valueOf("YYYYMM"));
        assertEquals(OutputFormat.YYYYMMDD, OutputFormat.valueOf("YYYYMMDD"));
    }
}