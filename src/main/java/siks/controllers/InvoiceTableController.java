package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import siks.models.Invoice;
import siks.models.InvoiceItem;
import siks.utils.DBUtil;
import siks.utils.InvoiceTableHandler;
import siks.utils.ReceiptBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InvoiceTableController {

    @FXML private TableView<Invoice> tblInvoices;
    @FXML private TableColumn<Invoice, String> colId, colType, colName, colDate;
    @FXML private TableColumn<Invoice, Double> colTotalBill, colPayment, colRemaining;
    @FXML private TextField txtSearch;
    @FXML private DatePicker dpFrom, dpTo;

    private ObservableList<Invoice> invoiceList;
    private FilteredList<Invoice> filteredData;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadInvoices();
        setupFilters();
        tblInvoices.setRowFactory(tv -> {
            TableRow<Invoice> row = new TableRow<>();
            row.setStyle("-fx-cursor: hand;");
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Invoice inv = row.getItem();

                    // 🔥 YAHAN CALL HOGA
                    int numericId = Integer.parseInt(inv.getInvoiceId());
                    openInvoiceDirect(numericId);
                }
            });

            return row;
        });
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getInvoiceId()));
        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPartyName()));
        colDate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDateTime()));
        colTotalBill.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getTotalBill()).asObject());
        colPayment.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getPayment()).asObject());
        colRemaining.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getRemaining()).asObject());
    }

    private void loadInvoices() {
        invoiceList = FXCollections.observableArrayList(InvoiceTableHandler.getInvoices());
        filteredData = new FilteredList<>(invoiceList, p -> true);
        SortedList<Invoice> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblInvoices.comparatorProperty());
        tblInvoices.setItems(sortedData);
    }

    private void setupFilters() {
        // Search by name
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(inv -> {
                String search = newVal.toLowerCase();
                return inv.getPartyName().toLowerCase().contains(search) || inv.getInvoiceId().toLowerCase().contains(search);
            });
        });

        // Date filter
        dpFrom.valueProperty().addListener((obs, oldVal, newVal) -> applyDateFilter());
        dpTo.valueProperty().addListener((obs, oldVal, newVal) -> applyDateFilter());
    }

    private void applyDateFilter() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        filteredData.setPredicate(inv -> {
            boolean dateOk = true;
            if (from != null || to != null) {
                LocalDate invDate = LocalDate.parse(inv.getDateTime().substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                if (from != null && invDate.isBefore(from)) dateOk = false;
                if (to != null && invDate.isAfter(to)) dateOk = false;
            }
            // Keep search filter as well
            String search = txtSearch.getText().toLowerCase();
            boolean searchOk = inv.getPartyName().toLowerCase().contains(search) || inv.getInvoiceId().toLowerCase().contains(search);
            return dateOk && searchOk;
        });
    }
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
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // no header
        alert.setContentText(message);
        alert.showAndWait();
    }

}
