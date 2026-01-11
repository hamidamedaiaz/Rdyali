package com.softpath.riverpath.controller;

import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventManager;
import com.softpath.riverpath.fileparser.MeshResolution;
import com.softpath.riverpath.meshconverter.MeshConverterFactory;
import com.softpath.riverpath.opengl.OpenGLViewer;
import com.softpath.riverpath.service.RunnerService;
import com.softpath.riverpath.util.DomainProperties;
import com.softpath.riverpath.util.NewProjectDialog;
import com.softpath.riverpath.util.ProgressReporter;
import com.softpath.riverpath.util.UtilityClass;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.softpath.riverpath.custom.event.EventEnum.*;
import static com.softpath.riverpath.fileparser.MeshFileParser.parseFile2TriangleMesh;
import static com.softpath.riverpath.util.UtilityClass.detectDimensionImportCase;
import static com.softpath.riverpath.util.UtilityClass.workspaceDirectory;

@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class ProjectSetupController implements Initializable {

    private static final String FX_BASE_LIGHTGREEN = "-fx-base: #c1ee90";

    private static final String FX_BASE_LIGHTRED = "-fx-base: #ee9090";
    @FXML
    private Button conditionButton;
    @FXML
    private Button runButton;
    @FXML
    private SplitPane projectSetupPane;
    @FXML
    private Button runConfigurationButton;
    @FXML
    private Button importMSHButton;
    @FXML
    private Button boundaryButton;
    @FXML
    private Button dataEngineeringButton;

    @FXML
    private Button timeDiscretizationButton;

    @FXML
    private MainController mainController;

    @FXML
    private LeftBottomPaneController leftBottomPaneController;

    private boolean isTimeDiscretizationSet;

    private RunnerService runnerService;

    private String domainExtentionT;
    @FXML
    private Button stopButton;

    private Process currentProcess; // To store the current process

    @FXML
    private Button meshingParametersButton;

    @FXML
    private void handleMeshingParameters() {
        leftBottomPaneController.displayMeshingParametersPane();
    }

    @FXML
    private void importGmeshFile() {
        Optional<NewProjectDialog.ProjectConfig> result =
                NewProjectDialog.showAndWait((Stage) projectSetupPane.getScene().getWindow());
        if (result.isEmpty()) {
            return;
        }
        NewProjectDialog.ProjectConfig config = result.get();
        try {
            // Create project directory
            File projectDir = config.getProjectDirectory();
            if (!projectDir.mkdirs()) {
                mainController.displayMessageConsoleOutput(
                        "Failed to create project directory: " + projectDir.getAbsolutePath()
                );
                return;
            }
            UtilityClass.workspaceDirectory = projectDir;
            int dimension = MeshConverterFactory.detectDimension(config.getDomainFile().getAbsolutePath());
            DomainProperties.getInstance().setDimension(dimension);
            // convert msh file to .t file
            convertAndDisplayDomain(config.getDomainFile());
        } catch (Exception e) {
            log.error("Error creating project", e);
            mainController.displayMessageConsoleOutput(
                    "Error creating project: " + e.getMessage()
            );
            File projectDir = config.getProjectDirectory();
            if (projectDir.exists()) {
                try {
                    org.apache.commons.io.FileUtils.deleteDirectory(projectDir);
                } catch (IOException ex) {
                    log.warn("Failed to cleanup project directory", ex);
                }
            }
        }
    }

    /**
     * Use a Task to convert Gmsh to .t file.
     * This will help to display process messages in real time bu javafx UI thread
     *
     * @param selectedFile .msh selected file by user
     */
    private void convertAndDisplayDomain(File selectedFile) {
        Task<String> convertTask = convertGmshToT(selectedFile);
        convertTask.setOnSucceeded(e -> {
            domainExtentionT = convertTask.getValue();
            File tFile = new File(workspaceDirectory, domainExtentionT);
            Task<MeshResolution> displayTask = loadAndDisplayMesh(tFile);
            displayTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, ev -> {
                UtilityClass.copyCimLibResources();
                EventManager.fireCustomEvent(new CustomEvent(NEW_PROJECT, domainExtentionT));
            });
        });
        new Thread(convertTask).start();
    }

    /**
     * Convert Gmsh to .t file
     *
     * @param selectedFile the selected .msh file
     * @return the task to convert the file containing the .t file name
     */
    private Task<String> convertGmshToT(File selectedFile) {
        return new Task<>() {
            @Override
            protected String call() {
                ProgressReporter.report("Start MSH conversion");
                String result = MeshConverterFactory.convert(selectedFile.getAbsolutePath());
                ProgressReporter.report("End MSH conversion");
                return result;
            }
        };
    }

    /**
     * Load and display mesh in seperate task.
     * This will be helpful to display progress messages in real time
     *
     * @param tFile
     * @return the executed task containing the mesh to load and display the mesh
     */
    private Task<MeshResolution> loadAndDisplayMesh(File tFile) {
        Task<MeshResolution> task = new Task<>() {
            @Override
            protected MeshResolution call() {
                ProgressReporter.report("Start displaying domain");
                return parseFile2TriangleMesh(tFile);
            }
        };
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, ev -> {
            ProgressReporter.report("End displaying domain");
            MeshResolution meshResolution = task.getValue();
            // TODO display light version
            mainController.getRightPaneController().initiateDomain(meshResolution);
            // TODO propose this only for hight resolution version
            if (DomainProperties.getInstance().is3D()) {
                OpenGLViewer.show(meshResolution.getTriangleMesh());
            }
            mainController.getRightPaneController().applySelectedDisplayMode();
            importMSHButton.setStyle(FX_BASE_LIGHTGREEN);
            boundaryButton.setDisable(false);
            ProgressReporter.report("File imported successfully");
            runnerService = new RunnerService(leftBottomPaneController, workspaceDirectory);
        });
        new Thread(task).start();
        return task;
    }

    /**
     * Set up the project and display the domain mesh file (only for import project)
     *
     * @param meshFileExtentionT the domain mesh file
     * @return Load .t task file
     */
    protected Task<MeshResolution> setupAndDisplay(File meshFileExtentionT) {
        setupProject(meshFileExtentionT);
        // detect domain dimension
        int dimension = detectDimensionImportCase(meshFileExtentionT);
        DomainProperties.getInstance().setDimension(dimension);
        // import domain mesh file
        return loadAndDisplayMesh(meshFileExtentionT);
    }

    /**
     * Set up the project by setting the workspace directory, the domain extension and RunnerService
     *
     * @param meshFileExtentionT the domain mesh file
     */
    private void setupProject(File meshFileExtentionT) {
        workspaceDirectory = meshFileExtentionT.getParentFile();
        domainExtentionT = meshFileExtentionT.getName();
    }

    @FXML
    private void handleBoundaries() {
        if (leftBottomPaneController.hasNoBoundaryPane()) {
            // add empty boundaryPane
            leftBottomPaneController.addBoundaryPane();
        } else {
            leftBottomPaneController.displayAllBoundaries();
        }
    }

    @FXML
    protected void handleBoundaryCondition(ActionEvent actionEvent) {
        leftBottomPaneController.displayBoundaryConditionPane();
    }

    @FXML
    private void handleDataEngineering() {
        leftBottomPaneController.displayDataEngineerPane();
    }

    @FXML
    private void handleTimeDiscretization() {
        leftBottomPaneController.displayTimeDiscretizationPane();
    }

    @FXML
    private void handleRunConfiguration() {
        leftBottomPaneController.reloadRunConfigurationPane();
    }

    @FXML
    private void handleRun(ActionEvent e) {
        // setup cimlib project
        try {
            runnerService.generateAllMTCFiles(domainExtentionT);
        } catch (RuntimeException ex) {
            log.error(ex.getMessage(), ex);
            mainController.displayMessageConsoleOutput("Error while setting up the project: " + ex.getMessage());
        }
        // disable run button and show stop button
        runButton.setDisable(true);
        stopButton.setVisible(true);
        // run cimlib
        CompletableFuture.runAsync(() -> {
            // use python programme to convert to .t
            List<String> command = buildCimlibCommanLine();
            currentProcess = runnerService.startProcess(mainController, workspaceDirectory, command);
            // log cimblib output
            runThreadTologCimlibOutput();
            // handle cimlib end run
            handleCimblibEndRun();
        });
    }

    /**
     * Handle the end of the cimlib process
     * If the process end correctly, display the result in ParaView
     */
    private void handleCimblibEndRun() {
        try {
            if (currentProcess != null) {
                int exitCode = currentProcess.waitFor();
                Platform.runLater(() -> {
                    stopButton.setVisible(false);
                    runButton.setDisable(false);
                });
                EventManager.fireCustomEvent(new CustomEvent(CIMLIB_PROCESS_END));
                // if exit ok then display the result in ParaView
                if (exitCode == 0) {
                    List<String> paraViewcommand = Arrays.asList("paraview.exe", "bulles_..vtu");
                    String dimensionContext = DomainProperties.getInstance().is3D() ? "3d" : "2d";
                    UtilityClass.runCommand(new File(workspaceDirectory, "Resultats" + File.separator + dimensionContext),
                            paraViewcommand);
                } else {
                    // if exit not ok then display error message
                    mainController.displayMessageConsoleOutput("Error while running cimllib");
                }
            }
        } catch (InterruptedException ex) {
            log.error("CIMLib process was interrupted", ex);
            Platform.runLater(() -> {
                mainController.displayMessageConsoleOutput("Error: " + ex.getMessage());
                stopButton.setVisible(false);
                runButton.setDisable(false);
            });
        }
    }

    /**
     * Run a thread to log the cimlib output
     * Wait for the process to end and flush the console
     * End the process properly
     */
    private void runThreadTologCimlibOutput() {
        // Step 1: Create an ExecutorService with a single thread
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        // Step 2: Submit a Runnable task to the executor
        Future<?> future = executorService.submit(() -> {
            ConsolePaneController consolePaneController = mainController.getRightPaneController().getConsolePaneController();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String output = line;
                    Platform.runLater(() -> consolePaneController.displayMessageIfNeeded(output));
                }
            } catch (IOException ex) {
                Platform.runLater(() -> consolePaneController.displayMessage("Error reading process output: " + ex.getMessage()));
            }
        });
        try {
            // Step 3: Wait for the task to complete (future.get will return null)
            future.get();
        } catch (Exception ex) {
            log.error("Error reading process output", ex);
            mainController.displayMessageConsoleOutput(ex.getMessage());
        } finally {
            // flush remaining logs
            mainController.getRightPaneController().getConsolePaneController().flush();
            // Step 4: Shutdown the executor
            executorService.shutdown();
        }
    }

    @FXML
    private void handleStop() {
        if (currentProcess != null && currentProcess.isAlive()) {
            // Terminate the process and all its child processes
            currentProcess.descendants().forEach(ProcessHandle::destroy);
            currentProcess.destroy();

            mainController.displayMessageConsoleOutput("Simulation stopped by user");

            Platform.runLater(() -> {
                stopButton.setVisible(false);
                runButton.setDisable(false);
            });
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // initialize ProgressReporter listener to display messages in console
        ProgressReporter.setListener(msg -> mainController.displayMessageConsoleOutput(msg));
        // Register to listen to all boundary definition are valid
        listenAndHandleAllBoundaryValid();
        // Register to listen to all condition are valid
        listenAndHandleAllConditionValid();
        // register to listen to a new invalid condition
        listenAndHandleInvalidCondition();
        // register to listen to data engineer are valid
        listenAndHandleDataEngineeringValid();
        // register to listen to time discretization data are valid
        listenAndHandleTimeDiscretizationValid();
        // register to listen to meshing parameters data if is valid
        listenAndHandleMeshingParametersValid();
        // register to listen to run configuration data if is valid
        listenAndHandleRunConfigurationValid();
        // register to listen to boundary removed
        listenAndHandleBoundaryRemoved();
        // register to listen to any modification in the titled panes
        listenAndHandleActionOnPane();
    }

    /**
     * Register a listener to handle a new validated boundary definition
     */
    private void listenAndHandleAllBoundaryValid() {
        EventManager.addEventHandler(BOUNDARY_VALIDATED, event -> {
            BoundaryDefinitionController boundaryDefController = (BoundaryDefinitionController) event.getObject();
            //leftBottomPaneController.addBoundaryDefinition(boundaryDefController);
            // add and display boundary in the right pane
            mainController.getRightPaneController().addAndDisplay(boundaryDefController);
            // Handle the custom event
            if (conditionButton.isDisable()) {
                boundaryButton.setStyle(FX_BASE_LIGHTGREEN);
                conditionButton.setDisable(false);
            }
        });
    }

    /**
     * Register a listener to handle the suppression of a specific boundary definition
     */
    private void listenAndHandleBoundaryRemoved() {
        EventManager.addEventHandler(BOUNDARY_REMOVED, event -> {
            // Handle the custom event
            BoundaryDefinitionController boundaryDefinitionController = (BoundaryDefinitionController) event.getObject();
            leftBottomPaneController.removeBoundaryDefinition(boundaryDefinitionController);
            mainController.getRightPaneController().removeAndDisplay(boundaryDefinitionController);
            leftBottomPaneController.displayAllBoundaries();
            mainController.getRightPaneController().applySelectedDisplayMode();
            handleModificationOrValidationAction();
        });
    }

    private void listenAndHandleAllConditionValid() {
        EventManager.addEventHandler(ALL_CONDITION_VALID, event -> {
            // Handle the custom event
            if (dataEngineeringButton.isDisable()) {
                conditionButton.setStyle(FX_BASE_LIGHTGREEN);
                dataEngineeringButton.setDisable(false);
            } else if (FX_BASE_LIGHTRED.equals(conditionButton.getStyle())) {
                conditionButton.setStyle(FX_BASE_LIGHTGREEN);
                if (runButton.isDisable() && isTimeDiscretizationSet) {
                    runButton.setDisable(false);
                }
            }
        });
    }

    private void listenAndHandleInvalidCondition() {
        EventManager.addEventHandler(INVALID_CONDITION, event -> {
            // Handle the custom event
            conditionButton.setStyle(FX_BASE_LIGHTRED);
            runButton.setDisable(true);
        });
    }

    private void listenAndHandleDataEngineeringValid() {
        EventManager.addEventHandler(DATA_ENGINEERING_VALID, event -> {
            // Handle the custom event
            if (timeDiscretizationButton.isDisable()) {
                dataEngineeringButton.setStyle(FX_BASE_LIGHTGREEN);
                timeDiscretizationButton.setDisable(false);
                mainController.displayMessageConsoleOutput("Data engineering has been set up successfully.");
            }
        });
    }

    private void listenAndHandleTimeDiscretizationValid() {
        EventManager.addEventHandler(TIME_DISCRETIZATION_VALID, event -> {
            timeDiscretizationButton.setStyle(FX_BASE_LIGHTGREEN);
            mainController.displayMessageConsoleOutput("Time discretization has been set up successfully.");
            isTimeDiscretizationSet = true;
            // Activate the Meshing Parameters button after Time Discretization
            meshingParametersButton.setDisable(false);
        });
    }

    private void listenAndHandleMeshingParametersValid() {
        EventManager.addEventHandler(MESHING_PARAMETERS_VALID, event -> {
            meshingParametersButton.setStyle(FX_BASE_LIGHTGREEN);
            runConfigurationButton.setDisable(false);
            mainController.displayMessageConsoleOutput("Meshing parameters have been set up successfully.");
        });
    }

    private void listenAndHandleRunConfigurationValid() {
        EventManager.addEventHandler(RUN_CONFIGURATION_VALID, event -> {
            runConfigurationButton.setStyle(FX_BASE_LIGHTGREEN);
            runButton.setDisable(false);
        });
    }

    /**
     * Register a listener to handle an action on TitlePane (typing, validate or cancel)
     * If any TitlePane is invalid for any step then tag it with color
     * else back to default color
     */
    private void listenAndHandleActionOnPane() {
        EventManager.addEventHandler(TITLE_PANE_VALIDATED, event -> {
            handleModificationOrValidationAction();
        });
        EventManager.addEventHandler(TITLE_PANE_MODIFIED, event -> {
            handleModificationOrValidationAction();
        });
        EventManager.addEventHandler(TITLE_PANE_BOUNDARY_DEF_CREATED, event -> {
            handleModificationOrValidationAction();
        });
    }

    private void handleModificationOrValidationAction() {
        // if any modification in boundary definition then flag the boundary button
        boolean isAnyBoundaryDefInvalid = leftBottomPaneController.getBoundaryDefinitionControllers()
                .stream()
                .anyMatch(ValidAndCancelController::isNotValidated);
        updateButtonStatus(boundaryButton, isAnyBoundaryDefInvalid);
        // if any modification in condition definition then flag the condition button
        boolean isAnyConditionInvalid = leftBottomPaneController.getConditionGlobalController()
                .getAllConditions()
                .stream()
                .anyMatch(ValidAndCancelController::isNotValidated);
        updateButtonStatus(conditionButton, isAnyConditionInvalid);
        // if any modification in data engineering then flag the data engineering button
        boolean isTimeDisInvalid = leftBottomPaneController.getTimeDiscretizationController().isNotValidated();
        updateButtonStatus(timeDiscretizationButton, isTimeDisInvalid);
        // if any modification in data engineering then flag the data engineering button
        boolean dataIngNotInvalid = leftBottomPaneController.getDataEngineeringController().isNotValidated();
        updateButtonStatus(dataEngineeringButton, dataIngNotInvalid);

        boolean meshingDataInvalid = leftBottomPaneController.getMeshingParametersController().isNotValidated();
        updateButtonStatus(meshingParametersButton, meshingDataInvalid);

        // Disable run button if any step is invalid
        boolean isAnyStepInvalid = isAnyBoundaryDefInvalid || isAnyConditionInvalid || isTimeDisInvalid || dataIngNotInvalid;
        runButton.setDisable(isAnyStepInvalid);
    }

    /**
     * Update the button style based on the validation state
     *
     * @param button           the button to update
     * @param isNeedsAttention Set to blue if valid, orange if not valid
     */
    private void updateButtonStatus(Button button, boolean isNeedsAttention) {
        if (!button.isDisabled()) {
            if (isNeedsAttention) {
                button.getStyleClass().add("needs-attention");
            } else {
                button.getStyleClass().removeAll("needs-attention");
            }
        }
    }

    private List<String> buildCimlibCommanLine() {
        File tempFile = UtilityClass.exeTempFile();
        List<String> command = new ArrayList<>();
        int numberOfCores = leftBottomPaneController.getRunConfigurationController().getNumberOfCores();
        if (numberOfCores > 1) {
            String[] pathDirs = System.getenv("PATH").split(";");
            String mpiexecPath = null;
            for (String dir : pathDirs) {
                if (dir.toLowerCase().endsWith("mpiexec.exe")) {
                    File mpiExec = new File(dir);
                    if (mpiExec.exists()) {
                        mpiexecPath = dir;
                        break;
                    }
                } else {
                    File mpiExec = new File(dir, "mpiexec.exe");
                    if (mpiExec.exists()) {
                        mpiexecPath = mpiExec.getAbsolutePath();
                        break;
                    }
                }
            }
            if (mpiexecPath == null) {
                mainController.displayMessageConsoleOutput("Error: mpiexec.exe not found. Please ensure MS-MPI is installed.");
                return null;
            }
            command.add("\"" + mpiexecPath + "\"");
            command.add("-n");
            command.add(String.valueOf(numberOfCores));
            command.add(tempFile.getAbsolutePath());
            command.add("Principale.mtc");
        } else {
            command.add(tempFile.getAbsolutePath());
            command.add("Principale.mtc");
        }
        return command;
    }

}
