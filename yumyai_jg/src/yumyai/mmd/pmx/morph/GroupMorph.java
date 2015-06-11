package yumyai.mmd.pmx.morph;

import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;
import yumyai.mmd.pmx.PmxMorph;

import java.io.IOException;
import java.util.ArrayList;

public class GroupMorph extends PmxMorph {
    public final ArrayList<Offset> offsets = new ArrayList<Offset>();

    public void readOffsets(SwappedDataInputStream fin, int morphIndexSize) throws IOException {
        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            Offset offset = new Offset();
            offset.read(fin, morphIndexSize);
            offsets.add(offset);
        }
    }

    public static class Offset {
        public int morphIndex;
        public float morphFraction;

        public void read(SwappedDataInputStream fin, int morphIndexSize) throws IOException {
            morphIndex = BinaryIo.readIntGivenSizeInBytes(fin, morphIndexSize, true);
            morphFraction = fin.readFloat();
        }
    }
}
