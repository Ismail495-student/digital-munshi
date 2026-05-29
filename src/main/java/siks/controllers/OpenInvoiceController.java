package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import siks.models.InvoiceItem;
import siks.models.Session;
import siks.utils.DBUtil;
import siks.utils.ReceiptBuilder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OpenInvoiceController {

    @FXML private TextField txtInvoiceId;
    @FXML private Button btnSearch, btnClear, btnPrint;
    @FXML private Label lblType,lblPartyType, lblPartyName, lblDateTime, lblItemsTotal, lblPrevBalance, lblTotalBill, lblPayment, lblRemaining;

    @FXML private TableView<InvoiceItem> tblItems;
    @FXML private TableColumn<InvoiceItem, String> colSr, colItemName;
    @FXML private TableColumn<InvoiceItem, Double> colRate, colAmount;
    @FXML private TableColumn<InvoiceItem, Integer> colCartons;
    @FXML private TableColumn<InvoiceItem, Double> colPieces;
    @FXML private Button btnAddPayment;
    private static final String URL = "jdbc:sqlite:database.db";
    private List<InvoiceItem> currentItems = new ArrayList<>();

    @FXML
    public void initialize() {
        setupColumns();

        tblItems.setEditable(false);
        btnSearch.setOnAction(e -> loadInvoiceById(txtInvoiceId.getText()));
        btnClear.setOnAction(e -> clearFields());
        btnPrint.setOnAction(e -> printInvoice());



    }

    /** Load invoice by full ID (e.g. SI0003 or PI0004) */
    private void loadInvoiceById(String invoiceId) {
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Missing", "Please enter an invoice ID (e.g. SI0001 or PI0002).");
            return;
        }

        invoiceId = invoiceId.trim().toUpperCase();
        if (invoiceId.length() < 3) {
            showAlert(Alert.AlertType.ERROR, "Invalid ID", "Invoice ID too short.");
            return;
        }

        String prefix = invoiceId.substring(0, 2);
        int numericId;
        try {
            numericId = Integer.parseInt(invoiceId.substring(2));
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Format", "Invoice ID must be like SI0001 or PI0003.");
            return;
        }

        String type = prefix.equals("SI") ? "SALE" : prefix.equals("PI") ? "PURCHASE" : null;
        if (type == null) {
            showAlert(Alert.AlertType.ERROR, "Invalid Prefix", "Invoice must start with SI or PI.");
            return;
        }

        try (Connection conn = DBUtil.getConnection();) {
            String sql = "SELECT * FROM invoices WHERE id = ? AND type = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, numericId);
                ps.setString(2, type);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        lblType.setText(type.equals("SALE") ? "Sales Invoice" : "Purchase Invoice");
                        lblPartyName.setText(rs.getString("partyName"));
                        lblDateTime.setText(rs.getString("dateTime"));
                        lblTotalBill.setText(String.format("%.2f", rs.getDouble("totalBill")));
                        lblPayment.setText(String.format("%.2f", rs.getDouble("payment")));
                        lblRemaining.setText(String.format("%.2f", rs.getDouble("remaining")));

                        // Load items and calculate prevBalance
                        loadInvoiceItems(numericId, rs.getDouble("totalBill"));
                        setInvoiceTypeUI(type);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Not Found", "No invoice found for ID: " + invoiceId);
                        clearFields();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Unable to load invoice.");
        }
    }

    /** Load invoice items and calculate prevBalance dynamically */
    private void loadInvoiceItems(int invoiceId, double totalBillFromDb) {
        ObservableList<InvoiceItem> list = FXCollections.observableArrayList();
        String sql = "SELECT itemName, rate, cartons, pieces, amount FROM invoice_items WHERE invoice_id = ?";
        double itemsTotal = 0.0;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvoiceItem item = new InvoiceItem(
                            null,
                            rs.getString("itemName"),
                            rs.getDouble("rate"),
                            rs.getInt("cartons"),
                            rs.getDouble("pieces"),
                            rs.getDouble("amount")
                    );
                    list.add(item);
                    itemsTotal += rs.getDouble("amount");
                }
            }

            tblItems.setItems(list);
            currentItems = new ArrayList<>(list);

            // 🔹 Calculate Derived Balances
            double prevBalance = totalBillFromDb - itemsTotal;
            if (prevBalance < 0) prevBalance = 0;

            lblItemsTotal.setText(String.format("%.2f", itemsTotal));
            lblPrevBalance.setText(String.format("%.2f", prevBalance));

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Unable to load invoice items.");
        }
    }

    /** Print Invoice */
    private void printInvoice() {
        if (currentItems.isEmpty() || lblType.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Invoice", "Please load an invoice before printing.");
            return;
        }

        try {
            Node receipt = ReceiptBuilder.buildReceiptNode(
                    txtInvoiceId.getText(),
                    lblDateTime.getText(),
                    lblPartyName.getText(),
                    currentItems,
                    Double.parseDouble(lblPrevBalance.getText()),
                    Double.parseDouble(lblItemsTotal.getText()),
                    Double.parseDouble(lblTotalBill.getText()),
                    Double.parseDouble(lblPayment.getText()),
                    Double.parseDouble(lblRemaining.getText())
            );

            PrintPreviewController.show(
                    receipt,
                    lblType.getText(),
                    txtInvoiceId.getText(),
                    lblDateTime.getText(),
                    lblPartyName.getText(),
                    currentItems,
                    Double.parseDouble(lblPrevBalance.getText()),
                    Double.parseDouble(lblItemsTotal.getText()),
                    Double.parseDouble(lblTotalBill.getText()),
                    Double.parseDouble(lblPayment.getText()),
                    Double.parseDouble(lblRemaining.getText())
            );

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Print Error", "Unable to open print preview.");
        }
    }

    /** Setup table columns */
    private void setupColumns() {
        colSr.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(tblItems.getItems().indexOf(d.getValue()) + 1)));
        colItemName.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getItemName()));
        colRate.setCellValueFactory(d ->
                new javafx.beans.property.SimpleDoubleProperty(d.getValue().getRate()).asObject());
        colCartons.setCellValueFactory(d ->
                new javafx.beans.property.SimpleIntegerProperty(d.getValue().getCartons()).asObject());
        colPieces.setCellValueFactory(d ->
                new javafx.beans.property.SimpleDoubleProperty(d.getValue().getPieces()).asObject());
        colAmount.setCellValueFactory(d ->
                new javafx.beans.property.SimpleDoubleProperty(d.getValue().getAmount()).asObject());
    }

    /** Clear all fields */
    private void clearFields() {
        txtInvoiceId.clear();
        lblType.setText("");
        lblPartyName.setText("");
        lblDateTime.setText("");
        lblItemsTotal.setText("");
        lblPrevBalance.setText("");
        lblTotalBill.setText("");
        lblPayment.setText("");
        lblRemaining.setText("");
        tblItems.getItems().clear();
        currentItems.clear();
    }

    /** Show alert box */
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
    public void setInvoiceTypeUI(String invoiceType) {

        String label = switch (invoiceType == null ? "" : invoiceType.toUpperCase()) {
            case "PURCHASE" -> "Supplier";
            case "SALE" -> "Customer";
            default -> "Name";
        };

        lblPartyType.setText(label);
    }


}