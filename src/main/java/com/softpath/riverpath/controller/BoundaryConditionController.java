package com.softpath.riverpath.controller;

import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventEnum;
import com.softpath.riverpath.custom.event.EventManager;
import com.softpath.riverpath.custom.pane.BoundaryTitledPane;
import com.softpath.riverpath.model.BoundaryCondition;
import com.softpath.riverpath.util.DomainProperties;
import com.softpath.riverpath.util.UtilityClass;
import com.softpath.riverpath.util.ValidatedField;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * A class to handle boundary condition
 *
 * @author rhajou
 */
@NoArgsConstructor
@Getter
@Slf4j
public class BoundaryConditionController extends ValidAndCancelController implements Initializable {

    @FXML
    @ValidatedField(nullable = true)
    private TextField priorityValue;
    @FXML
    private Label titledName;
    @FXML
    private HBox originHBox;
    @FXML
    @ValidatedField(nullable = true, isDouble = true)
    private TextField vxValue;
    @FXML
    @ValidatedField(nullable = true, isDouble = true)
    private TextField vyValue;
    @FXML
    @ValidatedField(nullable = true, is3D = true, isDouble = true)
    private TextField vzValue;
    @FXML
    private BoundaryTitledPane boundaryConditionPane;
    @FXML
    @ValidatedField
    private TextField conditionNameValue;
    @FXML
    @ValidatedField(nullable = true, isDouble = true)
    private TextField pressureValue;

    /**
     * @see Initializable#initialize(URL, ResourceBundle)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize();
        Platform.runLater(() -> {
            if (!DomainProperties.getInstance().is3D()) {
                originHBox.getChildren().remove(vzValue);
                // Define a default value for Z in 2D
                vzValue.setText("");
            }
        });
    }

    /**
     * Reload the initial values
     */
    @Override
    protected void customReload() {
        // do nothing
    }

    /**
     * Used to import boundary condition data from simulation.json
     *
     * @param boundaryCondition the boundary condition to import
     */
    public void importConditionData(BoundaryCondition boundaryCondition) {
        try {
            // Set condition name
            conditionNameValue.setText(boundaryCondition.getName());
            // Set velocity if available
            vxValue.setText(boundaryCondition.getVelocity().getX());
            vyValue.setText(boundaryCondition.getVelocity().getY());
            if (DomainProperties.getInstance().is3D()) {
                vzValue.setText(boundaryCondition.getVelocity().getZ());
            }
            // Set pressure if available
            pressureValue.setText(boundaryCondition.getPressure());
            // Set priority if available
            priorityValue.setText(boundaryCondition.getPriority());
            // Validate the data
            handleValidate(null);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }

    @FXML
    private void checkNotBlank(KeyEvent keyEvent) {
        UtilityClass.checkNotBlank((TextField) keyEvent.getSource());
    }

    @FXML
    private void handleKeyReleased(KeyEvent keyEvent) {
        UtilityClass.handleTextWithDigitOnly(keyEvent);
    }

    /**
     * Validate the boundary condition with all input data given by the user
     */
    @Override
    protected boolean customValidate() {
        return true;
    }

    public String getLabel() {
        return titledName.getText();
    }

    public void setLabel(String text) {
        titledName.setText(text);
    }

    @Override
    protected Parent getRoot() {
        return boundaryConditionPane;
    }

    @Override
    protected void dispatchValidState() {
        EventManager.fireCustomEvent(new CustomEvent(EventEnum.NEW_CONDITION_VALIDATED, titledName.getText(), this));
    }
}

