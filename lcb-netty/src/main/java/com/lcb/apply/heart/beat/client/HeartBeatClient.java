package com.lcb.apply.heart.beat.client;

import com.lcb.apply.heart.beat.client.init.HeartBeatClientInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Random;

public class HeartBeatClient {

    int port;
    Channel channel;
    Random random;

    public HeartBeatClient(int port) {
        this.port = port;
        random = new Random();
    }

    public void start() {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new HeartBeatClientInitializer());

            connect(bootstrap, port);
            String text = "I am alive";
            while (channel.isActive()) {
                sendMsg(text);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // do something
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    public void connect(Bootstrap bootstrap, int port) throws Exception {
        channel = bootstrap.connect("localhost", 8090).sync().channel();
    }

    public void sendMsg(String text) throws Exception {
        int num = random.nextInt(10);
        Thread.sleep(num * 1000);
        channel.writeAndFlush(text);
    }
}