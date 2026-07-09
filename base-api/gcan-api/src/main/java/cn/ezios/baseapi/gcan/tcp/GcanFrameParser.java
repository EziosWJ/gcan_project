package cn.ezios.baseapi.gcan.tcp;

import cn.ezios.baseapi.gcan.common.BoxIdUtil;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import java.time.LocalDateTime;

public final class GcanFrameParser {

    private GcanFrameParser() {
    }

    public static RawCanFrame parse(byte[] datagram) {
        String boxIdHex = BoxIdUtil.normalizeHex(String.format("%02X", datagram[3]));
        String canId = String.format("%02X%02X%02X%02X", datagram[5], datagram[6], datagram[7], datagram[8]);
        int[] values = new int[8];
        for (int i = 0; i < values.length; i++) {
            values[i] = datagram[i + 9] & 0xFF;
        }
        return new RawCanFrame(boxIdHex, BoxIdUtil.toDec(boxIdHex), canId, values, LocalDateTime.now());
    }
}
