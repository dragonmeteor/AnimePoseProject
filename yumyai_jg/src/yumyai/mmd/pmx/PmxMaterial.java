package yumyai.mmd.pmx;

import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.io.IOException;
import java.nio.charset.Charset;

public class PmxMaterial {
    public static final int NO_SPHERE_MAP = 0;
    public static final int MULTIPLY_SPHERE_MAP = 1;
    public static final int ADD_SPHERE_MAP = 2;
    public static final int SUBTEX_SPHERE_MAP = 3;

    public String japaneseName;
    public String englishName;
    public final Vector4f diffuse = new Vector4f();
    public final Vector3f specular = new Vector3f();
    public float shininess;
    public final Vector3f ambient = new Vector3f();
    public int renderFlag;
    public final Vector4f edgeColor = new Vector4f();
    public float edgeSize;
    public int textureIndex;
    public int sphereTextureIndex;
    public int sphereMode;
    public boolean useSharedToonTexture;
    public int toonTextureIndex;
    public String memo;
    public int vertexCount;

    public void read(SwappedDataInputStream fin, Charset charset, int textureIndexSize) throws IOException {
        japaneseName = BinaryIo.readVariableLengthString(fin, charset);
        englishName = BinaryIo.readVariableLengthString(fin, charset);
        BinaryIo.readTuple4f(fin, diffuse);
        BinaryIo.readTuple3f(fin, specular);
        shininess = fin.readFloat();
        BinaryIo.readTuple3f(fin, ambient);
        renderFlag = fin.readByte();
        BinaryIo.readTuple4f(fin, edgeColor);
        edgeSize = fin.readFloat();
        textureIndex = BinaryIo.readIntGivenSizeInBytes(fin, textureIndexSize, false);
        sphereTextureIndex = BinaryIo.readIntGivenSizeInBytes(fin, textureIndexSize, false);
        sphereMode = fin.readByte();
        useSharedToonTexture = fin.readByte() == 1;
        if (useSharedToonTexture)
            toonTextureIndex = fin.readByte();
        else
            toonTextureIndex = BinaryIo.readIntGivenSizeInBytes(fin, textureIndexSize, false);
        memo = BinaryIo.readVariableLengthString(fin, charset);
        vertexCount = fin.readInt();
    }
}
