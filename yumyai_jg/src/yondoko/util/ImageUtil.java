package yondoko.util;

import javax.vecmath.Color3f;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageUtil {
    public static void unpackRgba(int rgb, int[] output) {
        output[0] = (rgb >> 16) & 0xff;
        output[1] = (rgb >> 8) & 0xff;
        output[2] = (rgb) & 0xff;
        output[3] = (rgb >> 24);
    }

    public static int packRgba(int[] rgba) {
        int output = 0;
        output += (rgba[2] & 0xff);
        output += (rgba[1] & 0xff) << 8;
        output += (rgba[0] & 0xff) << 16;
        output += (rgba[3] << 24);
        return output;
    }

    // From http://stackoverflow.com/questions/13605248/java-converting-image-to-bufferedimage
    public static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage)
        {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    public static Color getAverageBorderColor(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        int[] comps = new int[4];
        Color3f sum = new Color3f(0,0,0);
        int count = 0;
        for (int x = 0; x < w-1; x++) {
            int rgb = image.getRGB(x,0);
            unpackRgba(rgb, comps);
            sum.x += comps[0];
            sum.y += comps[1];
            sum.z += comps[2];
            count++;

            rgb = image.getRGB(1+x, h-1);
            unpackRgba(rgb, comps);
            sum.x += comps[0];
            sum.y += comps[1];
            sum.z += comps[2];
            count++;
        }
        for (int y = 0; y < h-1; y++) {
            int rgb = image.getRGB(w-1,y);
            unpackRgba(rgb, comps);
            sum.x += comps[0];
            sum.y += comps[1];
            sum.z += comps[2];
            count++;

            rgb = image.getRGB(0,y+1);
            unpackRgba(rgb, comps);
            sum.x += comps[0];
            sum.y += comps[1];
            sum.z += comps[2];
            count++;
        }
        int r = (int)(sum.x / count);
        int g = (int)(sum.y / count);
        int b = (int)(sum.z / count);
        Color output = new Color(r, g, b);
        return output;
    }
}
