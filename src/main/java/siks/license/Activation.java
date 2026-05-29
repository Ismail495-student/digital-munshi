package siks.license;

import java.nio.file.*;
import java.util.List;

public class Activation {

    private static final String SECRET = "SIKS-ERP-2026";

    private static final String PATH =
            System.getenv("PROGRAMDATA") + "\\SIKS\\license.dat";

    public static boolean activate(String inputKey) {

        try {

            String machineId = MachineId.get();
            String expected = sha256(machineId + SECRET);

            if (!expected.equals(inputKey)) {
                return false;
            }

            String data =
                    "STATUS=LICENSED\n" +
                            "KEY=" + inputKey;

            Files.write(Paths.get(PATH), data.getBytes());

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private static String sha256(String input) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes());

        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}