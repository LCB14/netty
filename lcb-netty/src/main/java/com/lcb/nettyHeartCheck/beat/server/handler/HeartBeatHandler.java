package com.lcb.nettyHeartCheck.beat.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;

/**
 *  主要完成下面操作：
 *
 *  1、启动服务端，启动客户端
 *  2、客户端向服务端发送"I am alive"，并sleep随机时间，用来模拟空闲。
 *  3、服务端接收客户端消息，并返回"copy that"，客户端空闲时 计数+1.
 *  4、服务端客户端继续通信
 *  5、服务端检测客户端空闲太多，关闭连接。客户端发现连接关闭了，就退出了。
 */
public class HeartBeatHandler extends SimpleChannelInboundHandler<String> {

    int readIdleTimes = 0;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
        System.out.println(" ====== > [server] message received : " + s);
        if ("I am alive".equals(s)) {
            ctx.channel().writeAndFlush("copy that");
        } else {
            System.out.println(" 其他信息处理 ... ");
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        IdleStateEvent event = (IdleStateEvent) evt;

        String eventType = null;
        switch (event.state()) {
            case READER_IDLE:
                eventType = "读空闲";
                readIdleTimes++; // 读空闲的计数加1
                break;
            case WRITER_IDLE:
                eventType = "写空闲";
                // 不处理
                break;
            case ALL_IDLE:
                eventType = "读写空闲";
                // 不处理
                break;
        }
        System.out.println(ctx.channel().remoteAddress() + "超时事件：" + eventType);
        if (readIdleTimes > 3) {
            System.out.println(" [server]读空闲超过3次，关闭连接");
            ctx.channel().writeAndFlush("you are out");
            ctx.channel().close();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.err.println("=== " + ctx.channel().remoteAddress() + " is active ===");
    }

}