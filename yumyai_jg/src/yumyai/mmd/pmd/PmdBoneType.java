package yumyai.mmd.pmd;

import java.util.HashMap;
import java.util.Map;

public enum PmdBoneType
{
    Rotate(0),
    RotateAndTranslate(1),
    Ik(2),
    Undefined(3),
    IkInduced(4),
    RotationInduced(5),
    IkEndEffector(6),
    Disabled(7),
    Twist(8),
    Gyrate(9);
    
    private final int value;

    private PmdBoneType(int value)
    {
        this.value = value;
    }
    private static final Map<Integer, PmdBoneType> intToTypeMap = new HashMap<Integer, PmdBoneType>();

    static
    {
        for (PmdBoneType type : PmdBoneType.values())
        {
            intToTypeMap.put(type.value, type);
        }
    }

    public static PmdBoneType fromInt(int i)
    {
        PmdBoneType type = intToTypeMap.get(Integer.valueOf(i));
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
