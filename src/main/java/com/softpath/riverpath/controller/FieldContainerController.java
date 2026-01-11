package com.softpath.riverpath.controller;

import com.softpath.riverpath.model.HyperLinkFieldHandler;
import com.softpath.riverpath.model.TextFieldHandler;
import com.softpath.riverpath.util.DomainProperties;
import com.softpath.riverpath.util.ValidatedField;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.softpath.riverpath.util.UtilityClass.flagTextFieldWarning;

/**
 * @author rahajou
 */
public abstract class FieldContainerController {

    private final List<TextFieldHandler> textFieldHandlers = new ArrayList<>();
    private final List<HyperLinkFieldHandler> hyperLinkFieldHandlers = new ArrayList<>();

    @FXML
    public void initialize() {
        addAllTextFieldHandlers();
    }

    private void addAllTextFieldHandlers() {
        boolean mode3D = DomainProperties.getInstance().is3D();
        for (Field f : this.getClass().getDeclaredFields()) {
            if (f.isAnnotationPresent(FXML.class) && f.isAnnotationPresent(ValidatedField.class)) {
                ValidatedField validatedField = f.getAnnotation(ValidatedField.class);
                if (validatedField.is3D() && !mode3D) {
                    continue;
                }
                f.setAccessible(true);
                try {
                    if (f.get(this) instanceof TextField tf) {
                        // Basic default FieldState: string not null
                        textFieldHandlers.add(new TextFieldHandler(tf, validatedField, this::updateRootModifiedState));
                    } else if (f.get(this) instanceof Hyperlink hl) {
                        hyperLinkFieldHandlers.add(new HyperLinkFieldHandler(hl));
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("unexpected error while handling ValidatedField", e);
                }
            }
        }
    }

    /**
     * save all text fields
     */
    protected void saveFields() {
        textFieldHandlers.forEach(TextFieldHandler::commit);
        hyperLinkFieldHandlers.forEach(HyperLinkFieldHandler::commit);
    }

    public boolean hasDirtyFields() {
        if (this.textFieldHandlers != null) {
            for (TextFieldHandler tfh : this.textFieldHandlers) {
                if (tfh.isDirty()) return true;
            }
        }

        if (this.hyperLinkFieldHandlers != null) {
            for (HyperLinkFieldHandler hlf : this.hyperLinkFieldHandlers) {
                if (hlf.isDirty()) return true;
            }
        }

        return false;
    }
    /**
     * Rollback all text fields
     */
    protected void rollbackField() {
        textFieldHandlers.forEach(TextFieldHandler::rollback);
        hyperLinkFieldHandlers.forEach(HyperLinkFieldHandler::rollback);
    }

    protected boolean checkValidCommit() {
        boolean mode3D = DomainProperties.getInstance().is3D();
        boolean validGlobal = true;
        for (TextFieldHandler h : textFieldHandlers) {
            boolean valid = true;
            //controls on parameters of validated annotated
            ValidatedField ann = h.getAnnotatedField();

            // 1. control on is3D()
            if (ann.is3D()) { // means this is a field only considered in 3D mode
                if (!mode3D) continue; // so we exempt it from checks if not in 3D mode
            }
            //2. control on nullable()
            if (!ann.nullable()) {
                valid = valid && !StringUtils.isBlank(h.getField().getText());
            }
            //3. control on isUnique()
            if (ann.isUnique()) {
                valid = valid && !isUsedName();
            }
            // after all controls, set the field validity
            if (!valid) {
                flagTextFieldWarning(h.getField());
                validGlobal = false;
            }
        }

        return validGlobal;
    }

    protected boolean isUsedName() {
        return true;
    }

    /**
     * Get the initial value of a field (text or hyperlink)
     * Look for the field in textFieldHandlers and hyperLinkFieldHandlers
     *
     * @param field the field to check
     * @return the initial value
     */
    protected String getInitialValue(Node field) {
        return hyperLinkFieldHandlers.stream()
                .filter(h -> h.getHyperLink().equals(field))
                .map(HyperLinkFieldHandler::getLastValidatedValue)
                .findFirst()
                .orElseGet(() -> textFieldHandlers.stream()
                        .filter(t -> t.getField().equals(field))
                        .map(TextFieldHandler::getLastValidatedValue)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Field not found in bindings: " + field))
                );
    }


    protected abstract void updateRootModifiedState();
}
