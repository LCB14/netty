package lcb.reactor.handler;

import lcb.reactor.event.Event;

public abstract class EventHandler {

    public abstract void handle(Event event);

}