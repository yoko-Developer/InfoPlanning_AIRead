package jp.co.ariseinnovation.dateconverter.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GengoYearTableのテストクラス
 */
class GengoYearTableTest {

    @Test
    void testTableSize() {
        assertEquals(10, GengoYearTable.table.size());
    }

    @Test
    void testTableOrder() {
        // 長い表記・優先度の高いものから順に配置されていることを確認
        assertSame(GengoYearTable.SHOWA, GengoYearTable.table.get(0));
        assertSame(GengoYearTable.HEISEI, GengoYearTable.table.get(1));
        assertSame(GengoYearTable.REIWA, GengoYearTable.table.get(2));
        assertSame(GengoYearTable.SHOWA_S, GengoYearTable.table.get(3));
        assertSame(GengoYearTable.HEISEI_H, GengoYearTable.table.get(4));
        assertSame(GengoYearTable.REIWA_R, GengoYearTable.table.get(5));
        assertSame(GengoYearTable.REIWA_L, GengoYearTable.table.get(6));
        assertSame(GengoYearTable.SHOWA_1, GengoYearTable.table.get(7));
        assertSame(GengoYearTable.HEISEI_1, GengoYearTable.table.get(8));
        assertSame(GengoYearTable.REIWA_1, GengoYearTable.table.get(9));
    }

    @Test
    void testGengoStrings() {
        assertEquals("昭和", GengoYearTable.SHOWA.gengo);
        assertEquals("平成", GengoYearTable.HEISEI.gengo);
        assertEquals("令和", GengoYearTable.REIWA.gengo);
        assertEquals("S", GengoYearTable.SHOWA_S.gengo);
        assertEquals("H", GengoYearTable.HEISEI_H.gengo);
        assertEquals("R", GengoYearTable.REIWA_R.gengo);
        assertEquals("L", GengoYearTable.REIWA_L.gengo);
        assertEquals("昭", GengoYearTable.SHOWA_1.gengo);
        assertEquals("平", GengoYearTable.HEISEI_1.gengo);
        assertEquals("令", GengoYearTable.REIWA_1.gengo);
    }

    @Test
    void testYearAddValues() {
        // 元号の基準年（DateConstantsと一致していること）
        assertEquals(DateConstants.SHOWA_BASE_YEAR, GengoYearTable.SHOWA.yearAdd);
        assertEquals(DateConstants.HEISEI_BASE_YEAR, GengoYearTable.HEISEI.yearAdd);
        assertEquals(DateConstants.REIWA_BASE_YEAR, GengoYearTable.REIWA.yearAdd);

        // 略称も同じ基準年であること
        assertEquals(GengoYearTable.SHOWA.yearAdd, GengoYearTable.SHOWA_S.yearAdd);
        assertEquals(GengoYearTable.SHOWA.yearAdd, GengoYearTable.SHOWA_1.yearAdd);
        assertEquals(GengoYearTable.HEISEI.yearAdd, GengoYearTable.HEISEI_H.yearAdd);
        assertEquals(GengoYearTable.HEISEI.yearAdd, GengoYearTable.HEISEI_1.yearAdd);
        assertEquals(GengoYearTable.REIWA.yearAdd, GengoYearTable.REIWA_R.yearAdd);
        assertEquals(GengoYearTable.REIWA.yearAdd, GengoYearTable.REIWA_L.yearAdd);
        assertEquals(GengoYearTable.REIWA.yearAdd, GengoYearTable.REIWA_1.yearAdd);
    }

    @Test
    void testGengoNenCalculation() {
        // 元年（1年）が正しい西暦に変換されることを確認
        assertEquals(1926, 1 + GengoYearTable.SHOWA.yearAdd);  // 昭和元年 = 1926年
        assertEquals(1989, 1 + GengoYearTable.HEISEI.yearAdd); // 平成元年 = 1989年
        assertEquals(2019, 1 + GengoYearTable.REIWA.yearAdd);  // 令和元年 = 2019年
    }

    @Test
    void testTableIsImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> {
            GengoYearTable.table.add(new GengoYearTable("test", 0));
        });
    }

    @Test
    void testConstructor() {
        GengoYearTable custom = new GengoYearTable("テスト", 100);
        assertEquals("テスト", custom.gengo);
        assertEquals(100, custom.yearAdd);
    }
}
