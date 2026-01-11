package com.softpath.riverpath.custom.event;

// EventManager.java

import javafx.event.EventHandler;

public class EventManager {

    // A method to fire a custom event
    public static void fireCustomEvent(CustomEvent event) {
        event.consume();
        EventDispatcher.getInstance().dispatchEvent(event);
    }

    // A method to add an event handler for a specific event type
    public static void addEventHandler(EventEnum eventEnum, EventHandler<CustomEvent> eventHandler) {
        EventDispatcher.getInstance().addEventHandler(eventEnum, eventHandler);
    }
}

