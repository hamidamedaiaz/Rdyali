package com.softpath.riverpath.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.softpath.riverpath.controller.BoundaryConditionController;
import com.softpath.riverpath.controller.BoundaryDefinitionController;
import com.softpath.riverpath.controller.DataEngineeringController;
import com.softpath.riverpath.controller.MeshingParametersController;
import com.softpath.riverpath.controller.TimeDiscretizationController;
import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventEnum;
import com.softpath.riverpath.custom.event.EventManager;
import com.softpath.riverpath.model.Boundary;
import com.softpath.riverpath.model.BoundaryCondition;
import com.softpath.riverpath.model.Coordinates;
import com.softpath.riverpath.model.CubeBoundary;
import com.softpath.riverpath.model.HalfPlaneBoundary;
import com.softpath.riverpath.model.ImmersedBoundary;
import com.softpath.riverpath.model.RadialBoundary;
import com.softpath.riverpath.model.ShapeType;
import com.softpath.riverpath.model.Simulation;
import com.softpath.riverpath.util.DomainProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

import static com.softpath.riverpath.util.UtilityClass.workspaceDirectory;

@Slf4j
public class SimulationStateService {

    private static SimulationStateService instance;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Simulation initSimulation;
    private Simulation runtimeSimulation;

    private SimulationStateService() {
        runtimeSimulation = new Simulation();
        initSimulation = new Simulation();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static SimulationStateService getInstance() {
        if (instance == null) {
            instance = new SimulationStateService();
        }
        return instance;
    }

    public void initialize() {

        /* ---------- NEW PROJECT ---------- */
        EventManager.addEventHandler(EventEnum.NEW_PROJECT, event -> {
            try {
                handleNewProject(event);
            } catch (Exception e) {
                log.error("Error initializing new project", e);
            }
        });

        /* ---------- IMPORT PROJECT ---------- */
        EventManager.addEventHandler(EventEnum.IMPORT_PROJECT, event -> {
            try {
                handleImportProject(event);
            } catch (Exception e) {
                log.error("Error importing project", e);
            }
        });

        /* ---------- PARAMETER VALIDATIONS ---------- */
        EventManager.addEventHandler(EventEnum.MESHING_PARAMETERS_VALID, event -> {
            try {
                MeshingParametersController ctr = (MeshingParametersController) event.getObject();
                updateMeshingParameters(ctr);
                log.info("Meshing parameters saved");
            } catch (Exception e) {
                log.error("Error updating meshing parameters", e);
            }
        });

        EventManager.addEventHandler(EventEnum.DATA_ENGINEERING_VALID, event -> {
            try {
                DataEngineeringController ctr = (DataEngineeringController) event.getObject();
                updateDataEngineeringParameters(ctr);
                log.info("Data engineering parameters saved");
            } catch (Exception e) {
                log.error("Error updating data engineering parameters", e);
            }
        });

        EventManager.addEventHandler(EventEnum.TIME_DISCRETIZATION_VALID, event -> {
            try {
                TimeDiscretizationController ctr = (TimeDiscretizationController) event.getObject();
                updateTimeDiscretizationParameters(ctr);
                log.info("Time discretization parameters saved");
            } catch (Exception e) {
                log.error("Error updating time discretization parameters", e);
            }
        });

        /* ---------- BOUNDARY OPERATIONS ---------- */
        EventManager.addEventHandler(EventEnum.BOUNDARY_REMOVED, event -> {
            try {
                BoundaryDefinitionController ctr = (BoundaryDefinitionController) event.getObject();
                Boundary boundary = runtimeSimulation.getBoundarybyName(ctr.getLabel());
                if (boundary != null) {
                    runtimeSimulation.getBoundaries().remove(boundary);
                    log.info("Boundary '{}' removed", ctr.getLabel());
                } else {
                    log.warn("Tried to remove non-existing boundary '{}'", ctr.getLabel());
                }
            } catch (Exception e) {
                log.error("Error removing boundary", e);
            }
        });

        EventManager.addEventHandler(EventEnum.BOUNDARY_NAME_CHANGED, event -> {
            try {
                if (event.getMessage() == null || event.getMessage().isEmpty()) {
                    // Triggered before validation
                    return;
                }
                Boundary target = runtimeSimulation.getBoundarybyName(event.getMessage());
                if (target != null) {
                    target.setName(event.getNewValue());
                    if (target.getCondition() != null) {
                        target.getCondition().setName(event.getNewValue());
                    }
                    log.info("Boundary renamed from '{}' to '{}'", event.getMessage(), event.getNewValue());
                } else {
                    log.warn("Boundary '{}' not found for renaming", event.getMessage());
                }
            } catch (Exception e) {
                log.error("Error renaming boundary", e);
            }
        });

        EventManager.addEventHandler(EventEnum.BOUNDARY_VALIDATED, event -> {
            try {
                BoundaryDefinitionController ctr = (BoundaryDefinitionController) event.getObject();
                Boundary boundary = initiateBoundaryType(ctr);

                Boundary existing = runtimeSimulation.getBoundarybyName(ctr.getLabel());
                if (existing != null) {
                    boundary.setCondition(existing.getCondition());
                    runtimeSimulation.getBoundaries().remove(existing);
                }
                runtimeSimulation.getBoundaries().add(boundary);
                log.info("Boundary '{}' validated and updated", ctr.getLabel());
            } catch (Exception e) {
                log.error("Error validating boundary", e);
            }
        });

        EventManager.addEventHandler(EventEnum.NEW_CONDITION_VALIDATED, event -> {
            try {
                BoundaryConditionController ctr = (BoundaryConditionController) event.getObject();
                if (runtimeSimulation.getBoundarybyName(event.getMessage()) != null) {
                    addBoundaryCondition(ctr, event.getMessage());
                    log.info("Boundary condition added to '{}'", event.getMessage());
                } else {
                    log.warn("Tried to add condition to non-existent boundary '{}'", event.getMessage());
                }
            } catch (Exception e) {
                log.error("Error adding boundary condition", e);
            }
        });

        /* ---------- SIMULATION STATE ---------- */
        EventManager.addEventHandler(EventEnum.NEW_RUN_FIRED, event -> {
            initSimulation = deepCopy(runtimeSimulation);
            log.info("New run fired: simulation state snapshot taken");
        });

        EventManager.addEventHandler(EventEnum.SAVED_STATE, event -> {
            try {
                initSimulation = deepCopy(runtimeSimulation);
                saveSimulationToFile(runtimeSimulation);
                log.info("Simulation state saved to simulation.json");
            } catch (Exception e) {
                log.error("Error saving simulation state", e);
            }
        });
    }

    private void handleNewProject(CustomEvent event) {
        String fileName = event.getMessage();

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Missing mesh file name");
        }
        if (!fileName.endsWith(".t")) {
            throw new IllegalArgumentException("Invalid mesh file extension: " + fileName);
        }

        runtimeSimulation.setDomainMeshFile(fileName);
        log.info("New project created with mesh file '{}'", fileName);
    }

