package jp.co.ariseinnovation.dateconverter.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * FormatExceptionのテストクラス
 */
class FormatExceptionTest {

    @Test
    void testDefaultConstructor() {
        FormatException ex = new FormatException();
        assertNull(ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void testMessageConstructor() {
        FormatException ex = new FormatException("解析エラー");
        assertEquals("解析エラー", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void testMessageAndCauseConstructor() {
        Throwable cause = new IllegalArgumentException("原因");
        FormatException ex = new FormatException("解析エラー", cause);
        assertEquals("解析エラー", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void testCauseOnlyConstructor() {
        Throwable cause = new IllegalArgumentException("原因");
        FormatException ex = new FormatException(cause);
        assertSame(cause, ex.getCause());
        assertEquals(cause.toString(), ex.getMessage());
    }

    @Test
    void testIsCheckedException() {
        // FormatExceptionはExceptionを継承したチェック例外であること
        assertTrue(Exception.class.isAssignableFrom(FormatException.class));
        assertFalse(RuntimeException.class.isAssignableFrom(FormatException.class));
    }
}
