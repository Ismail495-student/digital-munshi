package siks.utils;

import siks.models.InvoiceItem;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

public class MiniReceiptBuilder {

    /** Build ESC/POS bytes for mini receipt */
    public static byte[] buildMiniReceiptBytes(
            String invoiceId,
            String dateTime,
            String customerName,
            List<InvoiceItem> items
    ) throws Exception {

        int pageWidth = 540;   // 80mm printer width
        int rowHeight = 35;
        int headerHeight = 60;
        int footerHeight = 40;
        int fontSize = 28;

        // ---------- Shop Title ----------
        BufferedImage shopTitle = ReceiptBuilder.createUrduImage(
                "ABDUL JABBAR & SONS", pageWidth, headerHeight, 42
        );

        // ---------- Customer Info ----------
        String type=InvoiceHandler.getInvoiceTypeById(Integer.parseInt(invoiceId));
        {
            if(Objects.equals(type, "PURCHASE")){ type="Supplier: ";}
            else type ="Customer: ";
        }
        BufferedImage custInfo = new BufferedImage(pageWidth, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = custInfo.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, pageWidth, 120);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, fontSize));
        g.drawString("Invoice #: " + invoiceId, 10, 30);
        g.drawString("Date: " + dateTime, 10, 65);
        g.drawString(type + customerName, 10, 100);
        g.dispose();

        // ---------- Items Table ----------
        int tableHeight = rowHeight + (items != null ? items.size() * rowHeight : 0);
        BufferedImage tableImg = new BufferedImage(pageWidth, tableHeight, BufferedImage.TYPE_INT_RGB);
        g = tableImg.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, pageWidth, tableHeight);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, fontSize));

        int xItem = 10;
        int xCtn = 320;
        int xPcs = 400;

        // Header row
        g.drawString("Item", xItem, rowHeight - 5);
        g.drawString("Ctn", xCtn, rowHeight - 5);
        g.drawString("Pcs", xPcs, rowHeight - 5);
        g.drawLine(0, rowHeight, pageWidth, rowHeight);

        int y = rowHeight * 2 - 5;
        int totalItems = 0;
        int totalCtn = 0;
        double totalPcs = 0;

        if (items != null) {
            for (InvoiceItem item : items) {
                g.drawString(item.getItemName(), xItem, y);
                g.drawString(String.valueOf(item.getCartons()), xCtn, y);
                g.drawString(String.valueOf(item.getPieces()), xPcs, y);
                g.drawLine(0, y + 5, pageWidth, y + 5);

                totalItems++;
                totalCtn += item.getCartons();
                totalPcs += item.getPieces();
                y += rowHeight;
            }
        }
        g.dispose();

        // ---------- Totals ----------
        int totalsHeight = rowHeight * 2;
        BufferedImage totalsImg = new BufferedImage(pageWidth, totalsHeight, BufferedImage.TYPE_INT_RGB);
        g = totalsImg.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, pageWidth, totalsHeight);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, fontSize));

        y = 30;
        g.drawString("Total Items: " + totalItems, 10, y); y += rowHeight;
        g.drawString("Total Qty: " + totalCtn + " Ctn, " + totalPcs + " Pcs", 10, y);
        g.dispose();

        // ---------- Footer ----------
        BufferedImage footer = ReceiptBuilder.createUrduImage(
                "GATE PASS ONLY", pageWidth, footerHeight, 30
        );

        // ---------- Combine All ----------
        int totalHeight = shopTitle.getHeight() + custInfo.getHeight() + tableImg.getHeight()
                + totalsImg.getHeight() + footer.getHeight();
        BufferedImage finalImg = new BufferedImage(pageWidth, totalHeight, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2 = finalImg.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, pageWidth, totalHeight);

        int offsetY = 0;
        g2.drawImage(shopTitle, 0, offsetY, null); offsetY += shopTitle.getHeight();
        g2.drawImage(custInfo, 0, offsetY, null); offsetY += custInfo.getHeight();
        g2.drawImage(tableImg, 0, offsetY, null); offsetY += tableImg.getHeight();
        g2.drawImage(totalsImg, 0, offsetY, null); offsetY += totalsImg.getHeight();
        g2.drawImage(footer, 0, offsetY, null);
        g2.dispose();

        // ---------- Convert to ESC/POS bytes ----------
        return EscPosImageHelper.convertImageToEscPos(finalImg);
    }

    /** Print mini receipt with correct customer name */
    public static void printMiniReceipt(String invoiceId, String customerName, List<InvoiceItem> items) {
        try {
            // Current date placeholder
            String dateTime = java.time.LocalDate.now().toString();
            byte[] data = buildMiniReceiptBytes(invoiceId, dateTime, customerName, items);
            data = PrintUtil.formatFor80mm(data);
            PrintUtil.printEscPos(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}