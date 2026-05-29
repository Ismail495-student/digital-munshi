package siks.utils;

import siks.models.ItemCheckResult;

import java.sql.*;
import java.util.*;

public class InvoiceCheckHandler {

    private static final String URL = "jdbc:sqlite:database.db";

    /**
     * Returns unique items (from given invoice IDs) whose stockQuantity <= 0
     */
    public static List<ItemCheckResult> getUnavailableItemsForInvoices(List<String> invoiceIds) {
        // 🔹 Use LinkedHashMap to avoid duplicates (preserves order)
        Map<String, ItemCheckResult> uniqueUnavailable = new LinkedHashMap<>();

        if (invoiceIds == null || invoiceIds.isEmpty()) return new ArrayList<>();

        // 🧩 Build placeholders (?, ?, ?)
        String placeholders = String.join(",", Collections.nCopies(invoiceIds.size(), "?"));

        String sql = "SELECT ii.itemName, i.id AS invoiceId " +
                "FROM invoice_items ii " +
                "JOIN invoices i ON i.id = ii.invoice_id " +
                "WHERE i.id IN (" + placeholders + ")";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 🔹 Bind numeric IDs (remove letters like SI, PI)
            for (int i = 0; i < invoiceIds.size(); i++) {
                String raw = invoiceIds.get(i).trim();
                String numeric = raw.replaceAll("\\D", ""); // e.g. "SI0005" → "5"
                ps.setInt(i + 1, Integer.parseInt(numeric));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String itemName = rs.getString("itemName");
                    String invoiceId = rs.getString("invoiceId");

                    // ✅ Skip if already processed this item
                    if (uniqueUnavailable.containsKey(itemName)) continue;

                    double stockQty = 0;

                    String stockSql = "SELECT stockQuantity FROM items WHERE name = ? OR printName = ?";
                    try (PreparedStatement ps2 = conn.prepareStatement(stockSql)) {
                        ps2.setString(1, itemName);
                        ps2.setString(2, itemName);
                        try (ResultSet rs2 = ps2.executeQuery()) {
                            if (rs2.next()) stockQty = rs2.getDouble("stockQuantity");
                        }
                    }

                    if (stockQty <= 0) {
                        String displayId = "INV" + String.format("%04d", Integer.parseInt(invoiceId));
                        uniqueUnavailable.put(itemName, new ItemCheckResult(displayId, itemName, stockQty));
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 🔹 Return only unique values
        return new ArrayList<>(uniqueUnavailable.values());
    }
}