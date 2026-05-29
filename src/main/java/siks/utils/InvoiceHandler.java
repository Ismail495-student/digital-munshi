package siks.utils;

import siks.models.Invoice;
import siks.models.InvoiceItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InvoiceHandler {
    private static final String URL = "jdbc:sqlite:database.db";

    public static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Save invoice: type = "SALE" or "PURCHASE"
     * items: list of InvoiceItem
     * prevBalance logic: totalBill = prevBalance + itemsSum; remaining = totalBill - payment; set prevBalance = remaining
     */
    public static boolean saveInvoice(String type, String partyName, List<InvoiceItem> items, double payment) {
        String invoicesSql = "INSERT INTO invoices(type, partyName, totalBill, payment, remaining, dateTime) VALUES(?,?,?,?,?,?)";
        String insertItemSql = "INSERT INTO invoice_items(invoice_id, item_id, itemName, rate, cartons, pieces, amount) VALUES(?,?,?,?,?,?,?)";
        String updateItemStockSql = "UPDATE items SET stockQuantity = ? WHERE id = ?";
        String getItemSql = "SELECT id, piecesPerCarton, stockQuantity FROM items " +
                "WHERE name = ? OR printName = ? OR id = ? LIMIT 1";
        String getPrevBalanceSql = type.equalsIgnoreCase("SALE") ?
                "SELECT prevBalance FROM customers WHERE name = ?" :
                "SELECT prevBalance FROM suppliers WHERE name = ?";
        String updatePrevBalanceSql = type.equalsIgnoreCase("SALE") ?
                "UPDATE customers SET prevBalance = ? WHERE name = ?" :
                "UPDATE suppliers SET prevBalance = ? WHERE name = ?";

        Connection conn = null;
        PreparedStatement insertInvoiceStmt = null;
        PreparedStatement insertItemStmt = null;
        PreparedStatement updateStockStmt = null;
        PreparedStatement getItemStmt = null;
        PreparedStatement getPrevStmt = null;
        PreparedStatement updPrevStmt = null;
        ResultSet generatedKeys = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // get current prevBalance
            double prevBalance = 0.0;
            getPrevStmt = conn.prepareStatement(getPrevBalanceSql);
            getPrevStmt.setString(1, partyName);
            try (ResultSet rs = getPrevStmt.executeQuery()) {
                if (rs.next()) prevBalance = rs.getDouble("prevBalance");
            }
            // compute items sum (we will also need numeric item id & stock updates)
            double itemsSum = 0.0;
            insertInvoiceStmt = conn.prepareStatement(invoicesSql, Statement.RETURN_GENERATED_KEYS);

            // first compute itemsSum
            for (InvoiceItem it : items) itemsSum += it.getAmount();

            double totalBill = prevBalance + itemsSum;
            double remaining = totalBill - payment;
            String dateTime = now();

            // insert invoice
            insertInvoiceStmt.setString(1, type);
            insertInvoiceStmt.setString(2, partyName);
            insertInvoiceStmt.setDouble(3, Math.round(totalBill * 100.0) / 100.0);
            insertInvoiceStmt.setDouble(4, payment);
            insertInvoiceStmt.setDouble(5, Math.round(remaining* 100.0) / 100.0);
            insertInvoiceStmt.setString(6, dateTime);
            insertInvoiceStmt.executeUpdate();
            generatedKeys = insertInvoiceStmt.getGeneratedKeys();
            int invoiceId;
            if (generatedKeys.next()) invoiceId = generatedKeys.getInt(1);
            else throw new SQLException("Failed to obtain invoice id.");

            // prepare statements for item lookup and insertion & stock update
            insertItemStmt = conn.prepareStatement(insertItemSql);
            getItemStmt = conn.prepareStatement(getItemSql);
            updateStockStmt = conn.prepareStatement(updateItemStockSql);

            // For each invoice item: find numeric item id (if exists), compute new stock, insert invoice_items
            for (InvoiceItem it : items) {
                // try to find numeric id for the item by name
                Integer numericItemId = null;
                int piecesPerCarton = 1;
                double currentStock = 0.0;
                getItemStmt.setString(1, it.getItemName());  // For name = ?
                getItemStmt.setString(2, it.getItemName());  // For printName = ?
                getItemStmt.setString(3, (it.getItemId() != null) ? it.getItemId().replaceAll("\\D","") : "");
                try (ResultSet rs = getItemStmt.executeQuery()) {
                    if (rs.next()) {
                        numericItemId = rs.getInt("id");
                        piecesPerCarton = rs.getInt("piecesPerCarton");
                        currentStock = rs.getDouble("stockQuantity");
                    }
                }

                double totalPieces = it.getCartons() * piecesPerCarton + it.getPieces();
                double newStock = currentStock;
                if ("SALE".equalsIgnoreCase(type)) {
                    newStock = currentStock - totalPieces;
                } else { // PURCHASE
                    newStock = currentStock + totalPieces;
                }

                // insert into invoice_items
                insertItemStmt.setInt(1, invoiceId);
                if (numericItemId != null && numericItemId > 0) insertItemStmt.setInt(2, numericItemId); else insertItemStmt.setNull(2, Types.INTEGER);
                insertItemStmt.setString(3, it.getItemName());
                insertItemStmt.setDouble(4, it.getRate());
                insertItemStmt.setInt(5, it.getCartons());
                insertItemStmt.setDouble(6, it.getPieces());
                insertItemStmt.setDouble(7, it.getAmount());
                insertItemStmt.executeUpdate();

                // if item exists in DB, update stock
                if (numericItemId != null && numericItemId > 0) {
                    updateStockStmt.setDouble(1, newStock);
                    updateStockStmt.setInt(2, numericItemId);
                    updateStockStmt.executeUpdate();
                }
            }

            // update party prevBalance = remaining
            updPrevStmt = conn.prepareStatement(updatePrevBalanceSql);
            updPrevStmt.setDouble(1, remaining);
            updPrevStmt.setString(2, partyName);
            updPrevStmt.executeUpdate();
// Update customer's lastBilled date to current invoice date (for SALE)
            if ("SALE".equalsIgnoreCase(type)) {
                String updateLastBilledSql = "UPDATE customers SET lastBilled = ? WHERE name = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateLastBilledSql)) {
                    // Save date in 'yyyy-MM-dd' format (or whatever your FXML expects)
                    String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    ps.setString(1, currentDate);
                    ps.setString(2, partyName);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ignore) {}
            return false;
        } finally {
            try { if (generatedKeys != null) generatedKeys.close(); } catch (Exception ignored) {}
            try { if (insertInvoiceStmt != null) insertInvoiceStmt.close(); } catch (Exception ignored) {}
            try { if (insertItemStmt != null) insertItemStmt.close(); } catch (Exception ignored) {}
            try { if (updateStockStmt != null) updateStockStmt.close(); } catch (Exception ignored) {}
            try { if (getItemStmt != null) getItemStmt.close(); } catch (Exception ignored) {}
            try { if (getPrevStmt != null) getPrevStmt.close(); } catch (Exception ignored) {}
            try { if (updPrevStmt != null) updPrevStmt.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
        }

    }
    public static String getInvoiceTypeById(int id) {
        String type = null;

        String sql = "SELECT type FROM invoices WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    type = rs.getString("type");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return type;
    }
}
