package com.lcb.reactor.handler;

import com.lcb.reactor.event.Event;

public abstract class EventHandler {

    public abstract void handle(Event event);

}