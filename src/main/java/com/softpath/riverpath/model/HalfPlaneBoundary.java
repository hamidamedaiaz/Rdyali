package com.softpath.riverpath.model;

import lombok.Data;

// Boundary for Half_Plane
@Data
public class HalfPlaneBoundary extends Boundary {
    private Coordinates normal;

    // Constructor by default
    public HalfPlaneBoundary() {
        this.type = ShapeType.Half_Plane;
    }
}