package siks;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import siks.license.*;

public class Main extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {

        this.primaryStage = stage;

        System.out.println("Machine ID: " + MachineUtil.getMachineId());

        handleAppFlow();
    }

    // ================= MAIN FLOW =================
    private void handleAppFlow() throws Exception {

        // 1. LICENSE ACTIVE → DIRECT LOGIN
        if (LicenseService.isLicensed()) {
            showLogin();
            return;
        }

        // 2. TRIAL ACTIVE → NORMAL MODE
        TrialManager.initTrial();

        if (TrialManager.isActive()) {

            long daysLeft = TrialManager.getDaysLeft();

            int choice = LicenseDialog.showTrialReminderWithActivation(daysLeft);

            if (choice == 1) {
                showLogin();
                return;
            }

            else if (choice == 2) {
                boolean ok = LicenseDialog.showActivationDialog();

                if (ok) {
                    handleAppFlow();
                } else {
                    showLogin();
                }
                return;
            }

            System.exit(0);
        }

        // 3. FIRST TIME USER ONLY
        if (!TrialManager.hasEverStarted()) {

            int choice = LicenseDialog.showStartupChoice();

            if (choice == 1) {
                TrialManager.initTrial();
                showLogin();
            }

            else if (choice == 2) {
                boolean ok = LicenseDialog.showActivationDialog();

                if (ok) {
                    handleAppFlow();
                } else {
                    System.exit(0);
                }
            }

            else {
                System.exit(0);
            }

            return;
        }

        // 4. 🔴 TRIAL EXPIRED STATE (FIXED WITH BUTTON FLOW)
        int choice = LicenseDialog.showExpiredDialog();

        if (choice == 1) {

            boolean ok = LicenseDialog.showActivationDialog();

            if (ok) {
                handleAppFlow(); // re-check license after activation
            } else {
                System.exit(0);
            }

        } else {
            System.exit(0);
        }
    }

    // ================= LOGIN SCREEN =================
    private void showLogin() throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml")
        );

        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("DIGITAL MUNSHI - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}