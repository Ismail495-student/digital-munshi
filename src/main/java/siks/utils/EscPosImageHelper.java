package siks.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class EscPosImageHelper {

    public static byte[] convertImageToEscPos(BufferedImage image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            baos.write(new byte[]{0x1B, 0x40}); // Init printer
            baos.write(new byte[]{0x1B, 0x33, 0x10}); // Line spacing

            for (int y = 0; y < height; y += 24) {

                baos.write(new byte[]{0x1B, 0x2A, 33, (byte)(width % 256), (byte)(width / 256)});
                for (int x = 0; x < width; x++) {
                    for (int k = 0; k < 3; k++) {
                        byte slice = 0;
                        for (int b = 0; b < 8; b++) {
                            int yy = y + (k * 8) + b;
                            int pixel = (yy < height) ? image.getRGB(x, yy) : 0xFFFFFF;
                            int luminance = ((pixel >> 16 & 0xFF) + (pixel >> 8 & 0xFF) + (pixel & 0xFF)) / 3;
                            if (luminance < 90) slice |= (1 << (7 - b));
                        }
                        baos.write(slice);
                    }
                }
                baos.write(0x0A); // Line feed
            }

            baos.write(new byte[]{0x1D, 'V', 66, 0}); // Cut command
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
