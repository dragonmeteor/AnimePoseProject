package yondoko.struct;

import javax.vecmath.Tuple2f;
import javax.vecmath.Vector2f;

public class Aabb2f {
    public final Vector2f pMin = new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);
    public final Vector2f pMax = new Vector2f(-Float.MAX_VALUE, -Float.MAX_VALUE);

    public void reset()
    {
        pMin.set(Float.MAX_VALUE, Float.MAX_VALUE);
        pMax.set(-Float.MAX_VALUE, -Float.MAX_VALUE);
    }

    public void expandBy(Tuple2f p)
    {
        pMin.x = Math.min(pMin.x, p.x);
        pMin.y = Math.min(pMin.y, p.y);
        pMax.x = Math.max(pMax.x, p.x);
        pMax.y = Math.max(pMax.y, p.y);
    }

    public void expandBy(float t)
    {
        pMin.x -= t;
        pMin.y -= t;
        pMax.x += t;
        pMax.y += t;
    }

    public void expandBy(Aabb2f other)
    {
        pMin.x = Math.min(pMin.x, other.pMin.x);
        pMin.y = Math.min(pMin.y, other.pMin.y);
        pMax.x = Math.max(pMax.x, other.pMax.x);
        pMax.y = Math.max(pMax.y, other.pMax.y);
    }

    public void expandBy(float x, float y) {
        pMin.x = Math.min(pMin.x, x);
        pMin.y = Math.min(pMin.y, y);
        pMax.x = Math.max(pMax.x, x);
        pMax.y = Math.max(pMax.y, y);
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
        else
        {
            return true;
        }
    }

    public boolean overlap(Tuple2f p)
    {
        if (p.x < pMin.x || p.x > pMax.x)
        {
            return false;
        }
        else if (p.y < pMin.y || p.y > pMax.y)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public void set(Aabb2f other)
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
        return (float)Math.max(pMax.x - pMin.x, pMax.y - pMin.y);
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

    public void getCornerPoint(int cornerId, Tuple2f out)
    {
        out.x = ((cornerId & 1) == 0) ? pMin.x : pMax.x;
        out.y = ((cornerId & 2) == 0) ? pMin.y : pMax.y;
    }

    public float getExtent(int dim)
    {
        switch(dim)
        {
            case 0:
                return pMax.x - pMin.x;
            case 1:
                return pMax.y - pMin.y;
            default:
                throw new RuntimeException("dim must be 0, 1, or 2");
        }
    }
}
