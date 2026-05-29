package siks.utils;

import siks.models.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerHandler {

    private static final String DB_URL = "jdbc:sqlite:database.db";

    /** Create customer table if not exists */
    public static void createCustomerTable() {
        String sql = "CREATE TABLE IF NOT EXISTS customers (" +
                "id TEXT PRIMARY KEY," +
                "name TEXT UNIQUE NOT NULL," +
                "printName TEXT," +
                "phone TEXT," +
                "address TEXT," +
                "category TEXT," +
                "lastBilled TEXT," +
                "prevBalance REAL" +
                ")";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Auto-generate new Customer ID */
    public static String generateCustomerId() {
        String sql = "SELECT id FROM customers ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                String lastId = rs.getString("id"); // CU005
                int num = Integer.parseInt(lastId.substring(2));
                num++;
                return String.format("CU%03d", num);
            } else {
                return "CU001";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "CU001";
        }
    }

    /** Add new customer */
    public static boolean addCustomer(Customer customer) {
        String sql = "INSERT INTO customers(id, name, printName, phone, address, category, lastBilled, prevBalance) " +
                "VALUES(?,?,?,?,?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getId());
            ps.setString(2, customer.getName());
            ps.setString(3, customer.getPrintName());
            ps.setString(4, customer.getPhone());
            ps.setString(5, customer.getAddress());
            ps.setString(6, customer.getCategory());
            ps.setString(7, customer.getLastBilled());
            ps.setDouble(8, customer.getPrevBalance());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("⚠ Add Customer Error: " + e.getMessage());
            return false;
        }
    }

    /** Update existing customer */
    public static boolean updateCustomer(Customer customer) {
        String sql = "UPDATE customers SET name=?, printName=?, phone=?, address=?, category=?, lastBilled=?, prevBalance=? WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getPrintName());
            ps.setString(3, customer.getPhone());
            ps.setString(4, customer.getAddress());
            ps.setString(5, customer.getCategory());
            ps.setString(6, customer.getLastBilled());
            ps.setDouble(7, customer.getPrevBalance());
            ps.setString(8, customer.getId());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("⚠ Update Customer Error: " + e.getMessage());
            return false;
        }
    }

    /** Delete customer */
    public static boolean deleteCustomer(String id) {
        String sql = "DELETE FROM customers WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("⚠ Delete Customer Error: " + e.getMessage());
            return false;
        }
    }

    /** Get all customers */
    public static List<Customer> getAllCustomers() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Customer c = new Customer(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("printName"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("category"),
                        rs.getString("lastBilled"),
                        rs.getDouble("prevBalance")
                );
                list.add(c);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    public static String getCustomerAddress(String customerName) {
        String address = null;

        // Database connection string (adjust path if needed)
        String url = "jdbc:sqlite:database.db";

        // SQL query to fetch address of the given customer name
        String query = "SELECT address FROM customers WHERE name = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            // Set parameter for prepared statement
            stmt.setString(1, customerName);

            // Execute the query
            ResultSet rs = stmt.executeQuery();

            // If record found, get address
            if (rs.next()) {
                address = rs.getString("address");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return address;  // Return null if not found
    }

    // 🔹 All customers
    public static Map<String, Double> getAllPrevBalance() {
        Map<String, Double> map = new HashMap<>();
        String query = "SELECT name, prevBalance FROM customers";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                map.put(rs.getString("name"), rs.getDouble("prevBalance"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    // 🔹 Single customer
    public static double getPrevBalanceByCustomer(String name) {
        String query = "SELECT prevBalance FROM customers WHERE name = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getDouble("prevBalance");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.0;
    }


        // Method to get phone number by customer name
        public static String getNoByName(String name) {
            String phone = null;
            String query = "SELECT phone FROM customers WHERE name = ?";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                ps.setString(1, name); // set name parameter
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    phone = rs.getString("phone"); // get phone value
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return phone;
        }

    }


