package com.softpath.riverpath.model;

import lombok.Getter;

/**
 * MurType
 *
 * @author rhajou
 */
public enum ShapeType {
    Half_Plane("DemiPlan"),
    Sphere("Boule"),
    Immersed("Immersed"),
    Cube("Brique"),
    Circle("Boule");

    @Getter
    private final String cimLibName;

    ShapeType(String cimLibName) {
        this.cimLibName = cimLibName;
    }

    public static ShapeType fromString(String murTypeStr) {
        for (ShapeType ShapeType : ShapeType.values()) {
            if (ShapeType.cimLibName.equalsIgnoreCase(murTypeStr)) {
                return ShapeType;
            }
        }
        throw new IllegalArgumentException("No enum constant with name " + murTypeStr);
    }
}
