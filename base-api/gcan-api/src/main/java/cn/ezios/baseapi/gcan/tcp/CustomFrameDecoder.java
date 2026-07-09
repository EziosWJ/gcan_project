package cn.ezios.baseapi.gcan.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomFrameDecoder extends ByteToMessageDecoder {

    private static final int CANID_LENGTH = 4;
    private static final int HEADER_LENGTH = 3;
    private static final int ADDRESS_LENGTH = 1;
    private static final int INFO_LENGTH = 1;

    private static final byte HEADER1 = (byte) 0xC0;
    private static final byte HEADER2 = (byte) 0xA8;
    private static final byte HEADER3 = (byte) 0x01;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (true) {
            if (in.readableBytes() < HEADER_LENGTH) {
                discardBytesBeforePossibleHeader(in);
                return;
            }

            int headerStart = findNextHeader(in);
            if (headerStart < 0) {
                discardBytesBeforePossibleHeader(in);
                return;
            }
            in.skipBytes(headerStart);

            if (in.readableBytes() < HEADER_LENGTH + ADDRESS_LENGTH + INFO_LENGTH) {
                return;
            }

            in.markReaderIndex();
            byte h1 = in.readByte();
            byte h2 = in.readByte();
            byte h3 = in.readByte();
            if (h1 != HEADER1 || h2 != HEADER2 || h3 != HEADER3) {
                in.resetReaderIndex();
                in.readByte();
                continue;
            }

            in.readByte();
            byte lengthByte = in.readByte();
            if (lengthByte != (byte) 0x88 && lengthByte != (byte) 0x08) {
                log.warn("Invalid GCAN frame length: {}", String.format("%02X", lengthByte));
                in.resetReaderIndex();
                in.readByte();
                continue;
            }
            int dataLength = lengthByte & 0x0F;
            int frameLength = dataLength + CANID_LENGTH + HEADER_LENGTH + ADDRESS_LENGTH + INFO_LENGTH;

            int remaining = in.readableBytes();
            if (remaining < frameLength - HEADER_LENGTH - ADDRESS_LENGTH - INFO_LENGTH) {
                in.resetReaderIndex();
                return;
            }

            in.resetReaderIndex();
            byte[] frame = new byte[frameLength];
            in.readBytes(frame);
            out.add(frame);
        }
    }

    private int findNextHeader(ByteBuf in) {
        int from = in.readerIndex();
        int end = in.writerIndex() - 2;
        for (int i = from; i < end; i++) {
            if (in.getByte(i) == HEADER1 && in.getByte(i + 1) == HEADER2 && in.getByte(i + 2) == HEADER3) {
                return i - from;
            }
        }
        return -1;
    }

    private void discardBytesBeforePossibleHeader(ByteBuf in) {
        int readable = in.readableBytes();
        if (readable == 0) {
            return;
        }

        int writer = in.writerIndex();
        int preserve = 0;
        if (readable >= 2 && in.getByte(writer - 2) == HEADER1 && in.getByte(writer - 1) == HEADER2) {
            preserve = 2;
        } else if (in.getByte(writer - 1) == HEADER1) {
            preserve = 1;
        }

        int discard = readable - preserve;
        if (discard > 0) {
            in.skipBytes(discard);
        }
    }
}
