package com.lcb.apply.heart.beat.client;

/**
 * @author changbao.li
 * @since 12 八月 2019
 */
public class ClientBootStrap {

    public static void main(String[] args) throws Exception {
        HeartBeatClient client = new HeartBeatClient(8090);
        client.start();
    }
}
