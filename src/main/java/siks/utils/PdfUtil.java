package siks.utils;

import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class PdfUtil {
    private static final String PATH = "D:/Invoices";

    /**
     * Save a JavaFX Node as PDF automatically in D:/Invoices with invoiceId as filename
     */
    public static boolean saveNodeAsPdf(Node node, String invoiceId) {
        if (node == null || invoiceId == null || invoiceId.isEmpty()) return false;

        try {
            // 1️⃣ Ensure folder exists
            File folder = new File("D:/Invoices");
            if (!folder.exists()) folder.mkdirs();

            // 2️⃣ PDF file path
            File file = new File(folder, "INV-" + invoiceId + ".pdf");

            // 3️⃣ Take snapshot
            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(javafx.scene.transform.Transform.scale(2, 2)); // 🔥 upscale

            WritableImage snapshot = node.snapshot(params, null);            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

            // 4️⃣ Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            // 5️⃣ Create PDF
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, "receipt.png");
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // 6️⃣ Fit image to page
            PDRectangle mediaBox = page.getMediaBox();
            float pageWidth = mediaBox.getWidth() - 40;   // 20pt margin left+right
            float pageHeight = mediaBox.getHeight() - 40;  // 20pt margin top+bottom
            float scale = Math.min(pageWidth / pdImage.getWidth(), pageHeight / pdImage.getHeight());
            float imgWidth = pdImage.getWidth() * scale;
            float imgHeight = pdImage.getHeight() * scale;
            float x = (mediaBox.getWidth() - imgWidth) / 2;
            float y = (mediaBox.getHeight() - imgHeight) / 2;

            contentStream.drawImage(pdImage, x, y, imgWidth, imgHeight);
            contentStream.close();

            // 7️⃣ Save and close
            document.save(file);
            document.close();

            System.out.println("✅ PDF saved: " + file.getAbsolutePath());
            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
    public static File generateInvoicePDF(String invoiceId, Node node) {

        if (node == null || invoiceId == null || invoiceId.isEmpty()) return null;

        try {
            // 1️⃣ Ensure folder exists
            File folder = new File(PATH);
            if (!folder.exists()) folder.mkdirs();

            // 2️⃣ PDF file path
            File file = new File(folder, "SI-" + invoiceId + ".pdf");

            // 3️⃣ Make sure node fully rendered
            node.applyCss();


            // 4️⃣ Take snapshot (same as A method)
            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(javafx.scene.transform.Transform.scale(2, 2));

            WritableImage snapshot = node.snapshot(params, null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

            // 5️⃣ Convert image to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            // 6️⃣ Create PDF
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                    document,
                    imageBytes,
                    "invoice.png"
            );

            PDPageContentStream contentStream =
                    new PDPageContentStream(document, page);

            // 7️⃣ Fit image inside page
            PDRectangle mediaBox = page.getMediaBox();

            float pageWidth = mediaBox.getWidth() - 40;
            float pageHeight = mediaBox.getHeight() - 40;

            float scale = Math.min(
                    pageWidth / pdImage.getWidth(),
                    pageHeight / pdImage.getHeight()
            );

            float imgWidth = pdImage.getWidth() * scale;
            float imgHeight = pdImage.getHeight() * scale;

            float x = (mediaBox.getWidth() - imgWidth) / 2;
            float y = (mediaBox.getHeight() - imgHeight) / 2;

            contentStream.drawImage(pdImage, x, y, imgWidth, imgHeight);
            contentStream.close();

            // 8️⃣ Save PDF
            document.save(file);
            document.close();

            System.out.println("✅ PDF saved: " + file.getAbsolutePath());

            return file;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}