package siks.license;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CryptoUtil {

    private static final String ALGO = "AES/ECB/PKCS5Padding";
    private static final String SECRET = "MySuperSecretKey"; // 16 chars

    public static String encrypt(String data) throws Exception {

        SecretKeySpec key = new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "AES"
        );

        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        return Base64.getEncoder().encodeToString(
                cipher.doFinal(data.getBytes(StandardCharsets.UTF_8))
        );
    }

    public static String decrypt(String data) throws Exception {

        SecretKeySpec key = new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "AES"
        );

        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decoded = Base64.getDecoder().decode(data);

        return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
    }
}