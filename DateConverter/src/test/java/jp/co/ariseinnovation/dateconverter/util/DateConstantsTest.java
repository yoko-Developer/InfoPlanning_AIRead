package jp.co.ariseinnovation.dateconverter.util;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DateConstantsのテストクラス
 */
class DateConstantsTest {

    @Test
    void testBaseYears() {
        assertEquals(1925, DateConstants.SHOWA_BASE_YEAR);
        assertEquals(1988, DateConstants.HEISEI_BASE_YEAR);
        assertEquals(2018, DateConstants.REIWA_BASE_YEAR);
    }

    @Test
    void testDefaultTargetColumns() {
        assertEquals(7, DateConstants.DEFAULT_TARGET_COLUMNS.length);
        assertTrue(Arrays.asList(DateConstants.DEFAULT_TARGET_COLUMNS).contains("取得"));
        assertTrue(Arrays.asList(DateConstants.DEFAULT_TARGET_COLUMNS).contains("取得日"));
        assertTrue(Arrays.asList(DateConstants.DEFAULT_TARGET_COLUMNS).contains("使用"));
        assertTrue(Arrays.asList(DateConstants.DEFAULT_TARGET_COLUMNS).contains("供用"));
        assertTrue(Arrays.asList(DateConstants.DEFAULT_TARGET_COLUMNS).contains("供用日"));
        assertTrue(Arrays.asList(DateConstants.DEFAULT_TARGET_COLUMNS).contains("事業供用開始日"));
        assertTrue(Arrays.asList(DateConstants.DEFAULT_TARGET_COLUMNS).contains("契約"));
    }

    @Test
    void testDefaultCharset() {
        assertEquals("UTF-8", DateConstants.DEFAULT_CHARSET);
    }

    @Test
    void testProgressInterval() {
        assertEquals(1000, DateConstants.PROGRESS_INTERVAL);
    }

    @Test
    void testPrivateConstructorThrowsAssertionError() throws Exception {
        Constructor<DateConstants> constructor = DateConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(AssertionError.class, thrown.getCause());
    }
}
