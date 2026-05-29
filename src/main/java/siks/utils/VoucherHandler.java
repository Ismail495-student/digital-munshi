package siks.utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import siks.models.Voucher;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VoucherHandler {
    private static final String URL = "jdbc:sqlite:database.db";

    /** Load all vouchers */
    public static ObservableList<Voucher> getVouchers() {
        ObservableList<Voucher> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM vouchers ORDER BY id DESC";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String voucherId = String.format("V%04d", rs.getInt("id"));
                list.add(new Voucher(
                        voucherId,
                        rs.getString("type"),
                        rs.getString("name"),
                        rs.getString("dateTime"),
                        rs.getDouble("amount"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Add voucher & adjust prevBalance */
    public static boolean addVoucher(Voucher v) {
        String insertSql = "INSERT INTO vouchers (type, name, dateTime, amount, description) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false); // start transaction

            // 1. Insert voucher
            PreparedStatement ps = conn.prepareStatement(insertSql);
            ps.setString(1, v.getType());
            ps.setString(2, v.getName());
            ps.setString(3, v.getDateTime());
            ps.setDouble(4, v.getAmount());
            ps.setString(5, v.getDescription());

            int rows = ps.executeUpdate();

            if (rows == 0) {
                conn.rollback();
                return false;
            }

            // 2. Update balance (same connection, no DBUtil call inside)
            String table = v.getType().equalsIgnoreCase("Supplier") ? "suppliers" : "customers";
            String updateSql = "UPDATE " + table + " SET prevBalance = prevBalance - ? WHERE name = ?";

            PreparedStatement ps2 = conn.prepareStatement(updateSql);
            ps2.setDouble(1, v.getAmount());
            ps2.setString(2, v.getName());

            int updatedRows = ps2.executeUpdate();

            if (updatedRows == 0) {
                conn.rollback(); // important
                return false;
            }

            // 3. Commit if everything successful
            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();

            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            return false;

        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    /** Delete voucher by ID */
    public static void deleteVoucher(Voucher v) {
        if (v == null) return;
        int numericId = Integer.parseInt(v.getVoucherId().substring(1));
        String sql = "DELETE FROM vouchers WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numericId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Deduct payment amount from prevBalance */
    private static void updatePrevBalance(String type, String name, double payment) {
        String table = type.equalsIgnoreCase("Supplier") ? "suppliers" : "customers";
        String sql = "UPDATE " + table + " SET prevBalance = prevBalance - ? WHERE name = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, payment);
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Get current timestamp */
    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    public static String[] getFullData(String type, String name) {

        String[] data = {"", "", "0"};

        String table = type.equalsIgnoreCase("Customer")
                ? "customers"
                : "suppliers";

        String sql =
                "SELECT phone, address, prevBalance FROM " + table + " WHERE name=?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                data[0] = rs.getString("phone");
                data[1] = rs.getString("address");
                data[2] = String.valueOf((int)rs.getDouble("prevBalance"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;
    }
}
