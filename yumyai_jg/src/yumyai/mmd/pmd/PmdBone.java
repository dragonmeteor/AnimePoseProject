package yumyai.mmd.pmd;

import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.io.DataOutputStream;
import java.io.IOException;

public class PmdBone {
    public String japaneseName;
    public String englishName;
    public short parentIndex;
    public short tailIndex;
    public PmdBoneType type;
    public short ikParentIndex;
    public final Point3f headPosition = new Point3f();
    public PmdBone parent;
    public boolean isKnee;
    public boolean controlledByPhysics = false;

    public PmdBone() {
        // NOP
    }

    public void getDisplacement(Vector3f result) {
        if (parent == null) {
            result.set(headPosition);
        } else {
            result.sub(headPosition, parent.headPosition);
        }
    }

    public void read(SwappedDataInputStream fin) throws IOException {
        japaneseName = BinaryIo.readShiftJisString(fin, 20);
        englishName = japaneseName;
        parentIndex = fin.readShort();
        tailIndex = fin.readShort();
        type = PmdBoneType.fromInt(fin.readByte());
        ikParentIndex = fin.readShort();

        BinaryIo.readTuple3f(fin, headPosition);
        //headPosition.z = -headPosition.z;

        if (japaneseName.equals("左ひざ") || japaneseName.equals("右ひざ")) {
            isKnee = true;
        } else {
            isKnee = false;
        }
    }

    public void write(DataOutputStream fout) throws IOException {
        byte[] japaneseNameArray = japaneseName.getBytes("Shift-JIS");
        BinaryIo.writeByteString(fout, japaneseNameArray, 20);
        BinaryIo.writeLittleEndianShort(fout, parentIndex);
        BinaryIo.writeLittleEndianShort(fout, tailIndex);
        fout.write(type.getValue());
        BinaryIo.writeLittleEndianShort(fout, ikParentIndex);
        BinaryIo.writeLittleEndianTuple3f(fout, headPosition);
    }

}
