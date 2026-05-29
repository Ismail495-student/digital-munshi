package siks.license;

public class LicenseValidator {

    public static boolean validate(String licenseKey) {
        try {

            String decrypted = CryptoUtil.decrypt(licenseKey).trim();

            String expected = (MachineUtil.getMachineId() + "|SIKS_APP").trim();

            return decrypted.equals(expected);

        } catch (Exception e) {
            return false;
        }
    }
}