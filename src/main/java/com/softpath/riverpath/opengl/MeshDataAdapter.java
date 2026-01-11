package com.softpath.riverpath.opengl;

import com.softpath.riverpath.fileparser.CFDTriangleMesh;
import javafx.collections.ObservableFloatArray;
import javafx.scene.shape.ObservableFaceArray;

/**
 * Converts CFDTriangleMesh to OpenGL-compatible format
 */
public class MeshDataAdapter {

    /**
     * Convert CFDTriangleMesh to OpenGL MeshData
     * Renders the full mesh (triangles)
     */
    public static MeshData fromCFDMesh(CFDTriangleMesh mesh) {
        // Get vertices directly from TriangleMesh points
        ObservableFloatArray points = mesh.getPoints();
        float[] vertices = new float[points.size()];
        points.toArray(vertices);

        // Get faces directly from TriangleMesh faces
        // Format: v1, t1, v2, t2, v3, t3 (every 2nd value is texture index)
        ObservableFaceArray faces = mesh.getFaces();
        int[] indices = new int[faces.size() / 2]; // Half size (skip texture indices)

        int j = 0;
        for (int i = 0; i < faces.size(); i += 2) {
            indices[j++] = faces.get(i); // Skip texture index at i+1
        }

        // Compute normals
        float[] normals = computeNormals(vertices, indices);

        return new MeshData(vertices, normals, indices);
    }

    private static float[] computeNormals(float[] vertices, int[] indices) {
        int numVertices = vertices.length / 3;
        float[] normals = new float[numVertices * 3];

        for (int i = 0; i < indices.length; i += 3) {
            int i0 = indices[i], i1 = indices[i + 1], i2 = indices[i + 2];

            float x0 = vertices[i0 * 3], y0 = vertices[i0 * 3 + 1], z0 = vertices[i0 * 3 + 2];
            float x1 = vertices[i1 * 3], y1 = vertices[i1 * 3 + 1], z1 = vertices[i1 * 3 + 2];
            float x2 = vertices[i2 * 3], y2 = vertices[i2 * 3 + 1], z2 = vertices[i2 * 3 + 2];

            float ux = x1 - x0, uy = y1 - y0, uz = z1 - z0;
            float vx = x2 - x0, vy = y2 - y0, vz = z2 - z0;

            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;

            normals[i0 * 3] += nx;
            normals[i0 * 3 + 1] += ny;
            normals[i0 * 3 + 2] += nz;
            normals[i1 * 3] += nx;
            normals[i1 * 3 + 1] += ny;
            normals[i1 * 3 + 2] += nz;
            normals[i2 * 3] += nx;
            normals[i2 * 3 + 1] += ny;
            normals[i2 * 3 + 2] += nz;
        }

        // Normalize
        for (int i = 0; i < numVertices; i++) {
            float nx = normals[i * 3], ny = normals[i * 3 + 1], nz = normals[i * 3 + 2];
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0) {
                normals[i * 3] /= len;
                normals[i * 3 + 1] /= len;
                normals[i * 3 + 2] /= len;
            }
        }

        return normals;
    }
}