    private void handleImportProject(CustomEvent event) throws IOException {
        File projectDirectory = (File) event.getObject();
        File simulationJsonFile = new File(projectDirectory, "simulation.json");

        if (!simulationJsonFile.exists()) {
            throw new RuntimeException("Missing simulation.json in " + projectDirectory);
        }

        Simulation simulation = objectMapper.readValue(simulationJsonFile, Simulation.class);
        runtimeSimulation = simulation;
        initSimulation = deepCopy(simulation);
        log.info("Imported project from {}", simulationJsonFile.getAbsolutePath());
    }

    private void updateMeshingParameters(MeshingParametersController ctr) {
        runtimeSimulation.setAdaptateur(ctr.getAdaptateur().getText());
        runtimeSimulation.setErr2(ctr.getErr2().getText());
        runtimeSimulation.setErr1(ctr.getErr1().getText());
        runtimeSimulation.setHMin(ctr.getHMin().getText());
        runtimeSimulation.setLMin(ctr.getLMin().getText());
        runtimeSimulation.setLMax(ctr.getLMax().getText());
        runtimeSimulation.setNbElements(ctr.getNbElements().getText());
        runtimeSimulation.setNScaling(ctr.getNScaling().getText());
        runtimeSimulation.setScaleNorme(ctr.getScaleNorme().getText());
    }

    private void updateDataEngineeringParameters(DataEngineeringController ctr) {
        runtimeSimulation.setDensity(ctr.getDensity().getText());
        runtimeSimulation.setViscosity(ctr.getViscosity().getText());
    }

