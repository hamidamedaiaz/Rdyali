package com.softpath.riverpath.model;

import lombok.Data;

// Boundary for Immersed
@Data
public class ImmersedBoundary extends Boundary {
    private String immersedObjectFileName;

    // Constructor by default
    public ImmersedBoundary() {
        this.type = ShapeType.Immersed;
    }
}