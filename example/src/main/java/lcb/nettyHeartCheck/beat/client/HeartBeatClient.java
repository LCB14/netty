package lcb.nettyHeartCheck.beat.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lcb.nettyHeartCheck.beat.client.init.HeartBeatClientInitializer;

import java.util.Random;

public class HeartBeatClient {

    private static final String text = "I am alive";

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
        channel = bootstrap.connect("localhost", port).sync().channel();
    }

    public void sendMsg(String text) throws Exception {
        int num = random.nextInt(10);
        Thread.sleep(num * 1000);
        channel.writeAndFlush(text);
    }
}