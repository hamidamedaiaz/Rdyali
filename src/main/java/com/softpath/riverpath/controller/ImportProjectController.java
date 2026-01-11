package com.softpath.riverpath.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventEnum;
import com.softpath.riverpath.custom.event.EventManager;
import com.softpath.riverpath.fileparser.MeshResolution;
import com.softpath.riverpath.model.Boundary;
import com.softpath.riverpath.model.Simulation;
import com.softpath.riverpath.util.ProgressReporter;
import com.softpath.riverpath.util.UtilityClass;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Controller class for importing a project from a directory.
 *
 * @author rhajou
 */
@Slf4j
public class ImportProjectController {

    private final MainController mainController;
    private final ObjectMapper objectMapper;

    public ImportProjectController(MainController mainController) {
        this.mainController = mainController;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Handle the import project action.
     */
    public void handleImportProject() {
        // select project we need to import
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(UtilityClass.getHomeDirectory());
        directoryChooser.setTitle("Select Project Directory");
        Stage stage = new Stage();
        File selectedDirectory = directoryChooser.showDialog(stage);
        // If a directory is selected, import the project
        if (selectedDirectory != null) {
            try {
                EventManager.fireCustomEvent(new CustomEvent(EventEnum.IMPORT_PROJECT, selectedDirectory));
                importProject(selectedDirectory);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    handleError("Error importing project: " + e.getMessage(), e);
                });
            }
        }
    }

    private void importProject(File projectDirectory) throws IOException {
        // Encapsulate project description in #Simulation.java object
        File simulationJsonFile = new File(projectDirectory, "simulation.json");
        if (!simulationJsonFile.exists()) {
            Platform.runLater(() -> mainController.displayMessageConsoleOutput("simulation.json file not found in "
                    + "the selected directory."));
            return;
        }
        // map json file to Simulation object
        Simulation simulation = objectMapper.readValue(simulationJsonFile, Simulation.class);
        // get domain mesh file
        File domainMeshFile = searchAndGetFile(projectDirectory, simulation.getDomainMeshFile());
        if (domainMeshFile == null) {
            Platform.runLater(() -> mainController.displayMessageConsoleOutput("Mesh file (.t) not found in the "
                    + "selected directory. Project structure is corrupted"));
            return;
        }
        // start import
        try {
            ProjectSetupController projectSetupController = mainController.getProjectSetupController();
            // display mesh
            Task<MeshResolution> loadMeshTask = projectSetupController.setupAndDisplay(domainMeshFile);
            // Populate boundary data
            populateBoundaryData(projectSetupController, simulation);
            // Populate boundary condition data
            populateBoundaryConditionData(projectSetupController, simulation);
            LeftBottomPaneController leftBottomPaneController = projectSetupController.getLeftBottomPaneController();
            // populate data engineering
            leftBottomPaneController.getDataEngineeringController().importData(simulation);
            // populate time discretization
            leftBottomPaneController.getTimeDiscretizationController().importData(simulation);
            // populate meshing parameters
            leftBottomPaneController.getMeshingParametersController().importData(simulation);
            // wait for mesh to be loaded before displaying success message
            loadMeshTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, ev -> {
                ProgressReporter.report("Project imported successfully");
            });
            loadMeshTask.setOnFailed(ev -> {
                handleError("An error occurred while importing the project.", loadMeshTask.getException());
            });
        } catch (Exception e) {
            handleError("An error occurred while importing the project.", e);
        }
    }

    private void handleError(String message, Throwable e) {
        ProgressReporter.report(message);
        log.error("An error occurred while importing the project.", e);
    }

    private void populateBoundaryData(ProjectSetupController projectSetupController, Simulation simulation) {

        LeftBottomPaneController leftBottomPaneController = projectSetupController.getLeftBottomPaneController();
        // for each boundary in json file create a boundary pane

        for (Boundary boundary : simulation.getBoundaries()) {
            // Add a new boundary pane
            BoundaryDefinitionController boundaryController = leftBottomPaneController.addBoundaryPane();
            // Set the boundary data & trigger the validation to update the UI
            boundaryController.applyImportValues(boundary);
        }
        // Display all boundaries

        leftBottomPaneController.displayAllBoundaries();
    }

    private void populateBoundaryConditionData(ProjectSetupController projectSetupController, Simulation simulation) {
        LeftBottomPaneController leftBottomPaneController = projectSetupController.getLeftBottomPaneController();
        // reload boundary definition => this will also initialize required condition
        projectSetupController.handleBoundaryCondition(null);
        // condition are initialize => need to be filled
        leftBottomPaneController.getConditionGlobalController().importConditionData(simulation);
    }

    private File searchAndGetFile(File directory, String fileName) throws FileNotFoundException {
        return Arrays.stream(Objects.requireNonNull(directory.listFiles()))
                .filter(e -> fileName.equals(e.getName()))
                .findFirst()
                .orElseThrow(() -> new FileNotFoundException("Directory is empty" + directory.getAbsolutePath()));
    }
}