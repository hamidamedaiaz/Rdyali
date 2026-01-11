package com.softpath.riverpath.model;

import javafx.scene.control.Hyperlink;
import lombok.Getter;
import org.apache.commons.lang3.Strings;

/**
 * Handle a hyperlink field with original value, dirty check, commit and rollback
 */
@Getter
public class HyperLinkFieldHandler {

    // the text field to handle
    private final Hyperlink hyperLink;
    // the original value of the text field
    private String lastValidatedValue;

    public HyperLinkFieldHandler(Hyperlink field) {
        this.hyperLink = field;
        lastValidatedValue = field.getText();
    }

    public void commit() {
        lastValidatedValue = hyperLink.getText();
    }

    public void rollback() {
        hyperLink.setText(lastValidatedValue);
    }

    public boolean isDirty() {
        return !Strings.CS.equals(hyperLink.getText(), lastValidatedValue);
    }

}
