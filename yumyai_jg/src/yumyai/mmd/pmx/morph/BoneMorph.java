package yumyai.mmd.pmx.morph;

import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;
import yumyai.mmd.pmx.PmxMorph;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.io.IOException;
import java.util.ArrayList;

public class BoneMorph extends PmxMorph {
    public final ArrayList<Offset> offsets = new ArrayList<Offset>();

    public void readOffsets(SwappedDataInputStream fin, int boneIndexSize) throws IOException {
        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            Offset offset = new Offset();
            offset.read(fin, boneIndexSize);
            offsets.add(offset);
        }
    }

    public static class Offset {
        public int boneIndex;
        public final Vector3f position = new Vector3f();
        public final Quat4f rotation = new Quat4f();

        public void read(SwappedDataInputStream fin, int boneIndexSize) throws IOException {
            BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, true);
            BinaryIo.readTuple3f(fin, position);
            BinaryIo.readTuple4f(fin, rotation);
        }
    }
}
