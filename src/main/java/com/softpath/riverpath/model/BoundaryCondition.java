package com.softpath.riverpath.model;

import lombok.Data;

/**
 * Boundary condition model
 *
 * @author rhajou
 */
@Data
public class BoundaryCondition {

    private String name;
    private Coordinates velocity;
    private String pressure;
    private String priority;

}
