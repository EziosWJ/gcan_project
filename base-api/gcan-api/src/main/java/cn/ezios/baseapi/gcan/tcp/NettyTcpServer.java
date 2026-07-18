package cn.ezios.baseapi.gcan.tcp;

import cn.ezios.baseapi.gcan.config.GcanProperties;
import cn.ezios.baseapi.gcan.raw.RawCanFrameSnapshotStore;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NettyTcpServer {

    private final GcanProperties properties;
    private final RawCanFrameSnapshotStore snapshotStore;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    public NettyTcpServer(GcanProperties properties, RawCanFrameSnapshotStore snapshotStore) {
        this.properties = properties;
        this.snapshotStore = snapshotStore;
    }

    @PostConstruct
    public void start() throws InterruptedException {
        if (properties.getMirror().isEnabled()) {
            log.info("GCAN TCP server disabled because raw CAN mirror is enabled");
            return;
        }
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup(8);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new CustomFrameDecoder());
                        ch.pipeline().addLast(new TcpServerHandler(snapshotStore));
                    }
                });

        channelFuture = bootstrap.bind(properties.getTcp().getPort()).sync();
        log.info("GCAN TCP server started on port {}", properties.getTcp().getPort());

        Thread thread = new Thread(() -> {
            try {
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("GCAN TCP server interrupted", e);
            } finally {
                shutdown();
            }
        }, "GcanTcpServerThread");
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("GCAN TCP server stopped");
    }
}
