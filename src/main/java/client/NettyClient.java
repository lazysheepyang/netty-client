package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.HashedWheelTimer;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

/**
 * Created by ly on 2017/12/2.
 */
public class NettyClient {

    private final static InternalLogger log = InternalLoggerFactory.getInstance(NettyClient.class);
    private SocketChannel socketChannel;
    private static  HashedWheelTimer timer = new HashedWheelTimer();
    private ConnectorIdleStateTrigger idleStateTrigger;

    private  boolean isSessionAvaliable() {
        return socketChannel.isOpen() && socketChannel.isActive();
    }

    public void sendMessage(String request) {
        if (!isSessionAvaliable()) {
              log.info("客户端与服务器连接不可用，发送消息失败！");
        } else {
            ByteBuf REQUEST = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(request + "\r\n",
                    CharsetUtil.UTF_8));
            ChannelFuture future = socketChannel.writeAndFlush(REQUEST.duplicate());
            future.addListener((ChannelFutureListener) future1 -> {
                if (future1.isSuccess()) {
                    log.info("客户端发送请求成功！" + "发送的内容为：" + request + "端口号=" + future.channel().localAddress().toString());
                } else {
                         log.info("客户端发送请求失败！");
                    Throwable cause = future1.cause();
                    cause.printStackTrace();
                }
            });
        }
    }


    public void connect(int port, String host) throws Exception {
        idleStateTrigger = new ConnectorIdleStateTrigger();
        EventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap boot = new Bootstrap();
        boot.group(group)
             .channel(NioSocketChannel.class)
             .remoteAddress(new InetSocketAddress(host, port))
             .handler(new LoggingHandler(LogLevel.INFO))
        ;
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(boot, timer, port, host, true) {
            public ChannelHandler[] handlers() {
                ByteBuf delimiter = Unpooled.copiedBuffer("\r\n".getBytes());

                return new ChannelHandler[]{
                        this,
                        new DelimiterBasedFrameDecoder(2048, delimiter), //分隔符
                        new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS),
                        new StringDecoder(),
                        new StringEncoder(),
                        idleStateTrigger,
                        new NettyHeartBeatHandler()
                };
            }
        };

        ChannelFuture future;
        //进行连接
        try {
            synchronized (boot) {
                boot.handler(new ChannelInitializer<Channel>() {

                    //初始化channel
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        InputStream resourceAsStream = NettyClient.class.getResourceAsStream("/client.keystore");
                        KeyStore keyStore = KeyStore.getInstance("JKS");
                        char[] password = "123456".toCharArray();
                        keyStore.load(resourceAsStream, password);
                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
                        trustManagerFactory.init(keyStore);
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null, null, null);
                        SSLEngine sslEngine = sslContext.createSSLEngine();
                        sslEngine.setUseClientMode(true);
                        ch.pipeline().addFirst("ssl", new SslHandler(sslEngine));
                        ch.pipeline().addLast(watchdog.handlers());

                    }
                });
                //创建连接的时候指定host和port
                future = boot.connect(host, port);
            }
            //以下代码在synchronized同步快外面是安全的

            future.sync();
            socketChannel = (SocketChannel) future.channel();

        } catch (Throwable t) {
            throw new Exception("connects fail!", t);
        }
    }
}
