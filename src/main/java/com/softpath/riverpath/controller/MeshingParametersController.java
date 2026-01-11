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
public class MeshingParametersController extends ValidAndCancelController {

    @FXML
    private BoundaryTitledPane titledPane;

    @FXML
    @ValidatedField
    private TextField nbElements;
    @FXML
    @ValidatedField(isDouble = true)
    private TextField hMin;
    @FXML
    @ValidatedField(isDouble = true)
    private TextField lMax;
    @FXML
    @ValidatedField(isDouble = true)
    private TextField nScaling;
    @FXML
    @ValidatedField(isDouble = true)
    private TextField scaleNorme;
    @FXML
    @ValidatedField(isDouble = true)
    private TextField err1;
    @FXML
    @ValidatedField(isDouble = true)
    private TextField err2;
    @FXML
    @ValidatedField(isDouble = true)
    private TextField adaptateur;
    @FXML
    @ValidatedField(isDouble = true)
    private TextField lMin;

    @FXML
    private void handleKeyReleased(KeyEvent keyEvent) {
        TextField source = (TextField) keyEvent.getSource();
        UtilityClass.handleTextWithDigitOnly(keyEvent);
        UtilityClass.checkNotBlank(source);
    }

    @Override
    protected Parent getRoot() {
        return this.titledPane;
    }

    @Override
    protected boolean customValidate() {
        return true;
    }

    @Override
    protected void customReload() {
        // do nothing
    }

    public void importData(Simulation simulation) {
        // importer les données du fichier simulation
        try {
            if (!simulation.getNbElements().isEmpty()
                    || !simulation.getHMin().isEmpty()
                    || !simulation.getLMax().isEmpty()
                    || !simulation.getNScaling().isEmpty()
                    || !simulation.getScaleNorme().isEmpty()
                    || !simulation.getAdaptateur().isEmpty()
                    || !simulation.getLMin().isEmpty()
                    || !simulation.getErr1().isEmpty()
                    || !simulation.getErr2().isEmpty()
            ) {
                nbElements.setText(simulation.getNbElements());
                hMin.setText(simulation.getHMin());
                lMax.setText(simulation.getLMax());
                nScaling.setText(simulation.getNScaling());
                scaleNorme.setText(simulation.getScaleNorme());
                err1.setText(simulation.getErr1());
                err2.setText(simulation.getErr2());
                adaptateur.setText(simulation.getAdaptateur());
                lMin.setText(simulation.getLMin());

                // validate imported data
                handleValidate(null);
            }
        } catch (NullPointerException e) {
            log.warn("No data to import for module");
        }
    }

    @Override
    protected void dispatchValidState() {
        EventManager.fireCustomEvent(new CustomEvent(EventEnum.MESHING_PARAMETERS_VALID, this));
    }

}