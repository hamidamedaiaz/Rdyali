package com.softpath.riverpath.custom.event;

// EventDispatcher.java

import javafx.event.EventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDispatcher {

    private static EventDispatcher instance;

    // Map to store event handlers for different event types
    private final Map<EventEnum, List<EventHandler<CustomEvent>>> eventHandlers = new HashMap<>();

    private EventDispatcher() {
    }

    // Singleton pattern
    public static EventDispatcher getInstance() {
        if (instance == null) {
            instance = new EventDispatcher();
        }
        return instance;
    }

    // A method to add an event handler for a specific event type
    public void addEventHandler(EventEnum eventEnum, EventHandler<CustomEvent> eventHandler) {
        if (eventHandlers.containsKey(eventEnum)) {
            eventHandlers.get(eventEnum).add(eventHandler);
        } else {
            List<EventHandler<CustomEvent>> handlers = new ArrayList<>();
            handlers.add(eventHandler);
            eventHandlers.put(eventEnum, handlers);
        }
    }

    // A method to dispatch an event to its handlers
    public void dispatchEvent(CustomEvent event) {
        for (EventHandler<CustomEvent> h : eventHandlers.get(event.getEventEnum())) {
            h.handle(event);
        }
    }
}

