package yondoko.images;

public interface Image2D {
    public int getWidth();
    public int getHeight();
    public int getNumChannels();
    public float get(int x, int y, int channel);
    public void get(int x, int y, float[] values);
}
