package siks.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.text.NumberFormat;
import java.util.Locale;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.DatePicker;

import siks.utils.DBUtil;

public class AnalyticsController {

    // ========== CHARTS ==========
    @FXML private BarChart<String, Number> chartHighBalanceCustomers;
    @FXML private BarChart<String, Number> chartHighStockItems;
    @FXML private BarChart<String, Number> chartHighBalanceSuppliers;

    // ========== TOP CARDS ==========
    @FXML private Label lblTotalSales;
    @FXML private Label lblTotalPurchase;
    @FXML private Label lblPaymentsReceived;
    @FXML private Label lblCustomerBalance;

    // ========== DATE PICKERS ==========
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;

    private Connection conn;

    private LocalDate fromDate;
    private LocalDate toDate;

    // ===================== INIT =====================
    public void initialize() {

        try {
            conn = DBUtil.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        toDate = LocalDate.now();
        fromDate = toDate.minusDays(7);

        if (dpFrom != null) dpFrom.setValue(fromDate);
        if (dpTo != null) dpTo.setValue(toDate);

        reloadDashboard();
    }

    // ===================== APPLY FILTER =====================
    @FXML
    private void applyDateFilter() {

        if (dpFrom.getValue() == null || dpTo.getValue() == null) return;

        if (dpFrom.getValue().isAfter(dpTo.getValue())) return;

        fromDate = dpFrom.getValue();
        toDate = dpTo.getValue();

        reloadDashboard();
    }

    // ===================== RELOAD =====================
    private void reloadDashboard() {
        loadDashboardCards();
        loadCharts();
    }

    // ===================== FORMATTER =====================
    private String format(double value) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        return nf.format(value);
    }

    // ===================== CARDS =====================
    private void loadDashboardCards() {

        lblTotalSales.setText(format(getTotalSales()));
        lblTotalPurchase.setText(format(getTotalPurchase()));
        lblPaymentsReceived.setText(format(getPaymentsReceived()));
        lblCustomerBalance.setText(format(getCustomerBalance()));
    }

    // ===================== SALES =====================
    private double getTotalSales() {

        String sql =
                "SELECT COALESCE(SUM(ii.amount),0) " +
                        "FROM invoice_items ii " +
                        "JOIN invoices i ON ii.invoice_id = i.id " +
                        "WHERE i.type = 'SALE' " +
                        "AND i.dateTime >= ? AND i.dateTime <= ?";

        return getSum(sql);
    }

    // ===================== PURCHASE =====================
    private double getTotalPurchase() {

        String sql =
                "SELECT COALESCE(SUM(ii.amount),0) " +
                        "FROM invoice_items ii " +
                        "JOIN invoices i ON ii.invoice_id = i.id " +
                        "WHERE i.type = 'PURCHASE' " +
                        "AND i.dateTime >= ? AND i.dateTime <= ?";

        return getSum(sql);
    }

    // ===================== PAYMENTS =====================
    private double getPaymentsReceived() {

        String sql1 =
                "SELECT COALESCE(SUM(payment),0) FROM invoices " +
                        "WHERE type = 'SALE' " +
                        "AND dateTime >= ? AND dateTime <= ?";

        String sql2 =
                "SELECT COALESCE(SUM(amount),0) FROM vouchers " +
                        "WHERE type = 'Customer' " +
                        "AND dateTime >= ? AND dateTime <= ?";

        return getSum(sql1) + getSum(sql2);
    }

    // ===================== CUSTOMER BALANCE =====================
    private double getCustomerBalance() {

        String sql = "SELECT COALESCE(SUM(prevBalance),0) FROM customers";

        return getSingleValue(sql);
    }

    // ===================== SQLITE SAFE SUM =====================
    private double getSum(String sql) {

        try (PreparedStatement pst = conn.prepareStatement(sql)) {

            String from = fromDate.toString() + " 00:00:00";
            String to = toDate.toString() + " 23:59:59";

            pst.setString(1, from);
            pst.setString(2, to);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getDouble(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private double getSingleValue(String sql) {

        try (PreparedStatement pst = conn.prepareStatement(sql)) {

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getDouble(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // ===================== CHARTS =====================
    private void loadCharts() {
        loadHighBalanceCustomers();
        loadHighStockItems();
        loadHighBalanceSuppliers();
    }

    // -------- Customers --------
    private void loadHighBalanceCustomers() {

        chartHighBalanceCustomers.getData().clear();

        String sql =
                "SELECT name, prevBalance FROM customers " +
                        "WHERE prevBalance > 0 ORDER BY prevBalance DESC LIMIT 10";

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Top Customers");

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                series.getData().add(
                        new XYChart.Data<>(rs.getString("name"), rs.getDouble("prevBalance"))
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        chartHighBalanceCustomers.getData().add(series);
    }

    // -------- Stock --------
    private void loadHighStockItems() {

        chartHighStockItems.getData().clear();

        String sql =
                "SELECT name, stockQuantity FROM items " +
                        "ORDER BY stockQuantity DESC LIMIT 10";

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Top Stock Items");

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                series.getData().add(
                        new XYChart.Data<>(rs.getString("name"), rs.getInt("stockQuantity"))
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        chartHighStockItems.getData().add(series);
    }

    // -------- Suppliers --------
    private void loadHighBalanceSuppliers() {

        chartHighBalanceSuppliers.getData().clear();

        String sql =
                "SELECT name, prevBalance FROM suppliers " +
                        "WHERE prevBalance > 0 ORDER BY prevBalance DESC LIMIT 10";

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Top Suppliers");

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                series.getData().add(
                        new XYChart.Data<>(rs.getString("name"), rs.getDouble("prevBalance"))
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        chartHighBalanceSuppliers.getData().add(series);
    }
}