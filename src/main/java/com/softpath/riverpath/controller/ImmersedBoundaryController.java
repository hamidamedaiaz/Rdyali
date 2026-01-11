package com.softpath.riverpath.controller;

import com.softpath.riverpath.fileparser.MeshFileParser;
import com.softpath.riverpath.fileparser.MeshResolution;
import com.softpath.riverpath.meshconverter.MeshConverterFactory;
import com.softpath.riverpath.model.Boundary;
import com.softpath.riverpath.model.Coordinates;
import com.softpath.riverpath.model.ImmersedBoundary;
import com.softpath.riverpath.util.DomainProperties;
import com.softpath.riverpath.util.ValidatedField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.shape.Shape;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;

import java.io.File;

import static com.softpath.riverpath.util.UtilityClass.workspaceDirectory;

/**
 * CircleBoundaryController
 *
 * @author rhajou
 */
@NoArgsConstructor
public class ImmersedBoundaryController extends BaseBoundaryController {

    @FXML
    private Label objectFileName;
    @FXML
    @ValidatedField
    private Hyperlink importObject;

    /**
     * Import immersed object from .msh file, convert it to .t file and throw an event to display it in mesh view
     *
     * @param keyEvent the event
     */
    @FXML
    private void importImmersedObject(ActionEvent keyEvent) {
        // Create a FileChooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a File to Import");
        // Show the file dialog
        Stage stage = new Stage();
        // select mesh file .msh
        File selectedFile = fileChooser.showOpenDialog(stage);
        // convert msh file to .t file using python program gmsh4mtc.exe
        String selectedFileConvertedT = MeshConverterFactory.convert(selectedFile.getAbsolutePath());
        // store it only if the file is imported and converted successfully
        importObject.setText(selectedFileConvertedT);
        setDirty(true);
    }

    /**
     * @see BaseBoundaryController#checkValidCommit()
     * @deprecated must be removed when a new handler for Hyperlink
     */
    @Override
    protected boolean checkValidCommit() {
        boolean isTextFieldsValid = super.checkValidCommit();
        return !importObject.getText().equals("Import file") && isTextFieldsValid;
    }

    /**
     * @see BaseBoundaryController#importValues(Boundary)
     */
    @Override
    void importValues(Boundary boundary) {
        if (boundary instanceof ImmersedBoundary immersedBoundary) {
            importObject.setText(immersedBoundary.getImmersedObjectFileName());
        }
    }

    /**
     * @see BaseBoundaryController#getShape(DomainProperties, Coordinates)
     */
    @Override
    Shape getShape(DomainProperties domainProperties, Coordinates origin) {
        // return nothing since it is a custom object handle by mesh file
        return null;
    }

    /**
     * Build a CFDTriangleMesh based on the immersed object file name
     *
     * @return the immersed object mesh as {@link com.softpath.riverpath.fileparser.CFDTriangleMesh}
     */
    public MeshResolution getImmersedObjectMesh() {
        // parse the .t file to get the mesh object
        return MeshFileParser.parseFile2TriangleMesh(new File(workspaceDirectory, importObject.getText()));
    }

    /**
     * Return the selected mesh file name (.t extension)
     *
     * @return mesh file name
     */
    public String getMeshFileName() {
        return getInitialValue(importObject);
    }


    public Hyperlink getImportObject() {
        return importObject;
    }

}
