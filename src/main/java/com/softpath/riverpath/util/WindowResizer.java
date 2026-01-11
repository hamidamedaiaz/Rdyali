package com.softpath.riverpath.util;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class WindowResizer {
    private static final int RESIZE_BORDER = 5; // Largeur de détection des bords et coins
    private boolean isResizing = false;
    private ResizeType currentResizeType = ResizeType.NONE;
    private double initialMouseX;
    private double initialMouseY;
    private double initialStageX;
    private double initialStageY;
    private double initialStageWidth;
    private double initialStageHeight;
    private Stage stage;

    public void makeResizable(Stage stage, Scene scene) {
        this.stage = stage;

        // Ajouter un padding à la racine pour créer une zone de détection
        scene.getRoot().setStyle("-fx-padding: 5;"); // Correspond à RESIZE_BORDER
        ((Region) scene.getRoot()).setBackground(Background.EMPTY);

        scene.setOnMouseMoved(this::handleMouseMoved);
        scene.setOnMousePressed(this::handleMousePressed);
        scene.setOnMouseDragged(this::handleMouseDragged);
        scene.setOnMouseReleased(this::handleMouseReleased);
    }

    private void handleMouseMoved(MouseEvent event) {
        if (!isResizing) {
            ResizeType resizeType = getResizeType(event);
            updateCursor(resizeType);
            currentResizeType = resizeType;
        }
    }

    private void handleMousePressed(MouseEvent event) {
        if (currentResizeType != ResizeType.NONE) {
            isResizing = true;
            initialMouseX = event.getScreenX();
            initialMouseY = event.getScreenY();
            initialStageX = stage.getX();
            initialStageY = stage.getY();
            initialStageWidth = stage.getWidth();
            initialStageHeight = stage.getHeight();
            event.consume();
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (isResizing && currentResizeType != ResizeType.NONE) {
            performResize(event.getScreenX(), event.getScreenY());
            event.consume();
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (isResizing) {
            isResizing = false;
            currentResizeType = ResizeType.NONE;
            updateCursor(ResizeType.NONE);
        }
    }

    private ResizeType getResizeType(MouseEvent event) {
        double mouseX = event.getSceneX();
        double mouseY = event.getSceneY();
        double width = stage.getWidth();
        double height = stage.getHeight();

        boolean isWest = mouseX < RESIZE_BORDER;
        boolean isEast = mouseX > width - RESIZE_BORDER;
        boolean isNorth = mouseY < RESIZE_BORDER;
        boolean isSouth = mouseY > height - RESIZE_BORDER;

        if (isNorth && isWest) return ResizeType.NORTH_WEST;
        if (isNorth && isEast) return ResizeType.NORTH_EAST;
        if (isSouth && isWest) return ResizeType.SOUTH_WEST;
        if (isSouth && isEast) return ResizeType.SOUTH_EAST;
        if (isNorth) return ResizeType.NORTH;
        if (isSouth) return ResizeType.SOUTH;
        if (isWest) return ResizeType.WEST;
        if (isEast) return ResizeType.EAST;

        return ResizeType.NONE;
    }

    private void performResize(double screenX, double screenY) {
        double deltaX = screenX - initialMouseX;
        double deltaY = screenY - initialMouseY;

        double newX = initialStageX;
        double newY = initialStageY;
        double newWidth = initialStageWidth;
        double newHeight = initialStageHeight;

        switch (currentResizeType) {
            case NORTH_WEST:
                newWidth = initialStageWidth - deltaX;
                newHeight = initialStageHeight - deltaY;
                if (newWidth >= stage.getMinWidth()) {
                    newX = initialStageX + deltaX;
                } else {
                    newWidth = stage.getMinWidth();
                    newX = initialStageX + (initialStageWidth - newWidth);
                }
                if (newHeight >= stage.getMinHeight()) {
                    newY = initialStageY + deltaY;
                } else {
                    newHeight = stage.getMinHeight();
                    newY = initialStageY + (initialStageHeight - newHeight);
                }
                break;
            case NORTH_EAST:
                newWidth = initialStageWidth + deltaX;
                newHeight = initialStageHeight - deltaY;
                if (newWidth < stage.getMinWidth()) {
                    newWidth = stage.getMinWidth();
                }
                if (newHeight >= stage.getMinHeight()) {
                    newY = initialStageY + deltaY;
                } else {
                    newHeight = stage.getMinHeight();
                    newY = initialStageY + (initialStageHeight - newHeight);
                }
                break;
            case SOUTH_WEST:
                newWidth = initialStageWidth - deltaX;
                newHeight = initialStageHeight + deltaY;
                if (newWidth >= stage.getMinWidth()) {
                    newX = initialStageX + deltaX;
                } else {
                    newWidth = stage.getMinWidth();
                    newX = initialStageX + (initialStageWidth - newWidth);
                }
                if (newHeight < stage.getMinHeight()) {
                    newHeight = stage.getMinHeight();
                }
                break;
            case SOUTH_EAST:
                newWidth = initialStageWidth + deltaX;
                newHeight = initialStageHeight + deltaY;
                if (newWidth < stage.getMinWidth()) {
                    newWidth = stage.getMinWidth();
                }
                if (newHeight < stage.getMinHeight()) {
                    newHeight = stage.getMinHeight();
                }
                break;
            case NORTH:
                newHeight = initialStageHeight - deltaY;
                if (newHeight >= stage.getMinHeight()) {
                    newY = initialStageY + deltaY;
                } else {
                    newHeight = stage.getMinHeight();
                    newY = initialStageY + (initialStageHeight - newHeight);
                }
                break;
            case SOUTH:
                newHeight = initialStageHeight + deltaY;
                if (newHeight < stage.getMinHeight()) {
                    newHeight = stage.getMinHeight();
                }
                break;
            case WEST:
                newWidth = initialStageWidth - deltaX;
                if (newWidth >= stage.getMinWidth()) {
                    newX = initialStageX + deltaX;
                } else {
                    newWidth = stage.getMinWidth();
                    newX = initialStageX + (initialStageWidth - newWidth);
                }
                break;
            case EAST:
                newWidth = initialStageWidth + deltaX;
                if (newWidth < stage.getMinWidth()) {
                    newWidth = stage.getMinWidth();
                }
                break;
            default:
                break;
        }

        // Appliquer les nouvelles positions et dimensions
        stage.setX(newX);
        stage.setY(newY);
        stage.setWidth(newWidth);
        stage.setHeight(newHeight);
    }

    private void updateCursor(ResizeType type) {
        Cursor cursor;
        switch (type) {
            case NORTH_WEST:
                cursor = Cursor.NW_RESIZE;
                break;
            case NORTH_EAST:
                cursor = Cursor.NE_RESIZE;
                break;
            case SOUTH_WEST:
                cursor = Cursor.SW_RESIZE;
                break;
            case SOUTH_EAST:
                cursor = Cursor.SE_RESIZE;
                break;
            case NORTH:
                cursor = Cursor.N_RESIZE;
                break;
            case SOUTH:
                cursor = Cursor.S_RESIZE;
                break;
            case WEST:
                cursor = Cursor.W_RESIZE;
                break;
            case EAST:
                cursor = Cursor.E_RESIZE;
                break;
            default:
                cursor = Cursor.DEFAULT;
                break;
        }
        stage.getScene().setCursor(cursor);
    }

    private enum ResizeType {
        NONE,
        NORTH_WEST,
        NORTH_EAST,
        SOUTH_WEST,
        SOUTH_EAST,
        NORTH,
        SOUTH,
        WEST,
        EAST
    }
}
