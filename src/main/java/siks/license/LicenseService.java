package siks.license;

public class LicenseService {

    public static boolean isLicensed() {
        try {
            if (!LicenseFileManager.exists()) return false;

            String key = LicenseFileManager.read();
            return LicenseValidator.validate(key);

        } catch (Exception e) {
            return false;
        }
    }

    public static boolean activate(String key) {
        try {
            if (LicenseValidator.validate(key)) {
                LicenseFileManager.save(key);
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }
}