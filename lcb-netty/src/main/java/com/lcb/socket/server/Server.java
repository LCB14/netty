package com.lcb.socket.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author changbao.li
 * @Description 服务端
 * @Date 2019-07-29 14:59
 */
public class Server {

    private ServerSocket serverSocket;

    public Server(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
            System.out.println("服务端启动成功...");
        } catch (IOException exception) {
            exception.printStackTrace();
            System.out.println("服务端启动失败");
        }
    }

    /**
     * 为每个客户端单独创建一个线程进行通信
     */
    public void start() {
        new Thread(() -> {
            doStart();
        }).start();
    }

    public void doStart() {
        while (true) {
            try {
                Socket client = serverSocket.accept();
                new ServerHandler(client).start();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("服务端异常");
            }
        }
    }
}
