package com.softpath.riverpath.controller;

import com.softpath.riverpath.model.Boundary;
import com.softpath.riverpath.model.Coordinates;
import com.softpath.riverpath.model.CubeBoundary;
import com.softpath.riverpath.util.DomainProperties;
import com.softpath.riverpath.util.UtilityClass;
import com.softpath.riverpath.util.ValidatedField;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CircleBoundaryController
 *
 * @author rhajou
 */
@NoArgsConstructor
public class RectangleBoundaryController extends BaseBoundaryController {

    @FXML
    @ValidatedField(isDouble = true)
    @Getter
    private TextField rectangleWidth;
    @FXML
    @ValidatedField(isDouble = true)
    @Getter
    private TextField rectangleHeight;

    /**
     * Import values of the rectangle boundary
     *
     * @param boundary the rectangle boundary
     */
    @Override
    void importValues(Boundary boundary) {
        if (boundary instanceof CubeBoundary cubeBoundary) {
            rectangleWidth.setText(cubeBoundary.getWidth());
            UtilityClass.unflagTextFieldWarning(rectangleWidth);
            rectangleHeight.setText(cubeBoundary.getHeight());
            UtilityClass.unflagTextFieldWarning(rectangleHeight);
        }
    }

    /**
     * @see BaseBoundaryController#getShape(DomainProperties, Coordinates)
     */
    @Override
    Shape getShape(DomainProperties domainProperties, Coordinates origin) {
        Rectangle square = new Rectangle();
        double scaleFactor = domainProperties.getScaleFactor();
        square.setX(Double.parseDouble(origin.getX()) * scaleFactor);
        // ⚠️JAVAFX_INVERTED_AXIS_Y
        square.setY(Double.parseDouble(origin.getY()) * -scaleFactor);
        square.setWidth(Double.parseDouble(getInitialValue(rectangleWidth)) * scaleFactor);
        square.setHeight(Double.parseDouble(getInitialValue(rectangleHeight)) * scaleFactor);
        return square;
    }

}
