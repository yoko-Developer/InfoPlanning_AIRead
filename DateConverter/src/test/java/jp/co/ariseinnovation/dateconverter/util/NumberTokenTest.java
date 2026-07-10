package jp.co.ariseinnovation.dateconverter.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * NumberTokenのテストクラス
 */
class NumberTokenTest {

    @Test
    void testIntConstructor() {
        NumberToken token = new NumberToken(42);
        assertEquals(42, token.token);
        assertEquals("42", token.tokenAsString);
        assertEquals("", token.afterWord);
    }

    @Test
    void testStringConstructor() {
        NumberToken token = new NumberToken("123");
        assertEquals(123, token.token);
        assertEquals("123", token.tokenAsString);
        assertEquals("", token.afterWord);
    }

    @Test
    void testStringConstructor_無効な数値文字列() {
        assertThrows(NumberFormatException.class, () -> new NumberToken("abc"));
    }

    @Test
    void testStringWithAfterWordConstructor() {
        NumberToken token = new NumberToken("10", "月");
        assertEquals(10, token.token);
        assertEquals("10", token.tokenAsString);
        assertEquals("月", token.afterWord);
    }

    @Test
    void testStringWithNullAfterWordConstructor() {
        NumberToken token = new NumberToken("10", null);
        assertEquals("", token.afterWord);
    }

    @Test
    void testHasAfter_含む場合() {
        NumberToken token = new NumberToken("10", "年月日");
        assertTrue(token.hasAfter("年"));
        assertTrue(token.hasAfter("月"));
        assertTrue(token.hasAfter("日"));
        assertTrue(token.hasAfter("年月"));
    }

    @Test
    void testHasAfter_含まない場合() {
        NumberToken token = new NumberToken("10", "年");
        assertFalse(token.hasAfter("月"));
    }

    @Test
    void testHasAfter_null引数() {
        NumberToken token = new NumberToken("10", "年");
        assertFalse(token.hasAfter(null));
    }

    @Test
    void testHasAfter_afterWordが空文字() {
        NumberToken token = new NumberToken(5);
        assertFalse(token.hasAfter("年"));
        assertTrue(token.hasAfter("")); // 空文字はcontains内で常にtrue
    }

    @Test
    void testCreateArray_int可変長引数() {
        NumberToken[] tokens = NumberToken.createArray(1, 2, 3);
        assertEquals(3, tokens.length);
        assertEquals(1, tokens[0].token);
        assertEquals(2, tokens[1].token);
        assertEquals(3, tokens[2].token);
    }

    @Test
    void testCreateArray_int空配列() {
        NumberToken[] tokens = NumberToken.createArray(new int[0]);
        assertEquals(0, tokens.length);
    }

    @Test
    void testCreateArray_String可変長引数() {
        NumberToken[] tokens = NumberToken.createArray("10", "20", "30");
        assertEquals(3, tokens.length);
        assertEquals(10, tokens[0].token);
        assertEquals(20, tokens[1].token);
        assertEquals(30, tokens[2].token);
    }

    @Test
    void testCreateArray_String無効な値を含む場合() {
        assertThrows(NumberFormatException.class, () -> NumberToken.createArray("10", "abc"));
    }
}
