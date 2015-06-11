package yumyai.mmd.pmx;

import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

import javax.vecmath.Vector3f;
import java.io.IOException;

public class PmxIkLink {
    public int boneIndex;
    public boolean angleLimited;
    public final Vector3f angleLowerBound = new Vector3f();
    public final Vector3f angleUpperBound = new Vector3f();

    public void read(SwappedDataInputStream fin, int boneIndexSize) throws IOException {
        boneIndex = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, true);
        angleLimited = fin.readByte() == 1;
        if (angleLimited) {
            BinaryIo.readTuple3f(fin, angleLowerBound);
            BinaryIo.readTuple3f(fin, angleUpperBound);
        }
    }
}
