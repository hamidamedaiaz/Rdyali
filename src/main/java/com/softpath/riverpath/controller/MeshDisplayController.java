package com.softpath.riverpath.controller;

import com.softpath.riverpath.custom.pane.ZoomableScrollPane;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Controller for managing 3D mesh display with camera controls.
 * <p>
 * This controller handles the initialization and rendering of 3D meshes in a JavaFX SubScene,
 * providing interactive camera controls including pan, rotation, and zoom operations.
 * The controller ensures proper initialization timing by waiting for valid scene bounds
 * before centering and adjusting the camera position.
 * </p>
 *
 * @author rhajou
 */
@NoArgsConstructor
public class MeshDisplayController {

    // Camera configuration constants
    private static final double CAMERA_NEAR_CLIP = 0.1;
    private static final double CAMERA_FAR_CLIP = 10000.0;

    // Mouse interaction speed constants
    private static final double MOUSE_SPEED = 0.1;
    private static final double ROTATION_SPEED = 2.0;
    private static final double ZOOM_SPEED = 0.1;

    /**
     * Initial distance factor applied to camera positioning.
     * Values > 1.0 move camera further away (wider view), values < 1.0 move closer (zoomed view).
     */
    private static final double INITIAL_DISTANCE_FACTOR = 1.1;

    // Scene components
    @Getter
    private Node target;
    private Group sceneRoot;
    private PerspectiveCamera camera;
    private SubScene subScene;

    // Mouse tracking for drag operations
    private double mouseOldX;
    private double mouseOldY;

    // Camera state tracking
    private double initialCameraDistance;
    private final double minZoom = 0.1;
    private final double maxZoom = 10.0;
    private double currentZoom = 1.0;

    // Transform components
    private final Scale sceneScale = new Scale(1, 1, 1);
    private double translateX = 0;
    private double translateY = 0;

    @FXML
    private ZoomableScrollPane scrollablePane;

    /**
     * Initializes the 3D view with the given target node.
     * <p>
     * This method sets up the complete 3D scene hierarchy, camera, and interaction handlers.
     * It implements a deferred initialization strategy to ensure proper bounds calculation:
     * <ol>
     *   <li>Creates the scene graph structure</li>
     *   <li>Waits for the SubScene to be attached to a visible Scene</li>
     *   <li>Waits for valid bounds to be calculated</li>
     *   <li>Centers the view and adjusts camera distance</li>
     * </ol>
     * This approach guarantees consistent rendering regardless of the timing of JavaFX's
     * layout calculations.
     * </p>
     *
     * @param target the 3D node (typically a MeshView or Group) to display
     */
    public void applyPaneView(Node target) {
        this.target = target;

        // Build scene graph hierarchy: root -> group -> targetGroup -> target
        Group targetGroup = new Group(target);
        sceneRoot = new Group(new Group(targetGroup));
        sceneRoot.getTransforms().add(sceneScale);

        // Configure perspective camera for 3D rendering
        camera = new PerspectiveCamera(true);
        camera.setNearClip(CAMERA_NEAR_CLIP);
        camera.setFarClip(CAMERA_FAR_CLIP);
        camera.setFieldOfView(30);

        // Create SubScene with antialiasing enabled for smooth rendering
        subScene = new SubScene(sceneRoot, 0, 0, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.WHITE);
        subScene.setCamera(camera);

        // Bind SubScene dimensions to ScrollPane for responsive resizing
        subScene.widthProperty().bind(scrollablePane.widthProperty());
        subScene.heightProperty().bind(scrollablePane.heightProperty());

        // Initialize mouse event handlers
        handleMouse();

        scrollablePane.setContent(subScene);

        // Step 1: Wait for SubScene to be attached to a visible Scene
        // This is necessary because JavaFX only calculates bounds for nodes that are part of a rendered scene
        subScene.sceneProperty().addListener((obsScene, oldScene, newScene) -> {
            if (newScene != null) {
                // Step 2: Once visible, wait for valid bounds before centering
                waitForValidBounds(targetGroup);
            }
        });

        // Handle edge case where scene is already attached at initialization time
        if (subScene.getScene() != null) {
            waitForValidBounds(targetGroup);
        }
    }

