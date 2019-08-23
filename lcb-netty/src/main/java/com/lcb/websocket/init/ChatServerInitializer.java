package com.lcb.websocket.init;

import com.lcb.websocket.handler.HttpRequestHandler;
import com.lcb.websocket.handler.TextWebSocketFrameHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class ChatServerInitializer extends ChannelInitializer<Channel> {

    private final ChannelGroup group;

    public ChatServerInitializer(ChannelGroup group) {
        this.group = group;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        /**
         * HttpServerCodec -- 将字节解码为HttpRequest、HttpContent和LastHttpContent.
         * 并将HttpRequest、HttpContent和LastHttpContent编码为字节。
         */
        pipeline.addLast(new HttpServerCodec());

        /**
         * 写入一个文件内容
         */
        pipeline.addLast(new ChunkedWriteHandler());

        /**
         * 将一个HttpMessage和跟随它的多个HttpContent聚合为单个FullHttpRequest或者FullHttpResponse,
         * 经过该handler处理之后数据流向下一个handler将会收到完整的HTTP请求或响应。
         */
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));

        /**
         * 自定义处理FullHttpRequest（那些不发送到/ws URI的请求)
         */
        pipeline.addLast(new HttpRequestHandler("/ws"));

        /**
         * 按照WebSocket规范要求，处理WebSocket升级握手、PingWebSocketFrame、PongWebSocketFrame、CloseWebSocketFrame
         */
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

        /**
         * 处理TextWebSocketFrame和握手完成事件
         */
        pipeline.addLast(new TextWebSocketFrameHandler(group));
    }
}
