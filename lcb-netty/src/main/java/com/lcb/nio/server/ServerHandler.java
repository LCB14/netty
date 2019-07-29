package com.lcb.nio.server;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author changbao.li
 * @Description 服务端处理器
 * @Date 2019-07-29 22:49
 */
public class ServerHandler {

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private volatile boolean stop;

    public ServerHandler(int port) {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            /**
             *  @param backlog -- requested maximum length of the queue of incoming connections.
             *
             *  请求队列最大能缓存的请求连接数
             */
            serverSocketChannel.socket().bind(new InetSocketAddress(port), 1024);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务端启动成功...");
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
        while (!stop) {
            try {
                selector.select(1000);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                SelectionKey key = null;
                while (iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();
                    handInput(key);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        if (selector != null) {
            try {
                selector.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handInput(SelectionKey key) throws Exception {
        if (key.isValid()) {

            if (key.isAcceptable()) {
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                SocketChannel sc = serverSocketChannel.accept();
                sc.configureBlocking(false);
                sc.register(selector, SelectionKey.OP_READ);
            }

            if (key.isReadable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer bf = ByteBuffer.allocate(1024);
                int readBytes = sc.read(bf);
                if (readBytes > 0) {
                    bf.flip();
                    // remaining()方法返回ByteBuffer目前的实际数据容量
                    byte[] bytes = new byte[bf.remaining()];
                    bf.get(bytes);
                    String message = new String(bytes, "UTF-8");
                    System.out.println("收到客服端请求消息:" + message);

                    String toClient = "hello client";
                    doWrite(sc, toClient);
                } else if (readBytes < 0) {
                    key.cancel();
                    sc.close();
                }
            }
        }
    }

    private void doWrite(SocketChannel channel, String response) throws Exception {
        if (response != null && response.trim().length() > 0) {
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            channel.write(writeBuffer);
        }
    }
}
