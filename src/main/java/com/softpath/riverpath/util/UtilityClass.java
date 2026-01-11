package com.softpath.riverpath.util;

import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventManager;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.softpath.riverpath.custom.event.EventEnum.EVENT_PROCESS_MESSAGE;
import static org.apache.commons.lang3.Strings.CS;

/**
 * Utility class
 *
 * @author rhajou
 */
@Slf4j
public class UtilityClass {

    public static File workspaceDirectory;
    private static final String ZERO = "0";
    private static final String DOT = ".";
    private static final String EMPTY = "";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static int detectDimensionImportCase(File meshFileExtentionT) {
        try (BufferedReader reader = Files.newBufferedReader(meshFileExtentionT.toPath(), StandardCharsets.UTF_8)) {
            String[] parts = reader.readLine().trim().split("\\s+");
            return Integer.parseInt(parts[1]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String buildMessage(String message, Object... args) {
        return (args == null || args.length == 0) ? message : String.format(message, args);
    }

    /**
     * Create workspace project if not exist
     * The workspace project is created in the home directory
     * In workspace project create current simulation directory
     * workspace name = import + "_" + "yyyyMMddHHmmss"
     *
     * @param mshFile the msh file
     */
    public static void createWorkspace(File mshFile) {
        String workspaceName = FilenameUtils.getBaseName(mshFile.getName());
        File projectDirectory = new File(createOrGetHomeDirectory(), workspaceName);
        if (!projectDirectory.exists()) {
            projectDirectory.mkdir();
        }
        File workspaceDirectory = new File(projectDirectory, "import_" + LocalDateTime.now().format(FORMATTER));
        // create folder
        workspaceDirectory.mkdir();

        UtilityClass.workspaceDirectory = workspaceDirectory;
    }

    public static void copyCimLibResources() {
        try {
            // Template is located next to the app executable (installed by jpackage)
            // In development: Use resources
            // In production: Use installed directory
            File sourceDirectory = getWorkspaceTemplateDirectory();
            if (!sourceDirectory.exists()) {
                throw new RuntimeException("workspace_template not found at: " + sourceDirectory.getAbsolutePath());
            }
            FileUtils.copyDirectory(sourceDirectory, workspaceDirectory);

            // change context of files in Dimension to right dimension files - we could also erase them all and format Principale resource path of those files depending on the  dimension of domain
            String dimensionContext_selector = DomainProperties.getInstance().is3D() ? "3d" : "2d";
            File destDimensionContext = new File(workspaceDirectory, "Dimension");
            URI originURI = Path.of(getAppDirectory().getAbsolutePath(), "src", "main", "resources", dimensionContext_selector).toUri();
            File originDimensionContext = new File(originURI);
            // cp
            FileUtils.deleteDirectory(destDimensionContext);
            FileUtils.copyDirectory(originDimensionContext, destDimensionContext);
        } catch (Exception e) {
            throw new RuntimeException("Error copying workspace_template: " + e.getMessage());
        }
    }

    /**
     * Get the application directory where resources are located.
     * In development mode: returns current directory
     * In production mode (jpackage): returns the app directory set by jpackage
     *
     * @return File pointing to the app directory
     */
    private static File getAppDirectory() {
        // Production: jpackage sets this property via -Dapp.dir=$APPDIR
        String appDir = System.getProperty("app.dir");
        if (appDir != null) {
            return new File(appDir);
        }

        // Development: use current directory
        return new File(System.getProperty("user.dir"));
    }

    /**
     * Get the workspace template directory.
     * In development mode: reads from resources
     * In production mode: reads from installed app directory
     *
     * @return File pointing to workspace_template directory
     */
    private static File getWorkspaceTemplateDirectory() {
        // Try production path first (in app directory)
        File productionPath = new File(getAppDirectory(), "workspace_template");
        if (productionPath.exists()) {
            return productionPath;
        }

        // Fall back to development path (resources)
        URL resourceUrl = UtilityClass.class.getClassLoader().getResource("workspace_template");
        if (resourceUrl != null && "file".equals(resourceUrl.getProtocol())) {
            return new File(resourceUrl.getFile());
        }

        // Return production path even if it doesn't exist (will trigger error message)
        return productionPath;
    }

    public static File exeTempFile() {
        try {
            URL exeuril = UtilityClass.class.getResource("/cimlib_runner/cimlib_CFD_driver.exe");
            assert exeuril != null;
            try (InputStream exef = exeuril.openStream()) {
                // Create a temporary file
                File tempFile = File.createTempFile("temp", ".exe");
                // Write the InputStream to the temporary file
                try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = exef.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                return tempFile;
            }
        } catch (IOException e) {
            throw new RuntimeException("error running cimlib", e);
        }
    }

    // this method should be removed and the home directory should be created at installation time
    @Deprecated
    public static File createOrGetHomeDirectory() {
        File riverpathDirectory = getHomeDirectory();
        if (!riverpathDirectory.exists()) {
            // Attempt to create the directory
            riverpathDirectory.mkdir();
        }
        return riverpathDirectory;
    }

    public static File getHomeDirectory() {
        String homeDirectory = System.getProperty("user.home");
        return new File(homeDirectory, ".riverpath");
    }

    /**
     * Optimized version of runCommand with NumPy warning filtering
     */
    public static int runCommand(File directory, List<String> command) {
        return runCommand(directory, command, false);
    }

    /**
     * Executes a command with optimized message filtering.
     *
     * @param directory Working directory.
     * @param command   Command to execute.
     * @param silent    Silent mode (for warmup).
     * @return Exit code.
     */
    public static int runCommand(File directory, List<String> command, boolean silent) {
        int exitCode = -1;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            if (directory.exists() && directory.isDirectory()) {
                processBuilder.directory(directory);
            }

            Process process = processBuilder.start();

            // Read stdout in an optimized way
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!silent) {
                        EventManager.fireCustomEvent(new CustomEvent(EVENT_PROCESS_MESSAGE, line));
                    }
                }
            }

            // Read stderr with smart filtering of warnings
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    if (!silent && shouldDisplayError(line)) {
                        EventManager.fireCustomEvent(new CustomEvent(EVENT_PROCESS_MESSAGE, "ERROR: " + line));
                    }
                }
            }

            exitCode = process.waitFor();
            process.destroy();

        } catch (Exception e) {
            if (!silent) {
                EventManager.fireCustomEvent(new CustomEvent(EVENT_PROCESS_MESSAGE,
                        "Runtime error: " + e.getMessage()));
            }
            throw new RuntimeException(e);
        }
        return exitCode;
    }

    /**
     * Determines whether an error line should be displayed.
     * Filters non-critical NumPy warnings.
     */
    private static boolean shouldDisplayError(String line) {
        // Filter known NumPy warnings
        if (line.contains("DeprecationWarning") ||
                line.contains("Arrays of 2-dimensional vectors are deprecated") ||
                line.contains("in1d is deprecated") ||
                line.contains("Use arrays of 3-dimensional vectors instead") ||
                line.contains("Use `np.isin` instead")) {
            return false;
        }

        // Filter other non-critical warnings
        return !line.contains("FutureWarning") &&
                !line.contains("UserWarning") &&
                !line.contains("RuntimeWarning");// Show real errors
    }

    public static String buildTExtentionName(File selectedFile) {
        if (FilenameUtils.isExtension(selectedFile.getName(), "msh")) {
            return FilenameUtils.removeExtension(selectedFile.getName()) + ".t";
        } else {
            return selectedFile.getName();
        }
    }

    public static boolean checkNotBlank(TextField myTextField) {
        boolean isValid = false;
        String text = myTextField.getText();
        if (StringUtils.isBlank(text)) {
            flagTextFieldWarning(myTextField);
        } else {
            myTextField.setStyle("");
            isValid = true;
        }
        return isValid;
    }

    public static void flagTextFieldWarning(TextField myTextField) {
        myTextField.setStyle("-fx-border-color: red;");
    }

    public static void unflagTextFieldWarning(TextField myTextField) {
        myTextField.setStyle(null);
    }

    public static void handleTextWithDigitOnly(KeyEvent event) {
        if (!KeyCode.LEFT.equals(event.getCode()) &
                !KeyCode.RIGHT.equals(event.getCode()) &
                !KeyCode.UP.equals(event.getCode()) &
                !KeyCode.DOWN.equals(event.getCode())
        ) {
            TextField textField = (TextField) event.getSource();
            boolean startWithMinus = CS.startsWith(textField.getText(), "-");
            textField.setText(CS.removeStart(textField.getText(), "-"));

            // remove multiple dot
            String currentText = removeMultipleDot(textField);
            if (!startWithMinus || textField.getText().length() != 1) {
                if (!isValidDouble(currentText)) {
                    currentText = StringUtils.defaultIfBlank(currentText.replaceAll("[^\\d.]", EMPTY), ZERO);
                    // Handle case where text starts with dot
                    if (currentText.startsWith(".")) {
                        currentText = "0" + currentText;
                    }
                    textField.setText(currentText);
                    textField.positionCaret(textField.getText().length());
                } else if (CS.startsWith(currentText, ZERO)
                        && !CS.startsWith(currentText, "0.")
                        && !CS.equals(currentText, ZERO)) {
                    textField.setText(CS.removeStart(currentText, ZERO));
                    textField.positionCaret(textField.getText().length());
                }
            }
            if (startWithMinus) {
                textField.setText("-" + textField.getText());
            }
            if (textField.getText() != null) {
                textField.positionCaret(textField.getText().length());
            }
        }
    }

    public static boolean isValidDouble(String text) {
        try {
            if (StringUtils.isEmpty(text)) {
                return true; // Allow an empty field
            }
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String removeMultipleDot(TextField textField) {
        String currentText = textField.getText();
        int numberOf = StringUtils.countMatches(currentText, DOT);
        if (numberOf > 1) {
            int firstIndex = StringUtils.indexOf(currentText, DOT);
            currentText = StringUtils.remove(currentText, DOT);
            currentText = StringUtils.overlay(currentText, DOT, firstIndex, firstIndex);
            currentText = StringUtils.defaultIfBlank(currentText.replaceAll("[^\\d.]", EMPTY), ZERO);
            textField.setText(currentText);
            textField.positionCaret(textField.getText().length());
        }
        return currentText;
    }

    /**
     * @param textField a field element that is being checked by
     *                  {@link #handleTextWithDigitOnly(KeyEvent)}.
     *                  We reformat the text to a standard Java double print
     *                  for all cases, when focus is lost.
     */
    public static void prettyPrintDouble(TextField textField) {
        try {
            double prettyVal = Double.parseDouble(textField.getText());
            textField.setText(String.valueOf(prettyVal));
        } catch (Exception ignored) {
        }
    }
}