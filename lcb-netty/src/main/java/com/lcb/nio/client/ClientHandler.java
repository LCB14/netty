package com.lcb.nio.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author changbao.li
 * @Description 客户端处理器
 * @Date 2019-07-29 23:48
 */
public class ClientHandler {
    private String host;
    private int port;
    private Selector selector;
    private SocketChannel sc;
    private volatile boolean stop;

    public ClientHandler(String address, int port) {
        try {
            this.host = address;
            this.port = port;
            selector = Selector.open();
            sc = SocketChannel.open();
            sc.configureBlocking(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        this.stop = true;
    }

    public void start() {
        new Thread(() -> {
            doStart();
        }).start();
    }

    private void doStart() {
        try {
            doConnect();
            while (!stop) {
                selector.select(1000);
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        handInput(key);
                    } catch (Exception e) {
                        key.cancel();
                        if (key.channel() != null) {
                            key.channel().close();
                        }
                    }
                }
            }

            if (selector != null) {
                try {
                    selector.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void doConnect() throws IOException {
        if (sc.connect(new InetSocketAddress(host, port))) {
            sc.register(selector, SelectionKey.OP_READ);
            doWrite(sc);
        } else {
            sc.register(selector, SelectionKey.OP_CONNECT);
        }
    }

    private void handInput(SelectionKey key) throws IOException {
        if (key.isValid()) {
            SocketChannel sc = (SocketChannel) key.channel();
            if (key.isConnectable()) {
                if (sc.finishConnect()) {
                    sc.register(selector, SelectionKey.OP_READ);
                    doWrite(sc);
                }
            }

            if (key.isReadable()) {
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readBytes = sc.read(readBuffer);
                if (readBytes > 0) {
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String message = new String(bytes, "UTF-8");
                    System.out.println("收到服务端回复消息：" + message);
                    stop();
                } else if (readBytes < 0) {
                    key.cancel();
                    sc.close();
                }
            }
        }
    }

    private void doWrite(SocketChannel sc) throws IOException {
        byte[] req = "hello Server".getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
        writeBuffer.put(req);
        writeBuffer.flip();
        sc.write(writeBuffer);
        if (!writeBuffer.hasRemaining()) {
            System.out.println("成功向服务端发送请求");
        }
    }
}