    /**
     * Waits for the target node to have valid (non-zero) bounds before centering the view.
     * <p>
     * This method implements a listener pattern that monitors the bounds property of the target node.
     * Once valid bounds are detected (width and height > 0), it:
     * <ul>
     *   <li>Centers the target in the viewport</li>
     *   <li>Calculates and sets optimal camera distance</li>
     *   <li>Applies a small zoom to force visual refresh</li>
     *   <li>Removes itself to prevent memory leaks and redundant executions</li>
     * </ul>
     * The self-removing listener pattern ensures the initialization logic runs exactly once.
     * </p>
     *
     * @param targetGroup the group containing the target node, used for applying centering transforms
     */
    private void waitForValidBounds(Group targetGroup) {
        final Node targetNode = this.target;

        // Use array to store listener reference for self-removal
        // This is necessary because the listener needs to reference itself for removal,
        // but Java requires effectively final variables in lambda expressions
        javafx.beans.value.ChangeListener<Bounds>[] listenerRef = new javafx.beans.value.ChangeListener[1];

        javafx.beans.value.ChangeListener<Bounds> listener = (obs, oldBounds, newBounds) -> {
            // Check if bounds are now valid (JavaFX has calculated actual dimensions)
            if (newBounds.getWidth() > 0 && newBounds.getHeight() > 0) {

                // Initialize the view with proper centering and camera position
                centerTarget(targetGroup);
                adjustCameraDistance();
                initialCameraDistance = -camera.getTranslateZ();

                // Apply minimal zoom to force visual refresh
                zoom(1.001);

                // Remove this listener to prevent memory leaks and redundant executions
                targetNode.boundsInParentProperty().removeListener(listenerRef[0]);
            }
        };

        listenerRef[0] = listener;
        targetNode.boundsInParentProperty().addListener(listener);
    }

    /**
     * Centers the target node in the viewport by calculating its geometric center
     * and applying an offsetting translation.
     * <p>
     * The method calculates the center point of the target's bounding box in 3D space
     * and applies a negative translation to position this center at the origin.
     * This ensures the target appears centered regardless of its original position.
     * </p>
     *
     * @param targetGroup the group to which the centering translation will be applied
     */
    private void centerTarget(Group targetGroup) {
        Bounds bounds = target.getBoundsInParent();

        // Safety check: ensure bounds have been calculated by JavaFX
        if (bounds.getWidth() == 0 && bounds.getHeight() == 0) {
            return; // Bounds not yet valid, skip centering
        }

        // Calculate geometric center of the bounding box
        double centerX = (bounds.getMinX() + bounds.getMaxX()) / 2;
        double centerY = (bounds.getMinY() + bounds.getMaxY()) / 2;
        double centerZ = (bounds.getMinZ() + bounds.getMaxZ()) / 2;

        // Remove any existing translation to prevent accumulation
        targetGroup.getTransforms().removeIf(t -> t instanceof Translate);

        // Apply translation to center the target at the origin
        Translate translate = new Translate(-centerX, -centerY, -centerZ);
        targetGroup.getTransforms().add(translate);
    }

    /**
     * Calculates and sets the optimal camera distance to fit the entire target in the viewport.
     * <p>
     * This method uses trigonometry to determine the camera distance required to view
     * the complete target mesh within the SubScene's dimensions. The calculation accounts for:
     * <ul>
     *   <li>Camera field of view (FOV)</li>
     *   <li>Viewport aspect ratio</li>
     *   <li>Target mesh dimensions (width and height)</li>
     *   <li>A configurable distance factor for padding</li>
     * </ul>
     * The camera is positioned on the Z-axis, with negative Z moving away from the viewer.
     * </p>
     */
    private void adjustCameraDistance() {
        Bounds bounds = target.getBoundsInParent();
        double width = bounds.getWidth();
        double height = bounds.getHeight();

        // Ensure SubScene has valid dimensions before calculating
        if (subScene.getWidth() <= 0 || subScene.getHeight() <= 0) {
            return;
        }

        // Calculate field of view angles in radians
        double fov = camera.getFieldOfView();
        double aspectRatio = subScene.getWidth() / subScene.getHeight();
        double vFovRadians = Math.toRadians(fov);
        double hFovRadians = 2 * Math.atan(Math.tan(vFovRadians / 2) * aspectRatio);

        // Calculate required distance for vertical and horizontal fit
        double distanceV = (height / 2) / Math.tan(vFovRadians / 2);
        double distanceH = (width / 2) / Math.tan(hFovRadians / 2);

        // Use the larger distance to ensure complete visibility
        // Apply distance factor for padding (e.g., 1.1 = 10% margin)
        double distance = Math.max(distanceV, distanceH) * INITIAL_DISTANCE_FACTOR;
        camera.setTranslateZ(-distance);
    }

