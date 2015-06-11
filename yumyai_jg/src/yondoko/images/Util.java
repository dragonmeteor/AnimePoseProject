package yondoko.images;

public class Util {
    public static float getBufferedImageRGBChannel(int rgb, int channel) {
        switch (channel) {
            case 0:
                return ((rgb & 0x00ff0000) >> 16) * 1.0f / 255;
            case 1:
                return ((rgb & 0x0000ff00) >> 8) * 1.0f / 255;
            case 2:
                return ((rgb & 0x000000ff)) * 1.0f / 255;
            case 3:
                return ((rgb & 0xff000000) >> 24) * 1.0f / 255;
            default:
                throw new RuntimeException(String.format("invalid channel (%d)", channel));
        }
    }

    public static void unpackBufferedImageRGB(int rgb, float[] values) {
        values[0] = ((rgb & 0x00ff0000) >> 16) * 1.0f / 255;
        values[1] = ((rgb & 0x0000ff00) >> 8) * 1.0f / 255;
        values[2] = ((rgb & 0x000000ff)) * 1.0f / 255;
        values[3] = ((rgb & 0xff000000) >> 24) * 1.0f / 255;
    }

    public static void unpackBufferedImageRGB(int rgb, int[] values) {
        values[0] = ((rgb & 0x00ff0000) >> 16);
        values[1] = ((rgb & 0x0000ff00) >> 8);
        values[2] = ((rgb & 0x000000ff));
        values[3] = ((rgb & 0xff000000) >> 24);
    }

    public static int replaceBufferedImageRGBChannel(int rgb, int channel, float value) {
        value = Math.min(1, Math.max(0, value));
        int v = (int)Math.round(255*value);
        switch (channel) {
            case 0:
                rgb = ((rgb & ~(0x00ff0000)) | (v << 16));
                break;
            case 1:
                rgb = ((rgb & ~(0x0000ff00)) | (v << 8));
                break;
            case 2:
                rgb = ((rgb & ~(0x000000ff)) | (v << 0));
                break;
            case 3:
                rgb = ((rgb & ~(0xff000000)) | (v << 24));
                break;
            default:
                throw new RuntimeException(String.format("invalid channel (%d)", channel));
        }
        return rgb;
    }

    public static int packBufferedImageRGB(float[] values) {
        int rgb = 0;
        rgb = replaceBufferedImageRGBChannel(rgb, 0, values[0]);
        rgb = replaceBufferedImageRGBChannel(rgb, 1, values[1]);
        rgb = replaceBufferedImageRGBChannel(rgb, 2, values[2]);
        rgb = replaceBufferedImageRGBChannel(rgb, 3, values[3]);
        return rgb;
    }

    /**
     * Copy the whole of the given source image to the destination image so that
     * the top-left corner of the copied region is at point (x,y).
     *
     * @param source the source image
     * @param dest the destination image
     * @param x the x-coordinate of the top-left corner of the copied portion
     * @param y the y-coordinate of the top-left corner of the copied portion
     */
    public static void blit(Image2D source, WritableImage2D dest, int x, int y) {
        if (source.getNumChannels() != dest.getNumChannels()) {
            throw new RuntimeException("source and destimation image does not have the same number of channels");
        }
        float[] values = new float[source.getNumChannels()];
        for (int dx = 0; dx < source.getWidth(); dx++) {
            for (int dy = 0; dy < source.getHeight(); dy++) {
                source.get(dx, dy, values);
                if (x + dx >= 0 && x + dx < dest.getWidth() &&
                        y + dy >= 0 && y + dy < dest.getHeight()) {
                    dest.set(x + dx, y + dy, values);
                }
            }
        }
    }

    public static double l2DistanceSquared(Image2D A, Image2D B) {
        if (A.getWidth() != B.getWidth() || A.getHeight() != B.getHeight()) {
            throw new RuntimeException("the two images do not have the same size");
        }
        if (A.getNumChannels() != B.getNumChannels()) {
            throw new RuntimeException("the two images do not have the same number of channels");
        }
        float[] av = new float[A.getNumChannels()];
        float[] bv = new float[A.getNumChannels()];
        double result = 0;
        for (int x = 0; x < A.getWidth(); x++) {
            for (int y = 0; y < A.getHeight(); y++) {
                A.get(x, y, av);
                B.get(x, y, bv);
                for (int k = 0; k < A.getNumChannels(); k++) {
                    result += (av[k] - bv[k]) * (av[k] - bv[k]);
                }
            }
        }
        return result;
    }

    public static boolean sameSize(Image2D A, Image2D B) {
        return A.getWidth() == B.getWidth() &&
                A.getHeight() == B.getHeight();
    }

    public static boolean checkSize(Image2D A, int width, int height) {
        return A.getWidth() == width && A.getHeight() == height;
    }
}
