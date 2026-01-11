package com.softpath.riverpath.controller;

import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventEnum;
import com.softpath.riverpath.custom.event.EventManager;
import com.softpath.riverpath.custom.pane.BoundaryTitledPane;
import com.softpath.riverpath.model.Boundary;
import com.softpath.riverpath.model.ShapeType;
import com.softpath.riverpath.util.DomainProperties;
import com.softpath.riverpath.util.UtilityClass;
import com.softpath.riverpath.util.ValidatedField;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static com.softpath.riverpath.custom.event.EventEnum.INVALID_CONDITION;

/**
 * Boundary Definition Controller
 *
 * @author rhajou
 */
@NoArgsConstructor
@Slf4j
public class BoundaryDefinitionController extends ValidAndCancelController implements Initializable {

    @FXML
    private VBox dynamicShapeContainer;
    private FXMLLoader fxmlLoader;
    @Getter
    private BaseBoundaryController baseBoundaryController;
    @FXML
    private Button removeButton;
    @FXML
    private Label titledName;
    @Setter
    private List<BoundaryTitledPane> boundaryTitledPanes;
    @FXML
    private BoundaryTitledPane titledPane;
    @FXML
    @Getter
    @ValidatedField(isUnique = true)
    private TextField nameValue;
    @Getter
    private String nameInitialValue = "";
    @FXML
    private HBox originHBox;
    @FXML
    @Getter
    @ValidatedField(isDouble = true)
    private TextField originX;
    @FXML
    @Getter
    @ValidatedField(isDouble = true)
    private TextField originY;
    @FXML
    @Getter
    @ValidatedField(is3D = true, isDouble = true)
    private TextField originZ;
    @FXML
    //@ValidatedField
    private ComboBox<ShapeType> comboBox;
    @Getter
    private ShapeType comboBoxInitialValue;
    @FXML
    private Pane validatePane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize();

