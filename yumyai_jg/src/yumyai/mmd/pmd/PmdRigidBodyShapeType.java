package yumyai.mmd.pmd;

import java.util.HashMap;
import java.util.Map;

public enum PmdRigidBodyShapeType
{
    Sphere(0),
    Box(1),
    Capsule(2);
    
    private final int value;

    private PmdRigidBodyShapeType(int value)
    {
        this.value = value;
    }
    private static final Map<Integer, PmdRigidBodyShapeType> intToTypeMap = new HashMap<Integer, PmdRigidBodyShapeType>();

    static
    {
        for (PmdRigidBodyShapeType type : PmdRigidBodyShapeType.values())
        {
            intToTypeMap.put(type.value, type);
        }
    }

    public static PmdRigidBodyShapeType fromInt(int i)
    {
        PmdRigidBodyShapeType type = intToTypeMap.get(Integer.valueOf(i));
        if (type == null)
        {
            throw new RuntimeException("invalid value");
        }
        return type;
    }
    
    public int getValue()
    {
        return value;
    }
}
