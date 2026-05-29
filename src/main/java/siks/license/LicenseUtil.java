package siks.license;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class LicenseUtil {

    private static final String SECRET = "ERP_SECRET_2026_KEY";

    public static String generate(String machineId) {

        try {
            String clean = machineId.trim().toUpperCase();

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec key = new SecretKeySpec(
                    SECRET.getBytes("UTF-8"),
                    "HmacSHA256"
            );

            mac.init(key);

            byte[] hash = mac.doFinal(clean.getBytes("UTF-8"));

            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}