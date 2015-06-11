package yumyai.mmd.pmd;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.vecmath.Vector3f;
import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

public class PmdMorph
{
    public String japaneseName;
    public String englishName;
    public PmdMorphType type;
    public int[] vertexIndices;
    public Vector3f[] displacements;

    public PmdMorph()
    {
        // NOP
    }

    public void read(SwappedDataInputStream fin) throws IOException
    {
        englishName = japaneseName = BinaryIo.readShiftJisString(fin, 20);

        int vertexCount = fin.readInt();
        type = PmdMorphType.fromInt(fin.readByte());
        vertexIndices = new int[vertexCount];
        displacements = new Vector3f[vertexCount];
        for (int i = 0; i < vertexCount; i++)
        {
            int vertexIndex = fin.readInt();
            Vector3f v = new Vector3f();
            BinaryIo.readTuple3f(fin, v);
            //v.z = -v.z;
            vertexIndices[i] = vertexIndex;
            displacements[i] = v;
        }
    }

    void write(DataOutputStream fout) throws IOException
    {
        byte[] japaneseNameArray = japaneseName.getBytes("Shift-JIS");
        BinaryIo.writeByteString(fout, japaneseNameArray, 20);
        BinaryIo.writeLittleEndianInt(fout, vertexIndices.length);
        fout.write(type.getValue());
        for (int i = 0; i < vertexIndices.length; i++)
        {
            BinaryIo.writeLittleEndianInt(fout, vertexIndices[i]);
            BinaryIo.writeLittleEndianTuple3f(fout, displacements[i]);
        }
    }
}
