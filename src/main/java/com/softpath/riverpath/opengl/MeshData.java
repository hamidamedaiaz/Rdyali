package com.softpath.riverpath.opengl;

/**
 * Simple container for OpenGL mesh data
 */
public record MeshData(float[] vertices, float[] normals, int[] indices) {

    public int getVertexCount() {
        return vertices.length / 3;
    }

    public int getTriangleCount() {
        return indices.length / 3;
    }
}
