package com.softpath.riverpath.fileparser;

import com.softpath.riverpath.util.ProgressReporter;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

import static com.softpath.riverpath.util.UtilityClass.buildMessage;

/**
 * @author rhajou
 */
public class MeshResolution {

    private static final int MAX_REDUCED_TETRA = 250000;
    @Getter
    private final CFDTriangleMesh triangleMesh;
    private final CFDTriangleMesh triangleMeshReduced;
    @Getter
    private final CFDTriangleMesh triangleSurface;
    private boolean isReduced;
    private double reductionFactor;
    private final AtomicInteger currentNbTriangles;
    @Getter
    @Setter
    private Color color;
    @Getter
    @Setter
    private Point3D position = new Point3D(0, 0, 0);

    public MeshResolution(int nbTetra) {
        this.triangleMesh = new CFDTriangleMesh();
        this.triangleMeshReduced = new CFDTriangleMesh();
        this.triangleSurface = new CFDTriangleMesh();
        currentNbTriangles = new AtomicInteger(0);
        if (nbTetra > MAX_REDUCED_TETRA) {
            ProgressReporter.report(buildMessage("Warning: The mesh contains %d tetrahedrons which is more " +
                            "than the maximum allowed %d. The mesh will be reduced for performance reasons.",
                    nbTetra, MAX_REDUCED_TETRA));
            reductionFactor = (int) Math.ceil((double) nbTetra / MAX_REDUCED_TETRA);
            ProgressReporter.report("The mesh will be reduced by a factor of " + reductionFactor);
            isReduced = true;
        }
    }

    public CFDTriangleMesh getReducedMesh() {
        if (isReduced) {
            return triangleMeshReduced;
        } else {
            return triangleMesh;
        }
    }

    public void addPoint(Point3D point) {
        triangleMesh.addPoint(point);
        triangleSurface.addPoint(point);
        if (isReduced) {
            triangleMeshReduced.addPoint(point);
        }
    }

    public void add2DTriangle(int vertex1, int vertex2, int vertex3) {
        if (vertex3 == -1) {
            triangleSurface.addTriangle(vertex1, vertex2, vertex1);
            triangleMesh.addTriangle(vertex1, vertex2, vertex1);
            if (isReduced) {
                triangleMeshReduced.addTriangle(vertex1, vertex2, vertex1);
            }
        } else {
            triangleMesh.addTriangle(vertex1, vertex2, vertex3);
        }
    }

    public void addTriangleSurface(int vertex1, int vertex2, int vertex3) {
        triangleMesh.addTriangle(vertex1, vertex2, vertex3);
        triangleSurface.addTriangle(vertex1, vertex2, vertex3);
        if (isReduced) {
            triangleMeshReduced.addTriangle(vertex1, vertex2, vertex3);
        }
    }

    public void addTetraFaces(int vertex1, int vertex2, int vertex3, int vertex4) {
        // Face 1
        triangleMesh.addTriangle(vertex1, vertex2, vertex3);
        // Face 2
        triangleMesh.addTriangle(vertex1, vertex2, vertex4);
        // Face 3
        triangleMesh.addTriangle(vertex2, vertex3, vertex4);
        // Face 4
        triangleMesh.addTriangle(vertex1, vertex3, vertex4);
        if (isReduced) {
            int currentTetra = currentNbTriangles.getAndIncrement();
            if (currentTetra % reductionFactor == 0) {
                // Face 1
                triangleMeshReduced.addTriangle(vertex1, vertex2, vertex3);
                // Face 2
                triangleMeshReduced.addTriangle(vertex1, vertex2, vertex4);
                // Face 3
                triangleMeshReduced.addTriangle(vertex2, vertex3, vertex4);
                // Face 4
                triangleMeshReduced.addTriangle(vertex1, vertex3, vertex4);
            }
        }
    }

    public void addTexCoords() {
        triangleMesh.getTexCoords().addAll(1, 1);
        triangleMeshReduced.getTexCoords().addAll(1, 1);
        triangleSurface.getTexCoords().addAll(1, 1);
    }

}
