package yondoko.images;

public interface WritableImage2D extends Image2D {
    public void set(int x, int y, int channel, float value);
    public void set(int x, int y, float[] values);
}
