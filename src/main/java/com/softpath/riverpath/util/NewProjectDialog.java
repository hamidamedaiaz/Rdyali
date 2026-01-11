package com.softpath.riverpath.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Optional;

public class NewProjectDialog extends Dialog<NewProjectDialog.ProjectConfig> {

    // Remember last used location
    private static File lastUsedLocation = null;
    private final TextField projectNameField;
    private final TextField locationField;
    private final TextField domainFileField;
    private final Button browseLocationButton;
    private final Button browseDomainButton;
    private final Label validationLabel;
    private final Label pathPreviewValue;
    public NewProjectDialog(Stage owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("New Project");
        setHeaderText("Create New RiverPath Project");


        // Initialize fields
        projectNameField = new TextField();
        projectNameField.setText(generateDefaultProjectName()); // Set default name
        projectNameField.setPromptText("Enter project name");
        projectNameField.getStyleClass().add("dialog-text-field");
        projectNameField.selectAll();

        locationField = new TextField();
        locationField.setPromptText("Select project location");
        locationField.setEditable(false);
        locationField.getStyleClass().add("dialog-text-field");

        // Set default location
        File defaultLocation = getDefaultLocation();
        locationField.setText(defaultLocation.getAbsolutePath());

        domainFileField = new TextField();
        domainFileField.setPromptText("Select domain file (.msh)");
        domainFileField.setEditable(false);
        domainFileField.getStyleClass().add("dialog-text-field");

        browseLocationButton = new Button("Browse...");
        browseLocationButton.getStyleClass().add("browse-button");

        browseDomainButton = new Button("Browse...");
        browseDomainButton.getStyleClass().add("browse-button");

        validationLabel = new Label();
        validationLabel.getStyleClass().add("validation-error");
        validationLabel.setVisible(false);

        pathPreviewValue = new Label();
        pathPreviewValue.getStyleClass().add("preview-path");
        pathPreviewValue.setWrapText(true);

        DialogPane dialogPane = getDialogPane();
        dialogPane.setContent(createContent());
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);


        var cssUrl = getClass().getResource("/com/softpath/riverpath/controller/style.css");
        dialogPane.getStylesheets().add(cssUrl.toExternalForm());
        var headerText = dialogPane.lookup(".header-panel .label");
        headerText.setStyle("-fx-text-fill: black; -fx-font-size: 14px; -fx-font-weight: bold;");


        // Setup button actions
        setupButtonActions();

        // Setup validation
        setupValidation();

        // Disable OK button initially
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        // Set result converter
        setResultConverter(this::convertResult);

