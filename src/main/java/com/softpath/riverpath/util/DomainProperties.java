package com.softpath.riverpath.util;

import com.softpath.riverpath.custom.pane.ZoomableScrollPane;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import lombok.Getter;
import lombok.Setter;

/**
 * This class is used to store the properties of the domain. (size, scaleFactor, etc.)
 *
 * @author rahajou
 */
@Getter
public class DomainProperties {
    private double scaleFactor;
    // Ajout des variables pour stocker les limites du domaine
    private double domainMinX, domainMaxX, domainMinY, domainMaxY;
    @Setter
    private int dimension = -1;
    private static DomainProperties instance;

    public static DomainProperties getInstance() {
        if (instance == null) {
            instance = new DomainProperties();
        }
        return instance;
    }

    public boolean is3D() {
        return dimension == 3;
    }

    public boolean isDimensionInitiliazed() {
        return dimension != -1;
    }

    /**
     * Compute the domain scale and limits.
     *
     * @param meshPane the mesh pane
     * @param meshView the mesh view
     */
    public void computeDomainProperties(ZoomableScrollPane meshPane, MeshView meshView) {
        // compute scale factor
        calculateScaleFactor(meshPane, meshView);
        float[] points = ((TriangleMesh) meshView.getMesh()).getPoints().toArray(new float[0]);
        domainMinX = Float.MAX_VALUE;
        domainMaxX = -Float.MAX_VALUE;
        domainMinY = Float.MAX_VALUE;
        domainMaxY = -Float.MAX_VALUE;
        for (int i = 0; i < points.length; i += 3) {
            domainMinX = Math.min(domainMinX, points[i] * scaleFactor);
            domainMaxX = Math.max(domainMaxX, points[i] * scaleFactor);
            // ⚠️JAVAFX_INVERTED_AXIS_Y
            domainMinY = Math.min(domainMinY, -points[i + 1] * scaleFactor);
            domainMaxY = Math.max(domainMaxY, -points[i + 1] * scaleFactor);
        }
    }

    /**
     * Calculate the scale factor.
     *
     * @param meshPane the mesh pane
     * @param meshView the mesh view
     */
    private void calculateScaleFactor(ZoomableScrollPane meshPane, MeshView meshView) {
        double scaleX = meshPane.getWidth() / meshView.getBoundsInLocal().getWidth();
        double scaleY = meshPane.getHeight() / meshView.getBoundsInLocal().getHeight();
        scaleFactor = Math.min(scaleX, scaleY);
    }

}
