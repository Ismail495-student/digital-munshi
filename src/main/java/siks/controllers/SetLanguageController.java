package siks.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import siks.utils.AppSettings;

public class SetLanguageController {

    @FXML private RadioButton radioEnglish;
    @FXML private RadioButton radioUrdu;
    @FXML private Label lblCurrentLang;

    private ToggleGroup languageGroup;

    @FXML
    public void initialize() {
        // Create toggle group and assign it to radio buttons
        languageGroup = new ToggleGroup();
        radioEnglish.setToggleGroup(languageGroup);
        radioUrdu.setToggleGroup(languageGroup);

        // Load saved language from AppSettings
        if (AppSettings.isUrdu()) {
            radioUrdu.setSelected(true);
            lblCurrentLang.setText("Current Language: Urdu");
        } else {
            radioEnglish.setSelected(true);
            lblCurrentLang.setText("Current Language: English");
        }
    }

    @FXML
    private void saveLanguage() {
        RadioButton selected = (RadioButton) languageGroup.getSelectedToggle();
        if (selected != null) {
            String lang = selected.getText();
            AppSettings.setLanguage(lang);

            lblCurrentLang.setText("Current Language: " + lang);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Language Saved");
            alert.setHeaderText(null);
            alert.setContentText("Language set to: " + lang);
            alert.showAndWait();
        }
    }
}