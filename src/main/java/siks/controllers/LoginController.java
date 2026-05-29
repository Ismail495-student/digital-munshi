package siks.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import siks.models.Session;
import siks.utils.UserDAO;
import siks.models.User;

import java.io.IOException;
import java.util.List;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin, btnCancel;

    @FXML
    public void initialize() {
        btnLogin.setOnAction(e -> handleLogin());
        btnCancel.setOnAction(e -> handleCancel());
        txtPassword.setOnAction(e -> handleLogin()); // enter key support
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Info", "Please enter both username and password.");
            return;
        }

        try {
            User user = UserDAO.getUserByUsername(username);

            if (user == null) {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "User not found.");
                return;
            }

            if (!user.getPassword().equals(password)) {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid password.");
                return;
            }

            // Fetch permissions from DB
            List<String> permissions = UserDAO.getUserPermissions(user.getId());
            user.setPermissions(permissions);

            // Admin has full access
            if ("admin".equalsIgnoreCase(user.getRole())) {
                showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome Admin!");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome " + user.getUsername() + "!");
            }

            // Open dashboard
            openDashboard(user);
            Session.setCurrentUser(user);

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred: " + ex.getMessage());
        }
    }

    private void openDashboard(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Scene scene = new Scene(loader.load());

            // Pass user to dashboard controller
            DashboardController controller = loader.getController();
            controller.setCurrentUser(user);

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setTitle("DIGITAL MUNSHI- POS Dashboard - Logged in as " + user.getUsername());
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Unable to load dashboard.");
        }
    }

    private void handleCancel() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}