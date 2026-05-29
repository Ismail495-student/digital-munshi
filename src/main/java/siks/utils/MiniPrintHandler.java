package siks.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import siks.models.InvoiceItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MiniPrintHandler {

    /** Build the mini print UI node */
    public static Node buildMiniPrintNode() {
        VBox root = new VBox(5);
        root.setPadding(new Insets(5));
        root.setAlignment(Pos.TOP_CENTER);
        root.setPrefWidth(260);
        root.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ccc; -fx-border-width: 1;");

        Label lbl = new Label("Mini Print Pass");
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        lbl.setAlignment(Pos.CENTER);

        TextField txtInvoiceId = new TextField();
        txtInvoiceId.setPromptText("Enter Invoice ID");
        txtInvoiceId.setMaxWidth(200);

        HBox btnBox = new HBox(5);
        btnBox.setAlignment(Pos.CENTER);
        Button btnPrint = new Button("Print");
        Button btnCancel = new Button("Cancel");
        btnBox.getChildren().addAll(btnPrint, btnCancel);

        root.getChildren().addAll(lbl, txtInvoiceId, btnBox);

        // ---------- Print Action ----------
        btnPrint.setOnAction(e -> {
            String invoiceId = txtInvoiceId.getText().trim();
            if (invoiceId.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Input Missing", "Please enter an Invoice ID.");
                return;
            }

            // Fetch invoice data (items + customer name)
            InvoiceData data = fetchInvoiceData(invoiceId);
            if (data == null || data.items.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Not Found", "No invoice found for ID: " + invoiceId);
                return;
            }

            // Pass invoiceId + customerName + items to receipt builder
            MiniReceiptBuilder.printMiniReceipt(invoiceId, data.customerName, data.items);
        });

        btnCancel.setOnAction(e -> txtInvoiceId.clear());

        return root;
    }

    // ---------- Helper Classes & Methods ----------

    /** Helper class to store invoice items + customer name */
    private static class InvoiceData {
        String customerName;
        List<InvoiceItem> items;
        public InvoiceData(String customerName, List<InvoiceItem> items) {
            this.customerName = customerName;
            this.items = items;
            System.out.println("CustomerName"+customerName);
        }
    }

    /** Fetch invoice data from DB */
    private static InvoiceData fetchInvoiceData(String invoiceId) {
        List<InvoiceItem> items = new ArrayList<>();
        String customerName = "";

        try (Connection conn = DBUtil.getConnection()) {
            // Get customer/party name
            String sqlInvoice = "SELECT partyName FROM invoices WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlInvoice)) {
                ps.setString(1, invoiceId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) customerName = rs.getString("partyName");
                    else return null; // Invoice not found
                }
            }

            // Get invoice items
            String sqlItems = "SELECT itemName, cartons, pieces FROM invoice_items WHERE invoice_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlItems)) {
                ps.setString(1, invoiceId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(new InvoiceItem(
                                null,
                                rs.getString("itemName"),
                                0.0,
                                rs.getInt("cartons"),
                                rs.getDouble("pieces"),
                                0.0
                        ));
                    }
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return new InvoiceData(customerName, items);
    }

    /** Show alert */
    private static void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

}