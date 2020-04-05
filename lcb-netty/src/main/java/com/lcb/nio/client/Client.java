package com.lcb.nio.client;

/**
 * @author changbao.li
 * @Description 客户端
 * @Date 2019-07-29 23:48
 */
public class Client {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8000;

    public static void main(String[] args) {
        new ClientHandler(HOST, PORT).start();
    }
}
