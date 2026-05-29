package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import siks.utils.CustomerHandler;
import siks.utils.ReceiptBuilder;
import siks.utils.PrintUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class CustomerPrintController {

    @FXML private TextField searchField;
    @FXML private ListView<String> tempList;

    @FXML private TableView<CustomerRow> selectedTable;

    @FXML private TableColumn<CustomerRow, String> colName;
    @FXML private TableColumn<CustomerRow, String> colPhone;
    @FXML private TableColumn<CustomerRow, String> colAddress;
    @FXML private TableColumn<CustomerRow, Double> colPrevBalance;

    @FXML private Button btnPrintAll;
    @FXML private Button btnPrintSelected;

    private ObservableList<CustomerRow> selectedCustomers = FXCollections.observableArrayList();
    private Map<String, Double> allCustomers;

    @FXML
    private void initialize() {

        // Load data
        allCustomers = CustomerHandler.getAllPrevBalance();

        ObservableList<String> names =
                FXCollections.observableArrayList(allCustomers.keySet());

        tempList.setItems(names);

        // ================= TABLE SETUP =================
        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colPhone.setCellValueFactory(data -> data.getValue().phoneProperty());
        colAddress.setCellValueFactory(data -> data.getValue().addressProperty());
        colPrevBalance.setCellValueFactory(data -> data.getValue().prevBalanceProperty().asObject());

        selectedTable.setItems(selectedCustomers);

        // ================= SEARCH =================
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase();

            ObservableList<String> filtered = FXCollections.observableArrayList();

            for (String name : allCustomers.keySet()) {
                if (name.toLowerCase().contains(filter)) {
                    filtered.add(name);
                }
            }

            tempList.setItems(filtered);
        });

        // ================= KEY NAV =================
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DOWN) {
                if (!tempList.getItems().isEmpty()) {
                    tempList.requestFocus();
                    tempList.getSelectionModel().selectFirst();
                }
                event.consume();
            }
        });

        tempList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.UP &&
                    tempList.getSelectionModel().getSelectedIndex() == 0) {
                searchField.requestFocus();
                event.consume();
            }

            if (event.getCode() == KeyCode.ENTER) {
                addSelectedFromList();
                event.consume();
            }
        });

        tempList.setOnMouseClicked(e -> addSelectedFromList());

        // ================= BUTTONS =================
        btnPrintAll.setOnAction(e -> printAllCustomers());
        btnPrintSelected.setOnAction(e -> printSelected());
    }

    // ================= ADD CUSTOMER =================
    private void addSelectedFromList() {

        String name = tempList.getSelectionModel().getSelectedItem();

        if (name == null || !allCustomers.containsKey(name)) return;

        double balance = allCustomers.get(name);

        String phone = CustomerHandler.getNoByName(name);
        String address = CustomerHandler.getCustomerAddress(name);

        CustomerRow row = new CustomerRow(
                name,
                phone,
                address,
                balance
        );

        if (!selectedCustomers.contains(row)) {
            selectedCustomers.add(row);
        }

        searchField.clear();
    }

    // ================= PRINT ALL =================
    private void printAllCustomers() {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Print");
        alert.setHeaderText("Print All Customers");
        alert.setContentText("Do you really want to print ALL customers report?");

        ButtonType yes = new ButtonType("YES", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("NO", ButtonBar.ButtonData.NO);

        alert.getButtonTypes().setAll(yes, no);

        alert.showAndWait().ifPresent(response -> {
            if (response == yes) {
                try {
                    Map<String, Double> tableData = new LinkedHashMap<>();

                    for (Map.Entry<String, Double> entry : allCustomers.entrySet()) {
                        tableData.put(entry.getKey(), entry.getValue());
                    }

                    byte[] escBytes =
                            ReceiptBuilder.buildCustomerBalanceReceiptTable(tableData);

                    PrintUtil.printEscPos(escBytes);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    // ================= PRINT SELECTED =================
    private void printSelected() {

        try {
            Map<String, Double> tableData = new LinkedHashMap<>();

            for (CustomerRow row : selectedCustomers) {
                tableData.put(row.getName(), row.getPrevBalance());
            }

            byte[] escBytes =
                    ReceiptBuilder.buildCustomerBalanceReceiptTable(tableData);

            PrintUtil.printEscPos(escBytes);

            selectedCustomers.clear();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ================= MODEL =================
    public static class CustomerRow {

        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty phone;
        private final javafx.beans.property.SimpleStringProperty address;
        private final javafx.beans.property.SimpleDoubleProperty prevBalance;

        public CustomerRow(String name, String phone, String address, double prevBalance) {
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.phone = new javafx.beans.property.SimpleStringProperty(phone);
            this.address = new javafx.beans.property.SimpleStringProperty(address);
            this.prevBalance = new javafx.beans.property.SimpleDoubleProperty(prevBalance);
        }

        public String getName() { return name.get(); }
        public String getPhone() { return phone.get(); }
        public String getAddress() { return address.get(); }
        public double getPrevBalance() { return prevBalance.get(); }

        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.StringProperty phoneProperty() { return phone; }
        public javafx.beans.property.StringProperty addressProperty() { return address; }
        public javafx.beans.property.DoubleProperty prevBalanceProperty() { return prevBalance; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CustomerRow)) return false;
            return getName().equals(((CustomerRow) obj).getName());
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }
    }
}