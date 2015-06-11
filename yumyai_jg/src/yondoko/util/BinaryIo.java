package yondoko.util;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.vecmath.Tuple2f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Tuple4f;
import javax.vecmath.Tuple4i;

import org.apache.commons.io.input.SwappedDataInputStream;

public class BinaryIo {
    public static void readTuple2f(SwappedDataInputStream fin, Tuple2f v) throws IOException {
        v.x = fin.readFloat();
        v.y = fin.readFloat();
    }

    public static void readTuple3f(SwappedDataInputStream fin, Tuple3f v) throws IOException {
        v.x = fin.readFloat();
        v.y = fin.readFloat();
        v.z = fin.readFloat();
    }

    public static void readTuple4f(SwappedDataInputStream fin, Tuple4f v) throws IOException {
        v.x = fin.readFloat();
        v.y = fin.readFloat();
        v.z = fin.readFloat();
        v.w = fin.readFloat();
    }

    public static void readTuple4i(SwappedDataInputStream fin, Tuple4i v) throws IOException {
        v.x = fin.readInt();
        v.y = fin.readInt();
        v.z = fin.readInt();
        v.w = fin.readInt();
    }

    public static String readShiftJisString(SwappedDataInputStream fin, int length) throws IOException {
        byte[] data = new byte[length];
        int read = fin.read(data, 0, length);
        if (read == -1) {
            throw new EOFException("end of file reached");
        } else {
            int l = 0;
            while (l < data.length && data[l] != '\0') {
                l++;
            }
            byte[] s = new byte[l];
            for (int i = 0; i < l; i++) {
                s[i] = data[i];
            }
            return new String(s, "Shift-JIS");
        }
    }

    public static String readVariableLengthString(SwappedDataInputStream fin, Charset charset) throws IOException {
        int length = fin.readInt();
        byte[] data = new byte[length];
        fin.read(data);
        return new String(data, charset);
    }

    public static void writeByteString(DataOutputStream fout, byte[] b, int length) throws IOException {
        if (b.length < length) {
            fout.write(b);
            for (int i = 0; i < length - b.length; i++) {
                fout.write('\0');
            }
        } else {
            for (int i = 0; i < length; i++) {
                fout.write(b[i]);
            }
        }
    }

    public static void writeString(DataOutputStream fout, String s, int length) throws IOException {
        byte[] b = s.getBytes(Charset.forName("UTF-8"));
        writeByteString(fout, b, length);
    }

    public static void writeString(DataOutputStream fout, String s) throws IOException {
        byte[] b = s.getBytes(Charset.forName("UTF-8"));
        writeByteString(fout, b, b.length);
    }

    public static void writeLittleEndianVaryingLengthString(DataOutputStream fout, String s) throws IOException {
        byte[] b = s.getBytes(Charset.forName("UTF-8"));
        writeLittleEndianInt(fout, b.length);
        writeByteString(fout, b, b.length);
    }

    public static void writeShiftJISString(DataOutputStream fout, String s, int length) throws IOException {
        byte[] b = s.getBytes("Shift-JIS");
        writeByteString(fout, b, length);
    }

    public static void writeLittleEndianTuple3f(DataOutputStream fout, Tuple3f t) throws IOException {
        writeLittleEndianFloat(fout, t.x);
        writeLittleEndianFloat(fout, t.y);
        writeLittleEndianFloat(fout, t.z);
    }

    public static void writeLittleEndianTuple4f(DataOutputStream fout, Tuple4f t) throws IOException {
        writeLittleEndianFloat(fout, t.x);
        writeLittleEndianFloat(fout, t.y);
        writeLittleEndianFloat(fout, t.z);
        writeLittleEndianFloat(fout, t.w);
    }

    public static void writeLittleEndianTuple4i(DataOutputStream fout, Tuple4i t) throws IOException {
        writeLittleEndianInt(fout, t.x);
        writeLittleEndianInt(fout, t.y);
        writeLittleEndianInt(fout, t.z);
        writeLittleEndianInt(fout, t.w);
    }

    public static String readString(SwappedDataInputStream fin, int length) throws IOException {
        byte[] data = new byte[length];
        int read = fin.read(data, 0, length);
        if (read == -1) {
            throw new EOFException("end of file reached");
        } else {
            int l = 0;
            while (l < data.length && data[l] != '\0') {
                l++;
            }
            byte[] s = new byte[l];
            for (int i = 0; i < l; i++) {
                s[i] = data[i];
            }
            return new String(s, Charset.forName("UTF-8"));
        }
    }

    public static String readVariableLengthString(SwappedDataInputStream fin) throws IOException {
        int length = fin.readInt();
        String result = readString(fin, length);
        return result;
    }

    public static void writeLittleEndianShort(DataOutputStream out, short value) throws IOException {
        out.writeByte(value & 0xFF);
        out.writeByte((value >> 8) & 0xFF);
    }

    public static void writeLittleEndianInt(DataOutputStream out, int value) throws IOException {
        out.writeByte(value & 0xFF);
        out.writeByte((value >> 8) & 0xFF);
        out.writeByte((value >> 16) & 0xFF);
        out.writeByte((value >> 24) & 0xFF);
    }

    public static void writeLittleEndianFloat(DataOutputStream out, float value) throws IOException {
        writeLittleEndianInt(out, Float.floatToIntBits(value));
    }

    public static void writeLitteEndianTuple3f(DataOutputStream fout, Tuple3f t) throws IOException
    {
        writeLittleEndianFloat(fout, t.x);
        writeLittleEndianFloat(fout, t.y);
        writeLittleEndianFloat(fout, t.z);
    }

    public static int readIntGivenSizeInBytes(SwappedDataInputStream fin, int size, boolean unsigned) throws IOException {
        if (size == 1) {
            byte b = fin.readByte();
            if (unsigned) {
                return (b & 0xff);
            } else {
                return b;
            }
        } else if (size == 2) {
            short s = fin.readShort();
            if (unsigned) {
                return (s & 0xff00) | (s & 0xff);
            } else {
                return s;
            }
        } else if (size == 4) {
            return fin.readInt();
        } else {
            throw new RuntimeException("invalid size");
        }
    }
}
