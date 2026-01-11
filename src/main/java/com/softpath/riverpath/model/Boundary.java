package com.softpath.riverpath.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

// Abstract base class for all boundaries
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HalfPlaneBoundary.class, name = "Half_Plane"),
        @JsonSubTypes.Type(value = RadialBoundary.class, name = "Circle"),
        @JsonSubTypes.Type(value = RadialBoundary.class, name = "Sphere"),
        @JsonSubTypes.Type(value = CubeBoundary.class, name = "Cube"),
        @JsonSubTypes.Type(value = ImmersedBoundary.class, name = "Immersed")
})
@Data
public abstract class Boundary {
    protected String name;
    protected Coordinates origin;
    protected ShapeType type;
    protected BoundaryCondition condition;
}
