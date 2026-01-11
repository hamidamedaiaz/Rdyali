package com.softpath.riverpath.util;

import javafx.application.Platform;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * @author rahajou
 */
public class ProgressReporter {
    private static Consumer<String> listener;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void setListener(Consumer<String> l) {
        listener = l;
    }

    public static void report(String message) {
        String timestampMessage = LocalDateTime.now().format(FORMATTER) + " : " + message;
        if (listener != null) {
            Platform.runLater(() -> listener.accept(timestampMessage));
        }
    }
}
