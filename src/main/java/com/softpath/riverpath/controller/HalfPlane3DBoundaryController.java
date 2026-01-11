package com.softpath.riverpath.controller;

import com.softpath.riverpath.model.Boundary;
import com.softpath.riverpath.model.Coordinates;
import com.softpath.riverpath.model.HalfPlaneBoundary;
import com.softpath.riverpath.util.DomainProperties;
import com.softpath.riverpath.util.ValidatedField;
import javafx.fxml.FXML;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.control.TextField;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

/**
 * HalfPlane3DBoundaryController
 *
 * @author rahajou
 */
@Slf4j
@NoArgsConstructor
public class HalfPlane3DBoundaryController extends HalfPlaneBoundaryController {
    @FXML
    @ValidatedField(isDouble = true)
    @Getter
    private TextField normalZ;

    @FXML

    /**
     * @see BaseBoundaryController#importValues(Boundary)
     */
    @Override
    void importValues(Boundary boundary) {
        super.importValues(boundary);
        if (boundary instanceof HalfPlaneBoundary halfPlaneBoundary) {
            normalZ.setText(halfPlaneBoundary.getNormal().getZ());
        }
    }

    @Override
    public void setupChangeListeners() {
        super.setupChangeListeners();
        normalZ.textProperty().addListener((observable, oldValue, newValue) -> {
            setDirty(true);
        });
    }

    /**
     * @see BaseBoundaryController#getShape(DomainProperties, Coordinates)
     */
    @Override
    public MeshView getShape(DomainProperties domainProperties, Coordinates origin) {
        Point3D normal, originP;
        try {
            double nx = Double.parseDouble(super.getNormalX().getText());
            double ny = Double.parseDouble(super.getNormalY().getText());
            double nz = Double.parseDouble(normalZ.getText());

            double ox = Double.parseDouble(origin.getX());
            double oy = Double.parseDouble(origin.getY());
            double oz = Double.parseDouble(origin.getZ());
            originP = new Point3D(ox, oy, oz);
            normal = new Point3D(nx, ny, nz);
        }
        catch (NumberFormatException e) {
            log.error(e.getMessage());
            throw e;
        }
        // Ensure normal is valid
        Point3D n = normal.normalize();
        if (n.magnitude() < 1e-6)
            throw new IllegalArgumentException("Normal vector must not be zero.");

        // Choose a helper vector not parallel to n
        Point3D helper = Math.abs(n.getX()) < 0.9
                ? new Point3D(1, 0, 0)
                : new Point3D(0, 1, 0);

        // Construct orthonormal basis (u and v lie in the plane)
        Point3D u = n.crossProduct(helper).normalize();
        Point3D v = n.crossProduct(u).normalize();

        double w = 1000;
        double h = 1000;

        // Compute the 4 corner points of the plane
        Point3D p1 = originP.add( u.multiply(+w/2) ).add( v.multiply(+h/2) );
        Point3D p2 = originP.add( u.multiply(-w/2) ).add( v.multiply(+h/2) );
        Point3D p3 = originP.add( u.multiply(-w/2) ).add( v.multiply(-h/2) );
        Point3D p4 = originP.add( u.multiply(+w/2) ).add( v.multiply(-h/2) );

        // Build TriangleMesh
        TriangleMesh mesh = new TriangleMesh();

        mesh.getPoints().addAll(
                (float)p1.getX(), (float)p1.getY(), (float)p1.getZ(),
                (float)p2.getX(), (float)p2.getY(), (float)p2.getZ(),
                (float)p3.getX(), (float)p3.getY(), (float)p3.getZ(),
                (float)p4.getX(), (float)p4.getY(), (float)p4.getZ()
        );

        // Dummy texture coordinates
        mesh.getTexCoords().addAll(0, 0);

        // Two triangles forming the quad
        mesh.getFaces().addAll(
                0,0, 1,0, 2,0,
                0,0, 2,0, 3,0
        );

        MeshView meshView = new MeshView(mesh);
        meshView.setCullFace(CullFace.NONE); // visible from both sides

        return meshView;
    }

}
