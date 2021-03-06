package com.lcb.socket.client;

import java.io.InputStream;
import java.net.Socket;

/**
 * @author changbao.li
 * @Description 客户端处理器
 * @Date 2019-07-29 15:21
 */
public class ClientHandler {
    public static final int MAX_DATA_LEN = 1024;
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void read() {
        doRead();
    }

    public void doRead() {
        InputStream inputStream = null;
        byte[] data = new byte[MAX_DATA_LEN];
        int len;
        try {
            inputStream = socket.getInputStream();
            // inputStream.read(data)读取时会阻塞程序的执行，直到有数据返回。
            while ((len = inputStream.read(data)) != -1) {
                String message = new String(data, 0, len);
                System.out.println("收到服务端回复内容:" + message);
                // 尝试把下面两行代码都注释掉，看看会发生啥
                String toServer = "hello server,i come back";
                socket.getOutputStream().write(toServer.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("读取服务端数据失败");
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
