package com.softpath.riverpath.controller;

import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventManager;
import com.softpath.riverpath.custom.pane.BoundaryTitledPane;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.softpath.riverpath.custom.event.EventEnum.ALLOW_NEW_BOUNDARY_DEF;
import static com.softpath.riverpath.custom.event.EventEnum.TITLE_PANE_BOUNDARY_DEF_CREATED;

/**
 * This controller is to handle the Vbox in left bottom main page
 *
 * @author rhajou
 */
@NoArgsConstructor
@Getter
public class LeftBottomPaneController {

    private final List<BoundaryTitledPane> boundaryTitledPanes = new ArrayList<>();
    private final Set<BoundaryDefinitionController> boundaryDefinitionControllers = new LinkedHashSet<>();
    @FXML
    private ScrollPane leftBottomScrollPane;
    @Getter
    private MeshingParametersController meshingParametersController;
    @FXML
    private VBox leftBottomPane;
    private GridPane addNewBoundaryDefPane;
    private BoundaryConditionGlobalController conditionGlobalController;
    private DataEngineeringController dataEngineeringController;
    private TimeDiscretizationController timeDiscretizationController;
    private RunConfigurationController runConfigurationController;
    private boolean shouldAutoScroll = false;

    @FXML
    public void initialize() throws IOException {
        // init new boundary button and handle it action and event
        initAndHandleNewBoundaryAction();
        // load data engineering controller
        loadDataEngineeringController();
        // load time discretization controller
        loadTimeDiscretizationController();
        // load parallel computation controller
        loadParallelComputation();
        // load meshing parameters controller
        loadMeshingParametersController();
        // init global controller
        this.conditionGlobalController = new BoundaryConditionGlobalController();
        // Listen to the height property of the content (VBox)
        listenAddNewBoundaryToScrollDown();
    }

    public boolean hasNoBoundaryPane() {
        return leftBottomPane.getChildren().isEmpty();
    }

