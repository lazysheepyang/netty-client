package client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;


/**
 * Created by ly on 2017/12/2.
 */
public class NettyHeartBeatHandler extends ChannelInboundHandlerAdapter {

    private final static InternalLogger log = InternalLoggerFactory.getInstance(NettyHeartBeatHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SocketChannel channel = (SocketChannel) ctx.channel();
        log.info(channel + "channelActive!");

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        log.info("客户端与服务器的长连接断开！");
        SocketChannel channel = (SocketChannel) ctx.channel();
        log.info(channel + "channelInActive！");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = msg.toString();
        log.info("客户端收到服务端回复的信息 " + message);
        if ("HB".equals(message)) {
            log.info("客户端收到服务端发过来的HB===> " + ctx.channel().remoteAddress());
        } else {
            if (message != null && !"".equals(message.trim()) && !"null".equals(message.trim())) {
                log.info("客户端收到服务端回复的信息 " + message);
                //TODO 客户端对服务端回复的信息进行处理的逻辑
            }
        }
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
        SocketChannel channel = (SocketChannel) ctx.channel();
        log.info(channel + "exceptionCaught！" + cause.toString());
    }


}
