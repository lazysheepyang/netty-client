package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;


/**
 * 重连检测狗，当发现当前的链路不稳定关闭之后，进行2次重连
 */
@Sharable
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask, ChannelHandlerHolder {


    private final static InternalLogger log = InternalLoggerFactory.getInstance(ConnectionWatchdog.class);
    private final Bootstrap bootstrap;
    private final Timer timer;
    private final int port;
    private final String host;
    private volatile boolean reconnect = true;
    private int attempts;


    ConnectionWatchdog(Bootstrap bootstrap, Timer timer, int port, String host, boolean reconnect) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.port = port;
        this.host = host;
        this.reconnect = reconnect;
    }

    /**
     * channel链路每次active的时候，将其连接的次数重新☞ 0
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("当前链路已经激活了，重连尝试次数重新置为0");
        attempts = 0;
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("链接关闭");

        if (reconnect) {
            log.info("链接关闭，将进行重连");
            if (attempts < 2) {
                attempts++;
                //重连的间隔时间会越来越长
                int timeout = 2 << attempts;
                timer.newTimeout(this, timeout, TimeUnit.MILLISECONDS);
            }
        }
        ctx.fireChannelInactive();
    }


    public void run(Timeout timeout) throws Exception {

        ChannelFuture future;
        //bootstrap已经初始化好了，只需要将handler填入就可以了
        synchronized (bootstrap) {

            bootstrap.handler(new ChannelInitializer<Channel>() {

                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(handlers());
                    ByteBuf delimiter = Unpooled.copiedBuffer("\r\n".getBytes());
                    ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, delimiter));
                    ch.pipeline().addLast(new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS));
                    ch.pipeline().addLast(new StringDecoder());
                    ch.pipeline().addLast(new StringEncoder());
                    ch.pipeline().addLast(new ConnectorIdleStateTrigger());
                }
            });
            future = bootstrap.connect(host, port);
        }
        //future对象
        future.addListener((ChannelFutureListener) f -> {
            boolean succeed = f.isSuccess();
            //如果重连失败，则调用ChannelInactive方法，再次触发重连事件，尝试2次，如果失败则不再重连
            if (!succeed) {
                log.info("重连失败");
                f.channel().pipeline().fireChannelInactive();
            } else {
                log.info("重连成功");

            }
        });

    }

}
