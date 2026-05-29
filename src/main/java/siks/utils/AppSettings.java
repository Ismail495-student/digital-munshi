package siks.utils;

import java.io.*;
import java.util.Properties;

public class AppSettings {

    private static final String USER_DIR = System.getProperty("user.home") + File.separator + "FinalAppData";
    private static final String CONFIG_FILE = USER_DIR + File.separator + "config.properties";

    private static String language = "Urdu"; // default

    static {
        File dir = new File(USER_DIR);
        if (!dir.exists()) dir.mkdirs(); // ensure folder exists
        loadSettings(); // autoload
    }

    public static void setLanguage(String lang) {
        language = lang;
        saveSettings();
    }

    public static String getLanguage() {
        return language;
    }

    public static boolean isUrdu() {
        return "Urdu".equalsIgnoreCase(language);
    }

    private static void loadSettings() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            props.load(input);
            language = props.getProperty("language", "English");
        } catch (IOException ignored) {
            // first-run case — file doesn’t exist yet
        }
    }

    private static void saveSettings() {
        Properties props = new Properties();
        props.setProperty("language", language);
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            props.store(output, "App Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}