    /**
     * Configures mouse event handlers for interactive camera controls.
     * <p>
     * Supported interactions:
     * <ul>
     *   <li><b>Pan:</b> Left-click + drag - Translates the view horizontally and vertically</li>
     *   <li><b>Rotate:</b> Shift + left-click + drag OR right-click + drag - Rotates around X and Y axes</li>
     *   <li><b>Zoom:</b> Mouse wheel scroll - Scales the view in/out</li>
     *   <li><b>Reset:</b> Double left-click - Resets camera to initial position and orientation</li>
     * </ul>
     * All transformations are applied to the scene root, affecting the entire 3D scene.
     * </p>
     */
    private void handleMouse() {
        // Create rotation transforms for X and Y axes
        Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
        Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
        Translate translate = new Translate(0, 0);
        sceneRoot.getTransforms().addAll(rotateX, rotateY, translate);

        // Track initial mouse position for drag calculations
        subScene.setOnMousePressed(me -> {
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });

        // Handle drag operations (pan or rotate based on mouse button/modifiers)
        subScene.setOnMouseDragged(me -> {
            double mouseDeltaX = (me.getSceneX() - mouseOldX);
            double mouseDeltaY = (me.getSceneY() - mouseOldY);

            boolean shiftDown = me.isShiftDown();
            if ((me.getButton() == MouseButton.PRIMARY && shiftDown) || me.getButton() == MouseButton.SECONDARY) {
                // Rotation mode: Shift + left-click OR right-click
                rotateY.setAngle(rotateY.getAngle() - mouseDeltaX * MOUSE_SPEED * ROTATION_SPEED);
                rotateX.setAngle(rotateX.getAngle() + mouseDeltaY * MOUSE_SPEED * ROTATION_SPEED);
            } else if (me.getButton() == MouseButton.PRIMARY) {
                // Pan mode: simple left-click
                // Scale translation by zoom level for consistent feel
                translateX += (mouseDeltaX * 3.0) / currentZoom;
                translateY += (mouseDeltaY * 3.0) / currentZoom;
                translate.setX(translateX);
                translate.setY(translateY);
            }

            // Update mouse position for next drag calculation
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });

        // Handle mouse wheel for zoom
        subScene.setOnScroll((ScrollEvent event) -> {
            event.consume();
            // Convert scroll delta to exponential zoom factor for smooth scaling
            double zoomFactor = Math.exp(event.getDeltaY() * ZOOM_SPEED / 30);
            zoom(zoomFactor);
        });

        // Handle double-click for view reset
        subScene.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() >= 2) {
                resetView();
            }
        });
    }

    /**
     * Applies a zoom factor to the current view scale.
     * <p>
     * The zoom is implemented as a scale transform applied to X and Y axes only.
     * The Z-axis (depth) is intentionally NOT scaled to maintain proper 3D perspective
     * and prevent the object from jumping in depth space or disappearing from view.
     * Zoom level is clamped between {@code minZoom} and {@code maxZoom} to prevent
     * extreme scaling that could cause rendering issues or user disorientation.
     * </p>
     *
     * @param factor the multiplicative zoom factor (> 1.0 zooms in, < 1.0 zooms out)
     */
    private void zoom(double factor) {
        currentZoom *= factor;
        currentZoom = Math.max(minZoom, Math.min(maxZoom, currentZoom));
        sceneScale.setX(currentZoom);
        sceneScale.setY(currentZoom);
        sceneScale.setZ(currentZoom);
    }

    private void resetView() {
        currentZoom = 1.1;
        sceneScale.setX(1);
        sceneScale.setY(1);
        //sceneScale.setZ(1);
        sceneRoot.getTransforms().stream()
                .filter(t -> t instanceof Rotate)
                .forEach(t -> ((Rotate) t).setAngle(0));
        translateX = 0;
        translateY = 0;
        sceneRoot.getTransforms().stream()
                .filter(t -> t instanceof Translate)
                .forEach(t -> {
                    ((Translate) t).setX(0);
                    ((Translate) t).setY(0);
                });
        scrollablePane.setHvalue(0.5);
        scrollablePane.setVvalue(0.5);
    }
}