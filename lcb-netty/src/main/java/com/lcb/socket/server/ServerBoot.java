package com.lcb.socket.server;

/**
 * @author changbao.li
 * @Description 启动服务端
 * @Date 2019-07-29 14:56
 */
public class ServerBoot {
    private static final int PORT = 8000;

    public static void main(String[] args) {
        Server server = new Server(PORT);
        server.start();
    }
}
