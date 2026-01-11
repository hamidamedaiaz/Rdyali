package com.softpath.riverpath.controller;

import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventEnum;
import com.softpath.riverpath.custom.event.EventManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class MenuBarController {

    private static final double MIN_WIDTH = 600;
    private static final double MIN_HEIGHT = 400;
    private static final int MAXIMIZE_TRIGGER_AREA = 5; // Pixels detection zone
    @FXML
    private VBox topBar;
    @FXML
    private Button minimizeButton;
    @FXML
    private Button maximizeButton;
    @FXML
    private Button closeButton;
    private Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private Rectangle2D backupWindowBounds;
    @Setter
    private MainController mainController;


    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
    }

    @FXML
    private void handleMinimize() {
        stage.setIconified(true);
    }

    @FXML
    private void handleMaximize() {
        toggleMaximize();
    }

    @FXML
    private void handleClose() {
        // Check and stop the simulation if it is running
        if (mainController != null) {
            String actionOnexit = mainController.showActionsOnExitWithStableState();
            switch (actionOnexit) {
                case "SAVE" -> {
                    mainController.getProjectSetupController().getLeftBottomPaneController().handleSaveOnExit();
                    if (mainController.getProjectSetupController().getLeftBottomPaneController().hasUIResourcesToValidate()) {// means the validation was not successful
                        String actionOnValidationError = mainController.promptErrorOnValidation();
                        switch (actionOnValidationError) {
                            case "CLOSE_ANYWAY" -> {/*do nothing to pass to next phase*/}
                            case "RETURN_TO_UI" -> {
                                return; // close action is canceled
                            }
                            default -> {
                                return; //safeguard rollback
                            }
                        }
                    }
                    handleSaveProjectChanges();
                }
                case "CLOSE_ANYWAY" -> { // here we immediately close, so we just pass to next phase
                }
                default -> { //rollback on exit clicked
                    return;
                }
            }
            ProjectSetupController projectSetupController = mainController.getProjectSetupController();
            if (projectSetupController != null
                    && projectSetupController.getCurrentProcess() != null
                    && projectSetupController.getCurrentProcess().isAlive()) {

                // Stop the simulation
                projectSetupController.getCurrentProcess().descendants().forEach(ProcessHandle::destroy);
                projectSetupController.getCurrentProcess().destroy();
                mainController.displayMessageConsoleOutput("Simulation stopped due to window closing");
            }
        }
        stage.close();
    }

    @FXML
    private void handleMousePressed(MouseEvent event) {
        if (!isMaximized) {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        } else {
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            xOffset = (event.getScreenX() - screenBounds.getMinX()) * (backupWindowBounds.getWidth() / screenBounds.getWidth());
            yOffset = (event.getScreenY() - screenBounds.getMinY()) * (backupWindowBounds.getHeight() / screenBounds.getHeight());
        }
    }

    @FXML
    private void handleMouseDragged(MouseEvent event) {
        if (!isMaximized) {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        } else {
            restoreWindowFromMaximized(event.getScreenX(), event.getScreenY());
        }
    }

    @FXML
    private void handleMouseReleased(MouseEvent event) {
        if (event.getScreenY() <= MAXIMIZE_TRIGGER_AREA && !isMaximized) {
            toggleMaximize();
        }
    }

    @FXML
    private void handleSaveProjectChanges() {
        EventManager.fireCustomEvent(new CustomEvent(EventEnum.SAVED_STATE));
    }

    private void toggleMaximize() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        if (!isMaximized) {
            backupWindowBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            isMaximized = true;
        } else {
            double newWidth = Math.max(MIN_WIDTH, Math.min(screenBounds.getWidth() * 0.75,
                    backupWindowBounds.getWidth()));
            double newHeight = Math.max(MIN_HEIGHT, Math.min(screenBounds.getHeight() * 0.75,
                    backupWindowBounds.getHeight()));

            double newX = Math.min(Math.max(screenBounds.getMinX(), backupWindowBounds.getMinX()),
                    screenBounds.getMaxX() - newWidth);
            double newY = Math.min(Math.max(screenBounds.getMinY(), backupWindowBounds.getMinY()),
                    screenBounds.getMaxY() - newHeight);
            stage.setX(newX);
            stage.setY(newY);
            stage.setWidth(newWidth);
            stage.setHeight(newHeight);
            isMaximized = false;
        }
    }

    private void restoreWindowFromMaximized(double mouseX, double mouseY) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double newWidth = Math.max(MIN_WIDTH, Math.min(screenBounds.getWidth() * 0.75, backupWindowBounds.getWidth()));
        double newHeight = Math.max(MIN_HEIGHT, Math.min(screenBounds.getHeight() * 0.75, backupWindowBounds.getHeight()));

        double ratioX = (mouseX - screenBounds.getMinX()) / screenBounds.getWidth();
        double newX = mouseX - (newWidth * ratioX);
        newX = Math.min(Math.max(screenBounds.getMinX(), newX), screenBounds.getMaxX() - newWidth);

        double newY = mouseY - yOffset;
        newY = Math.min(Math.max(screenBounds.getMinY(), newY), screenBounds.getMaxY() - newHeight);

        stage.setX(newX);
        stage.setY(newY);
        stage.setWidth(newWidth);
        stage.setHeight(newHeight);
        isMaximized = false;

        xOffset = newWidth * ratioX;
        yOffset = mouseY - newY;
    }

    @FXML
    private void handleNewProject() {
        try {
            // Load the new window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("new-project.fxml"));
            BorderPane root = loader.load();
            MainController mainController = loader.getController();

            // Create a new scene
            Scene scene = new Scene(root);
            Stage newStage = new Stage();
            mainController.initializeStage(newStage);
            mainController.setStage(newStage);

            newStage.setScene(scene);
            newStage.setMaximized(true);
            newStage.show();
        } catch (IOException e) {
            log.error("Failed to create new project", e);
            mainController.displayMessageConsoleOutput("Error: Unable to create a new project.");
        }
    }

    @FXML
    private void handleImportProject() {
        try {
            // Load the new window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("new-project.fxml"));
            BorderPane root = loader.load();
            MainController mainController = loader.getController();

            // Create a new scene
            Scene scene = new Scene(root);
            Stage newStage = new Stage();
            mainController.initializeStage(newStage);
            mainController.setStage(newStage);

            newStage.setScene(scene);
            newStage.setMaximized(true);
            newStage.show();

            // Initiate project import
            ImportProjectController importProjectController = new ImportProjectController(mainController);
            importProjectController.handleImportProject();
        } catch (IOException e) {
            log.error("Failed to import project", e);
            mainController.displayMessageConsoleOutput("Error: Unable to import project.");
        }
    }
}