        comboBox.getItems().addAll(ShapeType.Half_Plane, ShapeType.Immersed);
        // Add a listener to the ComboBox selection
        comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            handleShapeSelection(newValue);
            updateRootModifiedState();
        });

        Platform.runLater(() -> {
            if (!DomainProperties.getInstance().is3D()) {
                originHBox.getChildren().remove(originZ);
                // Define a default value for Z in 2D
                originZ.setText("");
            }
        });
    }

    @Override
    protected boolean customValidate() {
        if (fxmlLoader == null)
            return false; // this serves as a check on save of a boundary without a comboBox type which instanciate the baseboundary added to the definitionBoundary
        BaseBoundaryController currentBaseBoundaryController = fxmlLoader.getController();
        // check if the shape data is valid
        if (!currentBaseBoundaryController.checkValidCommit()) return false;
        // if new boundary definition then add it
        if (!boundaryTitledPanes.contains(titledPane)) {
            // in case of new boundary definition and condition not defined yet
            EventManager.fireCustomEvent(new CustomEvent(INVALID_CONDITION));
        }
        // commit changes
        commitValues(currentBaseBoundaryController);
        return true;
    }

    /**
     * Remove the boundary definition
     *
     * @param actionEvent the event
     */
    @FXML
    private void handleRemove(ActionEvent actionEvent) {
        // remove the boundary definition, but we need to check there's at least one another boundary definition
        if (boundaryTitledPanes.size() > 1) {
            boundaryTitledPanes.remove(titledPane);
            EventManager.fireCustomEvent(new CustomEvent(EventEnum.BOUNDARY_REMOVED, this));
        }
    }

    /**
     * Cancel the changes and reload initial values
     */
    @Override
    public void customReload() {
        comboBox.setValue(comboBoxInitialValue);
        // reload boundaries data (shape or immersed object)
        baseBoundaryController.reload();
        dynamicShapeContainer.getChildren().clear();
        dynamicShapeContainer.getChildren().add(baseBoundaryController.getShapeGridpane());
    }

    public void applyImportValues(Boundary boundary) {
        try {
            // Set the boundary data
            nameValue.setText(boundary.getName());
            // Set origin coordinates and check
            originX.setText(boundary.getOrigin().getX());
            originY.setText(boundary.getOrigin().getY());
            originZ.setText(boundary.getOrigin().getZ());
            // Set type and check
            comboBox.setValue(boundary.getType());
            // Process specific boundary types
            loadBoundaryTypeData(boundary);
            // isInvalidMandatoryField values
            handleValidate(null);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }

    /**
     * Import the shape data and commit
     *
     * @param boundary the boundary
     */
    private void loadBoundaryTypeData(Boundary boundary) {
        this.baseBoundaryController = fxmlLoader.getController();
        baseBoundaryController.getDirtyProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                updateRootModifiedState();
            }
        });
        baseBoundaryController.importValues(boundary);
    }

    @FXML
    private void handleKeyReleased(KeyEvent keyEvent) {
        UtilityClass.checkNotBlank((TextField) keyEvent.getSource());
        UtilityClass.handleTextWithDigitOnly(keyEvent);
    }

    @FXML
    private void checkNotBlank(KeyEvent keyEvent) {
        UtilityClass.checkNotBlank((TextField) keyEvent.getSource());
    }

    @Override
    protected boolean isUsedName() {
        String currentName = nameValue.getText();
        return boundaryTitledPanes.stream()
                .filter(e -> e != titledPane) // Exclure le titledPane courant de la vérification
                .map(e -> ((Label) ((HBox) e.getGraphic()).getChildren().get(0)).getText())
                .anyMatch(e -> e.equals(currentName));
    }

    /**
     * Handle the shape selection in combobox
     *
     * @param shapeValue the new value of type selection
     */
    private void handleShapeSelection(ShapeType shapeValue) { // can be left in the new version with annotation @ValidatedField
        // Use dedicated methods for each type
        switch (shapeValue) {
            case Circle:
                createCircleSelection();
                break;
            case Cube:
                createRectangleSelection();
                break;
            case Immersed:
                createImmersedObject();
                break;
            case Half_Plane:
                createHalfPlaneSelection();
                break;
        }
        // Show validate button
        validatePane.setManaged(true);
        validatePane.setVisible(true);
    }

    /**
     * Handle the half plane selection
     */
    private void createHalfPlaneSelection() {
        if (DomainProperties.getInstance().is3D()) {
            loadShapeFXML("half-plane-3d-boundary.fxml");
        } else {
            loadShapeFXML("half-plane-boundary.fxml");
        }
    }

    /**
     * Handle the immersed object
     */
    private void createImmersedObject() {
        loadShapeFXML("immersed-boundary.fxml");
    }

    /**
     * Handle the rectangle selection
     * Load the rectangle FXML file
     */
    private void createRectangleSelection() {
        loadShapeFXML("rectangle-boundary.fxml");
    }

    /**
     * Handle the circle selection
     * Load the circle FXML file
     */
    private void createCircleSelection() {
        loadShapeFXML("circle-boundary.fxml");
    }

    /**
     * Load the FXML file of shape
     *
     * @param name the name of the file
     */
    private void loadShapeFXML(String name) {
        fxmlLoader = new FXMLLoader(getClass().getResource(name));
        try {
            dynamicShapeContainer.getChildren().clear();
            dynamicShapeContainer.getChildren().add(fxmlLoader.load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Save the changes made by the user
     *
     * @param currentBaseBoundaryController the current boundary controller
     */
    private void commitValues(BaseBoundaryController currentBaseBoundaryController) {
        String newName = nameValue.getText();
        // If the name has changed and there was already an initial name
        if (!nameInitialValue.equals(newName)) {
            // update associated condition label
            EventManager.fireCustomEvent(new CustomEvent(EventEnum.BOUNDARY_NAME_CHANGED, nameInitialValue, newName));
            nameInitialValue = newName;
        }
        // Update title
        titledName.setText(newName);
        // commit other values...
        comboBoxInitialValue = comboBox.getValue();
        // very important
        // commit values for shapes and immersed object
        currentBaseBoundaryController.commit();
        baseBoundaryController = currentBaseBoundaryController;
        dynamicShapeContainer.getChildren().clear();
        dynamicShapeContainer.getChildren().add(baseBoundaryController.getShapeGridpane());
        // Show remove button
        // KAN-76 removeButton.setVisible(true);
    }

    @Override
    public boolean hasDirtyFields() {
        boolean isTopDirty = super.hasDirtyFields() || comboBoxInitialValue == null;
        boolean isNestedDirty = (baseBoundaryController != null && baseBoundaryController.hasDirtyFields()) || this.fxmlLoader == null || getCurrentBaseBoundaryController().hasDirtyFields(); // current and previous nested boundary elements

        return isTopDirty || isNestedDirty;
    }

    private BaseBoundaryController getCurrentBaseBoundaryController() {
        return fxmlLoader.getController();
    }

    public boolean isStandardShape() {
        return !ShapeType.Immersed.equals(comboBoxInitialValue);
    }

    public boolean isImmersedObject() {
        return ShapeType.Immersed.equals(comboBoxInitialValue);
    }

    public String getLabel() {
        return titledName.getText();
    }

    /**
     * Cast the boundary controller to CircleBoundaryController
     *
     * @return the CircleBoundaryController
     */
    public CircleBoundaryController getCircleBoundaryController() {
        return (CircleBoundaryController) baseBoundaryController;
    }

    /**
     * Cast the boundary controller to RectangleBoundaryController
     *
     * @return the RectangleBoundaryController
     */
    public RectangleBoundaryController getRectangleBoundaryController() {
        return (RectangleBoundaryController) baseBoundaryController;
    }

    public HalfPlaneBoundaryController getHalfPlaneBoundaryController() {
        return (HalfPlaneBoundaryController) baseBoundaryController;
    }

    public ImmersedBoundaryController getImmersedBoundaryController() {
        return (ImmersedBoundaryController) baseBoundaryController;
    }

    @Override
    protected Parent getRoot() {
        return titledPane;
    }

    @Override
    protected void dispatchValidState() {
        // fire alert for new boundary validated and to allow the creation of new boundary
        EventManager.fireCustomEvent(new CustomEvent(EventEnum.BOUNDARY_VALIDATED, this));
        EventManager.fireCustomEvent(new CustomEvent(EventEnum.ALLOW_NEW_BOUNDARY_DEF));
    }
}
