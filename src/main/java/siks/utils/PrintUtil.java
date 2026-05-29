package siks.utils;

import javafx.print.*;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javax.print.*;
/**
 * PrintUtil for:
 * 1️⃣ ESC/POS Thermal printing via Windows Print Spooler
 * 2️⃣ PDF saving
 * 3️⃣ Auto 80mm receipt width
 */
public class PrintUtil {

    /** --- Save Node as PDF ---
     public static boolean printToPdf(Node node, Stage ownerStage, String preferredPrinterName) {
     Printer targetPrinter = Printer.getAllPrinters().stream()
     .filter(p -> p.getName().toLowerCase().contains(preferredPrinterName.toLowerCase()))
     .findFirst()
     .orElse(Printer.getDefaultPrinter());

     if (targetPrinter == null) return false;

     PrinterJob job = PrinterJob.createPrinterJob(targetPrinter);
     if (job == null) return false;

     PageLayout layout = targetPrinter.getDefaultPageLayout();
     scaleNodeToPage(node, layout);
     boolean printed = job.printPage(node);
     if (printed) job.endJob();

     node.setScaleX(1.0);
     node.setScaleY(1.0);
     return printed;
     }

     --- Thermal printing via Windows Spooler --- */


    /** --- ESC/POS printing via Windows Print Spooler --- */
    public static boolean printEscPos(byte[] data) {
        try {

            // Get default printer
            PrintService printer = PrintServiceLookup.lookupDefaultPrintService();

            if (printer == null) {
                showPrinterAlert("No default printer found. Please set a printer as default.");
                return false;
            }

            DocPrintJob job = printer.createPrintJob();
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;

            Doc doc = new SimpleDoc(data, flavor, null);

            PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
            aset.add(new JobName("Thermal Receipt", null));

            job.print(doc, aset);

            System.out.println("Printed on: " + printer.getName());
            return true;

        } catch (PrintException e) {
            e.printStackTrace();
            showPrinterAlert("Printer error: " + e.getMessage());
            return false;
        }
    }


    /** --- Format ESC/POS bytes for 80mm printers --- */
    public static byte[] formatFor80mm(byte[] data) {
        byte[] init = new byte[]{0x1B, 0x40};       // Initialize printer
        byte[] alignLeft = new byte[]{0x1B, 0x61, 0x00};  // Left align
        byte[] lineSpacing = new byte[]{0x1B, 0x33, 24};  // Line spacing

        byte[] full = new byte[init.length + alignLeft.length + lineSpacing.length + data.length];
        System.arraycopy(init, 0, full, 0, init.length);
        System.arraycopy(alignLeft, 0, full, init.length, alignLeft.length);
        System.arraycopy(lineSpacing, 0, full, init.length + alignLeft.length, lineSpacing.length);
        System.arraycopy(data, 0, full, init.length + alignLeft.length + lineSpacing.length, data.length);

        return full;
    }

    /** --- Helper: scale Node to page layout for PDF --- */
    private static void scaleNodeToPage(Node node, PageLayout layout) {
        double printableW = layout.getPrintableWidth();
        double printableH = layout.getPrintableHeight();

        double nodeW = node.getBoundsInParent().getWidth();
        double nodeH = node.getBoundsInParent().getHeight();
        if (nodeW <= 0) nodeW = node.prefWidth(-1);
        if (nodeH <= 0) nodeH = node.prefHeight(nodeW);

        double scaleX = printableW / nodeW;
        double scaleY = printableH / nodeH;
        double scale = Math.min(scaleX, scaleY);
        if (scale > 1.0) scale = 1.0;

        node.setScaleX(scale);
        node.setScaleY(scale);
    }

    /** --- Test thermal printing --- */
    public static void testPrint(String printerName) {
        String sample = "Hello Thermal Printer!\n-------------------\nThank you!\n\n\n";
        byte[] data = formatFor80mm(sample.getBytes());
        printEscPos(data);
    }
    public static boolean printToPdf(Node node, Stage stage, String printerName, String outputFilePath) {
        try {
            Printer printer = Printer.getAllPrinters().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(printerName))
                    .findFirst()
                    .orElse(Printer.getDefaultPrinter());

            PrinterJob job = PrinterJob.createPrinterJob(printer);
            if (job == null) return false;

            // Important: Set the job settings for PDF output path
            job.getJobSettings().setOutputFile(String.valueOf(new File(outputFilePath)));

            // Optional: scale or fit node to page
            boolean printed = job.printPage(node);
            if (printed) {
                job.endJob();
                return true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
    private static void showPrinterAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Printer Error");
            alert.setHeaderText("Printing Failed");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
