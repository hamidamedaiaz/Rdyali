package com.softpath.riverpath.controller;

import com.softpath.riverpath.custom.event.EventManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URL;
import java.util.ResourceBundle;

import static com.softpath.riverpath.custom.event.EventEnum.*;

@NoArgsConstructor
@Getter
@Setter
public class BottomMainController implements Initializable {

    @FXML
    private Label percentageLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private MainController mainController;

    private double totalIncrement;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // subscribe event listener
        EventManager.addEventHandler(PROGRESS_BAR_UPDATE, event -> {
            // update progress bar
            if (!progressBar.isVisible()) {
                progressBar.setVisible(true);
                percentageLabel.setVisible(true);
            }
            double v = ((Integer) event.getObject()) / totalIncrement;
            percentageLabel.setText(Math.round(v * 100) + "%");
            progressBar.setProgress(v);
        });
        EventManager.addEventHandler(NEW_TOTAL_INCREMENT_VALUE, event -> {
            this.totalIncrement = (Double) event.getObject();
        });
        EventManager.addEventHandler(CIMLIB_PROCESS_END, event -> {
            progressBar.setVisible(false);
            percentageLabel.setVisible(false);
        });
    }
}
