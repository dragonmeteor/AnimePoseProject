package yumyai.mmd.pmx;

import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class PmxBone {
    public String japaneseName;
    public String englishName;
    public final Point3f position = new Point3f();
    public int parentIndex;
    public int transformLevel;
    public int flags;
    public final Point3f connectionPositionOffset = new Point3f();
    public int connectionBoneIndex;
    public int copyParentBoneIndex;
    public float copyRate;
    public final Vector3f axis = new Vector3f();
    public final Vector3f xAxis = new Vector3f();
    public final Vector3f zAxis = new Vector3f();
    public int key;
    public int ikTargetBoneIndex;
    public int ikLoopCount;
    public float ikAngleLimit;
    public final ArrayList<PmxIkLink> ikLinks = new ArrayList<PmxIkLink>();
    public int boneIndex;
    public int boneOrder;
    public final Vector3f displacementFromParent = new Vector3f();

    public void read(SwappedDataInputStream fin, Charset charset, int boneIndexSize) throws IOException {
        japaneseName = BinaryIo.readVariableLengthString(fin, charset);
        englishName = BinaryIo.readVariableLengthString(fin, charset);
        BinaryIo.readTuple3f(fin, position);
        parentIndex = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, false);
        transformLevel = fin.readInt();
        flags = fin.readUnsignedShort();

        if (!displayConnectionToOtherBone()) {
            BinaryIo.readTuple3f(fin, connectionPositionOffset);
        } else {
            connectionBoneIndex = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, false);
        }

        if (copyRotation() || copyTranslation()) {
            copyParentBoneIndex = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, false);
            copyRate = fin.readFloat();
        }

        if (useFixedAxis()) {
            BinaryIo.readTuple3f(fin, axis);
        }

        if (useLocalAxes()) {
            BinaryIo.readTuple3f(fin, xAxis);
            BinaryIo.readTuple3f(fin, zAxis);
        }

        if (useExternalParentTransform()) {
            key = fin.readInt();
        }

        if (isIk()) {
            ikTargetBoneIndex = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, false);
            ikLoopCount = fin.readInt();
            ikAngleLimit = fin.readFloat();
            int count = fin.readInt();
            for (int i = 0; i < count; i++) {
                PmxIkLink ikLink = new PmxIkLink();
                ikLink.read(fin, boneIndexSize);
                ikLinks.add(ikLink);
            }
        }
    }

    public boolean displayConnectionToOtherBone() {
        return (flags & 0x0001) != 0;
    }

    public boolean canRotate() {
        return (flags & 0x0002) != 0;
    }

    public boolean canTranslate() {
        return (flags & 0x0004) != 0;
    }

    public boolean isDisplayed() {
        return (flags & 0x0008) != 0;
    }

    public boolean isControllable() {
        return (flags & 0x0010)  != 0;
    }

    public boolean isIk() {
        return (flags & 0x0020) != 0;
    }

    public boolean copyRotation() {
        return (flags & 0x0100) != 0;
    }

    public boolean copyTranslation() {
        return (flags & 0x0200) != 0;
    }

    public boolean useFixedAxis() {
        return (flags & 0x0400) != 0;
    }

    public boolean useLocalAxes() {
        return (flags & 0x0800) != 0;
    }

    public boolean transformAfterPhysics() {
        return (flags & 0x1000) != 0;
    }

    public boolean useExternalParentTransform() {
        return (flags & 0x2000) != 0;
    }
}
