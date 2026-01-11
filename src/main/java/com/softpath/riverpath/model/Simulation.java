package com.softpath.riverpath.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Json model for simulation
 *
 * @author rhajou
 */
@Data
@NoArgsConstructor
public class Simulation {

    // Domain mesh
    private String domainMeshFile;
    private List<Boundary> boundaries = new ArrayList<>();
    private String viscosity;
    private String density;
    private String timeStep;
    private String totalTime;
    private String frequency;
    //meshing parameters
    private String nbElements;
    private String hMin;
    private String lMax;
    private String nScaling;
    private String scaleNorme;
    private String err1;
    private String err2;
    private String adaptateur;
    private String lMin;

    /**
     * Get boundary by name
     *
     * @param name Boundary name
     * @return Boundary by name
     */
    public Boundary getBoundarybyName(String name) {
        return boundaries.stream().filter(boundary -> boundary.getName().equals(name)).findAny().orElse(null);
    }
}
