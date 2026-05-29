package siks.utils;

import siks.models.InvoiceItem;

import java.sql.*;
import java.util.List;

public class EditInvoiceHandler {

    public static boolean updateLastInvoice(
            int invoiceId,
            String partyName,
            List<InvoiceItem> newItems,
            double newPayment
    ) {

        String getInvoiceSql = "SELECT * FROM invoices WHERE id=?";
        String getOldItemsSql = "SELECT * FROM invoice_items WHERE invoice_id=?";
        String deleteItemsSql = "DELETE FROM invoice_items WHERE invoice_id=?";
        String insertItemSql =
                "INSERT INTO invoice_items(invoice_id,item_id,itemName,rate,cartons,pieces,amount) VALUES(?,?,?,?,?,?,?)";
        String updateInvoiceSql =
                "UPDATE invoices SET totalBill=?, payment=?, remaining=? WHERE id=?";
        String updateCustomerSql =
                "UPDATE customers SET prevBalance = prevBalance + ? WHERE name=?";
        String getItemSql =
                "SELECT id, stockQuantity, piecesPerCarton FROM items WHERE name=? OR printName=? LIMIT 1";
        String updateStockSql =
                "UPDATE items SET stockQuantity=? WHERE id=?";

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // =====================================================
            // 1. LOAD OLD INVOICE
            // =====================================================
            PreparedStatement psInv = conn.prepareStatement(getInvoiceSql);
            psInv.setInt(1, invoiceId);
            ResultSet rsInv = psInv.executeQuery();

            double oldTotal = 0;
            double oldPayment = 0;
            double oldRemaining = 0;

            if (rsInv.next()) {
                oldTotal = rsInv.getDouble("totalBill");
                oldPayment = rsInv.getDouble("payment");
                oldRemaining = rsInv.getDouble("remaining");
            }

            rsInv.close();
            psInv.close();

            // =====================================================
            // 2. LOAD OLD ITEMS + REVERSE STOCK (PIECES ONLY)
            // =====================================================
            PreparedStatement psOld = conn.prepareStatement(getOldItemsSql);
            psOld.setInt(1, invoiceId);
            ResultSet rsOld = psOld.executeQuery();

            double oldItemsTotal = 0;

            PreparedStatement psGetItem = conn.prepareStatement(getItemSql);
            PreparedStatement psStock = conn.prepareStatement(updateStockSql);

            while (rsOld.next()) {

                String name = rsOld.getString("itemName");
                int cartons = rsOld.getInt("cartons");
                double pieces = rsOld.getDouble("pieces");

                double amount = rsOld.getDouble("amount");
                oldItemsTotal += amount;

                psGetItem.setString(1, name);
                psGetItem.setString(2, name);

                ResultSet irs = psGetItem.executeQuery();

                if (irs.next()) {

                    int itemId = irs.getInt("id");
                    int ppc = irs.getInt("piecesPerCarton");
                    double stock = irs.getDouble("stockQuantity");

                    // CONVERT OLD TO PIECES
                    double totalPieces = (cartons * ppc) + pieces;

                    // REVERSE STOCK
                    stock += totalPieces;

                    psStock.setDouble(1, stock);
                    psStock.setInt(2, itemId);
                    psStock.executeUpdate();
                }

                irs.close();
            }

            rsOld.close();
            psOld.close();

            // =====================================================
            // 3. DELETE OLD ITEMS
            // =====================================================
            PreparedStatement psDel = conn.prepareStatement(deleteItemsSql);
            psDel.setInt(1, invoiceId);
            psDel.executeUpdate();
            psDel.close();

            // =====================================================
            // 4. APPLY NEW ITEMS (PIECES ONLY)
            // =====================================================
            PreparedStatement psInsert = conn.prepareStatement(insertItemSql);

            double newItemsTotal = 0;

            for (InvoiceItem it : newItems) {

                double amount = it.getAmount();
                newItemsTotal += amount;

                psGetItem.setString(1, it.getItemName());
                psGetItem.setString(2, it.getItemName());

                ResultSet irs = psGetItem.executeQuery();

                if (irs.next()) {

                    int itemId = irs.getInt("id");
                    int ppc = irs.getInt("piecesPerCarton");
                    double stock = irs.getDouble("stockQuantity");

                    // CONVERT NEW TO PIECES
                    double totalPieces = (it.getCartons() * ppc) + it.getPieces();

                    // APPLY STOCK
                    stock -= totalPieces;

                    psStock.setDouble(1, stock);
                    psStock.setInt(2, itemId);
                    psStock.executeUpdate();

                    psInsert.setInt(2, itemId);
                } else {
                    psInsert.setNull(2, Types.INTEGER);
                }

                irs.close();

                psInsert.setInt(1, invoiceId);
                psInsert.setString(3, it.getItemName());
                psInsert.setDouble(4, it.getRate());
                psInsert.setInt(5, it.getCartons());
                psInsert.setDouble(6, it.getPieces());
                psInsert.setDouble(7, amount);

                psInsert.executeUpdate();
            }

            psInsert.close();
            psGetItem.close();
            psStock.close();

            // =====================================================
            // 5. FINAL CALCULATION
            // =====================================================
            double oldBase = oldTotal - oldItemsTotal;
            double newTotal = oldBase + newItemsTotal;
            double newRemaining = newTotal - newPayment;

            // =====================================================
            // 6. UPDATE INVOICE
            // =====================================================
            PreparedStatement psUpd = conn.prepareStatement(updateInvoiceSql);
            psUpd.setDouble(1, newTotal);
            psUpd.setDouble(2, newPayment);
            psUpd.setDouble(3, newRemaining);
            psUpd.setInt(4, invoiceId);
            psUpd.executeUpdate();
            psUpd.close();

            // =====================================================
            // 7. UPDATE CUSTOMER BALANCE
            // =====================================================
            double diff = newRemaining - oldRemaining;

            PreparedStatement psCust = conn.prepareStatement(updateCustomerSql);
            psCust.setDouble(1, diff);
            psCust.setString(2, partyName);
            psCust.executeUpdate();
            psCust.close();

            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();

            try {
                if (conn != null) conn.rollback();
            } catch (Exception ignored) {}

            return false;

        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (Exception ignored) {}
        }
    }
}