package siks.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.text.FontWeight;
import siks.models.InvoiceItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReceiptBuilder {

    /** ------------------- Node builder for JavaFX preview ------------------- */
    public static Node buildReceiptNode(
            String invoiceId,
            String dateTime,
            String customerName,
            List<InvoiceItem> items,
            double previousBalance,
            double itemsTotal,
            double totalBill,
            double payment,
            double remaining
    ) {
        VBox root = new VBox(5);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.TOP_CENTER);
        root.setPrefWidth(280);
        root.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1;");

        // ---------- HEADER ----------
        Label shopName = new Label("Abdul Jabbar & Sons");
        shopName.setFont(Font.font("Algerian", 16));
        shopName.setStyle("-fx-font-weight: bold;");
        Label shopAddress = new Label(" Vehari Road Mailsi");
        shopAddress.setFont(Font.font(10));
        Label contact = new Label("Ph:03157363000, 03007736372");
        contact.setFont(Font.font(10));

        VBox header = new VBox(shopName, shopAddress, contact);
        header.setAlignment(Pos.CENTER);
        root.getChildren().add(header);
        root.getChildren().add(new Separator());

        // ---------- INVOICE INFO ----------
        Label lblInvoice = new Label("Invoice #: " + invoiceId);
        Label lblDate = new Label("Date: " + dateTime);
        HBox invoiceBox = new HBox(20, lblInvoice, lblDate);
        invoiceBox.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().add(invoiceBox);

        // ---------- CUSTOMER INFO ----------
        String prefix = invoiceId.substring(0, 2);

        String type =
                prefix.equals("SI") ? "SALE"
                        : prefix.equals("PI") ? "PURCHASE"
                        : "";

        if (Objects.equals(type, "PURCHASE")) {
            type = "Supplier: ";
        } else {
            type = "Customer: ";
        }

        Label lblCust = new Label(type+ customerName);
        Label lblPrev = new Label(String.format("Previous Balance: %.2f", previousBalance));
        VBox custBox = new VBox(lblCust, lblPrev);
        custBox.setSpacing(2);
        root.getChildren().add(custBox);
        root.getChildren().add(new Separator());

        // ---------- ITEM TABLE ----------
        VBox tableBox = new VBox(3);
        tableBox.setPrefWidth(Double.MAX_VALUE);

        // Header Row
        HBox headerRow = createRow("Item", "Rate", "Ctn", "Pcs", "Amount");
        headerRow.setStyle("-fx-font-weight: bold; -fx-background-color: #f0f0f0; -fx-padding: 3 0 3 0;");
        tableBox.getChildren().add(headerRow);

        int totalItems = 0;
        int totalCartons = 0;
        double totalPieces = 0;

        if (items != null) {
            for (InvoiceItem item : items) {
                totalItems++;
                totalCartons += item.getCartons();
                totalPieces += item.getPieces();

                HBox row = createRow(
                        item.getItemName(),
                        String.format("%.2f", item.getRate()),
                        String.valueOf(item.getCartons()),
                        String.valueOf(item.getPieces()),
                        String.format("%.2f", item.getAmount())
                );
                tableBox.getChildren().add(row);
                tableBox.getChildren().add(new Separator());
            }
        }

        root.getChildren().add(tableBox);

        // ---------- TOTALS ----------
        HBox totalsArea = new HBox(15);
        totalsArea.setAlignment(Pos.TOP_CENTER);

        // Left summary
        VBox summaryLeft = new VBox(2);
        summaryLeft.setAlignment(Pos.CENTER_LEFT);
        Label lblTotalItems = new Label("Total Items: " + totalItems);
        Label lblTotalQty = new Label("Total Qty: " + totalCartons + " Ctn, " + (int) totalPieces + " Pcs");
        summaryLeft.getChildren().addAll(lblTotalItems, lblTotalQty);

        // Right summary (bill)
        VBox summaryRight = new VBox(3);
        summaryRight.setAlignment(Pos.CENTER_RIGHT);
        Label lblItemsTotal = new Label(String.format("Items Total: %.2f", itemsTotal));
        Label lblPrevBal = new Label(String.format("Prev Balance: %.2f", previousBalance));
        Label lblTotalBill = new Label(String.format("Total Bill: %.2f", totalBill));
        Label lblPayment = new Label(String.format("Payment: %.2f", payment));
        Label lblRemaining = new Label(String.format("Remaining: %.2f", remaining));
        lblTotalBill.setStyle("-fx-font-weight: bold;");
        lblRemaining.setStyle("-fx-font-weight: bold;");
        summaryRight.getChildren().addAll(lblItemsTotal, lblPrevBal, lblTotalBill, lblPayment, lblRemaining);

        totalsArea.getChildren().addAll(summaryLeft, new VBox(), summaryRight);
        HBox.setHgrow(summaryRight, Priority.ALWAYS);
        root.getChildren().add(totalsArea);

        // ---------- FOOTER ----------
        Label thankYou = new Label("Filer Now! Adv M Wasim: 0304-8699100");
        thankYou.setFont(Font.font(10));
        VBox footer = new VBox(new Separator(), thankYou);
        footer.setAlignment(Pos.CENTER);
        root.getChildren().add(footer);

        return root;
    }

    private static HBox createRow(String item, String rate, String ctn, String pcs, String amt) {
        Label lblItem = new Label(item);
        Label lblRate = new Label(rate);
        Label lblCtn = new Label(ctn);
        Label lblPcs = new Label(pcs);
        Label lblAmt = new Label(amt);
        lblItem.setFont(Font.font("Jameel Noori Nastaleeq",FontWeight.BOLD, 14));
        lblItem.setPrefWidth(150);
        lblRate.setPrefWidth(60);
        lblCtn.setPrefWidth(30);
        lblPcs.setPrefWidth(40);
        lblAmt.setPrefWidth(50);

        HBox row = new HBox(10, lblItem, lblRate, lblCtn, lblPcs, lblAmt);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** ------------------- ESC/POS printer bytes builder ------------------- */
    public static byte[] buildReceiptBytes(
            String invoiceId,
            String dateTime,
            String customerName,
            List<InvoiceItem> items,
            double previousBalance,
            double itemsTotal,
            double totalBill,
            double payment,
            double remaining
    ) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Initialize printer
            baos.write(new byte[]{0x1B, 0x40}); // ESC @

            // ---------- HEADER ----------
            baos.write(new byte[]{0x1B, 0x61, 1}); // Center align
            baos.write(new byte[]{0x1D, 0x21, 0x11});// Double height & width
            baos.write(new byte[]{0x1B, 0x45, 1});
            baos.write("Abdul Jabbar & Sons \n".getBytes());
            baos.write(new byte[]{0x1D, 0x21, 0x00}); // Back to normal size
            baos.write("Vehari Road, Mailsi\n".getBytes());
            baos.write("Ph: 0315-7363000\n".getBytes());
            baos.write("-----------------------------------------------\n".getBytes());

            // ---------- INVOICE INFO ----------
            String prefix = invoiceId.substring(0, 2);

            String type =
                    prefix.equals("SI") ? "SALE"
                            : prefix.equals("PI") ? "PURCHASE"
                            : "";

            if (Objects.equals(type, "PURCHASE")) {
                type = "Supplier: ";
            } else {
                type = "Customer: ";
            }

            String add= CustomerHandler.getCustomerAddress(customerName);
            baos.write(new byte[]{0x1B, 0x61, 0}); // Left align
            baos.write(("Invoice #: " + invoiceId + "\n").getBytes());
            baos.write(("Date: " + dateTime+ "\n").getBytes());
            baos.write((type + customerName +", " + add+"\n").getBytes());
            baos.write(("Prev Balance: " + String.format("%.2f", previousBalance) + "\n").getBytes());

            baos.write("-----------------------------------------------\n".getBytes());

            // ---------- ITEM TABLE ----------
            baos.write(String.format("%-18s %6s %4s %4s %10s\n",
                    "Item", "Rate", "Ctn", "Pcs", "Amount").getBytes());
            baos.write("-----------------------------------------------\n".getBytes());

            int totalItems = 0, totalCartons = 0;
            double totalPieces = 0;

            if (items != null) {
                for (InvoiceItem it : items) {
                    totalItems++;
                    totalCartons += it.getCartons();
                    totalPieces += it.getPieces();

                    baos.write(String.format("%-18s %6.2f %4d %4.0f %10.2f\n",
                            truncate(it.getItemName(), 18),
                            it.getRate(),
                            it.getCartons(),
                            it.getPieces(),
                            it.getAmount()).getBytes());
                }
            }

            baos.write("-----------------------------------------------\n".getBytes());

            // ---------- TOTALS ----------
            baos.write(("Total Items: " + totalItems + "\n").getBytes());
            baos.write(("Total Qty: " + totalCartons + " Ctn, " + (int) totalPieces + " Pcs\n").getBytes());
            baos.write(makeLeftRight("Items Total:", String.format("%.2f", itemsTotal)).getBytes());
            baos.write(makeLeftRight("Prev Balance:", String.format("%.2f", previousBalance)).getBytes());
            baos.write(makeLeftRight("Total Bill:", String.format("%.2f", totalBill)).getBytes());
            baos.write(makeLeftRight("Payment:", String.format("%.2f", payment)).getBytes());
            baos.write(makeLeftRight("Remaining:", String.format("%.2f", remaining)).getBytes());
            baos.write("-----------------------------------------------\n".getBytes());

            // ---------- FOOTER ----------
            baos.write(new byte[]{0x1B, 0x61, 1}); // Center align
            baos.write("Advocate M Waseem     0304-5566100 \n\n\n".getBytes());

            // ---------- CUT ----------
            baos.write(new byte[]{0x1D, 0x56, 0x42, 0x00}); // Full cut

            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }


    private static String truncate(String s, int len){
        if(s.length()<=len) return s;
        return s.substring(0,len-1)+"."; // truncate with dot
    }
    // For 80 mm printer (~48 chars per line)
    private static String makeLeftRight(String left, String right) {
        int totalWidth = 48; // Adjust if your font is narrow
        int spaces = Math.max(1, totalWidth - (left.length() + right.length()));
        return left + " ".repeat(spaces) + right + "\n";
    }

    public static byte[] buildReceiptBytes2(
            String invoiceId,
            String dateTime,
            String customerName,
            List<InvoiceItem> items,
            double previousBalance,
            double itemsTotal,
            double totalBill,
            double payment,
            double remaining
    ) throws Exception {

        int pageWidth = 564;
        int rowHeight = 45;
        int fontSize = 30;

        // ------------------- Shop title -------------------
        String shopText =
                "Abdul Jabbar & Sons\n" +
                        "Vehari Road Mailsi.\n" +
                        "Ph: 0315-7363000";


        String[] linesArr = shopText.split("\n");

        int lineHeight = 45;
        int dynamicHeaderHeight = linesArr.length * lineHeight;

        BufferedImage shopTitle =
                new BufferedImage(pageWidth, dynamicHeaderHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D gTitle = shopTitle.createGraphics();

        gTitle.setColor(Color.WHITE);
        gTitle.fillRect(0, 0, pageWidth, dynamicHeaderHeight);

        gTitle.setColor(Color.BLACK);

        java.awt.Font titleFont =
                new java.awt.Font("Algerian", java.awt.Font.BOLD, 40);

        java.awt.Font subFont =
                new java.awt.Font("Noto Naskh Arabic", java.awt.Font.BOLD, 28);

        int y = 45;

        for (int i = 0; i < linesArr.length; i++) {

            String line = linesArr[i];

            java.awt.Font currentFont =
                    (i == 0) ? titleFont : subFont;

            gTitle.setFont(currentFont);

            FontMetrics fm = gTitle.getFontMetrics(currentFont);

            int textWidth = fm.stringWidth(line);
            int x = (pageWidth - textWidth) / 2;

            gTitle.drawString(line, x, y);

            y += lineHeight;
        }

        gTitle.dispose();

        // ------------------- Customer info -------------------
        BufferedImage custInfo =
                new BufferedImage(pageWidth, 140, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = custInfo.createGraphics();

        java.awt.Font font =
                new java.awt.Font("Jameel Noori Nastaleeq", java.awt.Font.PLAIN, fontSize);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, pageWidth, 140);

        g.setColor(Color.BLACK);
        g.setFont(font);

        String prefix = invoiceId.substring(0, 2);

        String type =
                prefix.equals("SI") ? "SALE"
                        : prefix.equals("PI") ? "PURCHASE"
                        : "";

        type = type.equals("PURCHASE") ? "Supplier: " : "Customer: ";

        String add = CustomerHandler.getCustomerAddress(customerName);

        g.drawString("Invoice #: " + invoiceId, 10, 30);
        g.drawString("Date: " + dateTime, 10, 65);
        g.drawString(type + customerName + ", " + add, 10, 100);
        g.drawString("Prev Balance: " + String.format("%.2f", previousBalance), 10, 135);

        g.dispose();

        // ------------------- Items table -------------------
        int tableHeight =
                rowHeight + (items != null ? items.size() * rowHeight : 0);

        BufferedImage tableImg =
                new BufferedImage(pageWidth, tableHeight, BufferedImage.TYPE_INT_RGB);

        g = tableImg.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, pageWidth, tableHeight);

        g.setColor(Color.BLACK);
        g.setFont(font);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int xItem = 10;
        int xRate = 220;
        int xCtn = 320;
        int xPcs = 375;
        int xAmount = 460;

        // Header rectangle
        g.setStroke(new BasicStroke(3));
        g.drawRect(2, 5, pageWidth - 4, rowHeight);

        y = rowHeight - 8;

        g.drawString("Item", xItem, y);
        g.drawString("Rate", xRate, y);
        g.drawString("Ctn", xCtn, y);
        g.drawString("Pcs", xPcs, y);
        g.drawString("Amount", xAmount, y);

        if (items != null) {

            y += rowHeight;

            g.setStroke(new BasicStroke(1));

            for (InvoiceItem item : items) {

                g.drawString(item.getItemName(), xItem, y);
                g.drawString(String.valueOf((float) item.getRate()), xRate, y);
                g.drawString(String.valueOf(item.getCartons()), xCtn, y);
                g.drawString(String.valueOf((float) item.getPieces()), xPcs, y);
                g.drawString(String.valueOf(Math.round(item.getAmount())), xAmount, y);

                g.drawLine(0, y + 5, pageWidth, y + 5);

                y += rowHeight;
            }
        }

        // Thick ending line
        g.setStroke(new BasicStroke(4));
        g.drawLine(0, y - 5, pageWidth, y - 5);

        g.dispose();

        // ------------------- Totals -------------------
        int totalsRows = 7;
        int totalsHeight = totalsRows * rowHeight + 30;

        BufferedImage totalsImg =
                new BufferedImage(pageWidth, totalsHeight, BufferedImage.TYPE_INT_RGB);

        g = totalsImg.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, pageWidth, totalsHeight);

        g.setColor(Color.BLACK);
        g.setFont(font);

        y = 30;

        int totalItems =
                items != null ? items.size() : 0;

        int totalCtn =
                items != null
                        ? items.stream().mapToInt(InvoiceItem::getCartons).sum()
                        : 0;

        int totalPcs =
                items != null
                        ? items.stream().mapToInt(i -> (int) i.getPieces()).sum()
                        : 0;

        g.drawString("Total Items: " + totalItems, 10, y);
        y += rowHeight;

        g.drawString("Total Qty: " + totalCtn + " Ctn, " + totalPcs + " Pcs", 10, y);
        y += rowHeight;

        drawLabelValue(g, "Items Total:",
                String.format("%.2f", itemsTotal), y, pageWidth, 20);
        y += rowHeight;

        drawLabelValue(g, "Prev Balance:",
                String.format("%.2f", previousBalance), y, pageWidth, 20);
        y += rowHeight;

        // Total Bill
        java.awt.Font totalBillFont =
                new java.awt.Font("Jameel Noori Nastaleeq", java.awt.Font.BOLD, 36);

        g.setFont(totalBillFont);

        g.setStroke(new BasicStroke(4));
        g.drawRect(5, y - 32, pageWidth - 10, 46);

        drawLabelValue(g, "Total Bill:",
                String.format("%.2f", totalBill), y, pageWidth, 20);

        g.setFont(font);

        y += rowHeight;

        drawLabelValue(g, "Payment:",
                String.format("%.2f", payment), y, pageWidth, 20);

        y += rowHeight;

        // Remaining
        java.awt.Font boldFont =
                new java.awt.Font("Noto Naskh Arabic", java.awt.Font.BOLD, 35);

        g.setFont(boldFont);

        drawLabelValue(g, "Remaining:",
                String.format("%.2f", remaining), y, pageWidth, 20);

        // Bold line under remaining
        g.setStroke(new BasicStroke(4));
        g.drawLine(0, y + 10, pageWidth, y + 10);

        g.dispose();

        // ------------------- Footer -------------------
        BufferedImage footer = createFooterImage(
                "ابھی فائلر بنیں !\n" +
                        "سیلز ٹیکس، انکم ٹیکس وغیرہ کی معلومات کے لئے رابطہ کریں\n" +
                        "Advocate M Waseem: 0304-5566100",
                pageWidth,
                140,
                32
        );


        BufferedImage footer2 = createFooterImage(
                "For Software: 03289958157",
                pageWidth,
                45,
                20
        );

        // ------------------- Combine all images -------------------
        int totalHeight =
                shopTitle.getHeight()
                        + custInfo.getHeight()
                        + tableImg.getHeight()
                        + totalsImg.getHeight()
                        + footer.getHeight()
                        + footer2.getHeight();

        BufferedImage finalImg =
                new BufferedImage(pageWidth, totalHeight, BufferedImage.TYPE_BYTE_BINARY);

        Graphics2D g2 = finalImg.createGraphics();

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, pageWidth, totalHeight);

        int offsetY = 0;

        g2.drawImage(shopTitle, 0, offsetY, null);
        offsetY += shopTitle.getHeight();

        g2.drawImage(custInfo, 0, offsetY, null);
        offsetY += custInfo.getHeight();

        g2.drawImage(tableImg, 0, offsetY, null);
        offsetY += tableImg.getHeight();

        g2.drawImage(totalsImg, 0, offsetY, null);
        offsetY += totalsImg.getHeight();

        g2.drawImage(footer, 0, offsetY, null);
        offsetY += footer.getHeight();

        g2.drawImage(footer2, 0, offsetY, null);

        g2.dispose();

        // ------------------- ESC/POS bytes -------------------
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(new byte[]{0x1B, 0x40});

        baos.write(EscPosImageHelper.convertImageToEscPos(finalImg));

        return baos.toByteArray();
    }
    public static BufferedImage createUrduImage(String text, int width, int height, int fontSize) throws Exception {
        // Use Noto Naskh Arabic font (ya installed Arabic-support font)
        java.awt.Font font = new java.awt.Font("ALgerian", java.awt.Font.PLAIN, fontSize);

        // BufferedImage: TYPE_INT_RGB for ESC/POS raster conversion
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);           // background white
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);           // text color black
        g.setFont(font);

        // Urdu text drawing
        FontMetrics fm = g.getFontMetrics();
        int textHeight = fm.getAscent();
        int y = (height + textHeight) / 2 - 5; // vertically center
        g.drawString(text, 10, y);

        g.dispose();
        return img;
    }
    // Helper function to draw label left, value right aligned
    private static void drawLabelValue(Graphics2D g, String label, String value, int y, int pageWidth, int rightMargin) {
        FontMetrics fm = g.getFontMetrics();
        int valueWidth = fm.stringWidth(value);
        int labelY = y;
        g.drawString(label, 10, labelY);
        g.drawString(value, pageWidth - valueWidth - rightMargin, labelY);
    }



        // Optional: if you want to print a whole table


    public static byte[] buildCustomerBalanceReceiptTable(Map<String, Double> data) throws Exception {
        if (data == null || data.isEmpty()) return new byte[0];

        int pageWidth = 540;
        int rowHeight = 40;
        int extraLineHeight = 40;
        int gapAfterLine = 20;
        int startY = 50;

        int totalHeight = startY + 100 + data.size() * (rowHeight + extraLineHeight + gapAfterLine) + 100;

        BufferedImage img = new BufferedImage(pageWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, pageWidth, totalHeight);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fonts
        java.awt.Font titleFont = new java.awt.Font("Noto Naskh Arabic", java.awt.Font.BOLD, 38);
        java.awt.Font dateFont = new java.awt.Font("Noto Naskh Arabic", java.awt.Font.PLAIN, 28);
        java.awt.Font headerFont = new java.awt.Font("Noto Naskh Arabic", java.awt.Font.BOLD, 28);
        java.awt.Font rowFont = new java.awt.Font("Noto Naskh Arabic", java.awt.Font.PLAIN, 26);

        int y = startY;

        // Title
        g.setFont(titleFont);
        String title = "Customer Previous Balance";
        int titleX = (pageWidth - g.getFontMetrics().stringWidth(title)) / 2;
        g.setColor(Color.BLACK);
        g.drawString(title, titleX, y);
        y += 50;

        // Date (next line)
        g.setFont(dateFont);
        String currentDate = new java.text.SimpleDateFormat("dd-MM-yyyy").format(new java.util.Date());
        int dateX = pageWidth - g.getFontMetrics().stringWidth(currentDate) - 20;
        g.drawString(currentDate, dateX, y);
        y += 40;

        // Header
        g.setFont(headerFont);
        int xName = 30;
        int xBalance = (int) (pageWidth * 0.7);

        g.drawRect(10, y - rowHeight + 10, pageWidth - 20, rowHeight);
        g.drawString("Customer Name", xName, y);
        g.drawString("Prev Bal.", xBalance, y);
        g.drawLine(10, y + 10, pageWidth - 10, y + 10);

        y += rowHeight + 10;

        // Rows
        g.setFont(rowFont);

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            String name = entry.getKey();
            double balance = entry.getValue();

            // Line 1: Name + Balance
            g.drawString(name, xName, y);
            g.drawString(String.format("%.2f", balance), xBalance, y);
            y += rowHeight;

            // Line 2: Phone + Address
            String phone = CustomerHandler.getNoByName(name);
            String address = CustomerHandler.getCustomerAddress(name);
            g.drawString(phone != null ? phone : "", xName, y);
            g.drawString(address != null ? address : "", xBalance, y);
            y += extraLineHeight;

            // Gap
            y += gapAfterLine;

            // 🔥 Separator (slightly UP)
            g.drawLine(10, y - 12, pageWidth - 10, y - 12);

            // Move down for next customer
            y += 15;
        }

        // Footer
        g.setFont(rowFont);
        String footer = "Generated by System";
        g.drawString(footer, (pageWidth - g.getFontMetrics().stringWidth(footer)) / 2, y + 50);

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[]{0x1B, 0x40});
        baos.write(EscPosImageHelper.convertImageToEscPos(img));

        return baos.toByteArray();
    }
    // Helper to truncate long strings

    public static BufferedImage createFooterImage(String text, int width, int height, int fontSize) {

        java.awt.Font font =
                new java.awt.Font("Jameel Noori Nastaleeq", java.awt.Font.BOLD, fontSize);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = img.createGraphics();

        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Text settings
        g.setColor(Color.BLACK);
        g.setFont(font);

        // 🔥 Top separator line
        g.drawLine(0, 2, width, 2);

        FontMetrics fm = g.getFontMetrics();

        String[] lines = text.split("\n");

        int lineHeight = fm.getHeight();
        int totalTextHeight = lines.length * lineHeight;

        // line ke neeche text start
        int y = ((height - totalTextHeight) / 2) + fm.getAscent() + 5;

        for (String line : lines) {

            int textWidth = fm.stringWidth(line);
            int x = (width - textWidth) / 2;

            g.drawString(line, x, y);

            y += lineHeight;
        }

        g.dispose();

        return img;
    }
}



