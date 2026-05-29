package siks.utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import siks.models.Supplier;

import java.sql.*;

public class SupplierHandler {
    private static final String URL = "jdbc:sqlite:database.db";

    /** Get all suppliers */
    public static ObservableList<Supplier> getSuppliers() {
        ObservableList<Supplier> list = FXCollections.observableArrayList();
        String query = "SELECT * FROM suppliers ORDER BY id";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String id = String.format("SU%03d", rs.getInt("id")); // SU001 format
                Supplier s = new Supplier(
                        id,
                        rs.getString("name"),
                        rs.getString("printName"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("company"),
                        rs.getString("category"),
                        rs.getString("lastBilled"),
                        rs.getDouble("prevBalance")
                );
                list.add(s);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Add supplier */
    public static boolean addSupplier(Supplier s) {
        if (exists(s.getName(), s.getCompany())) return false;

        String sql = "INSERT INTO suppliers (name, printName, phone, address, company, category, lastBilled, prevBalance) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getName());
            ps.setString(2, s.getPrintName());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getAddress());
            ps.setString(5, s.getCompany());
            ps.setString(6, s.getCategory());
            ps.setString(7, s.getLastBilled());
            ps.setDouble(8, s.getPrevBalance());

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Update supplier */
    public static void updateSupplier(Supplier s) {
        int numericId = Integer.parseInt(s.getId().substring(2));
        String sql = "UPDATE suppliers SET name=?, printName=?, phone=?, address=?, company=?, category=?, lastBilled=?, prevBalance=? WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getName());
            ps.setString(2, s.getPrintName());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getAddress());
            ps.setString(5, s.getCompany());
            ps.setString(6, s.getCategory());
            ps.setString(7, s.getLastBilled());
            ps.setDouble(8, s.getPrevBalance());
            ps.setInt(9, numericId);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Delete supplier */
    public static void deleteSupplier(Supplier s) {
        int numericId = Integer.parseInt(s.getId().substring(2));
        String sql = "DELETE FROM suppliers WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, numericId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Check if supplier exists (by name & company) */
    private static boolean exists(String name, String company) {
        String sql = "SELECT COUNT(*) FROM suppliers WHERE name=? AND company=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, company);
            ResultSet rs = ps.executeQuery();
            return rs.getInt(1) > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
