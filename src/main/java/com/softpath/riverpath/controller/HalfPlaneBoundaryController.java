package com.softpath.riverpath.controller;

import com.softpath.riverpath.model.Boundary;
import com.softpath.riverpath.model.Coordinates;
import com.softpath.riverpath.model.HalfPlaneBoundary;
import com.softpath.riverpath.util.DomainProperties;
import com.softpath.riverpath.util.ValidatedField;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * HalfPlaneBoundaryController
 * Controller for handling half plane boundaries (straight lines)
 */
@NoArgsConstructor
public class HalfPlaneBoundaryController extends BaseBoundaryController {

    @FXML
    private Label normalLabel;
    @FXML
    @ValidatedField(isDouble = true)
    @Getter
    private TextField normalX;
    @FXML
    @ValidatedField(isDouble = true)
    @Getter
    private TextField normalY;

    /**
     * Initialize the controller and set up dirty state listeners
     */
    @FXML
    public void initialize() {
        super.initialize();
        // add listeners to track changes and set dirty state
        setupChangeListeners();
    }

    /**
     * Set up change listeners for the form fields
     */

    public void setupChangeListeners() {
        normalX.textProperty().addListener((observable, oldValue, newValue) -> {
            // Mark as dirty when field changes
            setDirty(true);
        });

        normalY.textProperty().addListener((observable, oldValue, newValue) -> {
            // Mark as dirty when field changes
            setDirty(true);
        });
    }

    /**
     * @see BaseBoundaryController#importValues(Boundary)
     */
    @Override
    void importValues(Boundary boundary) {
        if (boundary instanceof HalfPlaneBoundary halfPlaneBoundary) {
            normalX.setText(halfPlaneBoundary.getNormal().getX());
            normalY.setText(halfPlaneBoundary.getNormal().getY());
        }
    }

    /**
     * @see BaseBoundaryController#getShape(DomainProperties, Coordinates)
     */
    @Override
    public <T extends Node> T getShape(DomainProperties domainProperties, Coordinates origin) {
        double normalX = Double.parseDouble(getInitialValue(this.normalX));
        double normalY = Double.parseDouble(getInitialValue(this.normalY));
        double length = Math.sqrt(normalX * normalX + normalY * normalY);
        normalX /= length;
        normalY /= length;

        // Calculate the perpendicular vector that will be the direction of the line
        double dirX = -normalY;
        double dirY = normalX;
        double scaleFactor = domainProperties.getScaleFactor();

        // User-specified origin point
        double originX = Double.parseDouble(origin.getX()) * scaleFactor;
        double originY = Double.parseDouble(origin.getY()) * scaleFactor;

        // Determine the size of the domain
        double domainWidth = domainProperties.getDomainMaxX() - domainProperties.getDomainMinX();
        double domainHeight = domainProperties.getDomainMaxY() - domainProperties.getDomainMinY();

        // Calculate the length of line required to exceed the domain
        double domainDiagonal = Math.sqrt(domainWidth * domainWidth + domainHeight * domainHeight);
        double lineLength = domainDiagonal * 0.6;

        // Calculate the endpoints of the line
        double startX = originX - dirX * lineLength;
        double startY = originY - dirY * lineLength;
        double endX = originX + dirX * lineLength;
        double endY = originY + dirY * lineLength;

        // Create the ‘infinite’ line
        // ⚠️JAVAFX_INVERTED_AXIS_Y
        Line line = new Line(startX, -startY, endX, -endY);
        line.setStroke(javafx.scene.paint.Color.RED);
        line.setStrokeWidth(2);

        return (T) line;
    }

    /**
     * Get the normal of the half plane boundary
     *
     * @param origin the origin point
     * @return the group representing the normal
     */
    public Group getPlanNormal(Coordinates origin) {
        // Addition for half-planes: create an arrow to represent the normal
        double normalX = Double.parseDouble(getInitialValue(this.normalX));
        double normalY = Double.parseDouble(getInitialValue(this.normalY));

        // Normalise the normal vector
        double magnitude = Math.sqrt(normalX * normalX + normalY * normalY);
        normalX /= magnitude;
        normalY /= magnitude;

        double scaleFactor = DomainProperties.getInstance().getScaleFactor();
        double originX = Double.parseDouble(origin.getX()) * scaleFactor;
        double originY = -Double.parseDouble(origin.getY()) * scaleFactor; // Inversion de Y comme dans getShape

        // Length of the arrow (adjust as needed)
        double arrowLength = 30.0;

        // Create an arrow to represent the normal
        return createArrow(
                originX,
                originY,
                originX + normalX * arrowLength,
                originY - normalY * arrowLength,  // Inversion de Y comme dans getShape
                Color.GREEN
        );
    }

    /**
     * Create an arrow shape
     *
     * @param startX origin X
     * @param startY origin Y
     * @param endX   end X
     * @param endY   end Y
     * @param color  color of the arrow
     * @return Group containing the arrow shape
     */
    private Group createArrow(double startX, double startY, double endX, double endY, Color color) {
        // Main line of the arrow
        Line mainLine = new Line(startX, startY, endX, endY);
        mainLine.setStroke(color);
        mainLine.setStrokeWidth(2);

        // Calculate the direction of the arrow
        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.sqrt(dx * dx + dy * dy);

        // Normalise the direction vector
        double unitDx = dx / length;
        double unitDy = dy / length;

        // Calculate the perpendicular vectors for the wings of the arrow
        double perpDx = -unitDy;
        double perpDy = unitDx;

        // Length of the arrow wings
        double arrowHeadSize = 10.0;

        // Create the wings of the arrow
        Line arrowHead1 = new Line(
                endX, endY,
                endX - arrowHeadSize * (unitDx + 0.5 * perpDx),
                endY - arrowHeadSize * (unitDy + 0.5 * perpDy)
        );
        arrowHead1.setStroke(color);
        arrowHead1.setStrokeWidth(2);

        Line arrowHead2 = new Line(
                endX, endY,
                endX - arrowHeadSize * (unitDx - 0.5 * perpDx),
                endY - arrowHeadSize * (unitDy - 0.5 * perpDy)
        );
        arrowHead2.setStroke(color);
        arrowHead2.setStrokeWidth(2);

        // Group the elements of the arrow
        Group arrowGroup = new Group(mainLine, arrowHead1, arrowHead2);
        return arrowGroup;
    }

}
