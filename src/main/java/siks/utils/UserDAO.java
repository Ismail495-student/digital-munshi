package siks.utils;

import siks.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private static final String DB_URL = "jdbc:sqlite:database.db"; // apne DB ka path

    /** Get DB connection */

    /** Get user by username */
    public static User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User u = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        new ArrayList<>() // empty perms for now, will fetch separately
                );
                return u;
            }
        }
        return null;
    }

    /** Get permissions for a user */
    public static List<String> getUserPermissions(int userId) throws SQLException {
        List<String> perms = new ArrayList<>();
        String sql = "SELECT p.name FROM permissions p " +
                "JOIN user_permissions up ON p.id = up.permission_id " +
                "WHERE up.user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                perms.add(rs.getString("name"));
            }
        }
        return perms;
    }

    /** Add new user with permissions */
    public static void addUser(String username, String password, String role, List<String> permissions) throws SQLException {
        String insertUser = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        String getPermId = "SELECT id FROM permissions WHERE name = ?";
        String insertUserPerm = "INSERT INTO user_permissions (user_id, permission_id) VALUES (?, ?)";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false); // start transaction
            try (PreparedStatement stmtUser = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                stmtUser.setString(1, username);
                stmtUser.setString(2, password);
                stmtUser.setString(3, role);
                stmtUser.executeUpdate();

                ResultSet generatedKeys = stmtUser.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);

                    try (PreparedStatement stmtPermId = conn.prepareStatement(getPermId);
                         PreparedStatement stmtUserPerm = conn.prepareStatement(insertUserPerm)) {

                        for (String perm : permissions) {
                            stmtPermId.setString(1, perm);
                            ResultSet rs = stmtPermId.executeQuery();
                            if (rs.next()) {
                                int permId = rs.getInt("id");
                                stmtUserPerm.setInt(1, userId);
                                stmtUserPerm.setInt(2, permId);
                                stmtUserPerm.executeUpdate();
                            }
                        }
                    }
                }
                conn.commit();
            } catch (SQLException ex){
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }

    }
    // ================= UPDATE USER =================
    public static void updateUser(String username, String password, String role, List<String> permissions) throws SQLException {

        String getUserId = "SELECT id FROM users WHERE username = ?";
        String updateUser;

        if (password == null || password.trim().isEmpty()) {
            updateUser = "UPDATE users SET role = ? WHERE username = ?";
        } else {
            updateUser = "UPDATE users SET password = ?, role = ? WHERE username = ?";
        }

        String deleteOldPerms = "DELETE FROM user_permissions WHERE user_id = ?";
        String getPermId = "SELECT id FROM permissions WHERE name = ?";
        String insertUserPerm = "INSERT INTO user_permissions (user_id, permission_id) VALUES (?, ?)";

        try (Connection conn = DBUtil.getConnection()) {

            conn.setAutoCommit(false);

            try {

                int userId = 0;

                // Get user id
                try (PreparedStatement ps = conn.prepareStatement(getUserId)) {

                    ps.setString(1, username);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        userId = rs.getInt("id");
                    } else {
                        throw new SQLException("User not found!");
                    }
                }

                // Update user
                try (PreparedStatement ps = conn.prepareStatement(updateUser)) {

                    if (password == null || password.trim().isEmpty()) {
                        ps.setString(1, role);
                        ps.setString(2, username);
                    } else {
                        ps.setString(1, password);
                        ps.setString(2, role);
                        ps.setString(3, username);
                    }

                    ps.executeUpdate();
                }

                // Delete old permissions
                try (PreparedStatement ps = conn.prepareStatement(deleteOldPerms)) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }

                // Insert new permissions
                try (PreparedStatement stmtPermId = conn.prepareStatement(getPermId);
                     PreparedStatement stmtUserPerm = conn.prepareStatement(insertUserPerm)) {

                    for (String perm : permissions) {

                        stmtPermId.setString(1, perm);
                        ResultSet rs = stmtPermId.executeQuery();

                        if (rs.next()) {

                            int permId = rs.getInt("id");

                            stmtUserPerm.setInt(1, userId);
                            stmtUserPerm.setInt(2, permId);
                            stmtUserPerm.executeUpdate();
                        }
                    }
                }

                conn.commit();

            } catch (Exception ex) {

                conn.rollback();
                throw ex;

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
    // ================= DELETE USER =================
    public static void deleteUser(String username) throws SQLException {

        String getUserId = "SELECT id FROM users WHERE username = ?";
        String deletePerms = "DELETE FROM user_permissions WHERE user_id = ?";
        String deleteUser = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DBUtil.getConnection()) {

            conn.setAutoCommit(false);

            try {

                int userId = 0;

                // Get user id
                try (PreparedStatement ps = conn.prepareStatement(getUserId)) {

                    ps.setString(1, username);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        userId = rs.getInt("id");
                    } else {
                        throw new SQLException("User not found!");
                    }
                }

                // Delete permissions first
                try (PreparedStatement ps = conn.prepareStatement(deletePerms)) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }

                // Delete user
                try (PreparedStatement ps = conn.prepareStatement(deleteUser)) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }

                conn.commit();

            } catch (Exception ex) {

                conn.rollback();
                throw ex;

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String role = rs.getString("role");
                List<String> perms = getUserPermissions(id);
                users.add(new User(id, username, password, role, perms));
            }
        }
        return users;
    }

}