package yumyai.mmd.vmd;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

public class VmdBoneKeyframe {
    public int frameNumber;
    public final Vector3f displacement = new Vector3f();
    public final Quat4f rotation = new Quat4f();
    public final VmdBoneBezierCurve curve = new VmdBoneBezierCurve();

    public VmdBoneKeyframe() {
        // NOP
    }

    public VmdBoneKeyframe(VmdBoneKeyframe other) {
        this.frameNumber = other.frameNumber;
        this.displacement.set(other.displacement);
        this.rotation.set(other.rotation);
        this.curve.set(other.curve);
    }

    public void read(SwappedDataInputStream reader) throws IOException {
        frameNumber = reader.readInt();
        BinaryIo.readTuple3f(reader, displacement);
        //displacement.z = -displacement.z;
        BinaryIo.readTuple4f(reader, rotation);
        //rotation.x = -rotation.x;
        //rotation.y = -rotation.y;
        curve.read(reader);
    }

    public void write(DataOutputStream fout) throws IOException {
        BinaryIo.writeLittleEndianInt(fout, frameNumber);
        BinaryIo.writeLitteEndianTuple3f(fout, displacement);
        BinaryIo.writeLittleEndianTuple4f(fout, rotation);
        curve.write(fout);
    }
}
