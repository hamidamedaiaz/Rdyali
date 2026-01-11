package com.softpath.riverpath.controller;


import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventManager;
import com.softpath.riverpath.custom.pane.BoundaryTitledPane;
import com.softpath.riverpath.model.Boundary;
import com.softpath.riverpath.model.Simulation;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.softpath.riverpath.custom.event.EventEnum.*;

/**
 * A global controller to handle all boundary condition
 *
 * @author rhajou
 */
public class BoundaryConditionGlobalController {

    /**
     * map of boundary ID (label text) with all controller condition
     */
    private final Map<String, BoundaryConditionController> controllerMap;

    public BoundaryConditionGlobalController() {
        this.controllerMap = new LinkedHashMap<>();
        // Register to listen to new condition validation
        EventManager.addEventHandler(NEW_CONDITION_VALIDATED, event -> {
            // get boundary ID
            boolean allValid = checkIfAllConditionValidated();
            if (allValid) {
                EventManager.fireCustomEvent(new CustomEvent(ALL_CONDITION_VALID));
            }
        });
        // add listener to isInvalidMandatoryField boundary name changed
        EventManager.addEventHandler(BOUNDARY_NAME_CHANGED, event -> {
            updateBoundaryName(event.getMessage(), event.getNewValue());
        });
    }

    public Collection<BoundaryConditionController> getAllConditions() {
        return controllerMap.values();
    }

    public void updateBoundaryName(String oldNameField, String newName) {
        BoundaryConditionController boundaryConditionController = controllerMap.get(oldNameField);
        if (boundaryConditionController != null) {
            boundaryConditionController.setLabel(newName);
            controllerMap.remove(oldNameField);
            boundaryConditionController.setLabel(newName);
            controllerMap.put(newName, boundaryConditionController);
        }
    }

    /**
     * Refresh boundary condition list
     * Return all existent and valid boundary condition then add new one if a new boundary definition created
     *
     * @return the list of valid boundary condition and new boundary condition to be defined
     */
    public Collection<BoundaryTitledPane> refreshBoundaryCondition() {
        // return all boundary condition list
        return controllerMap.values().stream().map(BoundaryConditionController::getBoundaryConditionPane).toList();
    }

    /**
     * Import condition data from simulation
     * This method is called when a project is imported
     *
     * @param simulation the simulation object
     */
    public void importConditionData(Simulation simulation) {
        Platform.runLater(() -> {
            for (Boundary boundary : simulation.getBoundaries()) {
                // Identify the current boundary in controller map Keyset
                String boundaryName = controllerMap.keySet().stream()
                        .filter(key -> key.equals(boundary.getName()))
                        .findAny()
                        .orElseThrow(() -> new RuntimeException("Boundary not found"));
                BoundaryConditionController currentConditionController = controllerMap.get(boundaryName);
                currentConditionController.importConditionData(boundary.getCondition());
            }
        });
    }

    public void createNewCondition(String boundaryName) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Objects.requireNonNull(getClass()
                    .getResource("boundary-condition-pane.fxml")));
            fxmlLoader.load();
            // add to controller map
            BoundaryConditionController controller = fxmlLoader.getController();
            // create new boundary condition titled
            controller.setLabel(boundaryName);
            // flag condition not valid
            controller.updateRootModifiedState();
            controllerMap.put(boundaryName, controller);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Remove boundary condition by name
     *
     * @param boundaryName boundary or boundary condition name
     */
    public void removeBoundaryCondition(String boundaryName) {
        controllerMap.remove(boundaryName);
    }

    /**
     * check if all condition are valid
     *
     * @return true if all condition are valide false if not
     */
    private boolean checkIfAllConditionValidated() {
        // Apply "AND" condition to all elements
        return controllerMap.values()
                .stream()
                .map(BoundaryConditionController::isValid)
                .reduce(true, (a, b) -> a && b);
    }

}