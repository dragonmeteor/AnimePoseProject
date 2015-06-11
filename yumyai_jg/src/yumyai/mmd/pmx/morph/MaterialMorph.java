package yumyai.mmd.pmx.morph;

import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;
import yumyai.mmd.pmx.PmxMorph;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.io.IOException;
import java.util.ArrayList;

public class MaterialMorph extends PmxMorph {
    public final ArrayList<Offset> offsets = new ArrayList<Offset>();

    public void readOffsets(SwappedDataInputStream fin, int materialIndexSize) throws IOException {
        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            Offset offset = new Offset();
            offset.read(fin, materialIndexSize);
            offsets.add(offset);
        }
    }

    public static class Offset {
        public int materialIndex;
        public int offsetMode;
        public final Vector4f diffuse = new Vector4f();
        public final Vector3f specular = new Vector3f();
        public float shininess;
        public final Vector3f ambient = new Vector3f();
        public final Vector4f edgeColor = new Vector4f();
        public float edgeSize;
        public final Vector4f textureCoeff = new Vector4f();
        public final Vector4f sphereTextureCoeff = new Vector4f();
        public final Vector4f toonTextureCoeff = new Vector4f();

        public void read(SwappedDataInputStream fin, int materialIndexSize) throws IOException {
            BinaryIo.readIntGivenSizeInBytes(fin, materialIndexSize, true);
            offsetMode = fin.readByte();
            BinaryIo.readTuple4f(fin, diffuse);
            BinaryIo.readTuple3f(fin, specular);
            shininess = fin.readFloat();
            BinaryIo.readTuple3f(fin, ambient);
            BinaryIo.readTuple4f(fin, edgeColor);
            edgeSize = fin.readFloat();
            BinaryIo.readTuple4f(fin, textureCoeff);
            BinaryIo.readTuple4f(fin, sphereTextureCoeff);
            BinaryIo.readTuple4f(fin, toonTextureCoeff);
        }
    }
}
