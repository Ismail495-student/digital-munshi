package siks.license;

import java.security.MessageDigest;

public class MachineId {

    public static String get() {
        try {
            String data =
                    System.getProperty("os.name") +
                            System.getProperty("os.arch") +
                            System.getProperty("user.name") +
                            System.getenv("PROCESSOR_IDENTIFIER");

            return sha256(data);
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private static String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes("UTF-8"));

        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}