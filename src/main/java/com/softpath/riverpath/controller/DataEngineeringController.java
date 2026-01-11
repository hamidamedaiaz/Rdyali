package com.softpath.riverpath.controller;

import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventManager;
import com.softpath.riverpath.custom.pane.BoundaryTitledPane;
import com.softpath.riverpath.model.Simulation;
import com.softpath.riverpath.util.ValidatedField;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static com.softpath.riverpath.custom.event.EventEnum.DATA_ENGINEERING_VALID;
import static com.softpath.riverpath.util.UtilityClass.handleTextWithDigitOnly;

@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class DataEngineeringController extends ValidAndCancelController {

    @FXML
    private BoundaryTitledPane titledPane;

    @FXML
    @ValidatedField(isDouble = true)
    private TextField viscosity;

    @FXML
    @ValidatedField(isDouble = true)
    private TextField density;

    /**
     * Key released event - allow only digit and dot
     *
     * @param event KeyEvent
     */
    @FXML
    private void handleKeyReleased(KeyEvent event) {
        handleTextWithDigitOnly(event);
    }

    /**
     * @return
     */
    @Override
    protected Parent getRoot() {
        return titledPane;
    }

    @Override
    protected boolean customValidate() {
        return true;
    }

    @Override
    protected void customReload() {
        // do nothing
    }

    protected void importData(Simulation simulation) {
        // import data
        try {
            if (!simulation.getViscosity().isEmpty() || !simulation.getDensity().isEmpty()) {
                viscosity.setText(simulation.getViscosity());
                density.setText(simulation.getDensity());
                handleValidate(null);
            }
        } catch (NullPointerException e) {
            log.warn("No data to import for module");
        }
    }

    @Override
    protected void dispatchValidState() {
        EventManager.fireCustomEvent(new CustomEvent(DATA_ENGINEERING_VALID, this));
    }

}
