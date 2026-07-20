package cn.ezios.baseapi.gcan.raw;

import java.time.LocalDateTime;
import java.util.Arrays;
import lombok.Getter;

@Getter
public class RawCanFrame {

    private final String boxIdHex;
    private final Integer boxIdDec;
    private final String canId;
    private final int[] values;
    private final LocalDateTime receivedAt;
    private final RawCanFrameSource source;

    public RawCanFrame(String boxIdHex, Integer boxIdDec, String canId, int[] values, LocalDateTime receivedAt) {
        this(boxIdHex, boxIdDec, canId, values, receivedAt, RawCanFrameSource.TCP);
    }

    public RawCanFrame(String boxIdHex,
                       Integer boxIdDec,
                       String canId,
                       int[] values,
                       LocalDateTime receivedAt,
                       RawCanFrameSource source) {
        this.boxIdHex = boxIdHex;
        this.boxIdDec = boxIdDec;
        this.canId = canId;
        this.values = Arrays.copyOf(values, values.length);
        this.receivedAt = receivedAt;
        this.source = source == null ? RawCanFrameSource.TCP : source;
    }

    public int getValue(int index) {
        return values[index];
    }

    public int[] values() {
        return Arrays.copyOf(values, values.length);
    }
}
