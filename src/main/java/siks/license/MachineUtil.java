package siks.license;

import java.net.NetworkInterface;
import java.util.Collections;

public class MachineUtil {

    public static String getMachineId() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {

                if (ni.isLoopback() || ni.isVirtual()) continue;

                byte[] mac = ni.getHardwareAddress();

                if (mac == null) continue;

                StringBuilder sb = new StringBuilder();

                for (byte b : mac) {
                    sb.append(String.format("%02X", b));
                }

                return sb.toString();
            }
        } catch (Exception ignored) {}

        return "UNKNOWN";
    }
}