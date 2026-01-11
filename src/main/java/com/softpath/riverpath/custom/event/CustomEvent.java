package com.softpath.riverpath.custom.event;

import javafx.event.Event;
import javafx.event.EventType;
import lombok.Getter;

/**
 * A custom event class
 *
 * @author rhajou
 */
@Getter
public class CustomEvent extends Event {

    private final EventEnum eventEnum;
    private String message;
    private Object object;
    private String newValue;

    public CustomEvent(EventEnum eventEnum) {
        super(EventType.ROOT);
        this.eventEnum = eventEnum;
    }

    public CustomEvent(EventEnum eventEnum, String message) {
        super(EventType.ROOT);
        this.eventEnum = eventEnum;
        this.message = message;
    }

    public CustomEvent(EventEnum eventEnum, Object object) {
        super(EventType.ROOT);
        this.eventEnum = eventEnum;
        this.object = object;
    }

    public CustomEvent(EventEnum eventEnum, String message, String newValue) {
        super(EventType.ROOT);
        this.eventEnum = eventEnum;
        this.message = message;
        this.newValue = newValue;
    }

    public CustomEvent(EventEnum eventEnum, String message, Object object) {
        super(EventType.ROOT);
        this.eventEnum = eventEnum;
        this.object = object;
        this.message = message;
    }
}
