package yondoko.images;

public class SubImage2D implements Image2D {
    protected Image2D source;
    protected int ox, oy;
    protected int width, height;

    public SubImage2D() {
        source = null;
        ox = oy = width = height = 0;
    }

    public SubImage2D(Image2D source, int x, int y, int width, int height) {
        changeParameters(source, x, y, width, height);
    }

    public void changeParameters(Image2D source, int ox, int oy, int width, int height) {
        this.source = source;
        this.ox = ox;
        this.oy = oy;
        this.width = width;
        this.height = height;
        checkArgumentsConsistency();
    }

    public void changeSource(Image2D source) {
        changeParameters(source, ox, oy, width, height);
    }

    public void changeRectangle(int ox, int oy, int width, int height) {
      changeParameters(source, ox, oy, width, height);
        }

    private void checkArgumentsConsistency() {
        if (ox < 0 || ox >= source.getWidth()) {
            throw new RuntimeException("origin x is out of range");
        }
        if (oy < 0 || oy >= source.getHeight()) {
            throw new RuntimeException("origin y is out of range");
        }
        if (width <= 0 || ox + width > source.getWidth()) {
            throw new RuntimeException("invalid sub-image horizontal extent");
        }
        if (height <= 0 || oy + height > source.getHeight()) {
            throw new RuntimeException("invalid sub-image vertical extent");
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
        return source.getNumChannels();
    }

    @Override
    public float get(int x, int y, int channel) {
        return source.get(ox + x, oy + y, channel);
    }

    @Override
    public void get(int x, int y, float[] values) {
        source.get(ox + x, oy + y, values);
    }
}
