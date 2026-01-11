package com.softpath.riverpath.custom.pane;

import javafx.scene.control.ScrollPane;

/**
 * A custom ScrollPane that is zoomable and pannable.
 *
 * @author rhajou
 * @see ScrollPane
 */
public class ZoomableScrollPane extends ScrollPane {

    public ZoomableScrollPane() {
        super();
        getStyleClass().add("zoomable-scroll-pane");
        setPannable(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.NEVER);
    }

}