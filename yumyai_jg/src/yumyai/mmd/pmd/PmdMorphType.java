package yumyai.mmd.pmd;

import java.util.HashMap;
import java.util.Map;

public enum PmdMorphType
{
    Base(0),
    EyeBrows(1),
    Eyes(2),
    Lips(3),
    Other(4);
    
    private final int value;   
    
    private PmdMorphType(int value)
    {
        this.value = value;
    }
    private static final Map<Integer, PmdMorphType> intToTypeMap = new HashMap<Integer, PmdMorphType>();

    static
    {
        for (PmdMorphType type : PmdMorphType.values())
        {
            intToTypeMap.put(type.value, type);
        }
    }

    public static PmdMorphType fromInt(int i)
    {
        PmdMorphType type = intToTypeMap.get(Integer.valueOf(i));
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
