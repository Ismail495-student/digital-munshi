package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import siks.models.Voucher;
import siks.utils.DBUtil;
import siks.utils.VoucherHandler;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VoucherController {

    @FXML private ComboBox<String> cmbType;
    @FXML private TextField txtName, txtAmount, txtSearch;
    @FXML private TextArea txtDescription;

    @FXML private Button btnSave, btnClear, btnDelete;

    @FXML private TableView<Voucher> tblVouchers;
    @FXML private TableColumn<Voucher, String> colId, colType, colName, colDateTime, colDescription;
    @FXML private TableColumn<Voucher, Double> colAmount;

    @FXML private Label lblName;
    @FXML private Label lblPhone;
    @FXML private Label lblAddress;
    @FXML private Label lblBalance;

    private FilteredList<Voucher> filteredData;
    private ObservableList<String> allNames = FXCollections.observableArrayList();

    // ✅ CLEAN ERP STYLE AUTOCOMPLETE
    private final ContextMenu suggestionMenu = new ContextMenu();

    @FXML
    public void initialize() {

        setupTypeCombo();
        setupTable();
        loadVouchers();
        setupColumnWidths();

        cmbType.setOnAction(e -> loadNames());

        btnSave.setOnAction(e -> saveVoucher());
        btnClear.setOnAction(e -> clearForm());

        txtSearch.setOnKeyReleased(e -> applyFilter());

        setupNameAutocomplete();

        txtDescription.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.TAB) {
                e.consume();
                btnSave.requestFocus();
            }
        });
    }

    private void setupTypeCombo() {
        cmbType.setItems(FXCollections.observableArrayList("Customer", "Supplier"));
    }

    private void loadNames() {
        allNames.clear();

        String selectedType = cmbType.getValue();
        if (selectedType == null) return;

        String table = selectedType.equals("Supplier") ? "suppliers" : "customers";
        String sql = "SELECT name FROM " + table;

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                allNames.add(rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupTable() {
        colId.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getVoucherId()));
        colType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getType()));
        colName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));
        colDateTime.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDateTime()));
        colAmount.setCellValueFactory(d -> new javafx.beans.property.SimpleDoubleProperty(d.getValue().getAmount()).asObject());
        colDescription.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDescription()));
    }

    private void loadCustomerData(String name) {
        if (cmbType.getValue() == null) return;

        String[] data = VoucherHandler.getFullData(cmbType.getValue(), name);

        lblName.setText(name);
        lblPhone.setText(data[0]);
        lblAddress.setText(data[1]);
        lblBalance.setText(data[2]);
    }

    private void loadVouchers() {
        filteredData = new FilteredList<>(VoucherHandler.getVouchers(), p -> true);
        SortedList<Voucher> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tblVouchers.comparatorProperty());
        tblVouchers.setItems(sorted);
    }

    private void saveVoucher() {

        String type = cmbType.getValue();
        String name = txtName.getText().trim();
        String desc = txtDescription.getText().trim();

        if (type == null || name.isEmpty() || txtAmount.getText().isEmpty()) {
            alert(Alert.AlertType.WARNING, "Missing Fields", "Fill all required fields.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(txtAmount.getText());
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Invalid Amount", "Enter valid number");
            return;
        }

        String dateTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Voucher v = new Voucher(null, type, name, dateTime, amount, desc);

        if (VoucherHandler.addVoucher(v)) {
            alert(Alert.AlertType.INFORMATION, "Success", "Saved successfully");
            loadVouchers();
            clearForm();
        }
    }



    private void applyFilter() {
        String text = txtSearch.getText().toLowerCase();

        filteredData.setPredicate(v -> {
            if (text.isEmpty()) return true;

            return v.getName().toLowerCase().contains(text)
                    || v.getType().toLowerCase().contains(text)
                    || (v.getDescription() != null &&
                    v.getDescription().toLowerCase().contains(text));
        });
    }

    private void clearForm() {
        cmbType.getSelectionModel().clearSelection();
        txtName.clear();
        txtAmount.clear();
        txtDescription.clear();
        suggestionMenu.hide();
    }

    private void alert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ==================================================
    // 🔥 ERP-GRADE AUTOCOMPLETE (CONTEXTMENU VERSION)
    // ==================================================
    private void setupNameAutocomplete() {

        txtName.textProperty().addListener((obs, oldText, newText) -> {

            suggestionMenu.hide();

            if (newText == null || newText.isEmpty()) return;

            ObservableList<MenuItem> items = FXCollections.observableArrayList();
            suggestionMenu.setPrefWidth(150);

            for (String name : allNames) {
                if (name.toLowerCase().contains(newText.toLowerCase())) {

                    MenuItem item = new MenuItem(name);

                    item.setOnAction(e -> {
                        txtName.setText(name);
                        suggestionMenu.hide();
                        loadCustomerData(name);
                    });

                    items.add(item);
                }
            }

            if (items.isEmpty()) return;

            suggestionMenu.getItems().clear();
            suggestionMenu.getItems().addAll(items);

            if (!suggestionMenu.isShowing()) {
                suggestionMenu.show(txtName, Side.BOTTOM, 80, 0);
            }
        });
    }
    private void setupColumnWidths() {

        double p = 20;

        colId.prefWidthProperty().bind(tblVouchers.widthProperty().subtract(p).multiply(0.07));
        colType.prefWidthProperty().bind(tblVouchers.widthProperty().subtract(p).multiply(0.10));
        colName.prefWidthProperty().bind(tblVouchers.widthProperty().subtract(p).multiply(0.18));
        colAmount.prefWidthProperty().bind(tblVouchers.widthProperty().subtract(p).multiply(0.12));
        colDescription.prefWidthProperty().bind(tblVouchers.widthProperty().subtract(p).multiply(0.25));
        colDateTime.prefWidthProperty().bind(tblVouchers.widthProperty().subtract(p).multiply(0.28));
    }
}