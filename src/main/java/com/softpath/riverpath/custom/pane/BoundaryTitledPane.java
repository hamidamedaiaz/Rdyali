package com.softpath.riverpath.custom.pane;

import javafx.scene.control.TitledPane;
import lombok.Getter;
import lombok.Setter;

/**
 * Custom node to handle boundary definition
 *
 * @author rhajou
 */
@Getter
@Setter
public class BoundaryTitledPane extends TitledPane {

    public BoundaryTitledPane() {
        super();
        // Set the behavior to toggle content visibility on click
        setOnMouseClicked(event -> setExpanded(isExpanded()));
    }

}
