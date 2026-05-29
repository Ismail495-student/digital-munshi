package siks.utils;

import javafx.scene.control.TableView;
import siks.controllers.CustomerReportController.LedgerRow;

import java.awt.*;
import java.awt.image.BufferedImage;

public class VoucherMiniPrinter {

    private static final int PAGE_WIDTH = 540;   // 80mm printer width
    private static final int ROW_HEIGHT = 35;
    private static final int HEADER_HEIGHT = 60;
    private static final int FOOTER_HEIGHT = 100;
    private static final int FONT_SIZE = 28;

    /**
     * Print top row (last transaction) from filtered ledger table
     * Works for Invoice or Voucher automatically
     */
    public static void printTopLedgerRow(String customerName, TableView<LedgerRow> tblLedger) {
        if (tblLedger == null || tblLedger.getItems().isEmpty()) return;

        LedgerRow row = tblLedger.getItems().get(0); // filtered top row
        if (row == null) return;

        try {
            // ---------- Shop Title ----------
            BufferedImage shopTitle = ReceiptBuilder.createUrduImage(
                    "ABDUL JABBAR & SONS", PAGE_WIDTH, HEADER_HEIGHT, 42
            );

            // ---------- Customer Info ----------
            BufferedImage custInfo = new BufferedImage(PAGE_WIDTH, ROW_HEIGHT * 4, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = custInfo.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, PAGE_WIDTH, custInfo.getHeight());
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, FONT_SIZE));

            int y = ROW_HEIGHT - 5;
            g.drawString("Customer: " + customerName, 10, y); y += ROW_HEIGHT;
            g.drawString("Type: " + row.getType(), 10, y); y += ROW_HEIGHT;
            g.drawString("Transaction: " + row.getDescription(), 10, y); y += ROW_HEIGHT;
            g.drawString("Date: " + row.getDate(), 10, y);
            g.dispose();

            // ---------- Last Transaction Details ----------
            BufferedImage transactionImg = new BufferedImage(PAGE_WIDTH, ROW_HEIGHT * 3, BufferedImage.TYPE_INT_RGB);
            g = transactionImg.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, PAGE_WIDTH, transactionImg.getHeight());
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, FONT_SIZE));

            y = ROW_HEIGHT - 5;
            g.drawString("Before Payment: " + String.format("%.2f", row.getTotalBefore()), 10, y); y += ROW_HEIGHT;
            g.drawString("Payment: " + row.getPayment(), 10, y); y += ROW_HEIGHT;
            g.drawString("Remaining: " + String.format("%.2f", row.getRemaining()), 10, y);
            g.dispose();

            // ---------- Footer ----------
            BufferedImage footer = ReceiptBuilder.createFooterImage("Become Filer Now by AJ Consultants\nAdv M Waseem:  0304-8699100\n For Software:03289958157", PAGE_WIDTH, FOOTER_HEIGHT, 20);

            // ---------- Combine All ----------
            int totalHeight = shopTitle.getHeight() + custInfo.getHeight() + transactionImg.getHeight() + footer.getHeight();
            BufferedImage finalImg = new BufferedImage(PAGE_WIDTH, totalHeight, BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D g2 = finalImg.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, PAGE_WIDTH, totalHeight);

            int offsetY = 0;
            g2.drawImage(shopTitle, 0, offsetY, null); offsetY += shopTitle.getHeight();
            g2.drawImage(custInfo, 0, offsetY, null); offsetY += custInfo.getHeight();
            g2.drawImage(transactionImg, 0, offsetY, null); offsetY += transactionImg.getHeight();
            g2.drawImage(footer, 0, offsetY, null);
            g2.dispose();

            // ---------- Convert to ESC/POS bytes and Print ----------
            byte[] data = EscPosImageHelper.convertImageToEscPos(finalImg);
            data = PrintUtil.formatFor80mm(data);
            PrintUtil.printEscPos(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}