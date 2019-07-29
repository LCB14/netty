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
        final Socket socket = new Socket(HOST, PORT);

        // 模拟多个客户端请求服务端
        new Thread(() -> {
            System.out.println("客户端成功启动...");
            while (true) {
                try {
                    String message = "from client: hello Server";
                    System.out.println("客户端向服务端发送内容为:" + message);
                    socket.getOutputStream().write(message.getBytes());
                    new ClientHandler(socket).read();
                } catch (Exception e) {
                    System.out.println("客户端发送数据出错");
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
