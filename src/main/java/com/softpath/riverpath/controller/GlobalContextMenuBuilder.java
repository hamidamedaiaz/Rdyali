package com.softpath.riverpath.controller;

import com.softpath.riverpath.fileparser.MeshResolution;
import com.softpath.riverpath.util.DisplayMode;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.util.Map;
import java.util.function.Consumer;

/**
  building the global context menu with
  Domain sectionwith mode switcher
  Objects section with individual mode switchers
 * *
 */
public class GlobalContextMenuBuilder {

    private final MeshObjectManager objectManager;
    private final SceneRenderer sceneRenderer;

    public GlobalContextMenuBuilder(MeshObjectManager objectManager, SceneRenderer sceneRenderer) {
        this.objectManager = objectManager;
        this.sceneRenderer = sceneRenderer;
    }

    /**
     * Build the complete global context menu
     *

     */
    public void buildContextMenu(ContextMenu contextMenu, Consumer<Void> onDisplayModeChanged) {
        contextMenu.getItems().clear();
        contextMenu.getStyleClass().add("global-context-menu");

        // Build domain section
        buildDomainSection(contextMenu, onDisplayModeChanged);

        // Build objects section
        buildObjectsSection(contextMenu, onDisplayModeChanged);
    }

    /**
     * Build the domain section of the menu
     */
    private void buildDomainSection(ContextMenu contextMenu, Consumer<Void> onDisplayModeChanged) {
        Menu domainSubmenu = new Menu(" Domain");
        domainSubmenu.getStyleClass().add("domain-menu");

        // Mode Simple option
        MenuItem domainSimpleItem = new MenuItem("   Mode Simple");
        domainSimpleItem.getStyleClass().add("mode-item");
        domainSimpleItem.setOnAction(e -> {
            sceneRenderer.setDomainDisplayMode(DisplayMode.SIMPLE);
            onDisplayModeChanged.accept(null);
        });

        // Mode Mesh option
        MenuItem domainMeshItem = new MenuItem("   Mode Mesh");
        domainMeshItem.getStyleClass().add("mode-item");
        domainMeshItem.setOnAction(e -> {
            sceneRenderer.setDomainDisplayMode(DisplayMode.MESH);
            onDisplayModeChanged.accept(null);
        });

        domainSubmenu.getItems().addAll(domainSimpleItem, domainMeshItem);
        contextMenu.getItems().add(domainSubmenu);

        // Add separator if there are objects
        if (objectManager.hasObjects()) {
            contextMenu.getItems().add(new SeparatorMenuItem());
        }
    }

    /**
     * Build the objects section of the menu
     */
    private void buildObjectsSection(ContextMenu contextMenu, Consumer<Void> onDisplayModeChanged) {
        if (!objectManager.hasObjects()) {
            // No objects message
            MenuItem noObjectsItem = new MenuItem("(No objects)");
            noObjectsItem.setDisable(true);
            noObjectsItem.getStyleClass().add("no-objects-item");
            contextMenu.getItems().add(noObjectsItem);
            return;
        }

        // Add header
        MenuItem objectsHeaderItem = new MenuItem(" Objects");
        objectsHeaderItem.setDisable(true);
        objectsHeaderItem.getStyleClass().add("objects-header");
        contextMenu.getItems().add(objectsHeaderItem);
        contextMenu.getItems().add(new SeparatorMenuItem());

        // Create submenu for each object
        for (Map.Entry<String, MeshResolution> entry : objectManager.getAllMeshes().entrySet()) {
            String objectId = entry.getKey();
            String displayName = objectManager.getDisplayName(objectId);

            Menu objectSubmenu = new Menu( displayName);
            objectSubmenu.getStyleClass().add("object-menu");

            // Mode Simple option
            MenuItem simpleModeItem = new MenuItem("     Mode Simple");
            simpleModeItem.getStyleClass().add("mode-item");
            simpleModeItem.setOnAction(e -> {
                objectManager.setDisplayMode(objectId, DisplayMode.SIMPLE);
                onDisplayModeChanged.accept(null);
            });

            // Mode Mesh option
            MenuItem meshModeItem = new MenuItem("     Mode Mesh");
            meshModeItem.getStyleClass().add("mode-item");
            meshModeItem.setOnAction(e -> {
                objectManager.setDisplayMode(objectId, DisplayMode.MESH);
                onDisplayModeChanged.accept(null);
            });

            objectSubmenu.getItems().addAll(simpleModeItem, meshModeItem);
            contextMenu.getItems().add(objectSubmenu);
        }
    }
}

