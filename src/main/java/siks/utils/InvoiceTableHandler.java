package siks.utils;

import siks.models.Invoice;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceTableHandler {
    private static final String DB_URL = "jdbc:sqlite:database.db";

    /** Get all invoices */
    public static List<Invoice> getInvoices() {
        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT id, type, PartyName, totalBill, payment, remaining, dateTime FROM invoices ORDER BY dateTime DESC";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Invoice inv = new Invoice(
                        rs.getString("Id"),
                        rs.getString("type"),
                        rs.getString("PartyName"),
                        rs.getDouble("totalBill"),
                        rs.getDouble("payment"),
                        rs.getDouble("remaining"),
                        rs.getString("dateTime")
                );
                list.add(inv);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
