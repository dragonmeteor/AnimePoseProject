package yumyai.mmd.pmd;

import java.io.DataOutputStream;
import java.io.IOException;
import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

public class PmdIkChain
{
    public int boneIndex;
    public int targetBoneIndex;
    public int iteration;
    public float controlWeight;
    public int[] chainBoneIndices;
    public boolean isLeg;
    public boolean isLeftLeg;
    public boolean isRightLeg;
    
    public PmdIkChain()
    {
        // NOP
    }
    
    
    public int length()
    {
        return chainBoneIndices.length;
    }
    
    public void read(SwappedDataInputStream fin) throws IOException
    {
        boneIndex = fin.readUnsignedShort();
        targetBoneIndex = fin.readShort();
        int chainLength = fin.readByte();
        chainBoneIndices = new int[chainLength];
        iteration = fin.readUnsignedShort();
        controlWeight = fin.readFloat();
        for (int i = 0; i < chainLength; i++)
        {
            chainBoneIndices[i] = fin.readShort();
        }        
    }

    void write(DataOutputStream fout) throws IOException
    {
        BinaryIo.writeLittleEndianShort(fout, (short)boneIndex);
        BinaryIo.writeLittleEndianShort(fout, (short)targetBoneIndex);
        fout.write(chainBoneIndices.length);
        BinaryIo.writeLittleEndianShort(fout, (short)iteration);
        BinaryIo.writeLittleEndianFloat(fout, controlWeight);
        for (int i = 0; i < chainBoneIndices.length; i++)
        {
            BinaryIo.writeLittleEndianShort(fout, (short)chainBoneIndices[i]);
        }
    }
}
