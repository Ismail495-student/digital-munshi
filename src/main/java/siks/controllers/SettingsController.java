package siks.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;

public class SettingsController {

    @FXML private VBox vboxSettingsRows;
    @FXML private StackPane paneSettingsContent;

    // Keep track of the currently selected row
    private Button selectedRowButton;

    @FXML
    public void initialize() {
        // Add all settings rows
        addSettingsRow("Font Settings", "/siks/fxml/setprint.fxml");
        addSettingsRow("User Settings", "/fxml/userManagement.fxml");
        addSettingsRow("Extra Settings", "/siks/fxml/Extra.fxml");
        addSettingsRow("Language Settings", "/fxml/setLanguage.fxml");
    }

    private void addSettingsRow(String title, String fxmlPath) {
        Button rowButton = new Button(title);
        rowButton.setPrefWidth(380);
        rowButton.setStyle(unselectedStyle());

        rowButton.setOnAction(e -> loadRowContent(fxmlPath, rowButton));

        vboxSettingsRows.getChildren().add(rowButton);
    }

    private void loadRowContent(String fxmlPath, Button clickedButton) {
        try {
            // Get FXML resource URL
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("FXML file not found: " + fxmlPath);
                return;
            }

            // Load FXML (works for any root: VBox, AnchorPane, StackPane)
            Parent pane = FXMLLoader.load(fxmlUrl);

            // Set content
            paneSettingsContent.getChildren().setAll(pane);

            // Highlight the clicked row
            highlightRow(clickedButton);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Highlight the selected row button
    private void highlightRow(Button rowButton) {
        if (selectedRowButton != null) {
            selectedRowButton.setStyle(unselectedStyle());
        }
        rowButton.setStyle(selectedStyle());
        selectedRowButton = rowButton;
    }

    private String selectedStyle() {
        return "-fx-background-color: #0078D7; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10; -fx-border-color: #ccc;";
    }

    private String unselectedStyle() {
        return "-fx-background-color: white; -fx-text-fill: black; -fx-alignment: CENTER_LEFT; -fx-padding: 10; -fx-border-color: #ccc;";
    }
}