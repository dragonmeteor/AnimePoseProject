package yumyai.mmd.vmd;

import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

public class VmdMorphKeyframe {
    public int frameNumber;
    public float weight;

    public VmdMorphKeyframe() {
        // NOP
    }

    public VmdMorphKeyframe(VmdMorphKeyframe other) {
        this.frameNumber = other.frameNumber;
        this.weight = other.weight;
    }

    public void read(SwappedDataInputStream reader) throws IOException {
        frameNumber = reader.readInt();
        weight = reader.readFloat();
    }

    public void write(DataOutputStream fout) throws IOException {
        BinaryIo.writeLittleEndianInt(fout, frameNumber);
        BinaryIo.writeLittleEndianFloat(fout, weight);
    }
}
