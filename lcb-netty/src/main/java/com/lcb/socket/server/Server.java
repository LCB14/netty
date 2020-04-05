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
        new Thread(() -> doStart()).start();
    }

    public void doStart() {
        int i = 0;
        // 与使用while(true)相比，下面方式可以避免CPU满负载
        while (!Thread.interrupted()) {
            try {
                Socket client = serverSocket.accept();
                System.out.println(client + "-" + i + "-" + Thread.interrupted());
                // 开辟一个新线程,这样后面的请求就不会因为只有一个主线程在处理客户端请求而阻塞了
                new ServerHandler(client).start();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("服务端异常");
            }
            i++;
            System.out.println("end");
        }
    }
}
