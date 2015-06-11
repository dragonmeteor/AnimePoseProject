package yondoko.util;

import java.nio.ByteBuffer;

public class BufferUtil {
    public static void setLittleEndianFloat(ByteBuffer buffer,
                                            int floatIndex, float v) {
        int iv = Float.floatToIntBits(v);
        byte a = (byte) ((iv >> 24) & 0xff);
        byte b = (byte) ((iv >> 16) & 0xff);
        byte c = (byte) ((iv >> 8) & 0xff);
        byte d = (byte) ((iv >> 0) & 0xff);
        buffer.put(4 * floatIndex + 0, d);
        buffer.put(4 * floatIndex + 1, c);
        buffer.put(4 * floatIndex + 2, b);
        buffer.put(4 * floatIndex + 3, a);
    }

    public static float getLittleEndianFloat(ByteBuffer buffer, int floatIndex) {
        int a = (buffer.get(4 * floatIndex + 0) & 0xff);
        int b = (buffer.get(4 * floatIndex + 1) & 0xff);
        int c = (buffer.get(4 * floatIndex + 2) & 0xff);
        int d = (buffer.get(4 * floatIndex + 3) & 0xff);
        int iv = ((d << 24) | (c << 16) | (b << 8) | (a));
        return Float.intBitsToFloat(iv);
    }

    public static void setLittleEndianInt(ByteBuffer buffer, int intIndex, int iv) {
        byte a = (byte) ((iv >> 24) & 0xff);
        byte b = (byte) ((iv >> 16) & 0xff);
        byte c = (byte) ((iv >> 8) & 0xff);
        byte d = (byte) ((iv >> 0) & 0xff);
        buffer.put(4 * intIndex + 0, d);
        buffer.put(4 * intIndex + 1, c);
        buffer.put(4 * intIndex + 2, b);
        buffer.put(4 * intIndex + 3, a);
    }

    public static int getLittleEndianInt(ByteBuffer buffer, int intIndex) {
        int a = (buffer.get(4 * intIndex + 0) & 0xff);
        int b = (buffer.get(4 * intIndex + 1) & 0xff);
        int c = (buffer.get(4 * intIndex + 2) & 0xff);
        int d = (buffer.get(4 * intIndex + 3) & 0xff);
        int iv = ((d << 24) | (c << 16) | (b << 8) | (a));
        return iv;
    }

}
