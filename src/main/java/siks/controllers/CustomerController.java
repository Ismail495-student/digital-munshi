package siks.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Alert.AlertType;
import siks.models.Customer;
import siks.utils.CustomerHandler;

import java.util.List;
import java.util.Optional;

public class CustomerController {

    @FXML private TextField txtName, txtPrintName, txtPhone, txtAddress, txtSearch;
    @FXML private ComboBox<String> cmbCategory, cmbFilterCategory;
    @FXML private Button btnAdd, btnUpdate, btnDelete, btnClear;
    @FXML private TableView<Customer> tblCustomers;
    @FXML private TableColumn<Customer,String> colId, colName, colPrintName, colPhone, colAddress, colCategory, colLastBilled;
    @FXML private TableColumn<Customer,Double> colPrevBalance;

    private ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private FilteredList<Customer> filteredList;

    @FXML
    public void initialize() {
        cmbCategory.getItems().addAll("Retailer", "Wholesaler");
        cmbFilterCategory.getItems().addAll("All", "Retailer", "Wholesaler");
        setupIcons();
        CustomerHandler.createCustomerTable();
        setupTableColumns();
        loadCustomers();
        setUpTables();

        btnAdd.setOnAction(e -> handleAdd());
        btnUpdate.setOnAction(e -> handleUpdate());
        btnDelete.setOnAction(e -> handleDelete());
        btnClear.setOnAction(e -> clearForm());

        txtSearch.setOnKeyReleased(this::filterCustomer);
        cmbFilterCategory.setOnAction(e -> filterCustomer(null));

        tblCustomers.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> populateForm(newSel));
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        colPrintName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPrintName()));
        colPhone.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPhone()));
        colAddress.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getAddress()));
        colCategory.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategory()));
        colLastBilled.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getLastBilled() == null ? "-" : data.getValue().getLastBilled()
        ));
        colPrevBalance.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getPrevBalance()).asObject());
    }


    private void loadCustomers() {
        customerList.clear();
        List<Customer> list = CustomerHandler.getAllCustomers();
        customerList.addAll(list);
        filteredList = new FilteredList<>(customerList, p -> true);
        tblCustomers.setItems(filteredList);
    }

    private void handleAdd() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            showAlert(AlertType.WARNING, "Name Required", "Please enter customer name.");
            return;
        }

        // Duplicate check
        for (Customer c : customerList) {
            if (c.getName().equalsIgnoreCase(name)) {
                showAlert(AlertType.ERROR, "Duplicate Customer", "Customer already exists.");
                return;
            }
        }

        Customer c = new Customer(
                CustomerHandler.generateCustomerId(),
                name,
                txtPrintName.getText().trim(),
                txtPhone.getText().trim(),
                txtAddress.getText().trim(),
                cmbCategory.getValue() != null ? cmbCategory.getValue() : "Retailer",
                null, // last billed
                0.0   // prev balance
        );

        if (CustomerHandler.addCustomer(c)) {
            customerList.add(c);
            showAlert(AlertType.INFORMATION, "Success", "Customer added successfully!");
            clearForm();
            txtName.requestFocus();
        }
    }

    private void handleUpdate() {
        Customer selected = tblCustomers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a customer to update.");
            return;
        }

        selected.setName(txtName.getText().trim());
        selected.setPrintName(txtPrintName.getText().trim());
        selected.setPhone(txtPhone.getText().trim());
        selected.setAddress(txtAddress.getText().trim());
        selected.setCategory(cmbCategory.getValue());

        if (CustomerHandler.updateCustomer(selected)) {
            tblCustomers.refresh();
            showAlert(AlertType.INFORMATION, "Updated", "Customer updated successfully!");
            clearForm();
        }
    }

    private void handleDelete() {
        Customer selected = tblCustomers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a customer to delete.");
            return;
        }

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Delete Confirmation");
        confirm.setHeaderText("Are you sure you want to delete customer: " + selected.getName() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (CustomerHandler.deleteCustomer(selected.getId())) {
                customerList.remove(selected);
                showAlert(AlertType.INFORMATION, "Deleted", "Customer deleted successfully!");
                clearForm();
            }
        }
    }

    private void clearForm() {
        txtName.clear();
        txtPrintName.clear();
        txtPhone.clear();
        txtAddress.clear();
        cmbCategory.getSelectionModel().clearSelection();
        tblCustomers.getSelectionModel().clearSelection();
    }

    private void populateForm(Customer c) {
        if (c != null) {
            txtName.setText(c.getName());
            txtPrintName.setText(c.getPrintName());
            txtPhone.setText(c.getPhone());
            txtAddress.setText(c.getAddress());
            cmbCategory.setValue(c.getCategory());
        }
    }

    private void filterCustomer(KeyEvent event) {
        String searchText = txtSearch.getText().toLowerCase();
        String categoryFilter = cmbFilterCategory.getValue() != null ? cmbFilterCategory.getValue() : "All";

        filteredList.setPredicate(c -> {
            boolean matchesName = c.getName().toLowerCase().contains(searchText);
            boolean matchesCategory = categoryFilter.equals("All") || c.getCategory().equalsIgnoreCase(categoryFilter);
            return matchesName && matchesCategory;
        });
    }

    private void showAlert(AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
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
    private void setUpTables(){

        colId.prefWidthProperty().bind(tblCustomers.widthProperty().multiply(0.08));
        colName.prefWidthProperty().bind(tblCustomers.widthProperty().multiply(0.18));
        colPrintName.prefWidthProperty().bind(tblCustomers.widthProperty().multiply(0.15));
        colPhone.prefWidthProperty().bind(tblCustomers.widthProperty().multiply(0.15));
        colAddress.prefWidthProperty().bind(tblCustomers.widthProperty().multiply(0.20));
        colPrevBalance.prefWidthProperty().bind(tblCustomers.widthProperty().multiply(0.08));
        colLastBilled.prefWidthProperty().bind(tblCustomers.widthProperty().multiply(0.08));
        colCategory.prefWidthProperty().bind(tblCustomers.widthProperty().multiply(0.08));
    }
}
