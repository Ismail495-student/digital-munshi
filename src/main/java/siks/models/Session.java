package siks.models;

public class Session {

    private static User currentUser; // private for safety

    // Set current logged-in user
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    // Get current user
    public static User getCurrentUser() {
        return currentUser;
    }

    // Check if current user is admin
    public static boolean isAdmin() {
        return currentUser != null && "admin".equalsIgnoreCase(currentUser.getRole());
    }

    // Check if current user has specific permission (optional, future-proof)
    public static boolean hasPermission(String permission) {
        if (currentUser == null) return false;
        // Example: you can add a list of permissions in User class later
        if ("ADD_PAYMENT".equalsIgnoreCase(permission)) {
            return isAdmin(); // Only admin can add payment for now
        }
        return false;
    }
}