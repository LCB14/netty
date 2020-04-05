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
            // 1、获取Selector选择器
            selector = Selector.open();

            // 2、获取通道
            serverSocketChannel = ServerSocketChannel.open();

            // 3.设置为非阻塞
            serverSocketChannel.configureBlocking(false);

            /**
             *  4、绑定连接
             *
             *  请求队列最大能缓存的请求连接数
             *
             *  @param backlog -- requested maximum length of the queue of incoming connections.
             */
//            serverSocketChannel.socket().bind(new InetSocketAddress(port), 1024);
            serverSocketChannel.bind(new InetSocketAddress(port), 1024);

            // 5、将通道注册到选择器上,并注册的操作为：“接收”操作
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
        // 6、采用轮询的方式，查询获取“准备就绪”的注册过的操作
        try {
            while (!stop) {
                selector.select(1000);
                // 7、获取当前选择器中所有注册的选择键（“已经准备就绪的操作”）
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                SelectionKey key = null;
                while (iterator.hasNext()) {
                    // 8、获取“准备就绪”的事件
                    key = iterator.next();
                    handInput(key);
                    // 15、移除选择键
                    iterator.remove();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            // 16.关闭资源
            if (serverSocketChannel != null) {
                try {
                    serverSocketChannel.close();
                } catch (Exception e) {
                    e.printStackTrace();
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
    }

    private void handInput(SelectionKey key) throws Exception {
        if (key.isValid()) {
            // 9、判断key是具体的什么事件
            if (key.isAcceptable()) {
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                // 10、若接受的事件是“接收就绪” 操作,就获取客户端连接
                SocketChannel sc = serverSocketChannel.accept();
                // 11、切换为非阻塞模式
                sc.configureBlocking(false);
                // 12、将该通道注册到selector选择器上
                sc.register(selector, SelectionKey.OP_READ);
            } else if (key.isReadable()) {
                // 13、获取该选择器上的“读就绪”状态的通道
                SocketChannel sc = (SocketChannel) key.channel();
                // 14、读取数据
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
