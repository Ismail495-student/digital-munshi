package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import siks.models.InvoiceItem;
import siks.utils.DBUtil;
import siks.utils.ReceiptBuilder;
import siks.utils.VoucherMiniPrinter;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CustomerReportController {

    @FXML private TextField txtCustomer;
    @FXML private DatePicker dpFrom, dpTo;
    @FXML private Button btnFilter;
    @FXML private Button btnPrintTop;

    @FXML private TableView<LedgerRow> tblLedger;
    @FXML private TableColumn<LedgerRow, Integer> colId;
    @FXML private TableColumn<LedgerRow, String> colType, colDate, colDescription;
    @FXML private TableColumn<LedgerRow, Double> colTotalBefore, colPayment, colRemaining;

    @FXML private Label lblAddress, lblPhone, lblCategory, lblPrevBalance;

    private final ObservableList<String> allCustomers = FXCollections.observableArrayList();
    private final ObservableList<LedgerRow> ledgerData = FXCollections.observableArrayList();

    private final ContextMenu suggestionMenu = new ContextMenu();

    @FXML
    public void initialize() {

        colId.setCellValueFactory(d ->
                new javafx.beans.property.SimpleIntegerProperty(
                        d.getValue().getId()
                ).asObject()
        );

        colType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getType()
                )
        );

        colDate.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getDate()
                )
        );

        colDescription.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getDescription()
                )
        );

        colTotalBefore.setCellValueFactory(d ->
                new javafx.beans.property.SimpleDoubleProperty(
                        (int) d.getValue().getTotalBefore()
                ).asObject()
        );

        colPayment.setCellValueFactory(d ->
                new javafx.beans.property.SimpleDoubleProperty(
                        d.getValue().getPayment()
                ).asObject()
        );

        colRemaining.setCellValueFactory(d ->
                new javafx.beans.property.SimpleDoubleProperty(
                        (int)d.getValue().getRemaining()
                ).asObject()
        );

        tblLedger.setItems(ledgerData);

        btnPrintTop.setOnAction(e -> printTopRowForCustomer());
        btnFilter.setOnAction(e -> loadCustomerLedger());

        loadCustomerNames();

        // 🔥 FIXED MENU SETTINGS
        suggestionMenu.setAutoHide(true);
        suggestionMenu.setHideOnEscape(true);

        txtCustomer.textProperty().addListener((obs, oldVal, newVal) -> {
            filterCustomerList(newVal);
        });

        tblLedger.setRowFactory(tv -> {
            TableRow<LedgerRow> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openRow(row.getItem());
                }
            });

            return row;
        });
    }

    // ================= CUSTOMER NAMES =================
    private void loadCustomerNames() {

        allCustomers.clear();

        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT name FROM customers ORDER BY name"
             )) {

            while (rs.next()) {
                allCustomers.add(rs.getString("name"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= 🔥 FIXED AUTOCOMPLETE MENU =================
    private void filterCustomerList(String text) {

        if (text == null || text.trim().isEmpty()) {
            suggestionMenu.hide();
            return;
        }

        VBox box = new VBox();
        box.setPrefWidth(150);
        box.setMinWidth(150);
        box.setMaxWidth(150);
        box.setStyle("-fx-background-color:white;");

        int count = 0;

        for (String name : allCustomers) {

            if (name.toLowerCase().contains(text.toLowerCase())) {

                Label lbl = new Label(name);

                lbl.setPrefWidth(150);
                lbl.setMinWidth(150);
                lbl.setMaxWidth(150);

                lbl.setStyle(
                        "-fx-padding:6 8 6 8;" +
                                "-fx-cursor:hand;"
                );

                lbl.setOnMouseEntered(e ->
                        lbl.setStyle(
                                "-fx-padding:6 8 6 8;" +
                                        "-fx-background-color:#e8f0fe;" +
                                        "-fx-cursor:hand;"
                        )
                );

                lbl.setOnMouseExited(e ->
                        lbl.setStyle(
                                "-fx-padding:6 8 6 8;" +
                                        "-fx-cursor:hand;"
                        )
                );

                lbl.setOnMouseClicked(e -> {
                    txtCustomer.setText(name);
                    suggestionMenu.hide();

                    loadCustomerDetails(name);
                    loadCustomerLedger();
                });

                box.getChildren().add(lbl);
                count++;
            }
        }

        if (count == 0) {
            suggestionMenu.hide();
            return;
        }

        ScrollPane scrollPane = new ScrollPane(box);

        scrollPane.setStyle(
                "-fx-background: white;" +
                        "-fx-background-color: white;" +
                        "-fx-padding:0;"
        );
        scrollPane.setPrefWidth(150);
        scrollPane.setMinWidth(150);
        scrollPane.setMaxWidth(150);

        scrollPane.setPrefHeight(180);

        scrollPane.setFitToWidth(true);

        scrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scrollPane.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        CustomMenuItem item =
                new CustomMenuItem(scrollPane, false);

        item.setHideOnClick(false);

        suggestionMenu.getItems().clear();
        suggestionMenu.getItems().add(item);

        if (suggestionMenu.isShowing()) {
            suggestionMenu.hide();
        }

        // 🔥 exact textbox ke neeche
        suggestionMenu.show(txtCustomer, Side.BOTTOM, 0, 0);

    }

    // ================= CUSTOMER DETAILS =================
    private void loadCustomerDetails(String name) {

        lblAddress.setText("-");
        lblPhone.setText("-");
        lblCategory.setText("-");
        lblPrevBalance.setText("0.0");

        String sql =
                "SELECT address, phone, category, prevBalance " +
                        "FROM customers WHERE name=?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, name);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                lblAddress.setText(rs.getString("address"));
                lblPhone.setText(rs.getString("phone"));
                lblCategory.setText(rs.getString("category"));
                lblPrevBalance.setText(
                        String.format("%.2f",
                                rs.getDouble("prevBalance"))
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= LEDGER =================
    /** -------- LEDGER DATA (INVOICE + VOUCHER) -------- */
    private void loadCustomerLedger() {
        ledgerData.clear();
        String name = txtCustomer.getText().trim();
        if (name.isEmpty()) return;

        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try (Connection conn = DBUtil.getConnection()) {
            ObservableList<LedgerRow> combined = FXCollections.observableArrayList();

            // --- Fetch Invoices
            String invoiceSQL = "SELECT id, dateTime, totalBill, payment, remaining FROM invoices WHERE partyName=? AND type='SALE'";
            if (from != null && to != null) invoiceSQL += " AND dateTime BETWEEN ? AND ?";
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
                                "Sale Invoice",
                                rs.getDouble("totalBill"),
                                rs.getDouble("payment"),
                                rs.getDouble("remaining")
                        ));
                    }
                }
            }

            // --- Fetch Vouchers
            String voucherSQL = "SELECT id, dateTime, amount, description FROM vouchers WHERE name=?";
            if (from != null && to != null) voucherSQL += " AND dateTime BETWEEN ? AND ?";
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

            // --- Sort descending (latest first)
            combined.sort(Comparator.comparing(LedgerRow::getDate).reversed());

            // --- Running total logic
            double running = 0;
            for (int i = combined.size() - 1; i >= 0; i--) {
                LedgerRow row = combined.get(i);

                if (row.getType().equals("Invoice")) {
                    row.setTotalBefore(row.getTotalBefore());
                    row.setPayment(row.getPayment());
                    row.setRemaining(row.getRemaining());
                    running = row.getRemaining();
                } else { // Voucher
                    row.setTotalBefore(running); // prevBalance before payment
                    row.setPayment(row.getPayment()); // voucher amount
                    row.setRemaining(running - row.getPayment());
                    running = row.getRemaining();
                }

                ledgerData.add(0, row); // descending order
            }

        } catch (Exception ex) { ex.printStackTrace(); }

        tblLedger.refresh();
    }
    /**
     * Returns the top (latest) row from the ledger table
     */


    // ================= OPEN ROW =================
    private void openRow(LedgerRow row) {

        if (row.getType().equalsIgnoreCase("Invoice")) {
            openInvoiceDirect(row.getId());
        } else {
            openVoucherDirect(row.getId());
        }
    }

    @FXML
    private void printTopRowForCustomer() {
        VoucherMiniPrinter.printTopLedgerRow(
                txtCustomer.getText(),
                tblLedger
        );
    }

    // ================= MODEL =================
    public static class LedgerRow {

        private final int id;
        private final String type;
        private final String date;
        private final String description;

        private double totalBefore;
        private double payment;
        private double remaining;

        public LedgerRow(
                int id,
                String type,
                String date,
                String description,
                double totalBefore,
                double payment,
                double remaining
        ) {
            this.id = id;
            this.type = type;
            this.date = date;
            this.description = description;
            this.totalBefore = totalBefore;
            this.payment = payment;
            this.remaining = remaining;
        }

        public int getId() { return id; }
        public String getType() { return type; }
        public String getDate() { return date; }
        public String getDescription() { return description; }
        public double getTotalBefore() { return totalBefore; }
        public double getPayment() { return payment; }
        public double getRemaining() { return remaining; }

        public void setTotalBefore(double v) { totalBefore = v; }
        public void setPayment(double v) { payment = v; }
        public void setRemaining(double v) { remaining = v; }
    }

    private void showAlert(
            Alert.AlertType type,
            String title,
            String msg
    ) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ===== KEEP YOUR EXISTING METHODS SAME =====

private void openInvoiceDirect(int id) {
    try (Connection conn = DBUtil.getConnection()) {

        // 1️⃣ Invoice fetch (type bhi lena zaroori)
        String sql = "SELECT * FROM invoices WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            showAlert(Alert.AlertType.ERROR, "Not Found", "Invoice not found!");
            return;
        }

        String type = rs.getString("type"); // SALE / PURCHASE
        String partyName = rs.getString("partyName");
        String dateTime = rs.getString("dateTime");
        double totalBill = rs.getDouble("totalBill");
        double payment = rs.getDouble("payment");
        double remaining = rs.getDouble("remaining");

        // 2️⃣ Prefix generate
        String prefix = type.equals("SALE") ? "SI" : "PI";
        String invoiceIdFormatted = String.format(prefix + "%04d", id);

        // 3️⃣ Items load
        String itemSql = "SELECT itemName, rate, cartons, pieces, amount FROM invoice_items WHERE invoice_id=?";
        PreparedStatement ps2 = conn.prepareStatement(itemSql);
        ps2.setInt(1, id);
        ResultSet rs2 = ps2.executeQuery();

        List<InvoiceItem> items = new ArrayList<>();
        double itemsTotal = 0;

        while (rs2.next()) {
            InvoiceItem item = new InvoiceItem(
                    null,
                    rs2.getString("itemName"),
                    rs2.getDouble("rate"),
                    rs2.getInt("cartons"),
                    rs2.getDouble("pieces"),
                    rs2.getDouble("amount")
            );
            items.add(item);
            itemsTotal += item.getAmount();
        }

        // 4️⃣ Derived values
        double prevBalance = totalBill - itemsTotal;
        if (prevBalance < 0) prevBalance = 0;

        // 5️⃣ Receipt Node build
        Node receipt = ReceiptBuilder.buildReceiptNode(
                invoiceIdFormatted,
                dateTime,
                partyName,
                items,
                prevBalance,
                itemsTotal,
                totalBill,
                payment,
                remaining
        );

        // 6️⃣ 🔥 Print Preview (MAIN GOAL)
        PrintPreviewController.show(
                receipt,
                type.equals("SALE") ? "Sales Invoice" : "Purchase Invoice",
                invoiceIdFormatted,
                dateTime,
                partyName,
                items,
                prevBalance,
                itemsTotal,
                totalBill,
                payment,
                remaining
        );

    } catch (Exception e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Error", "Failed to open invoice preview.");
    }
}
private void openVoucherDirect(int id) {
    try (Connection conn = DBUtil.getConnection()) {

        String sql = "SELECT * FROM vouchers WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            showAlert(Alert.AlertType.ERROR, "Not Found", "Voucher not found!");
            return;
        }

        String name = rs.getString("name");
        String dateTime = rs.getString("dateTime");
        double amount = rs.getDouble("amount");
        String description = rs.getString("description");

        // 🔹 Simple UI banana (temporary preview)
        VBox voucherLayout = new VBox(10);
        voucherLayout.setStyle("-fx-padding:20; -fx-background-color:white;");

        Label title = new Label("Voucher");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        Label lblId = new Label("Voucher ID: V" + String.format("%04d", id));
        Label lblName = new Label("Name: " + name);
        Label lblDate = new Label("Date: " + dateTime);
        Label lblAmount = new Label("Amount: " + String.format("%.2f", amount));
        Label lblDesc = new Label("Description: " + description);

        voucherLayout.getChildren().addAll(
                title, lblId, lblName, lblDate, lblAmount, lblDesc
        );

        // 🔹 Scroll + Stage (same feel as preview)
        ScrollPane scroll = new ScrollPane(voucherLayout);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 400, 400);
        Stage stage = new Stage();
        stage.setTitle("Voucher Preview");
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.showAndWait();

    } catch (Exception e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Error", "Failed to open voucher preview.");
    }
}
  }