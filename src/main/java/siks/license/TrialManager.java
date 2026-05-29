package siks.license;

import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class TrialManager {

    private static final String FILE =
            System.getProperty("user.home") + "/.siks_sys.dat";

    private static final int TRIAL_DAYS = 3;

    // ================= INIT ONLY ON FIRST TIME =================
    public static void initTrial() {

        try {
            File f = new File(FILE);

            if (!f.exists()) {
                LocalDate now = LocalDate.now();

                // installDate | lastRunDate | expiredFlag
                write(now.toString(), now.toString(), "0");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= CHECK TRIAL =================
    public static boolean isActive() {

        try {
            File f = new File(FILE);
            if (!f.exists()) return false;

            String[] data = read().split("\\|");

            LocalDate installDate = LocalDate.parse(data[0]);
            LocalDate lastRun = LocalDate.parse(data[1]);
            int expired = Integer.parseInt(data[2]);

            LocalDate today = LocalDate.now();

            // ❌ already expired flag set
            if (expired == 1) return false;

            // ❌ time rollback detection
            if (today.isBefore(lastRun)) {
                markExpired();
                return false;
            }

            long days = ChronoUnit.DAYS.between(installDate, today);

            if (days > TRIAL_DAYS) {
                markExpired();
                return false;
            }

            // update last run
            write(installDate.toString(), today.toString(), "0");

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    // ================= MARK EXPIRED =================
    private static void markExpired() {
        try {
            String[] data = read().split("\\|");

            write(data[0], data[1], "1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= DAYS LEFT =================
    public static long getDaysLeft() {
        try {
            String[] data = read().split("\\|");

            LocalDate install = LocalDate.parse(data[0]);
            long days = ChronoUnit.DAYS.between(install, LocalDate.now());

            return Math.max(0, TRIAL_DAYS - days);

        } catch (Exception e) {
            return 0;
        }
    }

    // ================= FILE OPS =================
    private static void write(String install, String lastRun, String expired) throws IOException {
        FileWriter fw = new FileWriter(FILE);
        fw.write(install + "|" + lastRun + "|" + expired);
        fw.close();
    }

    private static String read() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(FILE));
        return br.readLine();
    }

    public static boolean hasEverStarted() {
        try {
            File f = new File(FILE);

            // file exists → system has been used before
            return f.exists();

        } catch (Exception e) {
            return false;
        }
    }
}