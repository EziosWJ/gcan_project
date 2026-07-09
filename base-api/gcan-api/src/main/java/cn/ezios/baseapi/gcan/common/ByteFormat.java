package cn.ezios.baseapi.gcan.common;

public enum ByteFormat {
    HEX,
    BIN,
    DECIMAL;

    public static ByteFormat parse(String value) {
        if (value == null || value.isBlank()) {
            return HEX;
        }
        return switch (value.trim().toUpperCase()) {
            case "BIN", "BINARY" -> BIN;
            case "DEC", "DECIMAL" -> DECIMAL;
            default -> HEX;
        };
    }

    public String format(int value) {
        int unsigned = value & 0xFF;
        return switch (this) {
            case HEX -> String.format("0x%02X", unsigned);
            case BIN -> String.format("%8s", Integer.toBinaryString(unsigned)).replace(' ', '0');
            case DECIMAL -> String.valueOf(unsigned);
        };
    }
}
