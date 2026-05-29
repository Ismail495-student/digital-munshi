package siks.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import siks.utils.UserDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserManagementController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRole;

    @FXML private CheckBox chkCustomer, chkSupplier, chkItem, chkVoucher, chkInvoice;
    @FXML private CheckBox chkCustomerReport, chkSupplierReport, chkStockReport;
    @FXML private CheckBox chkOverview, chkAnalytics, chkSettings;
    @FXML private CheckBox chkSalesInvoice, chkPurchaseInvoice, chkOpenInvoice;

    @FXML private Button btnAddUser;
    @FXML private Button btnUpdateUser;
    @FXML private Button btnDeleteUser;
    @FXML private Button btnClear;

    @FXML private Label lblStatus;

    @FXML private TableView<UserRow> tblUsers;
    @FXML private TableColumn<UserRow, String> colUsername;
    @FXML private TableColumn<UserRow, String> colRole;
    @FXML private TableColumn<UserRow, String> colPermissions;

    private final ObservableList<UserRow> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        cmbRole.getItems().addAll("admin", "user");

        colUsername.setCellValueFactory(data -> data.getValue().usernameProperty());
        colRole.setCellValueFactory(data -> data.getValue().roleProperty());
        colPermissions.setCellValueFactory(data -> data.getValue().permissionsProperty());

        tblUsers.setItems(userList);

        btnAddUser.setOnAction(e -> handleAddUser());
        btnUpdateUser.setOnAction(e -> handleUpdateUser());
        btnDeleteUser.setOnAction(e -> handleDeleteUser());
        btnClear.setOnAction(e -> clearForm());

        tblUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, row) -> {
            if (row != null) {
                loadSelectedUser(row);
            }
        });

        loadUsers();
    }

    // ========================= ADD =========================
    private void handleAddUser() {

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String role = cmbRole.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            setError("All fields are required!");
            return;
        }

        try {
            UserDAO.addUser(username, password, role, collectPermissions());

            setSuccess("User added successfully!");
            clearForm();
            loadUsers();

        } catch (Exception ex) {
            ex.printStackTrace();
            setError(ex.getMessage());
        }
    }

    // ========================= UPDATE =========================
    private void handleUpdateUser() {

        UserRow row = tblUsers.getSelectionModel().getSelectedItem();

        if (row == null) {
            setError("Select user first!");
            return;
        }

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String role = cmbRole.getValue();

        if (username.isEmpty() || role == null) {
            setError("Username & Role required!");
            return;
        }

        try {
            UserDAO.updateUser(username, password, role, collectPermissions());

            setSuccess("User updated successfully!");
            clearForm();
            loadUsers();

        } catch (Exception ex) {
            ex.printStackTrace();
            setError(ex.getMessage());
        }
    }

    // ========================= DELETE =========================
    private void handleDeleteUser() {

        UserRow row = tblUsers.getSelectionModel().getSelectedItem();

        if (row == null) {
            setError("Select user first!");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText(null);
        alert.setContentText("Delete user: " + row.getUsername() + " ?");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {

            try {
                UserDAO.deleteUser(row.getUsername());

                setSuccess("User deleted successfully!");
                clearForm();
                loadUsers();

            } catch (Exception ex) {
                ex.printStackTrace();
                setError(ex.getMessage());
            }
        }
    }

    // ========================= LOAD SELECTED =========================
    private void loadSelectedUser(UserRow row) {

        txtUsername.setText(row.getUsername());
        cmbRole.setValue(row.getRole());

        txtPassword.clear();

        clearChecks();

        String perms = row.getPermissions();

        if (perms.contains("btnCustomer")) chkCustomer.setSelected(true);
        if (perms.contains("btnSupplier")) chkSupplier.setSelected(true);
        if (perms.contains("btnItem")) chkItem.setSelected(true);
        if (perms.contains("btnVoucher")) chkVoucher.setSelected(true);
        if (perms.contains("btnInvoice")) chkInvoice.setSelected(true);
        if (perms.contains("btnCustomerReport")) chkCustomerReport.setSelected(true);
        if (perms.contains("btnSupplierReport")) chkSupplierReport.setSelected(true);
        if (perms.contains("btnStockReport")) chkStockReport.setSelected(true);
        if (perms.contains("btnOverview")) chkOverview.setSelected(true);
        if (perms.contains("btnAnalytics")) chkAnalytics.setSelected(true);
        if (perms.contains("btnSettings")) chkSettings.setSelected(true);
        if (perms.contains("btnSalesInvoice")) chkSalesInvoice.setSelected(true);
        if (perms.contains("btnPurchaseInvoice")) chkPurchaseInvoice.setSelected(true);
        if (perms.contains("btnOpenInvoice")) chkOpenInvoice.setSelected(true);
    }

    // ========================= LOAD USERS =========================
    private void loadUsers() {

        try {
            userList.clear();

            var users = UserDAO.getAllUsers();

            for (var u : users) {
                userList.add(new UserRow(
                        u.getUsername(),
                        u.getRole(),
                        String.join(", ", u.getPermissions())
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========================= PERMISSIONS =========================
    private List<String> collectPermissions() {

        List<String> perms = new ArrayList<>();

        if (chkCustomer.isSelected()) perms.add("btnCustomer");
        if (chkSupplier.isSelected()) perms.add("btnSupplier");
        if (chkItem.isSelected()) perms.add("btnItem");
        if (chkVoucher.isSelected()) perms.add("btnVoucher");
        if (chkInvoice.isSelected()) perms.add("btnInvoice");
        if (chkCustomerReport.isSelected()) perms.add("btnCustomerReport");
        if (chkSupplierReport.isSelected()) perms.add("btnSupplierReport");
        if (chkStockReport.isSelected()) perms.add("btnStockReport");
        if (chkOverview.isSelected()) perms.add("btnOverview");
        if (chkAnalytics.isSelected()) perms.add("btnAnalytics");
        if (chkSettings.isSelected()) perms.add("btnSettings");
        if (chkSalesInvoice.isSelected()) perms.add("btnSalesInvoice");
        if (chkPurchaseInvoice.isSelected()) perms.add("btnPurchaseInvoice");
        if (chkOpenInvoice.isSelected()) perms.add("btnOpenInvoice");

        return perms;
    }

    // ========================= CLEAR =========================
    private void clearForm() {

        txtUsername.clear();
        txtPassword.clear();
        cmbRole.setValue(null);

        clearChecks();

        tblUsers.getSelectionModel().clearSelection();

        lblStatus.setText("");
    }

    private void clearChecks() {

        chkCustomer.setSelected(false);
        chkSupplier.setSelected(false);
        chkItem.setSelected(false);
        chkVoucher.setSelected(false);
        chkInvoice.setSelected(false);
        chkCustomerReport.setSelected(false);
        chkSupplierReport.setSelected(false);
        chkStockReport.setSelected(false);
        chkOverview.setSelected(false);
        chkAnalytics.setSelected(false);
        chkSettings.setSelected(false);
        chkSalesInvoice.setSelected(false);
        chkPurchaseInvoice.setSelected(false);
        chkOpenInvoice.setSelected(false);
    }

    // ========================= STATUS =========================
    private void setSuccess(String msg) {
        lblStatus.setStyle("-fx-text-fill: green;");
        lblStatus.setText(msg);
    }

    private void setError(String msg) {
        lblStatus.setStyle("-fx-text-fill: red;");
        lblStatus.setText(msg);
    }

    // ========================= TABLE MODEL =========================
    public static class UserRow {

        private final SimpleStringProperty username;
        private final SimpleStringProperty role;
        private final SimpleStringProperty permissions;

        public UserRow(String username, String role, String permissions) {
            this.username = new SimpleStringProperty(username);
            this.role = new SimpleStringProperty(role);
            this.permissions = new SimpleStringProperty(permissions);
        }

        public String getUsername() {
            return username.get();
        }

        public String getRole() {
            return role.get();
        }

        public String getPermissions() {
            return permissions.get();
        }

        public SimpleStringProperty usernameProperty() {
            return username;
        }

        public SimpleStringProperty roleProperty() {
            return role;
        }

        public SimpleStringProperty permissionsProperty() {
            return permissions;
        }
    }
}