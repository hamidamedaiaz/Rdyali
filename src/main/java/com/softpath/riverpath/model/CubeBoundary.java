package com.softpath.riverpath.model;

import lombok.Data;

// Boundary for Cube
@Data
public class CubeBoundary extends Boundary {
    private String width;
    private String height;
    private String depth;

    // Constructor by default
    public CubeBoundary() {
        this.type = ShapeType.Cube;
    }
}