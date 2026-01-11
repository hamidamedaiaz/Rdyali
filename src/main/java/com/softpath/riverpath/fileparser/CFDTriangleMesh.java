package com.softpath.riverpath.fileparser;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Class to store metadata of a mesh
 *
 * @author rhajou
 */
public class CFDTriangleMesh extends TriangleMesh {

    /**
     * Add a point to the mesh
     * Add the point to the vertex map
     *
     * @param point    the point coordinates to add
     */
    public void addPoint(Point3D point) {
        getPoints().addAll((float) point.getX(), (float) point.getY(), (float) point.getZ());
        //vertexMap.put(vertexId, new Point3D(point.getX(), point.getY(), point.getZ()));
    }

    /**
     * Add a triangle / face to the mesh based on 3 vertices
     *
     * @param vertex1 first vertex of the triangle
     * @param vertex2 second vertex of the triangle
     * @param vertex3 third vertex of the triangle
     */
    public void addTriangle(int vertex1, int vertex2, int vertex3) {
        getFaces().addAll(vertex1, 0, vertex2, 0, vertex3, 0);
        //triangles.add(new MyTriangle(vertex1, vertex2, vertex3));
    }

    /**
     * Define a triangle object with 3 vertices
     *
     * @author rhajou
     */
    @Getter
    @AllArgsConstructor
    public static class MyTriangle {
        private final int vertex1;
        private final int vertex2;
        private final int vertex3;
    }

}