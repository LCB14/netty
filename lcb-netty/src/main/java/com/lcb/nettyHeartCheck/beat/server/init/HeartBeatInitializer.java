package com.lcb.nettyHeartCheck.beat.server.init;

import com.lcb.nettyHeartCheck.beat.server.handler.HeartBeatHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class HeartBeatInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("decoder", new StringDecoder());
        pipeline.addLast("encoder", new StringEncoder());
        // 时间参数全部设置为2，表示客户端2秒没有读/写，这个超时时间就会被触发。
        pipeline.addLast(new IdleStateHandler(2, 2, 2, TimeUnit.SECONDS));
        pipeline.addLast(new HeartBeatHandler());
    }
}