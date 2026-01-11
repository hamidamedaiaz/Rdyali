package com.softpath.riverpath.controller;


import com.softpath.riverpath.model.Boundary;
import com.softpath.riverpath.model.Coordinates;
import com.softpath.riverpath.util.DomainProperties;
import com.softpath.riverpath.util.UtilityClass;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Shape;
import lombok.Getter;

/**
 * BaseBoundaryController
 *
 * @author rhajou
 */
public abstract class BaseBoundaryController extends FieldContainerController {

    @FXML
    @Getter
    private GridPane shapeGridpane;

    /**
     * Property to track if there are unsaved changes in this controller
     */
    @Getter
    private final BooleanProperty dirtyProperty = new SimpleBooleanProperty(false);

    /**
     * @see FieldContainerController#updateRootModifiedState()
     */
    @Override
    protected void updateRootModifiedState() {
        // do nothing
    }

    /**
     * Reload the controller to the initial state
     */
    protected void reload() {
        rollbackField();
        setDirty(false);
    }

    /**
     * Commit the changes made by the user
     */
    protected void commit() {
        saveFields();
        setDirty(false);
    }

    /**
     * Import the values of the boundary
     *
     * @param boundary the boundary (circle, rectangle, ...)
     */
    abstract void importValues(Boundary boundary);

    /**
     * Get the shape of the boundary adapted to domain properties
     *
     * @param domainProperties the domain properties
     * @param origin           the origin
     * @return the shape
     */
    abstract <T extends Node> T getShape(DomainProperties domainProperties, Coordinates origin);

    /**
     * Handle the key released event for numbers
     *
     * @param keyEvent the key event
     */
    @FXML
    private void handleKeyReleased(KeyEvent keyEvent) {
        UtilityClass.checkNotBlank((TextField) keyEvent.getSource());
        UtilityClass.handleTextWithDigitOnly(keyEvent);
    }

    /**
     * Set the dirty state
     *
     * @param dirty true if there are unsaved changes
     */
    public void setDirty(boolean dirty) {
        dirtyProperty.set(dirty);
    }


}
