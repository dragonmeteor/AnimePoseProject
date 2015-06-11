/*
 */
package yondoko.struct;

import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

public class Aabb3f
{
    public final Vector3f pMin = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
    public final Vector3f pMax = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

    public void reset()
    {
        pMin.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        pMax.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
    }

    public void expandBy(Tuple3f p)
    {
        pMin.x = Math.min(pMin.x, p.x);
        pMin.y = Math.min(pMin.y, p.y);
        pMin.z = Math.min(pMin.z, p.z);
        pMax.x = Math.max(pMax.x, p.x);
        pMax.y = Math.max(pMax.y, p.y);
        pMax.z = Math.max(pMax.z, p.z);
    }

    public void expandBy(float t)
    {
        pMin.x -= t;
        pMin.y -= t;
        pMin.z -= t;
        pMax.x += t;
        pMax.y += t;
        pMax.z += t;
    }
    
    public void expandBy(Aabb3f other)
    {
        pMin.x = Math.min(pMin.x, other.pMin.x);
        pMin.y = Math.min(pMin.y, other.pMin.y);
        pMin.z = Math.min(pMin.z, other.pMin.z);
        pMax.x = Math.max(pMax.x, other.pMax.x);
        pMax.y = Math.max(pMax.y, other.pMax.y);
        pMax.z = Math.max(pMax.z, other.pMax.z);
    }

    public void expandBy(float x, float y, float z) {
        pMin.x = Math.min(pMin.x, x);
        pMin.y = Math.min(pMin.y, y);
        pMin.z = Math.min(pMin.z, z);
        pMax.x = Math.max(pMax.x, x);
        pMax.y = Math.max(pMax.y, y);
        pMax.z = Math.max(pMax.z, z);
    }

    public boolean overlap(Aabb3f other)
    {
        if (pMin.x > other.pMax.x || pMax.x < other.pMin.x)
        {
            return false;
        }
        else if (pMin.y > other.pMax.y || pMax.y < other.pMin.y)
        {
            return false;
        }
        else if (pMin.z > other.pMax.z || pMax.z < other.pMin.z)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public boolean overlap(Tuple3f p)
    {
        if (p.x < pMin.x || p.x > pMax.x)
        {
            return false;
        }
        else if (p.y < pMin.y || p.y > pMax.y)
        {
            return false;
        }
        else if (p.z < pMin.z || p.z > pMax.z)
        {
            return false;
        }
        else
        {
            return true;
        }        
    }

    public void set(Aabb3f other)
    {
        pMin.set(other.pMin);
        pMax.set(other.pMax);
    }
    
    @Override
    public String toString()
    {
        return "Aabb[pMin = " + pMin.toString() + ", pMax = " + pMax.toString() + "]";
    }
    
    public float getMaxExtent()
    {
        return (float)Math.max(pMax.x - pMin.x, Math.max(pMax.y - pMin.y, pMin.z - pMin.z));
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Aabb3f)
        {
            Aabb3f other = (Aabb3f)obj;
            return other.pMin.equals(this.pMin) && other.pMax.equals(this.pMax);
        }
        else
        {
            return false;
        }
    }
    
    public void getCornerPoint(int cornerId, Tuple3f out)
    {
        out.x = ((cornerId & 1) == 0) ? pMin.x : pMax.x;
        out.y = ((cornerId & 2) == 0) ? pMin.y : pMax.y;
        out.z = ((cornerId & 4) == 0) ? pMin.z : pMax.z;
    }
    
    public float getExtent(int dim)
    {
        switch(dim)
        {
            case 0:
                return pMax.x - pMin.x;                
            case 1:
                return pMax.y - pMin.y;                
            case 2:
                return pMax.z - pMin.z;
            default:
                throw new RuntimeException("dim must be 0, 1, or 2");
        }
    }
}
