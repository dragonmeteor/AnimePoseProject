package yumyai.mmd.pmx;

import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;
import yumyai.mmd.pmd.PmdRigidBodyShapeType;
import yumyai.mmd.pmd.PmdRigidBodyType;

import javax.vecmath.Vector3f;
import java.io.IOException;
import java.nio.charset.Charset;

public class PmxRigidBody {
    public String japaneseName;
    public String englishName;
    public int boneIndex;
    public int groupIndex;
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

    public void read(SwappedDataInputStream fin, Charset charset, int boneIndexSize) throws IOException {
        japaneseName = BinaryIo.readVariableLengthString(fin, charset);
        englishName  = BinaryIo.readVariableLengthString(fin, charset);
        boneIndex = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, false);
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

        if (Float.isNaN(rotation.x))
            rotation.x = 0;
        if (Float.isNaN(rotation.y))
            rotation.y = 0;
        if (Float.isNaN(rotation.z))
            rotation.z = 0;

        mass = fin.readFloat();
        positionDamping = fin.readFloat();
        rotationDamping = fin.readFloat();
        restitution = fin.readFloat();
        friction = fin.readFloat();
        type = PmdRigidBodyType.fromInt(fin.readByte());
    }
}
