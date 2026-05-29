package siks.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import siks.models.Item;
import siks.models.InvoiceItem;
import siks.utils.*;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.text.Font;

public class SalesInvoiceController {


    @FXML
    private TextField txtCustomer; // TextField instead of ComboBox
    @FXML private TextField lblInvoiceId;
    @FXML private TextField lblDateTime;
    @FXML private TextField lblPhone;
    @FXML private TextField lblAddress;
    @FXML private TextField lblCustomerCategory;
    @FXML private TextField lblLastBilled;
    @FXML private TextField lblPrevBalance;
    @FXML
    private Button btnRefresh;
    @FXML
    private TextField txtItem; // TextField instead of ComboBox
    @FXML
    private TextField txtRate, txtCartons, txtPieces;
    @FXML
    private Label lblAvailable;
    @FXML
    private TextField txtPerCartonAmount;
    @FXML
    private RadioButton rbNetCash, rbManualPayment;
    private ToggleGroup paymentModeGroup = new ToggleGroup();
    @FXML
    private TableView<InvoiceItem> tblLineItems;
    @FXML
    private TableColumn<InvoiceItem, String> colSr, colItemName;
    @FXML
    private TableColumn<InvoiceItem, Double> colRate, colAmount;
    @FXML
    private TableColumn<InvoiceItem, Integer> colCartons;
    @FXML
    private TableColumn<InvoiceItem, Double> colPieces;

    @FXML
    private TextField txtItemsTotal, txtTotalBill, txtPayment, txtRemaining;
    @FXML
    private Button btnSave, btnCancel;

    private ObservableList<InvoiceItem> lineItems = FXCollections.observableArrayList();
    private ObservableList<String> customerNames = FXCollections.observableArrayList();
    private ObservableList<String> itemNames = FXCollections.observableArrayList();

    private ContextMenu customerMenu = new ContextMenu();
    private ContextMenu itemMenu = new ContextMenu();
    @FXML
    private VBox topRightPane; // ye wahi fx:id jo FXML me hai
    private int currentPPC = 0;
    private boolean isUpdating = false;

    @FXML
    public void initialize() {
        lblDateTime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        lblInvoiceId.setText(generateDisplayInvoiceId("SALE"));
        setupIcons();
        loadCustomers();
        loadItems();
        setupTableColumns();
        tblLineItems.setItems(lineItems);
        btnRefresh.setOnAction(e -> refreshData());
        txtCustomer.requestFocus();
        // Customer TextField listener
        txtCustomer.textProperty().addListener((obs, oldVal, newVal) -> showCustomerMenu(newVal));
        new MiniPrintHandler();

        rbNetCash.setToggleGroup(paymentModeGroup);
        rbManualPayment.setToggleGroup(paymentModeGroup);
        paymentModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            applyPaymentMode();
        });

        // payment field listener
        txtPayment.setOnKeyReleased(e -> recalcTotals());

        rbManualPayment.setSelected(true);
        // Piece → Carton
        txtRate.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdating) return;

            try {
                isUpdating = true;

                if (!newVal.isEmpty() && currentPPC != 0) {
                   String name= txtItem.getText();
                    currentPPC=ItemHandler.getPiecesPerCarton(name);
                    double pieceRate = Double.parseDouble(newVal);
                    double cartonRate = pieceRate * currentPPC;

                    txtPerCartonAmount.setText(String.format("%.2f", cartonRate));
                } else {
                    txtPerCartonAmount.clear();
                }

            } catch (Exception e) {
                txtPerCartonAmount.clear();
            } finally {
                isUpdating = false;
            }
        });


