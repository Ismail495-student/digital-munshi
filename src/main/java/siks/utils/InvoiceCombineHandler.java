package siks.utils;

import siks.models.CombinedItem;

import java.sql.*;
import java.util.*;

public class InvoiceCombineHandler {

    private static final String URL = "jdbc:sqlite:database.db";

    /**
     * Combine total cartons and pieces from multiple invoices
     */
    public static List<CombinedItem> getCombinedItemsForInvoices(List<String> invoiceIds) {
        Map<String, CombinedItem> combined = new LinkedHashMap<>();

        if (invoiceIds == null || invoiceIds.isEmpty()) return new ArrayList<>();

        String placeholders = String.join(",", Collections.nCopies(invoiceIds.size(), "?"));

        String sql = "SELECT ii.itemName, ii.cartons, ii.pieces, i.id AS invoiceId " +
                "FROM invoice_items ii " +
                "JOIN invoices i ON i.id = ii.invoice_id " +
                "WHERE i.id IN (" + placeholders + ")";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind invoice IDs
            for (int i = 0; i < invoiceIds.size(); i++) {
                String numeric = invoiceIds.get(i).replaceAll("\\D", ""); // e.g. SI0005 → 5
                ps.setInt(i + 1, Integer.parseInt(numeric));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String itemName = rs.getString("itemName");
                    int cartons = rs.getInt("cartons");
                    double pieces = rs.getDouble("pieces");

                    if (combined.containsKey(itemName)) {
                        CombinedItem existing = combined.get(itemName);
                        existing.setTotalCartons(existing.getTotalCartons() + cartons);
                        existing.setTotalPieces(existing.getTotalPieces() + pieces);
                    } else {
                        combined.put(itemName, new CombinedItem(itemName, cartons, pieces));
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new ArrayList<>(combined.values());
    }
}