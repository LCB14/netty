package com.lcb.socket.client;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * @author changbao.li
 * @Description 客户端
 * @Date 2019-07-29 15:21
 */
public class Client {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8000;
    private static final int SLEEP_TIME = 3;

    public static void main(String[] args) throws IOException {
        final Socket socketA = new Socket(HOST, PORT);
        // 模拟多个客户端请求服务端
        new Thread(() -> {
            System.out.println("客户端A成功启动...");
            while (true) {
                try {
                    String message = "from clientA: hello Server";
                    System.out.println("客户端A向服务端发送内容为:" + message);
                    socketA.getOutputStream().write(message.getBytes());
                    new ClientHandler(socketA).read();
                } catch (Exception e) {
                    System.out.println("客户端A发送数据出错");
                    e.printStackTrace();
                }

                sleep();
            }
        }).start();

        final Socket socketB = new Socket(HOST, PORT);
        new Thread(() -> {
            System.out.println("客户端B成功启动...");
            while (true) {
                try {
                    String message = "from clientA: hello Server";
                    System.out.println("客户端B向服务端发送内容为:" + message);
                    socketB.getOutputStream().write(message.getBytes());
                    new ClientHandler(socketB).read();
                } catch (Exception e) {
                    System.out.println("客户端B发送数据出错");
                    e.printStackTrace();
                }

                sleep();
            }
        }).start();
    }

    public static void sleep() {
        try {
            TimeUnit.SECONDS.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
            e.printStackTrace();
        }
    }
}
