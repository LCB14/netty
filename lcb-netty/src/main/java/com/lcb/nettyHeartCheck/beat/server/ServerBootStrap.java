package com.lcb.nettyHeartCheck.beat.server;

/**
 * @author changbao.li
 * @since 12 八月 2019
 */
public class ServerBootStrap {

    public static void main(String[] args) throws Exception {
        HeartBeatServer server = new HeartBeatServer(8090);
        server.start();
    }
}
