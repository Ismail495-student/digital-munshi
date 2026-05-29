package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import siks.models.InvoiceItem;
import siks.utils.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PurchaseInvoiceController {

    @FXML private TextField lblInvoiceId, lblDateTime;
    @FXML private TextField txtSupplier;            // TextField for autocomplete (matches Sales style)
    @FXML private TextField lblCategory, lblLastBilled, lblPrevBalance;
    @FXML private TextField txtItem;                // TextField for autocomplete
    @FXML private TextField txtRate, txtCartons, txtPieces;
    @FXML private Label lblAvailable;
    @FXML
    private TextField txtPerCartonAmount;
    @FXML private TableView<InvoiceItem> tblLineItems;
    @FXML private TableColumn<InvoiceItem, String> colSr, colItemName;
    @FXML private TableColumn<InvoiceItem, Double> colRate, colAmount;
    @FXML private TableColumn<InvoiceItem, Integer> colCartons;
    @FXML private TableColumn<InvoiceItem, Double> colPieces;

    @FXML private TextField txtItemsTotal, txtTotalBill, txtPayment, txtRemaining;
    @FXML private Button btnSave, btnCancel;

    private ObservableList<InvoiceItem> lineItems = FXCollections.observableArrayList();
    private ObservableList<String> supplierNames = FXCollections.observableArrayList();
    private ObservableList<String> itemNames = FXCollections.observableArrayList();

    private ContextMenu supplierMenu = new ContextMenu();
    private ContextMenu itemMenu = new ContextMenu();

    private int currentPPC = 0;
    private boolean isUpdating = false;
    @FXML
    public void initialize() {
        lblDateTime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        lblInvoiceId.setText(generateDisplayInvoiceId("PURCHASE"));

        loadSuppliers();
        loadItems();
        setupIcons();
        setupTableColumns();
        tblLineItems.setItems(lineItems);

        // Supplier autocomplete listeners
        txtSupplier.textProperty().addListener((obs, oldVal, newVal) -> showSupplierMenu(newVal));
        txtSupplier.focusedProperty().addListener((obs, oldV, newV) -> { if (newV) showSupplierMenu(txtSupplier.getText()); });

        // Item autocomplete listeners
        txtItem.textProperty().addListener((obs, oldVal, newVal) -> showItemMenu(newVal));
        txtItem.focusedProperty().addListener((obs, oldV, newV) -> { if (newV) showItemMenu(txtItem.getText()); });
        txtPerCartonAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdating) return;

            try {
                isUpdating = true;

                if (!newVal.isEmpty() && currentPPC != 0) {
                    double cartonRate = Double.parseDouble(newVal);
                    double pieceRate = cartonRate / currentPPC;

                    txtRate.setText(String.format("%.2f", pieceRate));
                } else {
                    txtRate.clear();
                }

            } catch (Exception e) {
                txtRate.clear();
            } finally {
                isUpdating = false;
            }
        });
        // double-click to edit row (load into inputs)
        tblLineItems.setRowFactory(tv -> {
            TableRow<InvoiceItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    InvoiceItem selected = row.getItem();

                    // Move data to input fields


                    // Remove that row from table (optional — for edit-like behavior)
                    lineItems.remove(selected);
                    tblLineItems.refresh();
                    recalcTotals();

                }
            });
            return row;
        });

        txtPieces.setOnAction(e -> addLineItem());
        txtCartons.setOnAction(e -> addLineItem());
        txtPayment.setOnKeyReleased(e -> recalcTotals());
        btnSave.setOnAction(e -> saveInvoice());
        btnCancel.setOnAction(e -> clearAll());
    }

    // ---------------- helpers ----------------
    private String generateDisplayInvoiceId(String type) {
        String sql = "SELECT seq FROM sqlite_sequence WHERE name='invoices'";
        int next = 1;
        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) next = rs.getInt("seq") + 1;
        } catch (Exception e) { }
        return type.equals("PURCHASE") ? String.format("PI%04d", next) : String.format("SI%04d", next);
    }

    private void loadSuppliers() {
        supplierNames.clear();
        String sql = "SELECT name FROM suppliers ORDER BY name";
        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) supplierNames.add(rs.getString("name"));
        } catch (Exception e) { e.printStackTrace(); }

        // keep menu initial items in case needed elsewhere
        supplierMenu.getItems().clear();
        for (String n : supplierNames) {
            MenuItem mi = new MenuItem(n);
            mi.setOnAction(ev -> { txtSupplier.setText(n); onSupplierSelect(); });
            supplierMenu.getItems().add(mi);
        }
    }

    private void loadItems() {
        itemNames.clear();
        String sql = "SELECT name FROM items ORDER BY name";
        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) itemNames.add(rs.getString("name"));
        } catch (Exception e) { e.printStackTrace(); }

        itemMenu.getItems().clear();
        for (String n : itemNames) {
            MenuItem mi = new MenuItem(n);
            mi.setOnAction(ev -> { txtItem.setText(n); onItemSelect(); });
            itemMenu.getItems().add(mi);
        }
    }

    // ---------------- supplier suggestion popup ----------------
    private void showSupplierMenu(String filterText) {
        supplierMenu.getItems().clear();

        ObservableList<String> filtered = supplierNames.filtered(s -> s.toLowerCase().contains((filterText==null?"":filterText).toLowerCase()));
        if (filtered.isEmpty()) return;

        ListView<String> listView = new ListView<>(filtered);
        listView.setPrefWidth(txtSupplier.getWidth());


        listView.setOnMouseClicked(e -> {
            String sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                txtSupplier.setText(sel);
                txtSupplier.positionCaret(sel.length());
                supplierMenu.hide();
                onSupplierSelect();
            }
        });

        listView.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> {
                    String sel = listView.getSelectionModel().getSelectedItem();
                    if (sel != null) {
                        txtSupplier.setText(sel);
                        txtSupplier.positionCaret(sel.length());
                        supplierMenu.hide();
                        onSupplierSelect();
                    }
                    e.consume();
                }
                case UP, DOWN -> listView.requestFocus();
            }
        });

        ScrollPane scroll = new ScrollPane(listView);
        scroll.setFitToWidth(true);
        CustomMenuItem c = new CustomMenuItem(scroll, false);
        supplierMenu.getItems().add(c);

        Bounds b = txtSupplier.localToScreen(txtSupplier.getBoundsInLocal());
        supplierMenu.show(txtSupplier, b.getMinX(), b.getMaxY());

        listView.requestFocus();
        if (!listView.getItems().isEmpty()) listView.getSelectionModel().selectFirst();
    }

    // ---------------- item suggestion popup ----------------
    private void showItemMenu(String filterText) {
        itemMenu.getItems().clear();

        ObservableList<String> filtered = itemNames.filtered(s -> s.toLowerCase().contains((filterText==null?"":filterText).toLowerCase()));
        if (filtered.isEmpty()) return;

        ListView<String> listView = new ListView<>(filtered);
        listView.setPrefWidth(txtItem.getWidth());

        listView.setOnMouseClicked(e -> {
            String sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                txtItem.setText(sel);
                txtItem.positionCaret(sel.length());
                itemMenu.hide();
                onItemSelect();
            }
        });

        listView.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> {
                    String sel = listView.getSelectionModel().getSelectedItem();
                    if (sel != null) {
                        txtItem.setText(sel);
                        txtItem.positionCaret(sel.length());
                        itemMenu.hide();
                        onItemSelect();
                    }
                    e.consume();
                }
                case UP, DOWN -> listView.requestFocus();
            }
        });

        ScrollPane scroll = new ScrollPane(listView);
        scroll.setFitToWidth(true);
        CustomMenuItem c = new CustomMenuItem(scroll, false);
        itemMenu.getItems().add(c);

        Bounds b = txtItem.localToScreen(txtItem.getBoundsInLocal());
        itemMenu.show(txtItem, b.getMinX(), b.getMaxY());

        listView.requestFocus();
        if (!listView.getItems().isEmpty()) listView.getSelectionModel().selectFirst();
    }

    // ---------------- supplier selection ----------------
    private void onSupplierSelect() {
        String name = txtSupplier.getText();
        if (name == null || name.isEmpty()) return;

        String sql = "SELECT category, lastBilled, prevBalance FROM suppliers WHERE name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblCategory.setText(rs.getString("category"));
                    String last = rs.getString("lastBilled");
                    lblLastBilled.setText(last == null || last.isEmpty() ? "-" : last);
                    lblPrevBalance.setText(String.valueOf(rs.getDouble("prevBalance")));
                    recalcTotals();
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ---------------- item selection ----------------
    private void onItemSelect() {
        String itemName = txtItem.getText();
        if (itemName == null || itemName.isEmpty()) return;

        String sql = "SELECT purchaseRate, stockQuantity, piecesPerCarton FROM items WHERE name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtRate.setText(String.valueOf(rs.getDouble("purchaseRate")));
                    lblAvailable.setText(String.valueOf(rs.getDouble("stockQuantity")));
                } else lblAvailable.setText("-");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ---------------- table setup ----------------
    private void setupTableColumns() {
        colSr.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.valueOf(tblLineItems.getItems().indexOf(d.getValue()) + 1)));
        colItemName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getItemName()));
        colRate.setCellValueFactory(d -> new javafx.beans.property.SimpleDoubleProperty(d.getValue().getRate()).asObject());
        colCartons.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getCartons()).asObject());
        colPieces.setCellValueFactory(d -> new javafx.beans.property.SimpleDoubleProperty(d.getValue().getPieces()).asObject());
        colAmount.setCellValueFactory(d -> new javafx.beans.property.SimpleDoubleProperty(d.getValue().getAmount()).asObject());
    }

    // ---------------- add line ----------------
    private void addLineItem() {
        if (ItemHandler.getItemByName(txtItem.getText()) == null) {
            showAlert(Alert.AlertType.ERROR,"Not Exists","Item does not exist in system");
            return;
        }
        try {
            String itemName = txtItem.getText().trim();
            String printName ;

            if (AppSettings.isUrdu()) {
                printName = ItemHandler.getPrintName(itemName);
            } else {
                printName = itemName;
            } // get printName here
            double rate = Double.parseDouble(txtRate.getText().trim());
            int cartons = txtCartons.getText().trim().isEmpty() ? 0 : Integer.parseInt(txtCartons.getText().trim());
            double pieces = txtPieces.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtPieces.getText().trim());

            int ppc = 1;
            String sql = "SELECT piecesPerCarton FROM items WHERE name = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, itemName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) ppc = rs.getInt("piecesPerCarton");
                }
            }

            double totalPieces = cartons * ppc + pieces;
            double amount = rate * totalPieces;

            // use printName in InvoiceItem so receipts will show printName
            InvoiceItem row = new InvoiceItem(null, printName, rate, cartons, pieces, amount);
            lineItems.add(row);
            recalcTotals();

            txtItem.clear();
            txtRate.clear();
            txtCartons.clear();
            txtPieces.clear();
            lblAvailable.setText("-");
            txtItem.requestFocus();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Invalid input", "Please check item, rate and quantity fields.");
        }
    }

    // ---------------- totals ----------------
    private void recalcTotals() {
        double sum = lineItems.stream().mapToDouble(InvoiceItem::getAmount).sum();
        txtItemsTotal.setText(String.format("%.2f", sum));

        double prev = 0;
        try { prev = Double.parseDouble(lblPrevBalance.getText()); } catch (Exception ignored) {}
        double totalBill = prev + sum;
        txtTotalBill.setText(String.format("%.2f", totalBill));

        double payment = 0;
        try { payment = Double.parseDouble(txtPayment.getText()); } catch (Exception ignored) {}
        double remaining = totalBill - payment;
        txtRemaining.setText(String.format("%.2f", remaining));
    }

    // ---------------- save (with stock increment) ----------------
    private void saveInvoice() {
        String supplier = txtSupplier.getText().trim();
        if (supplier.isEmpty() || lineItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing data", "Supplier and at least one item are required.");
            return;
        }

        double payment = 0;
        try {
            payment = Double.parseDouble(txtPayment.getText().trim());
        } catch (Exception ignored) {}

        List<InvoiceItem> items = new ArrayList<>(lineItems);

        // 🔹 Save invoice (InvoiceHandler will handle stock increment automatically)
        boolean ok = InvoiceHandler.saveInvoice("PURCHASE", supplier, items, payment);

        if (ok) {
            // 🔸 Ask if user wants to print purchase invoice
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to print the invoice?", ButtonType.YES, ButtonType.NO);
            a.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    // 🧾 Build printable purchase invoice
                    Node receipt = ReceiptBuilder.buildReceiptNode(
                            lblInvoiceId.getText(),
                            lblDateTime.getText(),
                            txtSupplier.getText(),
                            new ArrayList<>(lineItems),
                            Double.parseDouble(lblPrevBalance.getText()),
                            Double.parseDouble(txtItemsTotal.getText()),
                            Double.parseDouble(txtTotalBill.getText()),
                            txtPayment.getText().isEmpty() ? 0.0 : Double.parseDouble(txtPayment.getText()),
                            Double.parseDouble(txtRemaining.getText())
                    );

                    // 🖨 Show Print Preview Window (Purchase version)
                    PrintPreviewController.show(
                            receipt,
                            "Purchase Invoice",
                            lblInvoiceId.getText(),
                            lblDateTime.getText(),
                            txtSupplier.getText(),
                            new ArrayList<>(lineItems),
                            Double.parseDouble(lblPrevBalance.getText()),
                            Double.parseDouble(txtItemsTotal.getText()),
                            Double.parseDouble(txtTotalBill.getText()),
                            txtPayment.getText().isEmpty() ? 0.0 : Double.parseDouble(txtPayment.getText()),
                            Double.parseDouble(txtRemaining.getText())
                    );
                }
            });

            showAlert(Alert.AlertType.INFORMATION, "Saved", "Purchase invoice saved successfully.");
            clearAll();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save purchase invoice.");
        }
    }

    private void clearAll() {
        txtSupplier.clear();
        lblCategory.setText("-");
        lblLastBilled.setText("-");
        lblPrevBalance.setText("0.0");
        lineItems.clear();
        txtItemsTotal.clear();
        txtTotalBill.clear();
        txtPayment.clear();
        txtRemaining.clear();
        lblDateTime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        lblInvoiceId.setText(generateDisplayInvoiceId("PURCHASE"));
    }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void setupIcons() {
        setButtonIcon(btnSave,"/icons/save.png");
        setButtonIcon(btnCancel,"/icons/cancel.png");
    }

    private void setButtonIcon(Button button, String path) {
        try{
            Image img = new Image(getClass().getResourceAsStream(path));
            ImageView view = new ImageView(img);
            view.setFitWidth(18); view.setFitHeight(18);
            button.setGraphic(view); button.setContentDisplay(ContentDisplay.LEFT);
        } catch(Exception e){ System.err.println("⚠ Icon not found: "+path); }
    }
}
