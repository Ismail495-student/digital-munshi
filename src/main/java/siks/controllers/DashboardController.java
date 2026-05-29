package siks.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import siks.models.User;
import siks.utils.DBUtil;

public class DashboardController {

    // ================= UI =================
    @FXML
    private VBox sidebar;

    @FXML
    private BorderPane mainPane;

    @FXML
    private Label activeSectionLabel;

    // Sidebar buttons
    @FXML
    private Button btnCustomer, btnSupplier, btnItem, btnVoucher, btnInvoice;
    @FXML
    private Button btnCustomerReport, btnSupplierReport, btnStockReport;
    @FXML
    private Button btnOverview;
    @FXML
    private Button btnAnalytics;
    @FXML
    private Button btnSettings;
    @FXML
    private Label lastInvoiceLabel;
    @FXML
    private Button btnLanguage;
    // Header buttons
    @FXML
    private Button btnSalesInvoice, btnPurchaseInvoice, btnOpenInvoice,
            btnLogout, btnToggleMenu, btnCombo, btnBalance, btnEditInvoice;

    // ================= STATE =================
    private User currentUser;
    private boolean sidebarVisible = true;

    // ================= INIT =================
    @FXML
    public void initialize() {
        setupSidebarButtons();
        setupIcons();
        setupButtonActions();
        applyPermissions();
        setupBackground();
        setupEscapeKey();
        loadLastInvoiceInfo();
    }

    // ================= ICONS =================
    private void setupIcons() {
        setButtonIcon(btnCustomer, "/icons/customer.png");
        setButtonIcon(btnSupplier, "/icons/supplier.png");
        setButtonIcon(btnItem, "/icons/item.png");
        setButtonIcon(btnVoucher, "/icons/voucher.png");
        setButtonIcon(btnInvoice, "/icons/invoice.png");
        setButtonIcon(btnSettings, "/icons/setting.png");
        setButtonIcon(btnCustomerReport, "/icons/customerReport.png");
        setButtonIcon(btnStockReport, "/icons/stock.png");
        // ================= Top Buttons =================
        setTopButtonIcon(btnSalesInvoice, "/icons/sales.png");
        setTopButtonIcon(btnPurchaseInvoice, "/icons/purchase.png");
        setTopButtonIcon(btnOpenInvoice, "/icons/open.png");
        setTopButtonIcon(btnCombo, "/icons/combine.png");
        setTopButtonIcon(btnBalance, "/icons/balance.png");
        setTopButtonIcon(btnEditInvoice, "/icons/edit.png");
        setTopButtonIcon(btnLogout, "/icons/logout.png");
    }

    private void setButtonIcon(Button button, String path) {
        try {
            Image img = new Image(getClass().getResourceAsStream(path));
            ImageView view = new ImageView(img);
            view.setFitWidth(18);
            view.setFitHeight(18);
            button.setGraphic(view);
            button.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        } catch (Exception e) {
            System.err.println("⚠ Icon not found: " + path);
        }
    }

    // ================= SIDEBAR SETUP =================
    private void setupSidebarButtons() {
        double buttonHeight = 40;

        for (Button b : new Button[]{
                btnCustomer, btnSupplier, btnItem, btnVoucher, btnInvoice,
                btnCustomerReport, btnSupplierReport, btnStockReport
        }) {
            b.setPrefHeight(buttonHeight);
        }
    }

    // ================= BUTTON ACTIONS =================
    private void setupButtonActions() {

        // Sidebar navigation
        btnCustomer.setOnAction(e -> loadSection("/fxml/customer.fxml", "Customer"));
        btnSupplier.setOnAction(e -> loadSection("/fxml/supplier.fxml", "Supplier"));
        btnItem.setOnAction(e -> loadSection("/fxml/item.fxml", "Item"));
        btnVoucher.setOnAction(e -> loadSection("/fxml/voucher.fxml", "Voucher"));
        btnInvoice.setOnAction(e -> loadSection("/fxml/invoice_table.fxml", "Invoice"));

        btnOverview.setOnAction(e -> loadSection("/fxml/dashboard_overview.fxml", "Overview"));
        btnAnalytics.setOnAction(e -> loadSection("/fxml/analytics_overview.fxml", "Analytics"));

        btnCustomerReport.setOnAction(e -> loadSection("/fxml/customer_report.fxml", "Customer Report"));
        btnSupplierReport.setOnAction(e -> loadSection("/fxml/supplier_report.fxml", "Supplier Report"));
        btnStockReport.setOnAction(e -> loadSection("/fxml/stock.fxml", "Stock Report"));

        // Header
        btnSalesInvoice.setOnAction(e -> loadSection("/fxml/sales_invoice.fxml", "Sales Invoice"));
        btnPurchaseInvoice.setOnAction(e -> loadSection("/fxml/purchase_invoice.fxml", "Purchase Invoice"));
        btnOpenInvoice.setOnAction(e -> loadSection("/fxml/openInvoice.fxml", "Open Invoice"));

        btnSettings.setOnAction(e -> loadSection("/fxml/userManagement.fxml", "Management"));
        btnCombo.setOnAction(e -> loadSection("/fxml/combine_invoices.fxml", "Combo"));
        btnBalance.setOnAction(e -> loadSection("/fxml/CustomerPrintView.fxml", "CPB"));
        btnLanguage.setOnAction(e -> loadSection("/fxml/setLanguage.fxml", "Language"));
        btnEditInvoice.setOnAction(e -> loadSection("/fxml/editinvoice.fxml", "Edit Invoice"));

        btnLogout.setOnAction(e -> handleLogout());

        // 🔥 TOGGLE SIDEBAR
        btnToggleMenu.setOnAction(e -> toggleSidebar());
    }