    private void updateTimeDiscretizationParameters(TimeDiscretizationController ctr) {
        runtimeSimulation.setTimeStep(ctr.getTimeStep().getText());
        runtimeSimulation.setTotalTime(ctr.getTotalTime().getText());
        runtimeSimulation.setFrequency(ctr.getStorageFrequency().getText());
    }

    private void saveSimulationToFile(Simulation simulation) throws IOException {
        File output = new File(workspaceDirectory, "simulation.json");
        objectMapper.writeValue(output, simulation);
    }

    private Simulation deepCopy(Simulation simulation) {
        try {
            String json = objectMapper.writeValueAsString(simulation);
            return objectMapper.readValue(json, Simulation.class);
        } catch (IOException e) {
            log.error("Failed to deep copy simulation", e);
            return simulation; // fallback to reference
        }
    }


    public Simulation getCurrentSimulation() {
        return this.runtimeSimulation;
    }

    private static Boundary initiateBoundaryType(BoundaryDefinitionController controller) {
        Boundary boundary;
        ShapeType type = controller.getComboBoxInitialValue();
        // initialize the boundary based on the type
        switch (type) {
            case Half_Plane:
                // Set normal vector
                Coordinates normal = new Coordinates();
                normal.setX(controller.getHalfPlaneBoundaryController().getNormalX().getText());
                normal.setY(controller.getHalfPlaneBoundaryController().getNormalY().getText());
                HalfPlaneBoundary halfPlaneBoundary = new HalfPlaneBoundary();
                halfPlaneBoundary.setNormal(normal);
                boundary = halfPlaneBoundary;
                boundary.setType(type);
                break;
            case Circle:
            case Sphere:
                RadialBoundary radialBoundary = new RadialBoundary();
                radialBoundary.setRadius(controller.getCircleBoundaryController().getRadius().getText());
                boundary = radialBoundary;
                boundary.setType(type);
                break;
            case Cube:
                CubeBoundary cubeBoundary = new CubeBoundary();
                cubeBoundary.setWidth(controller.getRectangleBoundaryController().getRectangleWidth().getText());
                cubeBoundary.setHeight(controller.getRectangleBoundaryController().getRectangleHeight().getText());
                cubeBoundary.setDepth("0"); // For a 2D cube
                boundary = cubeBoundary;
                boundary.setType(type);
                break;
            case Immersed:
                ImmersedBoundary immersedBoundary = new ImmersedBoundary();
                immersedBoundary.setImmersedObjectFileName(controller.getImmersedBoundaryController().getMeshFileName());
                boundary = immersedBoundary;
                boundary.setType(type);
                break;
            default:
                throw new IllegalStateException("Boundary type is not supported " + type);
        }
        boundary.setName(controller.getLabel());
        Coordinates coordinates = new Coordinates();
        coordinates.setX(controller.getOriginX().getText());
        coordinates.setY(controller.getOriginY().getText());
        if (DomainProperties.getInstance().is3D()) coordinates.setZ(controller.getOriginZ().getText());
        boundary.setOrigin(coordinates);
        BoundaryCondition boundaryCondition = new BoundaryCondition();
        boundaryCondition.setName(controller.getLabel());
        boundary.setCondition(boundaryCondition);
        return boundary;
    }

    private void addBoundaryCondition(BoundaryConditionController controller, String boundaryName) {
        BoundaryCondition boundaryCondition = new BoundaryCondition();
        // Set name
        boundaryCondition.setName(controller.getLabel());
        // Initialize velocity coordinates
        Coordinates velocity = new Coordinates();
        velocity.setX(controller.getVxValue().getText());
        velocity.setY(controller.getVyValue().getText());
        velocity.setZ(controller.getVzValue().getText());
        boundaryCondition.setVelocity(velocity);
        // Set other properties
        boundaryCondition.setPressure(controller.getPressureValue().getText());
        boundaryCondition.setPriority(controller.getPriorityValue().getText());
        // Add the condition to the boundary
        runtimeSimulation.getBoundarybyName(boundaryName).setCondition(boundaryCondition);
    }

    public boolean isDifferentFromStart() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode nodeInit = mapper.valueToTree(initSimulation);
            JsonNode nodeCurrent = mapper.valueToTree(runtimeSimulation);

            // This compares structure and values — ignoring field order
            return !nodeInit.equals(nodeCurrent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compare simulations", e);
        }
    }
}
