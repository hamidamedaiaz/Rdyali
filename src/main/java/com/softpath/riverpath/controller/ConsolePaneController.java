package com.softpath.riverpath.controller;

import com.softpath.riverpath.custom.event.CustomEvent;
import com.softpath.riverpath.custom.event.EventEnum;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.softpath.riverpath.custom.event.EventManager.fireCustomEvent;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;

/**
 * Controller for the console pane
 *
 * @author rhajou
 */
@NoArgsConstructor
@Getter
@Setter
public class ConsolePaneController implements Initializable {

    private static final String CARRIAGE_RETURN = System.lineSeparator();
    private static final String FX_BACKGROUND_COLOR_BLACK = "-fx-background-color: #1E2329;";
    private static final int MESSAGE_BUFFER_SIZE = 50;
    public static final String INCREMENT_DE_COMPTEUR_TEMPS = "Increment de CompteurTemps :";

    @FXML
    private VirtualizedScrollPane<InlineCssTextArea> scrollConsole;
    @FXML
    private InlineCssTextArea consoleOutput;

    // message buffer used when Cimlib is called to avoir UI frequent refresh
    private List<String> messageBuffer = new ArrayList<>();
    private AtomicBoolean autoScrollEnabled = new AtomicBoolean(true);
    private ContextMenu contextMenu;

    /**
     * Display a message in the console output
     *
     * @param message the message to display
     */
    public void displayMessage(String message) {
        Platform.runLater(() -> {
            appendText(message + "\n");
        });
    }

    /**
     * Buffer a message to display in the console output<br>
     * Display it if the buffer is full
     *
     * @param message the message to buffer and display
     */
    public void displayMessageIfNeeded(String message) {
        Platform.runLater(() -> {
            Integer increment = extractIncrementDeCompteurTemps(message);
            if (increment != null) {
                CustomEvent event = new CustomEvent(EventEnum.PROGRESS_BAR_UPDATE, increment);
                fireCustomEvent(event);
            }
            messageBuffer.add(message);
            if (messageBuffer.size() > MESSAGE_BUFFER_SIZE) {
                formatAndDisplay();
                // clear buffer
                messageBuffer.clear();
            }
        });
    }

    /**
     * Flush the buffer to display the messages in the console output
     */
    public void flush() {
        Platform.runLater(this::formatAndDisplay);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set up context menu for copy/paste functionality or other actions
        handleContextMenu();
        // handle auto scroll
        handleAutoScroll();
    }

    /**
     * Handles the auto scroll feature
     */
    private void handleAutoScroll() {
        // autoscroll to bottom when text is added
        consoleOutput.textProperty().addListener((obs, oldText, newText) -> autoScrollToBottom());

        // do not autoscroll when the mouse is over the console output
        consoleOutput.setOnMouseEntered(e -> autoScrollEnabled.set(false));
        // resume autoscroll when the mouse leaves the console output
        consoleOutput.setOnMouseExited(e -> autoScrollEnabled.set(true));

        // Stop autoscroll on text selection
        consoleOutput.selectedTextProperty().addListener(
                (obs, oldText, newText) ->
                        autoScrollEnabled.set(!StringUtils.isNoneBlank(newText)));

        // stop autoscroll when scrollbar is clicked or hovered
        ScrollBar vbar = (ScrollBar) scrollConsole.lookup(".scroll-bar:vertical");
        vbar.addEventFilter(MOUSE_PRESSED, event -> {
            autoScrollEnabled.set(false); // Stop autoscroll
        });
        vbar.addEventFilter(MOUSE_ENTERED, event -> {
            autoScrollEnabled.set(false); // Stop autoscroll
        });
    }

    /**
     * Sets up the context menu for copy functionality
     */
    private void handleContextMenu() {
        contextMenu = new ContextMenu();
        MenuItem copyMenuItem = new MenuItem("Copy");
        // Copy action
        copyMenuItem.setOnAction(event -> {
            copySelectedText();
            contextMenu.hide(); // Hide the menu after copying
        });
        contextMenu.getItems().add(copyMenuItem);

        // Stop autoscroll when text is selected or context menu is shown
        consoleOutput.setOnContextMenuRequested(event -> {
            autoScrollEnabled.set(false); // Stop autoscroll when context menu is displayed
            contextMenu.show(consoleOutput, event.getScreenX(), event.getScreenY());
        });
    }

    /**
     * Copies the selected text to clipboard
     */
    private void copySelectedText() {
        String selectedText = consoleOutput.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(selectedText);
            clipboard.setContent(content);
        }
    }

    /**
     * Format and display the messages in the console output<br>
     */
    private void formatAndDisplay() {
        appendText(String.join(CARRIAGE_RETURN, messageBuffer));
    }

    private void autoScrollToBottom() {
        Platform.runLater(() -> {
            // Check if the consoleOutput is not null and scrolls the text area to the bottom
            if (consoleOutput != null && autoScrollEnabled.get()) {
                consoleOutput.moveTo(consoleOutput.getLength());
                consoleOutput.requestFollowCaret();
            }
        });
    }

    private void appendText(String text) {
        // Save the current caret and selection
        int caretPosition = consoleOutput.getCaretPosition();
        int anchorPosition = consoleOutput.getAnchor();
        consoleOutput.appendText(text);
        // Restore caret/selection or autoscroll
        if (caretPosition != anchorPosition) {
            consoleOutput.selectRange(anchorPosition, caretPosition);
        }
    }

    /**
     * Extract the digit from message if it contains INCREMENT_DE_COMPTEUR_TEMPS
     *
     * @param message the message to extract the increment from
     *                e.g. "Increment de CompteurTemps : 5"
     * @return the increment extracted from the message
     */
    private Integer extractIncrementDeCompteurTemps(String message) {
        Integer increment = null;
        if (message.contains(INCREMENT_DE_COMPTEUR_TEMPS)) {
            String[] split = message.split(":");
            increment = Integer.valueOf(split[1].trim());
        }
        return increment;
    }
}