// Carton → Piece
        txtPerCartonAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdating) return;

            try {
                isUpdating = true;

                if (!newVal.isEmpty() && currentPPC != 0) {
                    currentPPC=ItemHandler.getPiecesPerCarton(txtItem.getText());
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

        // Item TextField listener
        txtItem.textProperty().addListener((obs, oldVal, newVal) -> showItemMenu(newVal));
// 👇 Double-click to load selected row into input fields
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
        txtPerCartonAmount.setOnAction(e -> txtCartons.requestFocus());

        // Mini Print Pass ko add karo
        Node miniPrintPassNode = MiniPrintHandler.buildMiniPrintNode();
        topRightPane.getChildren().clear();
        topRightPane.getChildren().add(miniPrintPassNode);


    }


    private String generateDisplayInvoiceId(String type) {
        String url = "jdbc:sqlite:database.db";
        String sql = "SELECT seq FROM sqlite_sequence WHERE name='invoices'";
        int next = 1;
        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) next = rs.getInt("seq") + 1;
        } catch (Exception e) {
        }
        return type.equals("SALE") ? String.format("SI%04d", next) : String.format("PI%04d", next);
    }

    // ---------------------- Customers ----------------------
    private void loadCustomers() {
        customerNames.clear();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM customers")) {
            while (rs.next()) customerNames.add(rs.getString("name"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        customerMenu.getItems().clear();
        for (String name : customerNames) {
            MenuItem item = new MenuItem(name);
            item.setOnAction(e -> {
                txtCustomer.setText(name);
                onCustomerSelect();
            });
            customerMenu.getItems().add(item);
        }
    }

    private void filterCustomerMenu(String text) {
        customerMenu.getItems().clear();
        for (String name : customerNames) {
            if (name.toLowerCase().contains(text.toLowerCase())) {
                MenuItem item = new MenuItem(name);
                item.setOnAction(e -> {
                    txtCustomer.setText(name);
                    onCustomerSelect();
                });
                customerMenu.getItems().add(item);
            }
        }
        if (!customerMenu.getItems().isEmpty()) showCustomerMenu(text);
    }

    private void showCustomerMenu(String filterText) {
        customerMenu.getItems().clear();

        ObservableList<String> filtered = customerNames.filtered(name ->
                name.toLowerCase().contains(filterText.toLowerCase())
        );
        if (filtered.isEmpty()) return;

        ListView<String> listView = new ListView<>(filtered);
        listView.setPrefWidth(txtCustomer.getWidth());

        // 🖱 Mouse click selection
        listView.setOnMouseClicked(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                txtCustomer.setText(selected);
                txtCustomer.positionCaret(selected.length());
                customerMenu.hide();
                onCustomerSelect();
            }
        });

        // ⌨ Keyboard navigation + enter selection
        listView.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> {
                    String selected = listView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        txtCustomer.setText(selected);
                        txtCustomer.positionCaret(selected.length());
                        customerMenu.hide();
                        onCustomerSelect();
                    }
                    e.consume();
                }
                case UP, DOWN -> {
                    // Let ListView handle navigation
                    listView.requestFocus();
                }
            }
        });

        ScrollPane scroll = new ScrollPane(listView);
        scroll.setFitToWidth(true);
        CustomMenuItem customItem = new CustomMenuItem(scroll, false);
        customerMenu.getItems().add(customItem);

        Bounds bounds = txtCustomer.localToScreen(txtCustomer.getBoundsInLocal());
        customerMenu.show(txtCustomer, bounds.getMinX(), bounds.getMaxY());

        // Automatically focus list so arrow keys work immediately
        listView.requestFocus();
        if (!listView.getItems().isEmpty()) listView.getSelectionModel().selectFirst();
    }


    private void onCustomerSelect() {
        String name = txtCustomer.getText();
        if (name == null || name.isEmpty()) return;

        String sql = "SELECT category, lastBilled, prevBalance, address, phone FROM customers WHERE name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblCustomerCategory.setText(rs.getString("category"));
                    lblPhone.setText(rs.getString("phone"));
                    lblAddress.setText(rs.getString("address"));

                    // 🔹 Calculate days ago
                    String lastBilledStr = rs.getString("lastBilled");
                    if (lastBilledStr == null || lastBilledStr.isEmpty()) {
                        lblLastBilled.setText("-");
                    } else {
                        LocalDateTime lastDate = LocalDateTime.parse(lastBilledStr + "T00:00:00");
                        long daysAgo = java.time.Duration.between(lastDate, LocalDateTime.now()).toDays();
                        lblLastBilled.setText(daysAgo + " days ago");
                    }
                    lblPrevBalance.setText(String.format("%.2f", rs.getDouble("prevBalance")));
                    recalcTotals();
                }
                txtItem.requestFocus();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    // ---------------------- Items ----------------------
    private void loadItems() {
        itemNames.clear();
        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM items ORDER BY name")) {
            while (rs.next()) itemNames.add(rs.getString("name"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        itemMenu.getItems().clear();
        for (String name : itemNames) {
            MenuItem item = new MenuItem(name);
            item.setOnAction(e -> {
                txtItem.setText(name);
                onItemSelect();
            });
            itemMenu.getItems().add(item);
        }
    }

    private void filterItemMenu(String text) {
        itemMenu.getItems().clear();
        for (String name : itemNames) {
            if (name.toLowerCase().contains(text.toLowerCase())) {
                MenuItem item = new MenuItem(name);
                item.setOnAction(e -> {
                    txtItem.setText(name);
                    onItemSelect();
                });
                itemMenu.getItems().add(item);
            }
        }
        if (!itemMenu.getItems().isEmpty()) showItemMenu(text);
    }

    private void showItemMenu(String filterText) {
        itemMenu.getItems().clear();

        ObservableList<String> filtered = itemNames.filtered(name ->
                name.toLowerCase().contains(filterText.toLowerCase())
        );
        if (filtered.isEmpty()) return;

        ListView<String> listView = new ListView<>(filtered);

        listView.setPrefWidth(txtItem.getWidth());

        // 🖱 Mouse click selection
        listView.setOnMouseClicked(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                txtItem.setText(selected);
                txtItem.positionCaret(selected.length());
                itemMenu.hide();
                onItemSelect();
            }
        });

        // ⌨ Keyboard navigation + enter selection
        listView.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> {
                    String selected = listView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        txtItem.setText(selected);
                        txtItem.positionCaret(selected.length());
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
        CustomMenuItem customItem = new CustomMenuItem(scroll, false);
        itemMenu.getItems().add(customItem);

        Bounds bounds = txtItem.localToScreen(txtItem.getBoundsInLocal());
        itemMenu.show(txtItem, bounds.getMinX(), bounds.getMaxY());

        // Automatically focus list
        listView.requestFocus();
        if (!listView.getItems().isEmpty()) listView.getSelectionModel().selectFirst();
    }


    private void onItemSelect() {
        String itemName = txtItem.getText();
        if (itemName == null || itemName.isEmpty()) return;
        String sql = "SELECT purchaseRate, retailRate, wholesaleRate, stockQuantity, piecesPerCarton FROM items WHERE name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {

                    double retail = rs.getDouble("retailRate");
                    double wholesale = rs.getDouble("wholesaleRate");
                    double stock = rs.getDouble("stockQuantity");
                    currentPPC = rs.getInt("piecesPerCarton");
                    lblAvailable.setText(String.valueOf(stock));

                    if (stock > 0) {

                        lblAvailable.setStyle(
                                "-fx-text-fill: green; -fx-font-weight: bold;"
                        );

                        lblAvailable.setOpacity(1);

                    } else {

                        lblAvailable.setStyle(
                                "-fx-text-fill: red; -fx-font-weight: bold;"
                        );

                        javafx.animation.Timeline blink =
                                new javafx.animation.Timeline(

                                        new javafx.animation.KeyFrame(
                                                javafx.util.Duration.seconds(0),
                                                e -> lblAvailable.setOpacity(1)
                                        ),

                                        new javafx.animation.KeyFrame(
                                                javafx.util.Duration.seconds(0.5),
                                                e -> lblAvailable.setOpacity(0)
                                        ),

                                        new javafx.animation.KeyFrame(
                                                javafx.util.Duration.seconds(1),
                                                e -> lblAvailable.setOpacity(1)
                                        )
                                );

                        blink.setCycleCount(
                                javafx.animation.Animation.INDEFINITE
                        );

                        blink.play();
                    }                    String custCat = lblCustomerCategory.getText();
                    txtRate.setText(custCat.equalsIgnoreCase("Wholesaler") ? String.valueOf(wholesale) : String.valueOf(retail));
                } else lblAvailable.setText("-");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ---------------------- Table ----------------------
    private void setupTableColumns() {
        colSr.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(tblLineItems.getItems().indexOf(data.getValue()) + 1)));
        colItemName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getItemName()));
        colRate.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getRate()).asObject());
        colCartons.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getCartons()).asObject());
        colPieces.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getPieces()).asObject());
        colAmount.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getAmount()).asObject());
    }

    // ---------------------- Add Line ----------------------
    private void addLineItem() {
        if (ItemHandler.getItemByName(txtItem.getText()) == null) {
            showAlert(Alert.AlertType.ERROR,"Not Exists","Item does not exist in system");
            return;
        }
        try {

            String itemName = txtItem.getText().trim();
            String printName;

            if (AppSettings.isUrdu()) {
                printName = ItemHandler.getPrintName(itemName);
            } else {
                printName = itemName;
            }
            double rate = Double.parseDouble(txtRate.getText().trim());
            int cartons = txtCartons.getText().trim().isEmpty() ? 0 : Integer.parseInt(txtCartons.getText().trim());
            double pieces = txtPieces.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtPieces.getText().trim());

            int ppc = 1;
            String sql = "SELECT piecesPerCarton FROM items WHERE name = ? OR printName = ? Limit 1";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, itemName);
                ps.setString(2, printName);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) ppc = rs.getInt("piecesPerCarton");

                }
            }
            double totalPieces = cartons * ppc + pieces;
            double amount = Double.parseDouble((String.format("%.2f",rate * totalPieces)));

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

    // ===================== 4) recalcTotals() replace karo =====================
    private void recalcTotals() {

        double sum = 0;

        for (InvoiceItem it : lineItems) {
            sum += it.getAmount();
        }

        txtItemsTotal.setText(String.format("%.2f", sum));

        double prev = 0;

        try {
            prev = Double.parseDouble(lblPrevBalance.getText());
        } catch (Exception ignored) {
        }

        double totalBill = prev + sum;

        txtTotalBill.setText(String.format("%.2f", totalBill));

        // Net Cash mode me payment auto update hogi
        if (rbNetCash.isSelected()) {
            txtPayment.setText(String.format("%.2f", sum));
        }

        double payment = 0;

        try {
            payment = Double.parseDouble(txtPayment.getText());
        } catch (Exception ignored) {
        }

        txtRemaining.setText(String.format("%.2f", totalBill - payment));
    }

    private void saveInvoice() {
        String customer = txtCustomer.getText().trim();
        if (customer.isEmpty() || lineItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing data", "Customer and at least one item are required.");
            return;
        }
        double payment = 0;
        try {
            payment = Double.parseDouble(txtPayment.getText().trim());
        } catch (Exception ignored) {
        }

        List<InvoiceItem> items = new ArrayList<>(lineItems);
        boolean ok = InvoiceHandler.saveInvoice("SALE", customer, items, payment);
        if (ok) {

            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to print the invoice?", ButtonType.YES, ButtonType.NO);
            a.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    // Print logic placeholder
                    Node receipt = ReceiptBuilder.buildReceiptNode(
                            lblInvoiceId.getText(),
                            lblDateTime.getText(),
                            txtCustomer.getText(),
                            new ArrayList<>(lineItems),
                            Double.parseDouble(lblPrevBalance.getText()),
                            Double.parseDouble(txtItemsTotal.getText()),
                            Double.parseDouble(txtTotalBill.getText()),
                            txtPayment.getText().isEmpty() ? 0.0 : Double.parseDouble(txtPayment.getText()),
                            Double.parseDouble(txtRemaining.getText())
                    );


                    // 🔹 Show Print Preview Window
                    PrintPreviewController.show(
                            receipt,
                            "Sales Invoice",
                            lblInvoiceId.getText(),
                            lblDateTime.getText(),
                            txtCustomer.getText(),
                            new ArrayList<>(lineItems),
                            Double.parseDouble(lblPrevBalance.getText()),
                            Double.parseDouble(txtItemsTotal.getText()),
                            Double.parseDouble(txtTotalBill.getText()),
                            txtPayment.getText().isEmpty() ? 0.0 : Double.parseDouble(txtPayment.getText()),
                            Double.parseDouble(txtRemaining.getText())
                    );

                }
            });




            showAlert(Alert.AlertType.INFORMATION, "Saved", "Invoice saved successfully.");
            clearAll();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save invoice.");
        }
    }

    private void clearAll() {
        txtCustomer.clear();
        lblCustomerCategory.setText("-");
        lblLastBilled.setText("-");
        lblPrevBalance.setText("0.0");
        lineItems.clear();
        txtItemsTotal.clear();
        txtTotalBill.clear();
        txtPayment.clear();
        txtRemaining.clear();
        lblDateTime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        lblInvoiceId.setText(generateDisplayInvoiceId("SALE"));
        rbManualPayment.setSelected(true);
        txtPayment.setEditable(true);
    }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void setupIcons() {
        setButtonIcon(btnSave, "/icons/save.png");
        setButtonIcon(btnCancel, "/icons/cancel.png");

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

    private void showSuggestions(TextField field, ObservableList<String> suggestions) {
        if (suggestions.isEmpty()) return;

        ContextMenu menu = new ContextMenu();
        ListView<String> listView = new ListView<>(suggestions);
        listView.setMaxHeight(150); // max height
        listView.setPrefWidth(field.getWidth()); // align width with TextField

        listView.setOnMouseClicked(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                field.setText(selected);
                field.positionCaret(selected.length());
                menu.hide();
                if (field == txtCustomer) onCustomerSelect();
                else if (field == txtItem) onItemSelect();
            }
        });

        ScrollPane scroll = new ScrollPane(listView);
        scroll.setFitToWidth(true);
        CustomMenuItem customItem = new CustomMenuItem(scroll, false);
        menu.getItems().add(customItem);

        // position below field
        Bounds bounds = field.localToScreen(field.getBoundsInLocal());
        menu.show(field, bounds.getMinX(), bounds.getMaxY());
    }

    private void calculatePerCartonAmount() {
        try {
            double rate = Double.parseDouble(txtRate.getText().trim());
            double perCarton = rate * currentPPC;

            txtPerCartonAmount.setText(String.format("%.2f", perCarton));
        } catch (Exception e) {
            txtPerCartonAmount.clear();
        }
    }
    // ===================== 3) New Method add karo =====================
    private void applyPaymentMode() {

        if (rbNetCash.isSelected()) {

            // user manual edit na kare
            txtPayment.setEditable(false);

            // current total payment me daal do
            txtPayment.setText(txtTotalBill.getText());

        } else {

            // manual mode
            txtPayment.setEditable(true);

            // payment clear bhi kar sakte ho (optional)
            txtPayment.clear();
        }

        recalcTotals();
    }
    private void refreshData() {
        loadCustomers();
        loadItems();
        onCustomerSelect();
    }
}
