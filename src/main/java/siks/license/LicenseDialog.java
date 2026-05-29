package siks.license;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

public class LicenseDialog {

    // ================= FIRST TIME CHOICE =================
    public static int showStartupChoice() {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Welcome to Siks ERP");
        alert.setHeaderText("Get Started");

        alert.setContentText(
                "Choose how you want to continue:\n\n" +
                        "✔ Start Free Trial (3 days)\n" +
                        "✔ Activate License"
        );

        ButtonType trialBtn = new ButtonType("Start Trial");
        ButtonType licenseBtn = new ButtonType("Activate License");
        ButtonType exitBtn = new ButtonType("Exit");

        alert.getButtonTypes().setAll(trialBtn, licenseBtn, exitBtn);

        ButtonType result = alert.showAndWait().orElse(exitBtn);

        if (result == trialBtn) return 1;
        if (result == licenseBtn) return 2;

        return 0;
    }

    // ================= TRIAL REMINDER =================
    public static int showTrialReminderWithActivation(long daysLeft) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Trial Mode");

        if (daysLeft > 0) {
            alert.setHeaderText("Trial Active - " + daysLeft + " day(s) left");
        } else {
            alert.setHeaderText("Trial Expired");
        }

        alert.setContentText(
                "You are using Siks ERP in trial mode.\n\n" +
                        "You can continue or activate full license anytime."
        );

        ButtonType continueBtn = new ButtonType("Continue Trial");
        ButtonType activateBtn = new ButtonType("Activate License");
        ButtonType exitBtn = new ButtonType("Exit");

        alert.getButtonTypes().setAll(continueBtn, activateBtn, exitBtn);

        ButtonType result = alert.showAndWait().orElse(exitBtn);

        if (result == continueBtn) return 1;
        if (result == activateBtn) return 2;

        return 0;
    }

    // ================= EXPIRED TRIAL =================
    public static int showExpiredDialog() {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Trial Expired");
        alert.setHeaderText("Your trial period has ended");

        alert.setContentText(
                "To continue using Siks ERP,\n" +
                        "please activate a valid license."
        );

        ButtonType activateBtn = new ButtonType("Activate License");
        ButtonType exitBtn = new ButtonType("Exit");

        alert.getButtonTypes().setAll(activateBtn, exitBtn);

        ButtonType result = alert.showAndWait().orElse(exitBtn);

        if (result == activateBtn) return 1;

        return 0;
    }
    // ================= LICENSE ACTIVATION =================
    public static boolean showActivationDialog() {

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("License Activation");

        Label machineLabel = new Label("Machine ID:");
        TextField machineField = new TextField(MachineUtil.getMachineId());
        machineField.setEditable(false);

        Label keyLabel = new Label("Enter License Key:");
        TextField keyField = new TextField();

        VBox box = new VBox(10, machineLabel, machineField, keyLabel, keyField);
        box.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(box);

        ButtonType activateBtn = new ButtonType("Activate", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(activateBtn, cancelBtn);

        dialog.setResultConverter(btn -> {
            if (btn == activateBtn) {
                return keyField.getText();
            }
            return null;
        });

        String key = dialog.showAndWait().orElse(null);

        if (key == null || key.isEmpty()) return false;

        boolean success = LicenseService.activate(key);

        if (success) {
            showSuccess();
        } else {
            showError();
        }

        return success;
    }

    // ================= SUCCESS =================
    private static void showSuccess() {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Activation Successful");
        alert.setHeaderText("Welcome to Siks ERP 🎉");
        alert.setContentText("License activated successfully.");

        alert.showAndWait();
    }

    // ================= ERROR =================
    private static void showError() {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Activation Failed");
        alert.setHeaderText("Invalid License Key");
        alert.setContentText("Please try again.");

        alert.showAndWait();
    }
}