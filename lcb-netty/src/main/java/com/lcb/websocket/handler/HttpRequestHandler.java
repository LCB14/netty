package com.lcb.websocket.handler;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.RandomAccessFile;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String wsUri;

    public HttpRequestHandler(String wsUri) {
        this.wsUri = wsUri;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 如果是WebSocket请求，则保留数据并传递到下一个ChannelHandler
        if (wsUri.equalsIgnoreCase(request.uri())) {
            /**
             * 这里之所以需要调用retain()方法，是因为调用channelRead0()方法完成之后，它将调用
             * FullHttpRequest对象上的release()方法以释放它的资源。
             */
            ctx.fireChannelRead(request.retain());
        } else {
            if (HttpUtil.is100ContinueExpected(request)) {
                /**
                 * 收到100-continue，则返回给客户端100
                 */
                send100Continue(ctx);
            }
            boolean keepAlive;
            ChannelFuture future;
            try (RandomAccessFile file = new RandomAccessFile(this.getClass().getResource("/").getPath() + "chat.html", "r")) {
                HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());
                keepAlive = HttpUtil.isKeepAlive(request);
                if (keepAlive) {
                    // 如果需要keep-alive，则添加相应头信息
                    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }
                ctx.write(response);
                if (ctx.pipeline().get(SslHandler.class) == null) {
                    // 不用加密使用零内存复制发送文件
                    future = ctx.writeAndFlush(new DefaultFileRegion(file.getChannel(), 0, file.length()));
                } else {
                    future = ctx.writeAndFlush(new ChunkedNioFile(file.getChannel()));
                }
            }

            // 如果不是keep-alive，则关闭Channel
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }

    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
