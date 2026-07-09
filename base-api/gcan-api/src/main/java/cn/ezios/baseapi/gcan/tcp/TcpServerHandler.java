package cn.ezios.baseapi.gcan.tcp;

import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.raw.RawCanFrameSnapshotStore;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpServerHandler extends ChannelInboundHandlerAdapter {

    private final RawCanFrameSnapshotStore snapshotStore;

    public TcpServerHandler(RawCanFrameSnapshotStore snapshotStore) {
        this.snapshotStore = snapshotStore;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte[] bytes = (byte[]) msg;
        log.debug("Received GCAN frame: {}", ByteBufUtil.hexDump(bytes).toUpperCase());
        RawCanFrame frame = GcanFrameParser.parse(bytes);
        snapshotStore.put(frame);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in GCAN TCP handler", cause);
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("GCAN client connected: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("GCAN client disconnected: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }
}
