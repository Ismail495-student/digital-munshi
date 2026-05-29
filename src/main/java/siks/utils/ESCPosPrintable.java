package siks.utils;

import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.Graphics;

public class ESCPosPrintable implements Printable {

    private final byte[] data;

    public ESCPosPrintable(byte[] data) {
        this.data = data;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) return NO_SUCH_PAGE;
        graphics.drawBytes(data, 0, data.length, 0, 10);
        return PAGE_EXISTS;
    }
}