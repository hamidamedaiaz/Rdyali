package com.softpath.riverpath.controller;

import com.softpath.riverpath.util.UtilityClass;
import com.softpath.riverpath.util.WindowResizer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * This class to handle welcome page
 *
 * @author rhajou
 */
@Slf4j
@NoArgsConstructor
@Getter
@Setter
public class WelcomeController implements Initializable {

    @FXML
    private Button newProject;
    @FXML
    private Button exitButton;
    @FXML
    private Button importProject;

    private WindowResizer windowResizer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        UtilityClass.createOrGetHomeDirectory();
        windowResizer = new WindowResizer();
    }

    /**
     * Handle new project action
     *
     * @param event the event created after click on {@link #newProject} button
     * @throws IOException in error case
     */
    @FXML
    private void handleNewProject(ActionEvent event) throws IOException {
        if (event != null) {
            Node source = (Node) event.getSource();
            Stage welcomeStage = (Stage) source.getScene().getWindow();
            welcomeStage.hide();
            welcomeStage.close();
        }
        // load main page
        loadMainPage();
    }

    /**
     * Handle import project action
     *
     * @param event the event created after click on {@link #importProject} button
     * @throws IOException in error case
     */
    @FXML
    private void handleImportProject(ActionEvent event) {
        if (event != null) {
            Node source = (Node) event.getSource();
            Stage welcomeStage = (Stage) source.getScene().getWindow();
            welcomeStage.hide();
            welcomeStage.close();
        }

        Platform.runLater(() -> {
            try {
                // load main page
                MainController mainController = loadMainPage();
                ImportProjectController importProjectController = new ImportProjectController(mainController);
                importProjectController.handleImportProject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private MainController loadMainPage() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("new-project.fxml"));
        BorderPane root = loader.load();
        MainController mainController = loader.getController();

        Scene scene = new Scene(root);
        Stage projectStage = new Stage();

        // Get screen dimensions
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        mainController.initializeStage(projectStage);
        mainController.setStage(projectStage);
        projectStage.setScene(scene);

        // Set the maximum size to prevent overflow
        projectStage.setX(screenBounds.getMinX());
        projectStage.setY(screenBounds.getMinY());
        projectStage.setWidth(screenBounds.getWidth());
        projectStage.setHeight(screenBounds.getHeight());

        // Add WindowResizer to the window
        windowResizer.makeResizable(projectStage, scene);

        projectStage.show();
        return mainController;
    }

    /**
     * Handle exit action after click on {@link #exitButton}
     */
    @FXML
    private void handleExitButton() {
        Platform.exit();
    }
}