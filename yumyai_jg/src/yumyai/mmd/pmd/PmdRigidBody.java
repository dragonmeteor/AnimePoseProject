package yumyai.mmd.pmd;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.vecmath.Vector3f;
import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

public class PmdRigidBody
{
    public String name;
    public int boneIndex;
    public byte groupIndex;
    public int hitWithGroupFlags;
    public PmdRigidBodyShapeType shape;
    public float width;
    public float height;
    public float depth;
    public final Vector3f position = new Vector3f();
    public final Vector3f rotation = new Vector3f();
    public float mass;
    public float positionDamping;
    public float rotationDamping;
    public float restitution;
    public float friction;
    public PmdRigidBodyType type;

    public PmdRigidBody()
    {
        // NOP
    }

    public void read(SwappedDataInputStream fin) throws IOException
    {
        name = BinaryIo.readShiftJisString(fin, 20);
        boneIndex = fin.readShort();
        groupIndex = fin.readByte();
        hitWithGroupFlags = fin.readUnsignedShort();
        shape = PmdRigidBodyShapeType.fromInt(fin.readByte());
        width = fin.readFloat();
        height = fin.readFloat();
        depth = fin.readFloat();

        BinaryIo.readTuple3f(fin, position);
        //position.z = -position.z;
        BinaryIo.readTuple3f(fin, rotation);
        //rotation.x = -rotation.x;
        //rotation.y = -rotation.y;
        
        mass = fin.readFloat();
        positionDamping = fin.readFloat();
        rotationDamping = fin.readFloat();
        restitution = fin.readFloat();
        friction = fin.readFloat();
        type = PmdRigidBodyType.fromInt(fin.readByte());
    }

    void write(DataOutputStream fout) throws IOException
    {
        BinaryIo.writeShiftJISString(fout, name, 20);
        BinaryIo.writeLittleEndianShort(fout, (short)boneIndex);
        fout.write(groupIndex);
        BinaryIo.writeLittleEndianShort(fout, (short)(hitWithGroupFlags & 0xffff));
        fout.write(shape.getValue());
        BinaryIo.writeLittleEndianFloat(fout, width);
        BinaryIo.writeLittleEndianFloat(fout, height);
        BinaryIo.writeLittleEndianFloat(fout, depth);
        BinaryIo.writeLittleEndianTuple3f(fout, position);
        BinaryIo.writeLittleEndianTuple3f(fout, rotation);
        BinaryIo.writeLittleEndianFloat(fout, mass);
        BinaryIo.writeLittleEndianFloat(fout, positionDamping);
        BinaryIo.writeLittleEndianFloat(fout, rotationDamping);
        BinaryIo.writeLittleEndianFloat(fout, restitution);
        BinaryIo.writeLittleEndianFloat(fout, friction);
        fout.write(type.getValue());
    }
}
