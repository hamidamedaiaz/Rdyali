package com.softpath.riverpath.controller;

import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventEnum;
import com.softpath.riverpath.custom.event.EventManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract controller to handle validate/cancel
 *
 * @author rahajou
 */
public abstract class ValidAndCancelController extends FieldContainerController {

    @FXML
    private Button validateButton;
    @FXML
    private Button cancelButton;
    @Getter
    @Setter
    private boolean isValid = false;
    private boolean isNeverValidated = true;

    @FXML
    protected void handleValidate(ActionEvent event) {
        disableValidateAndCancel();
        boolean validCommit = checkValidCommit();
        boolean customValid = customValidate();
        if (validCommit && customValid) {
            saveFields();
            isNeverValidated = false;
            dispatchValidState();
        } else {
            updateRootModifiedState();
            enableValidateAndCancel();
        }
    }

    /**
     * Cancel Button - rollback changes
     */
    @FXML
    private void handleCancel(ActionEvent event) {
        reload();
        disableValidateAndCancel();
    }

    private void updateRootValidatedSate() {
        getRoot().setStyle("");
    }

    protected void updateRootModifiedState() {
        getRoot().setStyle("-fx-border-color: orange; -fx-border-width: 4;");
        enableValidateAndCancel();
    }

    /**
     * Disable validate and cancel buttons
     */
    private void disableValidateAndCancel() {
        cancelButton.setDisable(true);
        validateButton.setDisable(true);
        updateRootValidatedSate();
        isValid = true;
        EventManager.fireCustomEvent(new CustomEvent(EventEnum.TITLE_PANE_VALIDATED, this));
    }

    /**
     * Enable validate and cancel buttons
     */
    private void enableValidateAndCancel() {
        if (!isNeverValidated) {
            cancelButton.setDisable(false);
        }
        validateButton.setDisable(false);
        isValid = false;
        EventManager.fireCustomEvent(new CustomEvent(EventEnum.TITLE_PANE_MODIFIED, this));
    }

    /**
     * Reload all fields to their last validated value
     * Remove warning border
     */
    private void reload() {
        rollbackField();
        customReload();
        updateRootValidatedSate();
    }

    /**
     * @return true if the form is not validated
     */
    public boolean isNotValidated() {
        return !isValid;
    }

    protected abstract Parent getRoot();

    protected abstract boolean customValidate();

    protected abstract void customReload();
    protected abstract void dispatchValidState();
}
