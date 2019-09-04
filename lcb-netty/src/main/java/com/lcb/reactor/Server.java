package com.lcb.reactor;

import com.lcb.reactor.dispatch.Dispatcher;
import com.lcb.reactor.event.EventType;
import com.lcb.reactor.handler.AcceptEventHandler;

public class Server {
    Selector selector = new Selector();
    Dispatcher eventLooper = new Dispatcher(selector);
    Acceptor acceptor;

    Server(int port) {
        acceptor = new Acceptor(selector, port);
    }

    public void start() {
        // 注册事件处理器
        eventLooper.registEventHandler(EventType.ACCEPT, new AcceptEventHandler(selector));

        // 监听是否有时间产生并创建事件
        new Thread(acceptor, "Acceptor-" + acceptor.getPort()).start();

        // 分派事件
        eventLooper.handleEvents();
    }
}