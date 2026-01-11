package com.softpath.riverpath.controller;

import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventEnum;
import com.softpath.riverpath.custom.event.EventManager;
import com.softpath.riverpath.custom.pane.BoundaryTitledPane;
import com.softpath.riverpath.model.Simulation;
import com.softpath.riverpath.util.UtilityClass;
import com.softpath.riverpath.util.ValidatedField;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Getter
@Slf4j
public class TimeDiscretizationController extends ValidAndCancelController {

    @FXML
    private BoundaryTitledPane titledPane;
    @FXML
    @ValidatedField(isDouble = true)
    private TextField timeStep;
    @FXML
    @ValidatedField(isDouble = true)
    private TextField totalTime;
    @FXML
    @ValidatedField
    private TextField storageFrequency;

    /**
     * @return
     */
    @Override
    protected Parent getRoot() {
        return titledPane;
    }

    /**
     *
     */
    @Override
    protected boolean customValidate() {
        return true;
    }

    /**
     *
     */
    @Override
    protected void customReload() {
        // do nothing for this controller
    }

    @FXML
    private void handleKeyReleased(KeyEvent keyEvent) {
        UtilityClass.handleTextWithDigitOnly(keyEvent);
    }

    /**
     * Import previous simulation data
     *
     * @param simulation Simulation object
     */
    protected void importData(Simulation simulation) {
        try {
            if (!simulation.getTimeStep().isEmpty() || !simulation.getTotalTime().isEmpty() || !simulation.getFrequency().isEmpty()) {
                timeStep.setText(simulation.getTimeStep());
                totalTime.setText(simulation.getTotalTime());
                storageFrequency.setText(simulation.getFrequency());
                handleValidate(null);
            }
        } catch (NullPointerException e) {
            log.warn("No data to import for module");
        }
    }

    /**
     * Get the total increment by dividing the total time by the time step
     *
     * @return the total increment value as a double
     */
    public Double getTotalIncrement() {
        String totalTimeInitialValue = getInitialValue(totalTime);
        String timeStepInitialValue = getInitialValue(timeStep);
        return Double.parseDouble(totalTimeInitialValue) / Double.parseDouble(timeStepInitialValue);
    }

    @Override
    protected void dispatchValidState() {
        // fire time discretization valid event
        EventManager.fireCustomEvent(new CustomEvent(EventEnum.TIME_DISCRETIZATION_VALID, this));
        // fire total increment event
        EventManager.fireCustomEvent(new CustomEvent(EventEnum.NEW_TOTAL_INCREMENT_VALUE, getTotalIncrement()));
    }
}