        // Set preferred size
        dialogPane.setPrefSize(650, 450);
    }

    /**
     * Show the dialog and return the result
     */
    public static Optional<ProjectConfig> showAndWait(Stage owner) {
        NewProjectDialog dialog = new NewProjectDialog(owner);
        return dialog.showAndWait();
    }

    private VBox createContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("dialog-content");

        // Project Name Section
        content.getChildren().add(createSection(
                "Project Name",
                "Name of your CFD simulation project",
                projectNameField,
                null
        ));

        // Project Location Section
        content.getChildren().add(createSection(
                "Project Location",
                "Directory where the project will be created",
                locationField,
                browseLocationButton
        ));

        // Domain File Section
        content.getChildren().add(createSection(
                "Domain File",
                "Gmsh mesh file (.msh) to import",
                domainFileField,
                browseDomainButton
        ));

        // Full Path Preview
        Label fullPathLabel = new Label("Project will be created at:");
        fullPathLabel.getStyleClass().add("preview-label");

        // Update preview when name or location changes
        projectNameField.textProperty().addListener((obs, old, val) -> updatePathPreview());
        locationField.textProperty().addListener((obs, old, val) -> updatePathPreview());

        VBox previewBox = new VBox(8, fullPathLabel, pathPreviewValue);
        previewBox.setPadding(new Insets(15, 10, 15, 10));
        previewBox.getStyleClass().add("preview-box");
        content.getChildren().add(previewBox);

        content.getChildren().add(validationLabel);

        return content;
    }

    private VBox createSection(String title, String description, TextField field, Button button) {
        VBox section = new VBox(8);
        section.setPadding(new Insets(12));
        section.getStyleClass().add("section-box");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("section-description");

        HBox inputRow = new HBox(10);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);

        inputRow.getChildren().add(field);

        if (button != null) {
            button.setPrefWidth(100);

            inputRow.getChildren().add(button);
        }

        section.getChildren().addAll(titleLabel, descLabel, inputRow);
        return section;
    }

    private void setupButtonActions() {
        // Browse location button
        browseLocationButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Project Location");

            File initialDir = getInitialDirectory();
            if (initialDir != null && initialDir.exists()) {
                chooser.setInitialDirectory(initialDir);
            }

            File selectedDir = chooser.showDialog(getOwner());
            if (selectedDir != null) {
                locationField.setText(selectedDir.getAbsolutePath());
            }
        });

        // Browse domain file button
        browseDomainButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Domain File");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Gmsh files (*.msh)", "*.msh")
            );

            File selectedFile = chooser.showOpenDialog(getOwner());
            if (selectedFile != null) {
                domainFileField.setText(selectedFile.getAbsolutePath());

                // Only auto-fill project name if user hasn't changed it from default
                String currentName = projectNameField.getText().trim();
                if (currentName.isEmpty() || currentName.equals("NewSimulation")) {
                    String baseName = FilenameUtils.getBaseName(selectedFile.getName());
                    projectNameField.setText(baseName);
                    projectNameField.selectAll();
                }
            }
        });
    }

    private void setupValidation() {
        projectNameField.textProperty().addListener((obs, old, val) -> validateInput());
        locationField.textProperty().addListener((obs, old, val) -> validateInput());
        domainFileField.textProperty().addListener((obs, old, val) -> validateInput());
    }

    private void validateInput() {
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        validationLabel.setVisible(false);

        String projectName = projectNameField.getText().trim();
        String location = locationField.getText().trim();
        String domainFile = domainFileField.getText().trim();

        // Validate project name
        if (projectName.isEmpty()) {
            okButton.setDisable(true);
            return;
        }

        if (!isValidProjectName(projectName)) {
            showValidationError("Invalid project name. Use only letters, numbers, - and _");
            okButton.setDisable(true);
            return;
        }

        // Validate location
        if (location.isEmpty()) {
            okButton.setDisable(true);
            return;
        }

        File locationDir = new File(location);
        if (!locationDir.exists() || !locationDir.isDirectory()) {
            showValidationError("Location directory does not exist");
            okButton.setDisable(true);
            return;
        }

        // Check if project already exists
        File projectDir = new File(locationDir, projectName);
        if (projectDir.exists()) {
            showValidationError("A project with this name already exists at this location");
            okButton.setDisable(true);
            return;
        }

        // Validate domain file
        if (domainFile.isEmpty()) {
            okButton.setDisable(true);
            return;
        }

        File meshFile = new File(domainFile);
        if (!meshFile.exists() || !meshFile.isFile()) {
            showValidationError("Domain file does not exist");
            okButton.setDisable(true);
            return;
        }

        if (!meshFile.getName().toLowerCase().endsWith(".msh")) {
            showValidationError("Domain file must be a .msh file");
            okButton.setDisable(true);
            return;
        }

        okButton.setDisable(false);
    }

    private void updatePathPreview() {
        String projectName = projectNameField.getText().trim();
        String location = locationField.getText().trim();

        if (!projectName.isEmpty() && !location.isEmpty()) {
            File projectDir = new File(location, projectName);
            pathPreviewValue.setText(projectDir.getAbsolutePath());
        } else {
            pathPreviewValue.setText("");
        }
    }

    private boolean isValidProjectName(String name) {
        return name.matches("[a-zA-Z0-9_-]+");
    }

    private void showValidationError(String message) {
        validationLabel.setText(message);
        validationLabel.setVisible(true);
    }

    /**
     * Get default location for new projects
     * Uses last used location or falls back to .riverpath directory
     */
    private File getDefaultLocation() {
        if (lastUsedLocation != null && lastUsedLocation.exists()) {
            return lastUsedLocation;
        }
        // Fallback to .riverpath directory
        return UtilityClass.getHomeDirectory();
    }

    private File getInitialDirectory() {
        return getDefaultLocation();
    }

    /**
     * Generate a default project name
     */
    private String generateDefaultProjectName() {
        return "NewSimulation";
    }

    private ProjectConfig convertResult(ButtonType buttonType) {
        if (buttonType == ButtonType.OK) {
            File location = new File(locationField.getText().trim());
            lastUsedLocation = location;

            return new ProjectConfig(
                    projectNameField.getText().trim(),
                    location,
                    new File(domainFileField.getText().trim())
            );
        }
        return null;
    }

    @Getter
    public static class ProjectConfig {
        private final String projectName;
        private final File projectLocation;
        private final File domainFile;

        public ProjectConfig(String projectName, File projectLocation, File domainFile) {
            this.projectName = projectName;
            this.projectLocation = projectLocation;
            this.domainFile = domainFile;
        }

        public File getProjectDirectory() {
            return new File(projectLocation, projectName);
        }
    }
}