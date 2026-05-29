package siks.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import siks.models.InvoiceItem;
import siks.utils.PdfUtil;
import siks.utils.PrintUtil;
import siks.utils.ReceiptBuilder;

import java.io.File;
import java.util.List;

public class PrintPreviewController {

    @FXML
    private VBox receiptHolder;
    @FXML private ScrollPane scrollPreview;
    @FXML private Button btnSavePdf;
    @FXML private Button btnPrint;
    @FXML private Button btnClose;
    @FXML private Label lblStatus;

    private Stage stage;
    private Node receiptNode;

    private String invoiceId;
    private String dateTime;
    private String customerName;
    private List<InvoiceItem> items;
    private double prevBalance;
    private double itemsTotal;
    private double totalBill;
    private double payment;
    private double remaining;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setReceipt(Node node,
                           String invoiceId,
                           String dateTime,
                           String customerName,
                           List<InvoiceItem> items,
                           double prevBalance,
                           double itemsTotal,
                           double totalBill,
                           double payment,
                           double remaining) {

        this.receiptNode = node;
        this.invoiceId = invoiceId;
        this.dateTime = dateTime;
        this.customerName = customerName;
        this.items = items;
        this.prevBalance = prevBalance;
        this.itemsTotal = itemsTotal;
        this.totalBill = totalBill;
        this.payment = payment;
        this.remaining = remaining;

        receiptHolder.getChildren().clear();
        receiptHolder.getChildren().add(node);
    }

    @FXML
    private void initialize() {

        btnClose.setOnAction(e -> close());

        // 🔥 PRINT
        btnPrint.setOnAction(e -> {
            try {
                if (receiptNode == null) {
                    lblStatus.setText("No receipt to print");
                    return;
                }

                byte[] escBytes = ReceiptBuilder.buildReceiptBytes2(
                        invoiceId, dateTime, customerName,
                        items, prevBalance, itemsTotal,
                        totalBill, payment, remaining
                );

                boolean ok = PrintUtil.printEscPos(escBytes);

                lblStatus.setText(ok ? "✅ Printed successfully" : "⚠ Print failed");

            } catch (Exception ex) {
                ex.printStackTrace();
                lblStatus.setText("❌ Print error");
            }
        });

        // 🔥 SAVE PDF
        btnSavePdf.setOnAction(e -> {
            boolean ok = PdfUtil.saveNodeAsPdf(receiptNode, invoiceId);
            lblStatus.setText(ok ? "✅ PDF saved in D:/Invoices" : "⚠ PDF failed");
        });

        // 🔥 WHATSAPP BUTTON (NEW FIXED LOGIC)
        Button btnWhatsApp = new Button("Send WhatsApp");

        btnWhatsApp.setOnAction(e -> {
            try {

                if (receiptNode == null) {
                    lblStatus.setText("No receipt for WhatsApp");
                    return;
                }

                // IMPORTANT: force render before snapshot
                receiptNode.applyCss();


                File pdf = PdfUtil.generateInvoicePDF(invoiceId, receiptNode);

                if (pdf != null && pdf.exists()) {

                    // Open file location (simple WhatsApp share flow)
                    new ProcessBuilder(
                            "explorer.exe",
                            "/select,",
                            pdf.getAbsolutePath()
                    ).start();

                    lblStatus.setText("📤 Ready for WhatsApp share");

                } else {
                    lblStatus.setText("❌ PDF generation failed");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                lblStatus.setText("❌ WhatsApp error");
            }
        });

        // Add WhatsApp button dynamically if not in FXML
        if (btnWhatsApp.getParent() == null && btnPrint.getParent() != null) {
            ((HBox) btnPrint.getParent()).getChildren().add(btnWhatsApp);
        }
    }

    private void close() {
        if (stage != null) stage.close();
    }

    // ================= STATIC SHOW METHOD =================

    public static void show(Node receiptNode, String title,
                            String invoiceId, String dateTime, String customerName,
                            List<InvoiceItem> items, double prevBalance,
                            double itemsTotal, double totalBill,
                            double payment, double remaining) {

        ScrollPane scrollPane = new ScrollPane(receiptNode);
        scrollPane.setFitToWidth(true);
        scrollPane.setPadding(new Insets(10));

        Button btnPrint = new Button("Print");
        Button btnSavePdf = new Button("Save PDF");
        Button btnWhatsApp = new Button("WhatsApp");
        Button btnClose = new Button("Close");

        HBox left = new HBox(10, btnPrint, btnSavePdf, btnWhatsApp);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox right = new HBox(10, btnClose);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox footer = new HBox();
        footer.getChildren().addAll(left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        footer.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(scrollPane);
        root.setBottom(footer);

        Stage stage = new Stage(StageStyle.DECORATED);
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(root, 550, 650));

        PrintPreviewController c = new PrintPreviewController();
        c.stage = stage;
        c.receiptNode = receiptNode;
        c.invoiceId = invoiceId;
        c.dateTime = dateTime;
        c.customerName = customerName;
        c.items = items;
        c.prevBalance = prevBalance;
        c.itemsTotal = itemsTotal;
        c.totalBill = totalBill;
        c.payment = payment;
        c.remaining = remaining;

        // PRINT
        btnPrint.setOnAction(e -> {
            try {
                if (receiptNode == null) {
                    showAlert("Print Error", "Receipt not generated.");
                    return;
                }

                byte[] escBytes = ReceiptBuilder.buildReceiptBytes2(
                        invoiceId, dateTime, customerName,
                        items, prevBalance, itemsTotal,
                        totalBill, payment, remaining
                );

                boolean ok = PrintUtil.printEscPos(escBytes);

                if (ok) {
                    showAlert("Success", "Invoice printed successfully.");
                } else {
                    showAlert("Error", "Printer failed to respond.");
                }
                stage.close();

            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Exception", "Printing error: " + ex.getMessage());
            }
        });

        // PDF
        btnSavePdf.setOnAction(e -> {
            try {
                if (receiptNode == null) {
                    showAlert("Error", "Nothing to save as PDF.");
                    return;
                }

                boolean ok = PdfUtil.saveNodeAsPdf(receiptNode, invoiceId);

                if (ok) {
                    showAlert("Success", "PDF saved in D:/Invoices");
                } else {
                    showAlert("Error", "PDF generation failed.");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Exception", ex.getMessage());
            }
        });

        // WHATSAPP (IMPORTANT FIX)
        btnWhatsApp.setOnAction(e -> {

            receiptNode.applyCss();

            File pdf = PdfUtil.generateInvoicePDF(invoiceId, receiptNode);

            if (pdf != null && pdf.exists()) {
                try {
                    new ProcessBuilder("explorer.exe", "/select,", pdf.getAbsolutePath()).start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // CLOSE
        btnClose.setOnAction(e -> stage.close());

        stage.showAndWait();

    }
    private static void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}