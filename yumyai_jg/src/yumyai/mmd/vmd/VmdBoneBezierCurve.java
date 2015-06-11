package yumyai.mmd.vmd;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import org.apache.commons.io.input.SwappedDataInputStream;

public class VmdBoneBezierCurve
{
    public final Vector2f x1 = new Vector2f();
    public final Vector2f x2 = new Vector2f();
    public final Vector2f y1 = new Vector2f();
    public final Vector2f y2 = new Vector2f();
    public final Vector2f z1 = new Vector2f();
    public final Vector2f z2 = new Vector2f();
    public final Vector2f r1 = new Vector2f();
    public final Vector2f r2 = new Vector2f();

    public VmdBoneBezierCurve() {
        x1.set(1.0f / 127.0f, 1.0f / 127.0f);
        x2.set(126.0f / 127.0f, 126.0f / 127.0f);
        y1.set(1.0f / 127.0f, 1.0f / 127.0f);
        y2.set(126.0f / 127.0f, 126.0f / 127.0f);
        z1.set(1.0f / 127.0f, 1.0f / 127.0f);
        z2.set(126.0f / 127.0f, 126.0f / 127.0f);
        r1.set(1.0f / 127.0f, 1.0f / 127.0f);
        r2.set(126.0f / 127.0f, 126.0f / 127.0f);
    }

    public void read(SwappedDataInputStream fin) throws IOException
    {
        x1.x = fin.readByte() / 127.0f;
        y1.x = fin.readByte() / 127.0f;
        z1.x = fin.readByte() / 127.0f;
        r1.x = fin.readByte() / 127.0f;

        x1.y = fin.readByte() / 127.0f;
        y1.y = fin.readByte() / 127.0f;
        z1.y = fin.readByte() / 127.0f;
        r1.y = fin.readByte() / 127.0f;

        x2.x = fin.readByte() / 127.0f;
        y2.x = fin.readByte() / 127.0f;
        z2.x = fin.readByte() / 127.0f;
        r2.x = fin.readByte() / 127.0f;

        x2.y = fin.readByte() / 127.0f;
        y2.y = fin.readByte() / 127.0f;
        z2.y = fin.readByte() / 127.0f;
        r2.y = fin.readByte() / 127.0f;

        for (int i = 0; i < 48; i++)
        {
            fin.readByte();
        }
    }

    public void set(VmdBoneBezierCurve other) {
        this.x1.set(other.x1);
        this.x2.set(other.x2);
        this.y1.set(other.y1);
        this.y2.set(other.y2);
        this.z1.set(other.z1);
        this.z2.set(other.z2);
        this.r1.set(other.r1);
        this.r2.set(other.r2);
    }

    private byte convertToByte(float x) {
        return (byte) (x * 127.0f);
    }

    private void write16Bytes(DataOutputStream fout, byte[] b, int start) throws IOException {
        for (int i = 0; i < 16; i++) {
            fout.writeByte(b[i + start]);
        }
    }

    public void write(DataOutputStream fout) throws IOException {
        byte[] b = new byte[16+3];

        b[0] = convertToByte(x1.x);
        b[1] = convertToByte(y1.x);
        b[2] = convertToByte(z1.x);
        b[3] = convertToByte(r1.x);

        b[4] = convertToByte(x1.y);
        b[5] = convertToByte(y1.y);
        b[6] = convertToByte(z1.y);
        b[7] = convertToByte(r1.y);

        b[8]  = convertToByte(x2.x);
        b[9]  = convertToByte(y2.x);
        b[10] = convertToByte(z2.x);
        b[11] = convertToByte(r2.x);

        b[12] = convertToByte(x2.y);
        b[13] = convertToByte(y2.y);
        b[14] = convertToByte(z2.y);
        b[15] = convertToByte(r2.y);

        b[16] = 1;
        b[17] = 0;
        b[18] = 0;

        write16Bytes(fout, b, 0);
        write16Bytes(fout, b, 1);
        write16Bytes(fout, b, 2);
        write16Bytes(fout, b, 3);
    }
}
