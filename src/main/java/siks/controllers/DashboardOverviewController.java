package siks.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import siks.models.Customer;
import siks.models.Item;
import siks.utils.DBUtil;

public class DashboardOverviewController {

    // ================= BLACKLISTED CUSTOMERS =================
    @FXML private TableView<Customer> tblBlacklisted;

    @FXML private TableColumn<Customer, String> colCustomerName;
    @FXML private TableColumn<Customer, String> colCustomerPhone;
    @FXML private TableColumn<Customer, String> colLastBilled;

    // 🔥 NEW COLUMN ADDED
    @FXML private TableColumn<Customer, Double> colPrevBalance;

    // ================= LOW STOCK =================
    @FXML private TableView<Item> tblLowStock;

    @FXML private TableColumn<Item, String> colItemName;
    @FXML private TableColumn<Item, Integer> colStockQuantity;

    private Connection conn;

    public void initialize() {
        try {
            conn = DBUtil.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setupCustomerTable();
        setupLowStockTable();

        loadBlacklistedCustomers();
        loadLowStockItems();
    }

    // ================= CUSTOMER TABLE SETUP =================
    private void setupCustomerTable() {

        colCustomerName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCustomerPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colLastBilled.setCellValueFactory(new PropertyValueFactory<>("lastBilled"));

        // 🔥 NEW COLUMN BIND
        colPrevBalance.setCellValueFactory(new PropertyValueFactory<>("prevBalance"));
    }

    // ================= LOW STOCK TABLE =================
    private void setupLowStockTable() {
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStockQuantity.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
    }

    // ================= BLACKLIST LOGIC =================
    private void loadBlacklistedCustomers() {

        ObservableList<Customer> list = FXCollections.observableArrayList();

        String sql = "SELECT name, phone, lastBilled, prevBalance FROM customers";

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {

                String name = rs.getString("name");
                String phone = rs.getString("phone");
                String lastBilledStr = rs.getString("lastBilled");
                double prevBalance = Math.round(rs.getDouble("prevBalance") ) ;
                // safety check
                if (lastBilledStr == null || lastBilledStr.isEmpty()) continue;

                LocalDate lastBilledDate =
                        LocalDate.parse(lastBilledStr, DateTimeFormatter.ISO_DATE);

                // 🔥 BUSINESS RULE:
                // blacklist = inactive > 7 days + outstanding balance
                if (lastBilledDate.isBefore(LocalDate.now().minusDays(7))
                        && prevBalance > 100) {

                    list.add(new Customer(
                            name,
                            phone,
                            lastBilledStr,
                            prevBalance
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        tblBlacklisted.setItems(list);
    }

    // ================= LOW STOCK =================
    private void loadLowStockItems() {

        ObservableList<Item> list = FXCollections.observableArrayList();

        String sql = "SELECT name, stockQuantity FROM items WHERE stockQuantity < 10";

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {

                String name = rs.getString("name");
                int stockQuantity = rs.getInt("stockQuantity");

                list.add(new Item(name, stockQuantity));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        tblLowStock.setItems(list);
    }
}