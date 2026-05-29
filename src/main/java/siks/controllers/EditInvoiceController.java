package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import org.w3c.dom.Text;
import siks.models.InvoiceItem;
import siks.utils.DBUtil;
import siks.utils.EditInvoiceHandler;
import siks.utils.ItemHandler;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class EditInvoiceController {

    // ================= TOP =================
    @FXML private TextField txtCustomer;
    @FXML private Button btnLoadLatest;

    @FXML private TextField lblInvoiceId;
    @FXML private TextField lblDateTime;

    // ================= ITEM ENTRY =================
    @FXML private TextField txtItem,lblCustomerCategory,lblAddress,lblPhone;
    @FXML private TextField txtRate;
    @FXML private TextField txtCartons, txtPerCartonAmount;
    @FXML private TextField txtPieces;
    @FXML private Label lblAvailable;


    // ================= TOTALS =================
    @FXML private TextField lblPrevBalance;
    @FXML private TextField lblItemsTotal;
    @FXML private TextField lblTotalBill;
    @FXML private TextField txtPayment;
    @FXML private TextField lblRemaining;

    // ================= TABLE =================
    @FXML private TableView<InvoiceItem> tblItems;
    @FXML private TableColumn<InvoiceItem,String> colItem;
    @FXML private TableColumn<InvoiceItem,Double> colRate;
    @FXML private TableColumn<InvoiceItem,Integer> colCartons;
    @FXML private TableColumn<InvoiceItem,Double> colPieces;
    @FXML private TableColumn<InvoiceItem,Double> colAmount;

    @FXML private Button btnSave;

    // ================= DATA =================
    private ObservableList<String> customers = FXCollections.observableArrayList();
    private ObservableList<String> itemNames = FXCollections.observableArrayList();

    private ObservableList<InvoiceItem> lineItems =
            FXCollections.observableArrayList();

    private ContextMenu customerMenu = new ContextMenu();
    private ContextMenu itemMenu = new ContextMenu();

    private int currentInvoiceId = 0;
    private double oldRemaining = 0;
    private int currentPPC = 1;
    private boolean isUpdating = false;

    // ============================================
    @FXML
    public void initialize() {
        // 🔥 AUTO FOCUS (SCREEN OPEN)
        javafx.application.Platform.runLater(() -> {
            txtCustomer.requestFocus();
        });
        tblItems.setItems(lineItems);

        setupTable();
        loadCustomers();
        loadItems();

        txtCustomer.textProperty().addListener((a,b,c)->
                showCustomerMenu(c));

        txtItem.textProperty().addListener((a,b,c)->
                showItemMenu(c));

        btnLoadLatest.setOnAction(e -> loadLatestInvoice());


        txtPayment.textProperty().addListener((a,b,c)->recalc());
        txtRate.textProperty().addListener((o, a, b) -> syncPerCarton());
        txtPerCartonAmount.textProperty().addListener((o, a, b) -> syncPerPiece());
        txtPerCartonAmount.setOnAction(e->txtCartons.requestFocus());
        btnSave.setOnAction(e -> saveChanges());

        tblItems.setRowFactory(tv -> {
            TableRow<InvoiceItem> row = new TableRow<>();

            row.setOnMouseClicked(e -> {
                if(e.getClickCount()==2 && !row.isEmpty()) {

                    InvoiceItem it = row.getItem();

                    lineItems.remove(it);
                    recalc();
                }
            });

            return row;
        });
        txtCustomer.setOnAction(e->loadLatestInvoice());
        txtCartons.setOnAction(e->addLine());
        txtPieces.setOnAction(e->addLine());

    }

    // ============================================
    private void loadCustomers() {

        try(Connection con = DBUtil.getConnection();
            Statement st = con.createStatement();
            ResultSet rs =
                    st.executeQuery("SELECT name FROM customers")) {

            customers.clear();

            while(rs.next())
                customers.add(rs.getString(1));

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void loadItems() {

        try(Connection con = DBUtil.getConnection();
            Statement st = con.createStatement();
            ResultSet rs =
                    st.executeQuery("SELECT name FROM items")) {

            itemNames.clear();

            while(rs.next())
                itemNames.add(rs.getString(1));

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    // ============================================
    private void showCustomerMenu(String filterText) {
        customerMenu.getItems().clear();

        ObservableList<String> filtered = customers.filtered(name ->
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
                loadCustomerData();
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
                        loadCustomerData();
                        loadLatestInvoice();
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
        customerMenu.setMaxHeight(100);
        // Automatically focus list so arrow keys work immediately
        listView.requestFocus();
        if (!listView.getItems().isEmpty()) listView.getSelectionModel().selectFirst();
    }

    // ============================================
    private void showItemMenu(String filter) {

        itemMenu.getItems().clear();

        if (filter == null) return;

        ListView<String> listView =
                new ListView<>(itemNames.filtered(
                        x -> x.toLowerCase().contains(filter.toLowerCase())
                ));

        listView.setPrefHeight(120);

        // ================= MOUSE CLICK =================
        listView.setOnMouseClicked(e -> {
            String val = listView.getSelectionModel().getSelectedItem();
            if (val != null) {
                selectItem(val);
            }
        });

        // ================= KEYBOARD SUPPORT (FIX 🔥) =================
        listView.setOnKeyPressed(e -> {
            switch (e.getCode()) {

                case ENTER -> {
                    String val = listView.getSelectionModel().getSelectedItem();
                    if (val != null) {
                        selectItem(val);
                    }
                    e.consume();
                }

                case UP, DOWN -> {
                    listView.requestFocus(); // enable navigation
                }
            }
        });

        itemMenu.getItems().clear();
        itemMenu.getItems().add(new CustomMenuItem(listView, false));

        Bounds b = txtItem.localToScreen(txtItem.getBoundsInLocal());

        itemMenu.show(txtItem, b.getMinX(), b.getMaxY());

        // auto focus so keyboard works immediately
        listView.requestFocus();

        if (!listView.getItems().isEmpty()) {
            listView.getSelectionModel().selectFirst();
        }
    }
    private void selectItem(String val) {

        txtItem.setText(val);
        txtItem.positionCaret(val.length());

        itemMenu.hide();

        loadItemData();
    }

    // ============================================
    private void loadItemData() {

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps =
                     con.prepareStatement(
                             "SELECT retailRate, wholesaleRate, stockQuantity, piecesPerCarton " +
                                     "FROM items WHERE name=?")) {

            ps.setString(1, txtItem.getText());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                currentPPC = rs.getInt("piecesPerCarton");
                double stock = rs.getDouble("stockQuantity");

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
                }
                String category = lblCustomerCategory.getText();

                double rate;

                if ("Wholesaler".equalsIgnoreCase(category)) {
                    rate = rs.getDouble("wholesaleRate");
                } else {
                    rate = rs.getDouble("retailRate");
                }

                txtRate.setText(String.valueOf(rate));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================
    private void loadLatestInvoice() {

        lineItems.clear();

        try(Connection con = DBUtil.getConnection()) {

            PreparedStatement ps =
                    con.prepareStatement(
                            "SELECT * FROM invoices " +
                                    "WHERE partyName=? " +
                                    "ORDER BY id DESC LIMIT 1");

            ps.setString(1, txtCustomer.getText());

            ResultSet rs = ps.executeQuery();

            if(!rs.next()) {
                show("No invoice found");
                return;
            }

            currentInvoiceId = rs.getInt("id");

            lblInvoiceId.setText(
                    "SI" + String.format("%04d",
                            currentInvoiceId));

            lblDateTime.setText(
                    rs.getString("dateTime"));

            double totalBill =
                    rs.getDouble("totalBill");

            double payment =
                    rs.getDouble("payment");

            oldRemaining =
                    rs.getDouble("remaining");

            txtPayment.setText("" + payment);

            PreparedStatement ps2 =
                    con.prepareStatement(
                            "SELECT * FROM invoice_items " +
                                    "WHERE invoice_id=?");

            ps2.setInt(1, currentInvoiceId);

            ResultSet rs2 = ps2.executeQuery();

            double itemTotal = 0;

            while(rs2.next()) {

                InvoiceItem it =
                        new InvoiceItem(
                                null,
                                rs2.getString("itemName"),
                                rs2.getDouble("rate"),
                                rs2.getInt("cartons"),
                                rs2.getDouble("pieces"),
                                rs2.getDouble("amount")
                        );

                lineItems.add(it);
                itemTotal += it.getAmount();
            }

            double prev =
                    totalBill - itemTotal;

            lblPrevBalance.setText(
                    String.format("%.2f", prev));

            recalc();
            loadCustomerData();

        } catch(Exception e){
            e.printStackTrace();
            show("Load failed");
        }
    }

    private void loadCustomerData() {
        String name=txtCustomer.getText();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT  category, phone, address FROM customers WHERE name=?")) {

            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return;

            lblCustomerCategory.setText(rs.getString("category"));
            lblPhone.setText(rs.getString("phone"));
            lblAddress.setText(rs.getString("address"));



            recalc();
            txtItem.requestFocus();

        } catch (Exception e) {
            show("Customer load failed");
        }
    }

    // ============================================
    private void addLine() {

        try {

            if (txtItem.getText().trim().isEmpty()) {
                show("Item required");
                return;
            }
            if (ItemHandler.getItemByName(txtItem.getText()) == null) {
                show("Item does not exist in system");
                return;
            }

            double rate = parse(txtRate.getText());
            int cartons = parseInt(txtCartons.getText());
            double pieces = parse(txtPieces.getText());

            double totalPieces = cartons * currentPPC +   pieces;
            double amount = totalPieces * rate;
            String itemName=txtItem.getText();
            String printName= ItemHandler.getPrintName(itemName);
            lineItems.add(new InvoiceItem(null,
                    printName,
                    rate,
                    cartons,
                    pieces,
                    amount));

            clearItem();
            recalc();
            txtItem.requestFocus();

        } catch (Exception e) {
            show("Invalid item data");
        }

    }
    private void clearItem() {
        txtItem.clear();
        txtRate.clear();
        txtCartons.clear();
        txtPieces.clear();
        lblAvailable.setText("-");
    }

    // ============================================
    private void recalc() {

        double sum = 0;

        for(InvoiceItem it : lineItems)
            sum += it.getAmount();

        double prev = parse(lblPrevBalance.getText());
        double payment = parse(txtPayment.getText());

        double total = prev + sum;
        double rem = total - payment;

        lblItemsTotal.setText(
                String.format("%.2f", sum));

        lblTotalBill.setText(
                String.format("%.2f", total));

        lblRemaining.setText(
                String.format("%.2f", rem));
    }

    // ============================================
    private void saveChanges() {

        boolean ok =
                EditInvoiceHandler.updateLastInvoice(

                        currentInvoiceId,
                        txtCustomer.getText(),
                        new ArrayList<>(lineItems),
                        parse(txtPayment.getText())
                );

        if(ok) {show("Invoice Updated");
            clearAll();
        }
        else show("Failed");
    }

    // ============================================
    private void setupTable() {

        colItem.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getItemName()));

        colRate.setCellValueFactory(d ->
                new javafx.beans.property.SimpleDoubleProperty(
                        d.getValue().getRate()).asObject());

        colCartons.setCellValueFactory(d ->
                new javafx.beans.property.SimpleIntegerProperty(
                        d.getValue().getCartons()).asObject());

        colPieces.setCellValueFactory(d ->
                new javafx.beans.property.SimpleDoubleProperty(
                        d.getValue().getPieces()).asObject());

        colAmount.setCellValueFactory(d ->
                new javafx.beans.property.SimpleDoubleProperty(
                        d.getValue().getAmount()).asObject());
    }

    private double parse(String x){
        try{return Double.parseDouble(x);}
        catch(Exception e){return 0;}
    }

    private int parseInt(String x){
        try{return Integer.parseInt(x);}
        catch(Exception e){return 0;}
    }

    private void show(String msg){
        new Alert(Alert.AlertType.INFORMATION,msg)
                .showAndWait();
    }

    // =================================================
    private void syncPerCarton() {
        if (isUpdating) return;

        try {
            isUpdating = true;
            double rate = parse(txtRate.getText());
            txtPerCartonAmount.setText(String.format("%.2f", rate * currentPPC));
        } finally {
            isUpdating = false;
        }
    }

    private void syncPerPiece() {
        if (isUpdating) return;

        try {
            isUpdating = true;
            double val = parse(txtPerCartonAmount.getText());
            txtRate.setText(String.format("%.2f", val / currentPPC));
        } finally {
            isUpdating = false;
        }
    }
    private void clearAll() {

        // ================= INPUT FIELDS =================
        txtCustomer.clear();
        txtItem.clear();
        txtRate.clear();
        txtCartons.clear();
        txtPieces.clear();
        txtPerCartonAmount.clear();
        txtPayment.clear();

        // ================= LABELS =================
        lblInvoiceId.clear();
        lblDateTime.clear();

        lblCustomerCategory.clear();
        lblPhone.clear();
        lblAddress.clear();

        lblPrevBalance.setText("0");
        lblAvailable.setText("-");

        lblItemsTotal.setText("0");
        lblTotalBill.setText("0");
        lblRemaining.setText("0");

        // ================= TABLE =================
        lineItems.clear();

        // ================= RESET VARIABLES =================
        currentInvoiceId = 0;
        oldRemaining = 0;
        currentPPC = 1;

        // ================= REFRESH STATE =================
        javafx.application.Platform.runLater(() -> {
            txtCustomer.requestFocus();
        });
    }
}