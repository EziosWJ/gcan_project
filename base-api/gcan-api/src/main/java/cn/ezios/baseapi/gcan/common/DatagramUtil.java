package cn.ezios.baseapi.gcan.common;

public final class DatagramUtil {

    private DatagramUtil() {
    }

    public static int getBit(int value, int bit) {
        return (value >> bit) & 1;
    }
}
