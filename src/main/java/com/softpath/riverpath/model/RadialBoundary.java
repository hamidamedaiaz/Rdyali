package com.softpath.riverpath.model;

import lombok.Data;

// Boundary for Circle and Sphere (have the same properties)
@Data
public class RadialBoundary extends Boundary {
    private String radius;

    // Constructor with no parameters required by Jackson
    public RadialBoundary() {
        this.type = ShapeType.Circle; // Par défaut
    }

}