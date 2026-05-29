package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import siks.models.Item;
import siks.utils.ItemHandler;

import java.util.Arrays;

public class ItemController {

    @FXML private TextField txtName, txtPrintName, txtPurchaseRate, txtRetailRate, txtWholesaleRate;
    @FXML private TextField txtPiecesPerCarton, txtCompany, txtSearch;
    @FXML private ComboBox<String> cbCategory, cbCategoryFilter;
    @FXML private Button btnAdd, btnUpdate, btnDelete, btnClear;
    @FXML private TableView<Item> tblItems;
    @FXML private TableColumn<Item, String> colId, colName, colPrintName, colCompany, colCategory;
    @FXML private TableColumn<Item, Double> colPurchaseRate, colRetailRate, colWholesaleRate, colStockQuantity;
    @FXML private TableColumn<Item, Integer> colPiecesPerCarton;

    private FilteredList<Item> filteredData;

    @FXML
    public void initialize() {
        setupComboBoxes();
        setupTable();
        setupButtons();
        setupFilters();
        setupIcons();

    }

    private void setupComboBoxes() {
        cbCategory.setItems(FXCollections.observableArrayList(
                Arrays.asList("Kiryana", "Pansaar", "General", "Confectionary", "Beverages", "Stationary", "Others")));
        cbCategory.getSelectionModel().selectFirst();

        cbCategoryFilter.setItems(FXCollections.observableArrayList("All", "Kiryana", "Pansaar", "General", "Confectionary", "Beverages", "Stationary", "Others"));
        cbCategoryFilter.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        colPrintName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPrintName()));
        colPurchaseRate.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getPurchaseRate()).asObject());
        colRetailRate.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getRetailRate()).asObject());
        colWholesaleRate.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getWholesaleRate()).asObject());
        colPiecesPerCarton.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getPiecesPerCarton()).asObject());
        colCategory.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getItemCategory()));
        colCompany.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCompany()));
        colStockQuantity.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getStockQuantity()).asObject());

        filteredData = new FilteredList<>(ItemHandler.getItems(), p -> true);
        SortedList<Item> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblItems.comparatorProperty());
        tblItems.setItems(sortedData);

        tblItems.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> populateForm(newSel));
    }

    private void setupButtons() {
        btnAdd.setOnAction(e -> handleAdd());
        btnUpdate.setOnAction(e -> handleUpdate());
        btnDelete.setOnAction(e -> handleDelete());
        btnClear.setOnAction(e -> clearForm());
    }

    private void setupFilters() {
        txtSearch.setOnKeyReleased(e -> applyFilters());
        cbCategoryFilter.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        String searchText = txtSearch.getText().toLowerCase();
        String selectedCategory = cbCategoryFilter.getSelectionModel().getSelectedItem();

        filteredData.setPredicate(item -> {
            boolean matchesNameOrCompany = item.getName().toLowerCase().contains(searchText)
                    || item.getCompany().toLowerCase().contains(searchText);
            boolean matchesCategory = selectedCategory.equals("All") || item.getItemCategory().equals(selectedCategory);
            return matchesNameOrCompany && matchesCategory;
        });
    }

    private void handleAdd() {
        try {
            Item i = new Item(null, txtName.getText(), txtPrintName.getText(),
                    Double.parseDouble(txtPurchaseRate.getText()),
                    Double.parseDouble(txtRetailRate.getText()),
                    Double.parseDouble(txtWholesaleRate.getText()),
                    Integer.parseInt(txtPiecesPerCarton.getText()),
                    cbCategory.getSelectionModel().getSelectedItem(),
                    0.0, txtCompany.getText());

            if (!ItemHandler.addItem(i)) {
                showAlert("Duplicate Item", "This item already exists for this company!");
                return;
            }
            clearForm();
            refreshTable();
            txtName.requestFocus();

        } catch (Exception ex) {
            showAlert("Invalid Input", "Please check numeric fields!");
        }
    }

    private void handleUpdate() {
        Item selected = tblItems.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Item updated = new Item(selected.getId(), txtName.getText(), txtPrintName.getText(),
                Double.parseDouble(txtPurchaseRate.getText()),
                Double.parseDouble(txtRetailRate.getText()),
                Double.parseDouble(txtWholesaleRate.getText()),
                Integer.parseInt(txtPiecesPerCarton.getText()),
                cbCategory.getSelectionModel().getSelectedItem(),
                selected.getStockQuantity(), txtCompany.getText());

        ItemHandler.updateItem(updated);
        clearForm();
        refreshTable();
    }

    private void handleDelete() {
        Item selected = tblItems.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                ItemHandler.deleteItem(selected);
                clearForm();
                refreshTable();
            }
        });
    }

    private void clearForm() {
        txtName.clear();
        txtPrintName.clear();
        txtPurchaseRate.clear();
        txtRetailRate.clear();
        txtWholesaleRate.clear();
        txtPiecesPerCarton.clear();
        txtCompany.clear();
        cbCategory.getSelectionModel().selectFirst();
        tblItems.getSelectionModel().clearSelection();
    }

    private void populateForm(Item i) {
        if (i == null) return;
        txtName.setText(i.getName());
        txtPrintName.setText(i.getPrintName());
        txtPurchaseRate.setText(String.valueOf(i.getPurchaseRate()));
        txtRetailRate.setText(String.valueOf(i.getRetailRate()));
        txtWholesaleRate.setText(String.valueOf(i.getWholesaleRate()));
        txtPiecesPerCarton.setText(String.valueOf(i.getPiecesPerCarton()));
        txtCompany.setText(i.getCompany());
        cbCategory.getSelectionModel().select(i.getItemCategory());
    }

    private void refreshTable() {
        filteredData = new FilteredList<>(ItemHandler.getItems(), p -> true);
        SortedList<Item> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblItems.comparatorProperty());
        tblItems.setItems(sortedData);
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
