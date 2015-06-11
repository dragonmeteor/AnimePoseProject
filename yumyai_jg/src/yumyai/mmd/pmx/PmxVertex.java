package yumyai.mmd.pmx;

import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

import javax.vecmath.Point3f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.io.IOException;

public class PmxVertex {
    public static final int BDEF1 = 0;
    public static final int BDEF2 = 1;
    public static final int BDEF4 = 2;
    public static final int SDEF = 3;

    public final Point3f position = new Point3f();
    public final Vector3f normal = new Vector3f();
    public final Vector2f texCoords = new Vector2f();
    public final Vector4f[] additionalTexCoords;
    public int boneDataType;
    public final int[] boneIndices = new int[4];
    public final float[] boneWeights = new float[4];
    public float edgeScale;
    public Vector3f C;
    public Vector3f R0;
    public Vector3f R1;

    public PmxVertex(int additionalTexCoordCount) {
        additionalTexCoords = new Vector4f[additionalTexCoordCount];
        for (int i = 0; i < additionalTexCoords.length; i++) {
            additionalTexCoords[i] = new Vector4f();
        }
        for (int i = 0; i < 4; i++) {
            boneIndices[i] = -1;
            boneWeights[i] = 0;
        }
    }

    public void read(SwappedDataInputStream fin, int boneIndexSize) throws IOException {
        BinaryIo.readTuple3f(fin, position);
        BinaryIo.readTuple3f(fin, normal);
        BinaryIo.readTuple2f(fin, texCoords);
        for (int i = 0; i < additionalTexCoords.length; i++) {
            BinaryIo.readTuple4f(fin, additionalTexCoords[i]);
        }
        boneDataType = fin.readByte();
        switch (boneDataType) {
            case 0:
                boneIndices[0] = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, false);
                boneWeights[0] = 1;
                break;
            case 1:
                boneIndices[0] = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, false);
                boneIndices[1] = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, false);
                boneWeights[0] = fin.readFloat();
                boneWeights[1] = 1 - boneWeights[0];
                break;
            case 2:
                for (int i = 0; i < 4; i++) {
                    boneIndices[i] = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, false);
                }
                for (int i = 0; i < 4; i++) {
                    boneWeights[i] = fin.readFloat();
                }
                break;
            case 3:
                boneIndices[0] = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, false);
                boneIndices[1] = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, false);
                boneWeights[0] = fin.readFloat();
                boneWeights[1] = 1 - boneWeights[0];
                C = new Vector3f();
                R0 = new Vector3f();
                R1 = new Vector3f();
                BinaryIo.readTuple3f(fin, C);
                BinaryIo.readTuple3f(fin, R0);
                BinaryIo.readTuple3f(fin, R1);
                break;
            default:
                throw new RuntimeException("invalid bone data type " + boneDataType);
        }
        edgeScale = fin.readFloat();
    }
}
