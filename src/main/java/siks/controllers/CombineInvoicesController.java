package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import siks.models.CombinedItem;
import siks.utils.InvoiceCombineHandler;
import siks.utils.CombinedInvoicePrintBuilder;

import java.util.*;

public class CombineInvoicesController {

    @FXML private TextField txtInvoiceIds;
    @FXML private Button btnCombine, btnPrint;
    @FXML private TableView<CombinedItem> tblCombined;
    @FXML private TableColumn<CombinedItem, String> colItem;
    @FXML private TableColumn<CombinedItem, Integer> colCartons;
    @FXML private TableColumn<CombinedItem, Double> colPieces;
    @FXML private Label lblTotalCartons, lblTotalPieces;
    @FXML private TextField txtStartInvoice;
    @FXML private TextField txtEndInvoice;
    @FXML private Button btnRangePrint;

    private ObservableList<CombinedItem> combinedData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colItem.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getItemName()));
        colCartons.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getTotalCartons()).asObject());
        colPieces.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getTotalPieces()).asObject());

        tblCombined.setItems(combinedData);

        btnCombine.setOnAction(e -> combineInvoices());
        btnPrint.setOnAction(e -> printReport());
        btnRangePrint.setOnAction(e -> combineByRange());
    }

    private void combineInvoices() {
        String input = txtInvoiceIds.getText().trim();
        if (input.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please enter at least one invoice ID.").show();
            return;
        }

        List<String> ids = Arrays.asList(input.split(","));
        List<CombinedItem> data = InvoiceCombineHandler.getCombinedItemsForInvoices(ids);

        combinedData.setAll(data);
        updateTotals();
    }

    private void updateTotals() {
        int totalCartons = 0;
        double totalPieces = 0;
        for (CombinedItem it : combinedData) {
            totalCartons += it.getTotalCartons();
            totalPieces += it.getTotalPieces();
        }
        lblTotalCartons.setText(String.valueOf(totalCartons));
        lblTotalPieces.setText(String.format("%.0f", totalPieces));
    }

    private void printReport() {
        if (combinedData.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No data to print.").show();
            return;
        }
        CombinedInvoicePrintBuilder.printCombinedReport(new ArrayList<>(combinedData));
    }
    private void combineByRange() {

        String startText = txtStartInvoice.getText().trim();
        String endText = txtEndInvoice.getText().trim();

        if (startText.isEmpty() || endText.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Enter both start and end invoice numbers.").show();
            return;
        }

        try {
            int start = Integer.parseInt(startText);
            int end = Integer.parseInt(endText);

            if (start > end) {
                new Alert(Alert.AlertType.WARNING, "Start invoice cannot be greater than end invoice.").show();
                return;
            }

            // 🔥 generate invoice list automatically
            List<String> ids = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                ids.add(String.valueOf(i));
            }

            // same combine logic reuse
            List<CombinedItem> data = InvoiceCombineHandler.getCombinedItemsForInvoices(ids);

            combinedData.setAll(data);
            updateTotals();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid number format.").show();
        }
    }
}