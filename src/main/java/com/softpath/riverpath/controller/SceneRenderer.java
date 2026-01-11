package com.softpath.riverpath.controller;

import com.softpath.riverpath.fileparser.MeshResolution;
import com.softpath.riverpath.util.DisplayMode;
import com.softpath.riverpath.util.DomainProperties;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Map;

/**
 * Responsible for rendering the complete scene:
 * Domain with SIMPLE or MESH mode
 * Immersed objects with individual modes
 * Shapes standard boundaries
 * Normal arrows
 */
public class SceneRenderer {

    private final MeshObjectManager objectManager;
    @Setter
    private MeshView domainMeshView;
    @Setter
    @Getter
    private DisplayMode domainDisplayMode = DisplayMode.MESH;
    @Setter
    private MeshView surfaceDomainMeshView;

    public SceneRenderer(MeshObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    /**
     * Render the complete scene to the root pane
     */
    public void renderScene(Pane rootPane) {
        if (domainMeshView == null || rootPane == null) {
            return;
        }

        rootPane.getChildren().clear();
        Group mainGroup = new Group();

        // Display domain according to its mode
        renderDomain(mainGroup);

        // Display objects according to their individual modes
        renderObjects(mainGroup);

        // Add shapes and normal arrows
        renderShapes(mainGroup);
        mainGroup.getChildren().addAll(objectManager.getNormalArrows().values());
        // Add once to your scene
        AmbientLight light = new AmbientLight(Color.WHITE);
        mainGroup.getChildren().add(light);

        rootPane.getChildren().add(mainGroup);
    }

    /**
     * Render the domain in SIMPLE or MESH mode
     */
    private void renderDomain(Group mainGroup) {
        if (domainDisplayMode == DisplayMode.SIMPLE) {
            // Domain in SIMPLE mode
            surfaceDomainMeshView.setDrawMode(DrawMode.LINE);
            surfaceDomainMeshView.setMaterial(new PhongMaterial(Color.BLACK));
            // this can help for inside view
            surfaceDomainMeshView.setCullFace(CullFace.NONE);
            mainGroup.getChildren().add(surfaceDomainMeshView);
        } else {
            // Domain in MESH mode: show full mesh
            domainMeshView.setDrawMode(DrawMode.LINE);
            domainMeshView.setCullFace(CullFace.NONE);
            domainMeshView.setMaterial(new PhongMaterial(Color.BLACK));
            mainGroup.getChildren().add(domainMeshView);
        }
    }

    /**
     * Render all objects with their individual modes
     */
    private void renderObjects(Group mainGroup) {
        for (Map.Entry<String, MeshResolution> entry : objectManager.getAllMeshes().entrySet()) {
            String objectId = entry.getKey(); // Use the correct key (controllerId)
            MeshResolution meshResolution = entry.getValue();
            DisplayMode objectMode = objectManager.getDisplayMode(objectId);
            MeshView meshViewImmersedObj = meshViewImmersedObj(meshResolution, objectMode);
            Group objectGroup = new Group(meshViewImmersedObj);
            objectGroup.setPickOnBounds(true);
            mainGroup.getChildren().add(objectGroup);
        }
    }

    /**
     * Render all shapes (standard boundaries)
     */
    private void renderShapes(Group mainGroup) {
        Map<String, Shape> shapes = objectManager.getShapes();
        if (!shapes.isEmpty()) {
            Collection<Shape> shapesCollection = shapes.values();
            shapesCollection.forEach(shape -> {
                shape.setFill(Color.TRANSPARENT);
                shape.setStroke(Color.RED);
                shape.setStrokeWidth(1);
            });
            mainGroup.getChildren().addAll(shapesCollection);
        }
    }

    /**
     * Create nodes for an object based on its display mode
     */
    private MeshView meshViewImmersedObj(MeshResolution meshResolution, DisplayMode mode) {
        if (mode == DisplayMode.SIMPLE) {
            // Mode SIMPLE: show only borders
            MeshView meshView = new MeshView(meshResolution.getTriangleSurface());
            applyScale(meshView);
            meshView.setDrawMode(DrawMode.LINE);
            meshView.setCullFace(CullFace.NONE);
            meshView.setMaterial(new PhongMaterial(meshResolution.getColor()));
            applyTranslate(meshResolution, meshView);
            return meshView;
        } else {
            // For 3D objects, create MeshView
            MeshView objectMeshView = new MeshView(meshResolution.getReducedMesh());
            applyScale(objectMeshView);
            objectMeshView.setDrawMode(DrawMode.LINE);
            objectMeshView.setCullFace(CullFace.NONE);
            objectMeshView.setMaterial(new PhongMaterial(meshResolution.getColor()));
            applyTranslate(meshResolution, objectMeshView);
            return objectMeshView;
        }
    }

    private void applyTranslate(MeshResolution meshResolution, MeshView meshView) {
        meshView.setTranslateX(meshResolution.getPosition().getX());
        meshView.setTranslateY(meshResolution.getPosition().getY());
        meshView.setTranslateZ(meshResolution.getPosition().getZ());
    }

    /**
     * Apply scale transformation to a mesh view
     */
    public void applyScale(MeshView meshView) {
        double scaleFactor = DomainProperties.getInstance().getScaleFactor();
        // ⚠️JAVAFX_INVERTED_AXIS_Y
        Scale scale = new Scale(scaleFactor, -scaleFactor, scaleFactor);
        meshView.getTransforms().add(scale);
    }

}

