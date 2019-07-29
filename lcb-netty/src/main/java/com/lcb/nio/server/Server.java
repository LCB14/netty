package com.lcb.nio.server;

/**
 * @author changbao.li
 * @Description NIO服务端
 * @Date 2019-07-29 22:38
 */
public class Server {

    private static final int PORT = 8000;

    public static void main(String[] args) {

        new ServerHandler(PORT).start();
    }
}
