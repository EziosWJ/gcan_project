package cn.ezios.baseapi.gcan.tcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomFrameDecoderTest {

    private static final int FRAME_LEN = 17;
    private static final byte H1 = (byte) 0xC0;
    private static final byte H2 = (byte) 0xA8;
    private static final byte H3 = (byte) 0x01;

    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmbeddedChannel(new CustomFrameDecoder());
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void stickyFrames() {
        byte[] two = concat(
                frame((byte) 0x01, "1836FF30", 1, 2, 3, 4, 5, 6, 7, 8),
                frame((byte) 0x02, "043D9CF4", 9, 10, 11, 12, 13, 14, 15, 16));
        channel.writeInbound(Unpooled.wrappedBuffer(two));
        assertEquals(2, drainInbound().size());
    }

    @Test
    void dirtyByteBeforeHeader() {
        byte[] input = concat(new byte[]{0x55}, frame((byte) 0x01, "1836FF30", 1, 2, 3, 4, 5, 6, 7, 8));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
        assertEquals(1, drainInbound().size());
    }

    @Test
    void invalidLengthByteSkippedAndRecovered() {
        byte[] badLen = frame((byte) 0x01, "1836FF30", 1, 2, 3, 4, 5, 6, 7, 8);
        badLen[4] = 0x77;
        byte[] input = concat(badLen, frame((byte) 0x02, "043D9CF4", 9, 10, 11, 12, 13, 14, 15, 16));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
        assertEquals(1, drainInbound().size());
    }

    @Test
    void partialThenComplete() {
        byte[] full = frame((byte) 0x03, "11223344", 1, 2, 3, 4, 5, 6, 7, 8);
        channel.writeInbound(Unpooled.wrappedBuffer(full, 0, 5));
        assertNull(channel.readInbound());

        channel.writeInbound(Unpooled.wrappedBuffer(full, 5, full.length - 5));
        List<Object> out = drainInbound();
        assertEquals(1, out.size());
        assertTrue(java.util.Arrays.equals(full, (byte[]) out.get(0)));
    }

    private List<Object> drainInbound() {
        List<Object> out = new ArrayList<>();
        Object o;
        while ((o = channel.readInbound()) != null) {
            out.add(o);
        }
        return out;
    }

    private byte[] frame(byte addr, String canId, int... data) {
        byte[] f = new byte[FRAME_LEN];
        f[0] = H1;
        f[1] = H2;
        f[2] = H3;
        f[3] = addr;
        f[4] = (byte) 0x88;
        byte[] can = hexToBytes(canId);
        System.arraycopy(can, 0, f, 5, 4);
        for (int i = 0; i < 8; i++) {
            f[9 + i] = (byte) data[i];
        }
        return f;
    }

    private byte[] concat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) {
            len += p.length;
        }
        byte[] r = new byte[len];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, r, pos, p.length);
            pos += p.length;
        }
        return r;
    }

    private byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] d = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            d[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return d;
    }
}
