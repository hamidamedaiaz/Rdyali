package com.softpath.riverpath.controller;

import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventManager;
import com.softpath.riverpath.custom.pane.BoundaryTitledPane;
import com.softpath.riverpath.util.UtilityClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.softpath.riverpath.custom.event.EventEnum.RUN_CONFIGURATION_VALID;

@NoArgsConstructor
public class RunConfigurationController {

    @Getter
    @FXML
    private BoundaryTitledPane titledPane;

    @FXML
    private RadioButton serialMode;

    @FXML
    private RadioButton parallelMode;

    @FXML
    private Label coresLabel;

    @FXML
    private TextField coresField;

    @FXML
    private Button chooseButton;

    @Getter
    private int numberOfCores;

    @FXML
    public void initialize() {
        // Manages the visibility of the core selector
        parallelMode.selectedProperty().addListener((obs, oldVal, newVal) -> {
            coresLabel.setVisible(newVal);
            coresLabel.setManaged(newVal);
            coresField.setVisible(newVal);
            coresField.setManaged(newVal);
        });

        // Initially hides core controls
        coresLabel.setVisible(false);
        coresLabel.setManaged(false);
        coresField.setVisible(false);
        coresField.setManaged(false);
    }

    @FXML
    private void handleKeyReleased(KeyEvent event) {
        UtilityClass.handleTextWithDigitOnly(event);
    }

    @FXML
    private void handleChoose() {
        // Checks the validity of the input
        String coresText = coresField.getText();
        // Determines the number of cores
        this.numberOfCores = serialMode.isSelected() ? 1 : Integer.parseInt(coresText);
        EventManager.fireCustomEvent(new CustomEvent(RUN_CONFIGURATION_VALID));
    }

}