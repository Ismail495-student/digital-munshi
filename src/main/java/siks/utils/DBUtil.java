package siks.utils;

import javafx.scene.control.Alert;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DBUtil {

    // User home folder me ek dedicated folder create karenge DB ke liye
    private static final String USER_DIR = System.getProperty("user.home") + File.separator + "FinalAppData";
    private static final String DB_FILE_NAME = "database.db";

    static {
        try {
            File dir = new File(USER_DIR);

            // folder exist check (optional, but safe)
            if (!dir.exists()) {
                throw new RuntimeException("App data folder not found: " + USER_DIR);
            }

            File dbFile = new File(USER_DIR, DB_FILE_NAME);

            // ❌ IMPORTANT: no auto-copy, no auto-create
            if (!dbFile.exists()) {
                throw new RuntimeException(
                        "Database file missing at: " + dbFile.getAbsolutePath()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,"DB file Missing",
                    "Database file missing or not Found \n" + USER_DIR+"\nMissing File Name:"+DB_FILE_NAME
            );
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    // Single method to get Connection
    public static Connection getConnection() throws SQLException {
        String dbPath = USER_DIR + File.separator + DB_FILE_NAME;
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }
    private static void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

}