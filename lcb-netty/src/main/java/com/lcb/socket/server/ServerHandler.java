package com.lcb.socket.server;

import java.io.InputStream;
import java.net.Socket;

/**
 * @author changbao.li
 * @Description 服务端处理器
 * @Date 2019-07-29 15:05
 */
public class ServerHandler {
    public static final int MAX_DATA_LEN = 1024;
    private final Socket socket;

    public ServerHandler(Socket socket) {
        this.socket = socket;
    }

    public void start() {
        new Thread(() -> {
            System.out.println("新的客户端接入..." + Thread.currentThread().getName());
            doStart();
        }).start();
    }

    public void doStart() {
        InputStream inputStream = null;
        try {
            inputStream = socket.getInputStream();
            byte[] data = new byte[MAX_DATA_LEN];
            int len;
            // inputStream.read(data)读取时会阻塞程序的执行，直到有数据返回。
            while ((len = inputStream.read(data)) != -1) {
                String message = new String(data, 0, len);
                System.out.println("收到客户端传过来的消息:" + message);
                String toClient = "form server: hello client";
                socket.getOutputStream().write(toClient.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("服务端读取客户端请求数据异常");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
