package yondoko.images;

import java.awt.image.BufferedImage;

public class AwtBufferedImage2DAdapter implements WritableImage2D {
    private BufferedImage source;
    private int ox, oy;
    private int width, height;

    public AwtBufferedImage2DAdapter(BufferedImage source) {
        this(source, 0, 0, source.getWidth(), source.getHeight());
    }

    public AwtBufferedImage2DAdapter(BufferedImage source,
                                     int x, int y,
                                     int width, int height) {
        this.source = source;
        this.ox = x;
        this.oy = y;
        this.width = width;
        this.height = height;
        checkArgumentsConsistency();
    }

    private void checkArgumentsConsistency() {
        if (ox < 0 || ox >= source.getWidth()) {
            throw new RuntimeException("origin x is out of range");
        }
        if (oy < 0 || oy >= source.getHeight()) {
            throw new RuntimeException("origin y is out of range");
        }
        if (width <= 0 || ox + width > source.getWidth()) {
            throw new RuntimeException("invalid subimage horizontal extent");
        }
        if (height <= 0 || oy + height > source.getHeight()) {
            throw new RuntimeException("invalid subimage vertical extent");
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getNumChannels() {
        return 4;
    }

    @Override
    public float get(int x, int y, int channel) {
        if (source.getType() != BufferedImage.TYPE_INT_ARGB && source.getType() != BufferedImage.TYPE_4BYTE_ABGR && channel == 3) {
            return 1.0f;
        } else {
            int rgb = source.getRGB(ox+x, oy+y);
            return Util.getBufferedImageRGBChannel(rgb, channel);
        }

    }

    @Override
    public void get(int x, int y, float[] values) {
        Util.unpackBufferedImageRGB(source.getRGB(ox+x, oy+y), values);
        if (source.getType() != BufferedImage.TYPE_INT_ARGB && source.getType() != BufferedImage.TYPE_4BYTE_ABGR)
            values[3] = 1.0f;
    }

    @Override
    public void set(int x, int y, int channel, float value) {
        int rgb = source.getRGB(ox+x, oy+y);
        rgb = Util.replaceBufferedImageRGBChannel(rgb, channel, value);
        source.setRGB(ox+x, oy+y, rgb);
    }

    @Override
    public void set(int x, int y, float[] values) {
        int rgb = Util.packBufferedImageRGB(values);
        source.setRGB(ox+x, oy+y, rgb);
    }
}
