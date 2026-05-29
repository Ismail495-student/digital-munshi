package siks.license;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LicenseFileManager {

    private static final Path PATH =
            Paths.get(System.getProperty("user.home"), ".siks_license.dat");

    public static void save(String key) throws Exception {
        Files.write(PATH, key.getBytes());
    }

    public static String read() throws Exception {
        return new String(Files.readAllBytes(PATH));
    }

    public static boolean exists() {
        return Files.exists(PATH);
    }
}