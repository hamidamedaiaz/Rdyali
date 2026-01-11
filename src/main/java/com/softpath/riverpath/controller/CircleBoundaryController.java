package com.softpath.riverpath.controller;

import com.softpath.riverpath.model.Boundary;
import com.softpath.riverpath.model.Coordinates;
import com.softpath.riverpath.model.RadialBoundary;
import com.softpath.riverpath.util.DomainProperties;
import com.softpath.riverpath.util.UtilityClass;
import com.softpath.riverpath.util.ValidatedField;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CircleBoundaryController
 *
 * @author rhajou
 */
@NoArgsConstructor
public class CircleBoundaryController extends BaseBoundaryController {

    @FXML
    private Label circleLabel;
    @FXML
    @ValidatedField(isDouble = true)
    @Getter
    private TextField radius;

    /**
     * @see BaseBoundaryController#importValues(Boundary)
     */
    @Override
    void importValues(Boundary boundary) {
        if (boundary instanceof RadialBoundary radialBoundary) {
            radius.setText(radialBoundary.getRadius());
            UtilityClass.unflagTextFieldWarning(radius);
        }
    }

    /**
     * @see BaseBoundaryController#getShape(DomainProperties, Coordinates)
     */
    @Override
    Shape getShape(DomainProperties domainProperties, Coordinates origin) {
        Circle circle = new Circle();
        double scaleFactor = domainProperties.getScaleFactor();
        circle.setCenterX(Double.parseDouble(origin.getX()) * scaleFactor);
        // ⚠️JAVAFX_INVERTED_AXIS_Y
        circle.setCenterY(Double.parseDouble(origin.getY()) * -scaleFactor);
        circle.setRadius(Double.parseDouble(getInitialValue(radius)) * scaleFactor);
        return circle;
    }

}
