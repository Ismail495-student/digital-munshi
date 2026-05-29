package siks.models;

import java.util.List;

public class User {

    private int id;
    private String username;
    private String password;
    private String role; // "admin" or "user"
    private List<String> permissions; // button names

    public User(int id, String username, String password, String role, List<String> permissions) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.permissions = permissions;
    }

    // Getters & Setters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    // Check if user has a specific permission
    public boolean hasPermission(String perm) {
        if ("admin".equals(role)) return true; // admin always has full access
        return permissions.contains(perm);
    }
}