    public BoundaryDefinitionController addBoundaryPane() {
        shouldAutoScroll = true;
        // remove add button from the list
        leftBottomPane.getChildren().remove(addNewBoundaryDefPane);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("boundary-definition-pane.fxml"));
        BoundaryTitledPane boundaryDefAsTitledPane;
        BoundaryDefinitionController boundaryDefUnderConstructionController;
        try {
            boundaryDefAsTitledPane = fxmlLoader.load();
            boundaryDefUnderConstructionController = fxmlLoader.getController();
            boundaryDefUnderConstructionController.setBoundaryTitledPanes(boundaryTitledPanes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        boundaryDefAsTitledPane.prefWidthProperty().bind(leftBottomPane.widthProperty());
        leftBottomPane.getChildren().add(boundaryDefAsTitledPane);
        boundaryDefinitionControllers.add(boundaryDefUnderConstructionController);
        boundaryTitledPanes.add(boundaryDefAsTitledPane);
        EventManager.fireCustomEvent(new CustomEvent(TITLE_PANE_BOUNDARY_DEF_CREATED, boundaryDefAsTitledPane));
        // create dedicated condition for this boundary
        conditionGlobalController.createNewCondition(boundaryDefUnderConstructionController.getLabel());
        // Do not add the "New Boundary" button until the current boundary has been validated
        // leftBottomPane.getChildren().add(addNewBoundaryDefPane);
        return boundaryDefUnderConstructionController;
    }

    public void displayAllBoundaries() {
        if (!boundaryTitledPanes.isEmpty()) {
            boundaryTitledPanes.forEach(e -> e.setExpanded(false));
            clearPane();
            leftBottomPane.getChildren().addAll(boundaryTitledPanes);
            leftBottomPane.getChildren().add(addNewBoundaryDefPane);
        }
    }

    public void displayBoundaryConditionPane() {
        clearPane();
        leftBottomPane.getChildren().addAll(conditionGlobalController.refreshBoundaryCondition());
    }

    public void displayDataEngineerPane() {
        clearPane();
        leftBottomPane.getChildren().addAll(dataEngineeringController.getTitledPane());
    }

    public void displayTimeDiscretizationPane() {
        clearPane();
        leftBottomPane.getChildren().addAll(timeDiscretizationController.getTitledPane());
    }

    /**
     * Remove boundary definition controller from the list
     *
     * @param boundaryDefinitionController the boundary definition controller
     */
    public void removeBoundaryDefinition(BoundaryDefinitionController boundaryDefinitionController) {
        boundaryDefinitionControllers.remove(boundaryDefinitionController);
        conditionGlobalController.removeBoundaryCondition(boundaryDefinitionController.getLabel());

    }

    public void reloadRunConfigurationPane() {
        clearPane();
        leftBottomPane.getChildren().add(runConfigurationController.getTitledPane());
    }

    private void clearPane() {
        leftBottomPane.getChildren().clear();
    }

    private void initAndHandleNewBoundaryAction() throws IOException {
        addNewBoundaryDefPane = FXMLLoader.load(Objects.requireNonNull(getClass()
                .getResource("add-boundary-button.fxml")));
        Hyperlink addNewBoundaryDef = (Hyperlink) addNewBoundaryDefPane.lookup("#addButton");
        addNewBoundaryDef.setOnMouseClicked(event -> addBoundaryPane());
        // Register to listen for the custom event
        EventManager.addEventHandler(ALLOW_NEW_BOUNDARY_DEF, event -> {
            // remove add button from the list
            leftBottomPane.getChildren().remove(addNewBoundaryDefPane);
            // then add it at the end
            leftBottomPane.getChildren().add(addNewBoundaryDefPane);
        });
    }

    private void loadDataEngineeringController() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("data-engineering.fxml"));
        fxmlLoader.load();
        dataEngineeringController = fxmlLoader.getController();
    }

    private void loadTimeDiscretizationController() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("time-discretization.fxml"));
        fxmlLoader.load();
        timeDiscretizationController = fxmlLoader.getController();
    }

    private void loadParallelComputation() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("run-configuration.fxml"));
        loader.load();
        this.runConfigurationController = loader.getController();
    }

    /**
     * Auto scroll down when a new boundary is added
     */
    private void listenAddNewBoundaryToScrollDown() {
        leftBottomPane.heightProperty().addListener((observable, oldValue, newValue) -> {
            // Scroll to the bottom when the VBox height changes (i.e., a new item is added)
            if (shouldAutoScroll) {
                Platform.runLater(this::scrollToBottom);
                shouldAutoScroll = false;
            }
        });
    }

    private void scrollToBottom() {
        leftBottomScrollPane.layout();
        leftBottomScrollPane.setVvalue(1.0);
    }

    private void loadMeshingParametersController() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("meshing-parameters.fxml"));
        fxmlLoader.load();
        meshingParametersController = fxmlLoader.getController();
    }

    public void displayMeshingParametersPane() {
        clearPane();
        leftBottomPane.getChildren().addAll(meshingParametersController.getTitledPane());
    }

    public boolean hasUIResourcesToValidate() {
        // boundaries
        for (BoundaryDefinitionController ctr : boundaryDefinitionControllers) {
            if (ctr.hasDirtyFields()) return true;
        }
        // we should also make sure all clicks on add new boundary will produce an object to be checked in the set, if not retrieve them and test them
        // we should also make sure the baseboundary fields are handled inside the boundary def

        // conditions
        for (BoundaryConditionController ctr : conditionGlobalController.getAllConditions()) {
            if (ctr.hasDirtyFields()) return true;
        }

        // engineering data
        if (dataEngineeringController.hasDirtyFields()) return true;

        // meshing parameters
        if (meshingParametersController.hasDirtyFields()) return true;

        // time discretization
        if (timeDiscretizationController.hasDirtyFields()) return true;

        return false;
    }

    public void handleSaveOnExit() { // make this method return to UI if impossible to validate or prompt it
        // boundaries
        for (BoundaryDefinitionController ctr : boundaryDefinitionControllers) {
            if (ctr.hasDirtyFields()) ctr.handleValidate(null);
        }

        // conditions
        for (BoundaryConditionController ctr : conditionGlobalController.getAllConditions()) {
            if (ctr.hasDirtyFields()) ctr.handleValidate(null);
        }

        // data engineering
        if (dataEngineeringController.hasDirtyFields()) {
            dataEngineeringController.handleValidate(null);
        }

        // meshing parameters
        if (meshingParametersController.hasDirtyFields()) {
            meshingParametersController.handleValidate(null);
        }

        // time discretization
        if (timeDiscretizationController.hasDirtyFields()) {
            timeDiscretizationController.handleValidate(null);
        }
    }

}