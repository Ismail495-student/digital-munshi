package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import siks.models.Item;
import siks.utils.ItemHandler;
import siks.models.ItemStock;

import java.util.List;
import java.util.stream.Collectors;

public class StockController {

    @FXML private TextField txtItemId;
    @FXML private TextField txtItemName;
    @FXML private TextField txtCartons;
    @FXML private TextField txtPieces;
    @FXML private TextField txtSearch;
    @FXML private Button btnSave;

    @FXML private TableView<ItemStock> tblStock;
    @FXML private TableColumn<ItemStock, String> colItemId;
    @FXML private TableColumn<ItemStock, String> colItemName;
    @FXML private TableColumn<ItemStock, Integer> colCartons;
    @FXML private TableColumn<ItemStock, Integer> colPieces;
    @FXML private TableColumn<ItemStock, String> colLastEdited;
    @FXML private TableColumn<ItemStock, String> colChangeType;

    private ObservableList<ItemStock> stockList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Setup table columns
        colItemId.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colCartons.setCellValueFactory(new PropertyValueFactory<>("cartons"));
        colPieces.setCellValueFactory(new PropertyValueFactory<>("pieces"));
        colLastEdited.setCellValueFactory(new PropertyValueFactory<>("lastEdited"));
        colChangeType.setCellValueFactory(new PropertyValueFactory<>("changeType"));

        tblStock.setItems(stockList);

        populateTable();

        // Row double-click -> load header fields
        tblStock.setRowFactory(tv -> {
            TableRow<ItemStock> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    ItemStock sel = row.getItem();
                    txtItemId.setText(sel.getItemId());
                    txtItemName.setText(sel.getItemName());
                    txtCartons.setText(String.valueOf(sel.getCartons()));
                    txtPieces.setText(String.valueOf(sel.getPieces()));
                }
            });
            return row;
        });

        tblStock.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                ItemStock sel = tblStock.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    txtItemId.setText(sel.getItemId());
                    txtItemName.setText(sel.getItemName());
                    txtCartons.setText(String.valueOf(sel.getCartons()));
                    txtPieces.setText(String.valueOf(sel.getPieces()));
                }
            }
        });

        // Live search/filter
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            tblStock.setItems(FXCollections.observableArrayList(
                    stockList.stream().filter(i ->
                                    i.getItemId().toLowerCase().contains(newVal.toLowerCase()) ||
                                            i.getItemName().toLowerCase().contains(newVal.toLowerCase()))
                            .collect(Collectors.toList())
            ));
        });

        // Save button -> update stock
        btnSave.setOnAction(e -> {
            if (txtItemName.getText().isEmpty()) return;

            int cartons = 0, pieces = 0;
            try {
                cartons = Integer.parseInt(txtCartons.getText());
                pieces = Integer.parseInt(txtPieces.getText());
            } catch (NumberFormatException ex) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Invalid quantity entered");
                a.showAndWait();
                return;
            }

            // Update DB and get updated ItemStock
            ItemStock updatedStock = ItemHandler.updateStockQuantity(txtItemName.getText(), cartons, pieces);
            if (updatedStock != null) {
                // Update table row
                for (ItemStock row : stockList) {
                    if (row.getItemName().equals(updatedStock.getItemName())) {
                        row.cartonsProperty().set(updatedStock.getCartons());
                        row.piecesProperty().set(updatedStock.getPieces());
                        row.lastEditedProperty().set(updatedStock.getLastEdited());
                        row.changeTypeProperty().set(updatedStock.getChangeType());
                        break;
                    }
                }

                // Clear header
                txtItemId.clear();
                txtItemName.clear();
                txtCartons.clear();
                txtPieces.clear();
            }
        });
    }

    private void populateTable() {
        stockList.clear();
        List<Item> items = ItemHandler.getItems();
        for (Item it : items) {
            int cartons = (int) (it.getStockQuantity() / it.getPiecesPerCarton());
            int pieces = (int) (it.getStockQuantity() % it.getPiecesPerCarton());
            stockList.add(new ItemStock(it.getId(), it.getName(), cartons, pieces, "-", "-"));
        }
    }
}
