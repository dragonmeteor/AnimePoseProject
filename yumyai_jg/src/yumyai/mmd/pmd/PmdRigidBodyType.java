package yumyai.mmd.pmd;

import java.util.HashMap;
import java.util.Map;

public enum PmdRigidBodyType
{
    FollowBone(0),
    Physics(1),
    PhysicsWithBone(2);    
    
    private final int value;

    private PmdRigidBodyType(int value)
    {
        this.value = value;
    }
    private static final Map<Integer, PmdRigidBodyType> intToTypeMap = new HashMap<Integer, PmdRigidBodyType>();

    static
    {
        for (PmdRigidBodyType type : PmdRigidBodyType.values())
        {
            intToTypeMap.put(type.value, type);
        }
    }

    public static PmdRigidBodyType fromInt(int i)
    {
        PmdRigidBodyType type = intToTypeMap.get(Integer.valueOf(i));
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
