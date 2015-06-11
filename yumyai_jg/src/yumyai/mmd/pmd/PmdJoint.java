package yumyai.mmd.pmd;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.vecmath.Vector3f;
import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

public class PmdJoint
{
    public String name;
    public final int[] rigidBodies = new int[2];
    public final Vector3f position = new Vector3f();
    public final Vector3f rotation = new Vector3f();
    public final Vector3f linearLowerLimit = new Vector3f();
    public final Vector3f linearUpperLimit = new Vector3f();
    public final Vector3f angularLowerLimit = new Vector3f();
    public final Vector3f angularUpperLimit = new Vector3f();
    public final float[] springLinearStiffness = new float[3];
    public final float[] springAngularStiffness = new float[3];

    public PmdJoint()
    {
        // NOP
    }

    public void read(SwappedDataInputStream fin) throws IOException
    {
        name = BinaryIo.readShiftJisString(fin, 20);
        rigidBodies[0] = fin.readInt();
        rigidBodies[1] = fin.readInt();

        BinaryIo.readTuple3f(fin, position);
        //position.z = -position.z;
        BinaryIo.readTuple3f(fin, rotation);
        //rotation.x = -rotation.x;
        //rotation.y = -rotation.y;

        BinaryIo.readTuple3f(fin, linearLowerLimit);
        BinaryIo.readTuple3f(fin, linearUpperLimit);
        
        /*
        float temp = linearLowerLimit.z;
        linearLowerLimit.z = -linearUpperLimit.z;
        linearUpperLimit.z = -temp;
        */
        
        BinaryIo.readTuple3f(fin, angularLowerLimit);
        BinaryIo.readTuple3f(fin, angularUpperLimit);
        
        /*
        temp = angularLowerLimit.x;
        angularLowerLimit.x = -angularLowerLimit.x;
        angularLowerLimit.x = -temp;
        
        temp = angularLowerLimit.y;
        angularLowerLimit.y = -angularLowerLimit.y;
        angularLowerLimit.y = -temp;
        */

        for (int i = 0; i < 3; i++)
        {
            springLinearStiffness[i] = fin.readFloat();
        }
        for (int i = 0; i < 3; i++)
        {
            springAngularStiffness[i] = fin.readFloat();
        }
    }

    public void write(DataOutputStream fout) throws IOException
    {
        BinaryIo.writeShiftJISString(fout, name, 20);
        BinaryIo.writeLittleEndianInt(fout, rigidBodies[0]);
        BinaryIo.writeLittleEndianInt(fout, rigidBodies[1]);
        BinaryIo.writeLittleEndianTuple3f(fout, position);
        BinaryIo.writeLittleEndianTuple3f(fout, rotation);
        BinaryIo.writeLittleEndianTuple3f(fout, linearLowerLimit);
        BinaryIo.writeLittleEndianTuple3f(fout, linearUpperLimit);
        BinaryIo.writeLittleEndianTuple3f(fout, angularLowerLimit);
        BinaryIo.writeLittleEndianTuple3f(fout, angularUpperLimit);
        for (int i = 0; i < 3; i++)
        {
            BinaryIo.writeLittleEndianFloat(fout, springLinearStiffness[i]);
        }
        for (int i = 0; i < 3; i++)
        {
            BinaryIo.writeLittleEndianFloat(fout, springAngularStiffness[i]);
        }
    }
}
