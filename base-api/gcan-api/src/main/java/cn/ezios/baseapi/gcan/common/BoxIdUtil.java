package cn.ezios.baseapi.gcan.common;

import cn.ezios.baseapi.common.exception.BusinessException;
import org.springframework.util.StringUtils;

public final class BoxIdUtil {

    private BoxIdUtil() {
    }

    public static String normalizeHex(String input) {
        if (!StringUtils.hasText(input)) {
            throw new BusinessException("盒子ID(HEX)不能为空");
        }
        String value = input.trim().toUpperCase();
        if (value.startsWith("0X")) {
            value = value.substring(2);
        }
        if (value.length() == 1) {
            value = "0" + value;
        }
        if (!value.matches("[0-9A-F]{2}")) {
            throw new BusinessException("盒子ID(HEX)必须是 00-FF 范围内的十六进制值");
        }
        return value;
    }

    public static int toDec(String boxIdHex) {
        return Integer.parseInt(normalizeHex(boxIdHex), 16);
    }
}