    // ================= LOAD CENTER CONTENT =================
    private void loadSection(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);

            Scene scene = new Scene(root);

            stage.setScene(scene);

            // 🔥 make it resizable
            stage.setResizable(true);

            // 🔥 open as maximized (full screen feel)
            stage.setMaximized(true);

            // optional: center if not maximized properly
            stage.centerOnScreen();
            activeSectionLabel.setText(title);


            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= TOGGLE SIDEBAR (SHOW/HIDE) =================
    private void toggleSidebar() {

        if (sidebarVisible) {

            // 🔴 Hide with animation
            FadeTransition fade = new FadeTransition(Duration.millis(200), sidebar);
            fade.setFromValue(1);
            fade.setToValue(0);

            fade.setOnFinished(e -> {
                mainPane.setLeft(null);
                sidebarVisible = false;
            });

            fade.play();

        } else {

            // 🟢 Show sidebar
            mainPane.setLeft(sidebar);
            sidebar.setOpacity(0);

            FadeTransition fade = new FadeTransition(Duration.millis(200), sidebar);
            fade.setFromValue(0);
            fade.setToValue(1);

            fade.play();

            sidebarVisible = true;
        }
    }

    // ================= LOGOUT =================
    private void handleLogout() {
        try {
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.close();

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/fxml/login.fxml"));

            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(loader.load()));
            loginStage.setTitle("Digital Munshi Login");
            loginStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= BACKGROUND =================
    private void setupBackground() {
        mainPane.setStyle("-fx-background-color: #dfe6e9;");
    }

    // ================= USER =================
    public void setCurrentUser(User user) {
        this.currentUser = user;
        applyPermissions();
    }

    // ================= PERMISSIONS =================
    private void applyPermissions() {
        if (currentUser == null) return;

        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            enableAllButtons();
            return;
        }

        disableAllButtons();

        if (currentUser.hasPermission("btnCustomer")) btnCustomer.setDisable(false);
        if (currentUser.hasPermission("btnSupplier")) btnSupplier.setDisable(false);
        if (currentUser.hasPermission("btnItem")) btnItem.setDisable(false);
        if (currentUser.hasPermission("btnVoucher")) btnVoucher.setDisable(false);
        if (currentUser.hasPermission("btnInvoice")) btnInvoice.setDisable(false);
        if (currentUser.hasPermission("btnCustomerReport")) btnCustomerReport.setDisable(false);
        if (currentUser.hasPermission("btnSupplierReport")) btnSupplierReport.setDisable(false);
        if (currentUser.hasPermission("btnStockReport")) btnStockReport.setDisable(false);
        if (currentUser.hasPermission("btnOverview")) btnOverview.setDisable(false);
        if (currentUser.hasPermission("btnAnalytics")) btnAnalytics.setDisable(false);
        if (currentUser.hasPermission("btnSettings")) btnSettings.setDisable(false);
        if (currentUser.hasPermission("btnSalesInvoice")) btnSalesInvoice.setDisable(false);
        if (currentUser.hasPermission("btnPurchaseInvoice")) btnPurchaseInvoice.setDisable(false);
        if (currentUser.hasPermission("btnOpenInvoice")) btnOpenInvoice.setDisable(false);
    }

    private void disableAllButtons() {
        btnCustomer.setDisable(true);
        btnSupplier.setDisable(true);
        btnItem.setDisable(true);
        btnVoucher.setDisable(true);
        btnInvoice.setDisable(true);
        btnCustomerReport.setDisable(true);
        btnSupplierReport.setDisable(true);
        btnStockReport.setDisable(true);
        btnOverview.setDisable(true);
        btnAnalytics.setDisable(true);
        btnSettings.setDisable(true);
        btnSalesInvoice.setDisable(true);
        btnPurchaseInvoice.setDisable(true);
        btnOpenInvoice.setDisable(true);
    }

    private void enableAllButtons() {
        btnCustomer.setDisable(false);
        btnSupplier.setDisable(false);
        btnItem.setDisable(false);
        btnVoucher.setDisable(false);
        btnInvoice.setDisable(false);
        btnCustomerReport.setDisable(false);
        btnSupplierReport.setDisable(false);
        btnStockReport.setDisable(false);
        btnOverview.setDisable(false);
        btnAnalytics.setDisable(false);
        btnSettings.setDisable(false);
        btnSalesInvoice.setDisable(false);
        btnPurchaseInvoice.setDisable(false);
        btnOpenInvoice.setDisable(false);
    }

    private void setupEscapeKey() {
        mainPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case ESCAPE:
                            toggleSidebar();
                            btnCustomer.requestFocus();
                            break;
                    }
                });
            }
        });
    }
    private void loadLastInvoiceInfo() {
        String query = "SELECT id, totalBill,type FROM invoices ORDER BY id DESC LIMIT 1";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int id = rs.getInt("id");
                int amount = (int) rs.getDouble("totalBill");
                String type = rs.getString("type");
                lastInvoiceLabel.setText("Last Invoice("+type+"): #" + id + " --> Rs " + amount);

            } else {
                lastInvoiceLabel.setText("Last Invoice: None");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setTopButtonIcon(Button button, String path) {
        try {
            Image img = new Image(getClass().getResourceAsStream(path));

            ImageView view = new ImageView(img);

            // 🔥 Bigger icon for top buttons
            view.setFitWidth(30);
            view.setFitHeight(30);

            button.setGraphic(view);

            // 🔥 Icon upar
            button.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);

            // 🔥 Gap between icon and text
            button.setGraphicTextGap(6);

        } catch (Exception e) {
            System.err.println("⚠ Icon not found: " + path);
        }
    }
}