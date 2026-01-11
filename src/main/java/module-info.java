module com.softpath.riverpath {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires static lombok;
    requires org.apache.commons.lang3;
    requires org.apache.commons.io;
    requires velocity.engine.core;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires java.desktop;
    requires reactfx;
    requires lexactivator;
    requires java.prefs;
    requires java.logging;
    requires javafx.graphics;
    // LWJGL
    requires org.lwjgl;
    requires org.lwjgl.opengl;
    requires org.lwjgl.glfw;
    // JOML (math)
    requires org.joml;

    opens com.softpath.riverpath to javafx.fxml;
    exports com.softpath.riverpath;
    exports com.softpath.riverpath.custom.pane;
    opens com.softpath.riverpath.custom.pane to javafx.fxml;
    exports com.softpath.riverpath.controller;
    opens com.softpath.riverpath.controller to javafx.fxml;
    exports com.softpath.riverpath.model;
    opens com.softpath.riverpath.model to com.fasterxml.jackson.databind;
    exports com.softpath.riverpath.service;
    opens com.softpath.riverpath.service to javafx.fxml;
    exports com.softpath.riverpath.fileparser;
}