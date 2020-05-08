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
    private static final int PORT = 8000;

    private ServerSocket serverSocket;

    public Server(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
            System.out.println("服务端启动成功...");
        } catch (IOException exception) {
            System.out.println("服务端启动失败");
            exception.printStackTrace();
        }
    }

    /**
     * 为每个客户端单独创建一个线程进行通信
     */
    public void start() {
        new Thread(() -> doStart()).start();
    }

    public void doStart() {
        // 与使用while(true)相比，下面方式可以避免CPU满负载
        while (!Thread.interrupted()) {
            try {
                Socket client = serverSocket.accept();
                System.out.println("连接socket信息：" + client + "-" + Thread.interrupted());
                // 开辟一个新线程,这样后面的请求就不会因为只有一个主线程在处理客户端请求而阻塞了
                new ServerHandler(client).start();
            } catch (Exception e) {
                System.out.println("服务端异常");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server(PORT);
        server.start();
    }
}
