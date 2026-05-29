package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import siks.utils.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class SupplierReportController {

    @FXML private TextField txtSupplier;
    @FXML private DatePicker dpFrom, dpTo;
    @FXML private Button btnFilter;
    @FXML private TableView<LedgerRow> tblLedger;
    @FXML private TableColumn<LedgerRow, Integer> colId;
    @FXML private TableColumn<LedgerRow, String> colType, colDate, colDescription;
    @FXML private TableColumn<LedgerRow, Double> colTotalBefore, colPayment, colRemaining;

    @FXML private Label lblAddress, lblPhone, lblCategory, lblPrevBalance;

    private final ObservableList<String> allSuppliers = FXCollections.observableArrayList();
    private final ObservableList<LedgerRow> ledgerData = FXCollections.observableArrayList();

    private final ContextMenu suggestionMenu = new ContextMenu();
    private MenuItem[] currentItems = new MenuItem[0];
    private int selectedIndex = -1;

    private static final String URL = "jdbc:sqlite:database.db";

    @FXML
    public void initialize() {

        // TABLE SETUP
        colId.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getId()).asObject());
        colType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getType()));
        colDate.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDate()));
        colDescription.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDescription()));
        colTotalBefore.setCellValueFactory(d -> new javafx.beans.property.SimpleDoubleProperty(d.getValue().getTotalBefore()).asObject());
        colPayment.setCellValueFactory(d -> new javafx.beans.property.SimpleDoubleProperty(d.getValue().getPayment()).asObject());
        colRemaining.setCellValueFactory(d -> new javafx.beans.property.SimpleDoubleProperty(d.getValue().getRemaining()).asObject());

        tblLedger.setItems(ledgerData);

        loadSupplierNames();

        // LIVE FILTER
        txtSupplier.textProperty().addListener((obs, oldVal, newVal) -> filterSupplierList(newVal));

        // KEYBOARD NAVIGATION
        txtSupplier.setOnKeyPressed(e -> {

            if (!suggestionMenu.isShowing()) return;

            if (e.getCode() == KeyCode.DOWN) {
                selectedIndex = Math.min(selectedIndex + 1, currentItems.length - 1);
                highlightItem();
                e.consume();
            }

            if (e.getCode() == KeyCode.UP) {
                selectedIndex = Math.max(selectedIndex - 1, 0);
                highlightItem();
                e.consume();
            }

            if (e.getCode() == KeyCode.ENTER) {
                if (selectedIndex >= 0 && selectedIndex < currentItems.length) {
                    currentItems[selectedIndex].fire();
                }
                e.consume();
            }

            if (e.getCode() == KeyCode.ESCAPE) {
                suggestionMenu.hide();
            }
        });

        txtSupplier.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) suggestionMenu.hide();
        });

        btnFilter.setOnAction(e -> loadSupplierLedger());
    }

    // =========================
    // LOAD SUPPLIERS
    // =========================
    private void loadSupplierNames() {
        allSuppliers.clear();
        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM suppliers ORDER BY name")) {

            while (rs.next()) allSuppliers.add(rs.getString("name"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // FILTER + CONTEXT MENU
    // =========================
    private void filterSupplierList(String text) {

        suggestionMenu.hide();

        if (text == null || text.trim().isEmpty()) return;

        ObservableList<MenuItem> items = FXCollections.observableArrayList();

        for (String name : allSuppliers) {
            if (name.toLowerCase().contains(text.toLowerCase())) {

                MenuItem item = new MenuItem(name);

                item.setOnAction(e -> {
                    txtSupplier.setText(name);
                    suggestionMenu.hide();
                    loadSupplierDetails(name);
                    loadSupplierLedger();
                });

                items.add(item);
            }
        }

        if (items.isEmpty()) return;

        currentItems = items.toArray(new MenuItem[0]);
        selectedIndex = 0;

        suggestionMenu.getItems().setAll(items);

        showContextMenu();
    }

    // =========================
    // SHOW MENU UNDER TEXTFIELD (150 WIDTH)
    // =========================
    private void showContextMenu() {

        suggestionMenu.setPrefWidth(150);

        double x = txtSupplier.localToScreen(0, 0).getX();
        double y = txtSupplier.localToScreen(0, txtSupplier.getHeight()).getY();

        suggestionMenu.show(txtSupplier, x, y);

        highlightItem();
    }

    // =========================
    // HIGHLIGHT SELECTED ITEM
    // =========================
    private void highlightItem() {
        for (int i = 0; i < currentItems.length; i++) {
            MenuItem item = currentItems[i];

            if (i == selectedIndex) {
                item.setStyle("-fx-background-color:#2d89ef; -fx-text-fill:white;");
            } else {
                item.setStyle("");
            }
        }
    }

    // =========================
    // SUPPLIER DETAILS
    // =========================
    private void loadSupplierDetails(String name) {

        lblAddress.setText("-");
        lblPhone.setText("-");
        lblCategory.setText("-");
        lblPrevBalance.setText("0.0");

        String sql = "SELECT address, phone, company, prevBalance FROM suppliers WHERE name = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblAddress.setText(rs.getString("address") != null ? rs.getString("address") : "-");
                    lblPhone.setText(rs.getString("phone") != null ? rs.getString("phone") : "-");
                    lblCategory.setText(rs.getString("company") != null ? rs.getString("company") : "-");
                    lblPrevBalance.setText(String.format("%.2f", rs.getDouble("prevBalance")));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // LEDGER LOAD
    // =========================
    private void loadSupplierLedger() {

        ledgerData.clear();

        String name = txtSupplier.getText().trim();
        if (name.isEmpty()) return;

        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try (Connection conn = DBUtil.getConnection()) {

            ObservableList<LedgerRow> combined = FXCollections.observableArrayList();

            // INVOICES
            String invoiceSQL = "SELECT id, dateTime, totalBill, payment, remaining FROM invoices WHERE partyName=? AND type='PURCHASE'";
            if (from != null && to != null)
                invoiceSQL += " AND dateTime BETWEEN ? AND ?";

            try (PreparedStatement ps = conn.prepareStatement(invoiceSQL)) {
                ps.setString(1, name);

                if (from != null && to != null) {
                    ps.setString(2, from.format(df));
                    ps.setString(3, to.plusDays(1).format(df));
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        combined.add(new LedgerRow(
                                rs.getInt("id"),
                                "Invoice",
                                rs.getString("dateTime"),
                                "Purchase Invoice",
                                rs.getDouble("totalBill"),
                                rs.getDouble("payment"),
                                rs.getDouble("remaining")
                        ));
                    }
                }
            }

            // VOUCHERS
            String voucherSQL = "SELECT id, dateTime, amount, description FROM vouchers WHERE name=?";
            if (from != null && to != null)
                voucherSQL += " AND dateTime BETWEEN ? AND ?";

            try (PreparedStatement ps = conn.prepareStatement(voucherSQL)) {
                ps.setString(1, name);

                if (from != null && to != null) {
                    ps.setString(2, from.format(df));
                    ps.setString(3, to.plusDays(1).format(df));
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        combined.add(new LedgerRow(
                                rs.getInt("id"),
                                "Voucher",
                                rs.getString("dateTime"),
                                rs.getString("description"),
                                0,
                                rs.getDouble("amount"),
                                0
                        ));
                    }
                }
            }

            // SORT
            combined.sort(Comparator.comparing(LedgerRow::getDate).reversed());

            // RUNNING BALANCE
            double running = 0;

            for (int i = combined.size() - 1; i >= 0; i--) {

                LedgerRow row = combined.get(i);

                if (row.getType().equals("Invoice")) {
                    row.setTotalBefore(row.getAmount());
                    row.setRemaining(row.getRemaining());
                    running = row.getRemaining();
                } else {
                    row.setTotalBefore(running);
                    row.setRemaining(running - row.getPayment());
                    running = row.getRemaining();
                }
            }

            ledgerData.addAll(combined);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        tblLedger.refresh();
    }

    // =========================
    // MODEL
    // =========================
    public static class LedgerRow {

        private final int id;
        private final String type, date, description;
        private double amount, totalBefore, payment, remaining;

        public LedgerRow(int id, String type, String date, String description,
                         double amount, double payment, double remaining) {
            this.id = id;
            this.type = type;
            this.date = date;
            this.description = description;
            this.amount = amount;
            this.payment = payment;
            this.remaining = remaining;
        }

        public int getId() { return id; }
        public String getType() { return type; }
        public String getDate() { return date; }
        public String getDescription() { return description; }
        public double getAmount() { return amount; }
        public double getPayment() { return payment; }
        public double getTotalBefore() { return totalBefore; }
        public double getRemaining() { return remaining; }

        public void setTotalBefore(double v) { totalBefore = v; }
        public void setRemaining(double v) { remaining = v; }
    }
}