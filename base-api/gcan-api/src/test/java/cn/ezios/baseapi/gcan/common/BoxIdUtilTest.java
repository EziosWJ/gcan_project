package cn.ezios.baseapi.gcan.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.ezios.baseapi.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class BoxIdUtilTest {

    @Test
    void normalizeHexAcceptsCommonHumanInputs() {
        assertEquals("01", BoxIdUtil.normalizeHex("1"));
        assertEquals("0A", BoxIdUtil.normalizeHex("0x0a"));
        assertEquals("FF", BoxIdUtil.normalizeHex("ff"));
        assertEquals(10, BoxIdUtil.toDec("0A"));
    }

    @Test
    void normalizeHexRejectsValuesOutsideOneByteRange() {
        assertThrows(BusinessException.class, () -> BoxIdUtil.normalizeHex("100"));
        assertThrows(BusinessException.class, () -> BoxIdUtil.normalizeHex("GG"));
    }
}
