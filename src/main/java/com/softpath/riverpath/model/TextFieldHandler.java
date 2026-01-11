package com.softpath.riverpath.model;

import com.softpath.riverpath.util.UtilityClass;
import com.softpath.riverpath.util.ValidatedField;
import javafx.scene.control.TextField;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import static com.softpath.riverpath.util.UtilityClass.flagTextFieldWarning;
import static com.softpath.riverpath.util.UtilityClass.unflagTextFieldWarning;

/**
 * Handle a text field with original value, dirty check, commit and rollback
 */
@Getter
public class TextFieldHandler {

    // the text field to handle
    private final TextField field;
    // a copy of annotation if present
    private final ValidatedField annotatedField;
    // the original value of the text field
    private String lastValidatedValue;
    private Boolean isFormatting = false;

    public TextFieldHandler(TextField field, ValidatedField ann, Runnable changeListener) {
        this.annotatedField = ann;
        this.field = field;
        lastValidatedValue = field.getText();
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!isFormatting && !Strings.CI.equals(StringUtils.trimToEmpty(oldValue), StringUtils.trimToEmpty(newValue))) {
                updateTextFieldColor();
                if (changeListener != null) changeListener.run();
            }
        });
    }

    // to be checked and factorized across controllers
    private void updateTextFieldColor() {
        if (isDirty()) {
            flagTextFieldWarning(field);
        }
    }

    public void commit() {
        if (annotatedField.isDouble() && !(annotatedField.nullable() && StringUtils.isBlank(field.getText()))) {
            isFormatting = true;
            UtilityClass.prettyPrintDouble(field);
            isFormatting = false;
        }

        lastValidatedValue = field.getText();
        unflagTextFieldWarning(field);
    }

    public void rollback() {
        field.setText(lastValidatedValue);
        unflagTextFieldWarning(field);
    }

    public boolean isDirty() {
        return !Strings.CS.equals(field.getText(), lastValidatedValue);
    }

}
