package siks.utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import siks.models.Item;
import siks.models.ItemStock;

import java.sql.*;

public class ItemHandler {

    public static ObservableList<Item> getItems() {
        ObservableList<Item> list = FXCollections.observableArrayList();
        String query = "SELECT * FROM items ORDER BY id";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String id = String.format("IT%03d", rs.getInt("id"));
                Item item = new Item(
                        id,
                        rs.getString("name"),
                        rs.getString("printName"),
                        rs.getDouble("purchaseRate"),
                        rs.getDouble("retailRate"),
                        rs.getDouble("wholesaleRate"),
                        rs.getInt("piecesPerCarton"),
                        rs.getString("itemCategory"),
                        rs.getDouble("stockQuantity"),
                        rs.getString("company")
                );
                list.add(item);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean addItem(Item item) {
        if (exists(item.getName())) return false;

        String sql = "INSERT INTO items (name, printName, purchaseRate, retailRate, wholesaleRate, " +
                "piecesPerCarton, itemCategory, stockQuantity, company) VALUES (?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getPrintName());
            ps.setDouble(3, item.getPurchaseRate());
            ps.setDouble(4, item.getRetailRate());
            ps.setDouble(5, item.getWholesaleRate());
            ps.setInt(6, item.getPiecesPerCarton());
            ps.setString(7, item.getItemCategory());
            ps.setDouble(8, item.getStockQuantity());
            ps.setString(9, item.getCompany());

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void updateItem(Item item) {
        int numericId = Integer.parseInt(item.getId().substring(2));
        String sql = "UPDATE items SET name=?, printName=?, purchaseRate=?, retailRate=?, wholesaleRate=?, " +
                "piecesPerCarton=?, itemCategory=?, stockQuantity=?, company=? WHERE id=?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getPrintName());
            ps.setDouble(3, item.getPurchaseRate());
            ps.setDouble(4, item.getRetailRate());
            ps.setDouble(5, item.getWholesaleRate());
            ps.setInt(6, item.getPiecesPerCarton());
            ps.setString(7, item.getItemCategory());
            ps.setDouble(8, item.getStockQuantity());
            ps.setString(9, item.getCompany());
            ps.setInt(10, numericId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteItem(Item item) {
        int numericId = Integer.parseInt(item.getId().substring(2));
        String sql = "DELETE FROM items WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numericId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean exists(String name) {

        String sql = "SELECT COUNT(*) FROM items WHERE LOWER(name)=LOWER(?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name.trim());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
    public static String getPrintName(String itemName) {
        if (itemName == null || itemName.isEmpty()) return "";

        String printName = itemName; // fallback
        String sql = "SELECT printName FROM items WHERE name = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPrintName = rs.getString("printName");
                    if (dbPrintName != null && !dbPrintName.isEmpty()) {
                        printName = dbPrintName;

                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return printName;
    }

    public static ItemStock updateStockQuantity(String itemName, int cartons, int pieces) {

        Item item = getItemByName(itemName);
        if (item == null) return null;

        int ppc = item.getPiecesPerCarton(); // pieces per carton
        int oldPieces = (int) item.getStockQuantity();
        int newPieces = cartons * ppc + pieces;

        // Determine change type
        String changeType;
        if (newPieces > oldPieces) changeType = "INCREASE";
        else if (newPieces < oldPieces) changeType = "DECREASE";
        else changeType = "UNCHANGED";

        // Update stock in database
        String sql = "UPDATE items SET stockQuantity = ? WHERE name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newPieces);
            ps.setString(2, itemName);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // Generate lastEdited timestamp
        String lastEdited = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Convert back to cartons/pieces for UI display
        int updatedCartons = newPieces / ppc;
        int updatedPieces = newPieces % ppc;

        return new ItemStock(item.getId(), item.getName(), updatedCartons, updatedPieces, lastEdited, changeType);
    }

    public static Item getItemByName(String name) {

        String sql = "SELECT * FROM items WHERE name = ? OR printName = ? LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Item(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("printName"),
                            rs.getDouble("purchaseRate"),
                            rs.getDouble("retailRate"),
                            rs.getDouble("wholesaleRate"),
                            rs.getInt("piecesPerCarton"),
                            rs.getString("itemCategory"),
                            rs.getDouble("stockQuantity"),
                            rs.getString("company")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("⚠ Error fetching item by name: " + e.getMessage());
        }

        // If no match found or exception occurred
        return null;
    }
    public static int getPiecesPerCarton(String itemName) {
        if (itemName == null || itemName.isEmpty()) return 0; // fallback

        int piecesPerCarton = 0; // default fallback

        String sql = "SELECT piecesPerCarton FROM items WHERE name = ? OR printName=? LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    piecesPerCarton = rs.getInt("piecesPerCarton"); // SQLite returns 0 if NULL
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
          showAlert();

        }

        return piecesPerCarton;
    }

    private static void showAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText("PPC Failed");
        alert.showAndWait();
    }
}
