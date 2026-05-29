package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import siks.models.Supplier;
import siks.utils.SupplierHandler;

import java.util.Arrays;

public class SupplierController {

    @FXML private TextField txtName, txtPrintName, txtPhone, txtAddress, txtCompany, txtSearch;
    @FXML private ComboBox<String> cbCategory, cbCategoryFilter;
    @FXML private Button btnAdd, btnUpdate, btnDelete, btnClear;
    @FXML private TableView<Supplier> tblSuppliers;
    @FXML private TableColumn<Supplier, String> colId, colName, colPrintName, colPhone, colAddress, colCompany, colCategory, colLastBilled;
    @FXML private TableColumn<Supplier, Double> colPrevBalance;

    private FilteredList<Supplier> filteredData;

    private final ObservableList<String> categories = FXCollections.observableArrayList(
            Arrays.asList("Kiryana", "Pansaar", "General", "Confectionary", "Beverages", "Stationary", "Others")
    );

    @FXML
    public void initialize() {
        setupComboBoxes();
        setupTable();
        setupButtons();
        setupSearchAndFilter();
        setupIcons();

    }

    private void setupComboBoxes() {
        cbCategory.setItems(categories);
        cbCategory.getSelectionModel().selectFirst(); // default

        ObservableList<String> filterOptions = FXCollections.observableArrayList();
        filterOptions.add("All");
        filterOptions.addAll(categories);
        cbCategoryFilter.setItems(filterOptions);
        cbCategoryFilter.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        colPrintName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPrintName()));
        colPhone.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPhone()));
        colAddress.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getAddress()));
        colCompany.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCompany()));
        colCategory.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategory()));
        colLastBilled.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getLastBilled()));
        colPrevBalance.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getPrevBalance()).asObject());

        filteredData = new FilteredList<>(SupplierHandler.getSuppliers(), p -> true);
        SortedList<Supplier> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblSuppliers.comparatorProperty());
        tblSuppliers.setItems(sortedData);

        tblSuppliers.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> populateForm(newSel));
    }

    private void setupButtons() {
        btnAdd.setOnAction(e -> handleAdd());
        btnUpdate.setOnAction(e -> handleUpdate());
        btnDelete.setOnAction(e -> handleDelete());
        btnClear.setOnAction(e -> clearForm());
    }

    private void setupSearchAndFilter() {
        txtSearch.setOnKeyReleased(e -> applyFilters());
        cbCategoryFilter.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        String searchText = txtSearch.getText().toLowerCase();
        String categoryFilter = cbCategoryFilter.getSelectionModel().getSelectedItem();

        filteredData.setPredicate(s -> {
            boolean matchesSearch = s.getName().toLowerCase().contains(searchText);
            boolean matchesCategory = categoryFilter.equals("All") || s.getCategory().equals(categoryFilter);
            return matchesSearch && matchesCategory;
        });
    }

    private void handleAdd() {
        Supplier s = new Supplier(
                null, // ID will be auto-generated in DB
                txtName.getText(),
                txtPrintName.getText(),
                txtPhone.getText(),
                txtAddress.getText(),
                txtCompany.getText(),
                cbCategory.getSelectionModel().getSelectedItem(),
                null, // lastBilled
                0     // prevBalance
        );
        if (!SupplierHandler.addSupplier(s)) {
            showAlert("Duplicate Supplier", "This supplier already exists!");
            return;
        }
        clearForm();
        refreshTable();
    }

    private void handleUpdate() {
        Supplier selected = tblSuppliers.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Supplier updated = new Supplier(
                selected.getId(),
                txtName.getText(),
                txtPrintName.getText(),
                txtPhone.getText(),
                txtAddress.getText(),
                txtCompany.getText(),
                cbCategory.getSelectionModel().getSelectedItem(),
                selected.getLastBilled(),
                selected.getPrevBalance()
        );
        SupplierHandler.updateSupplier(updated);
        clearForm();
        refreshTable();
    }

    private void handleDelete() {
        Supplier selected = tblSuppliers.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                SupplierHandler.deleteSupplier(selected);
                clearForm();
                refreshTable();
            }
        });
    }

    private void clearForm() {
        txtName.clear();
        txtPrintName.clear();
        txtPhone.clear();
        txtAddress.clear();
        txtCompany.clear();
        cbCategory.getSelectionModel().selectFirst();
        tblSuppliers.getSelectionModel().clearSelection();
    }

    private void populateForm(Supplier s) {
        if (s == null) return;
        txtName.setText(s.getName());
        txtPrintName.setText(s.getPrintName());
        txtPhone.setText(s.getPhone());
        txtAddress.setText(s.getAddress());
        txtCompany.setText(s.getCompany());
        cbCategory.getSelectionModel().select(s.getCategory());
    }

    private void refreshTable() {
        filteredData = new FilteredList<>(SupplierHandler.getSuppliers(), p -> true);
        SortedList<Supplier> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblSuppliers.comparatorProperty());
        tblSuppliers.setItems(sortedData);
        applyFilters();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void setupIcons() {
        setButtonIcon(btnAdd, "/icons/add.png");
        setButtonIcon(btnUpdate, "/icons/update.png");
        setButtonIcon(btnDelete, "/icons/delete.png");
        setButtonIcon(btnClear, "/icons/clear.png");

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
}
