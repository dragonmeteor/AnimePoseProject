package yondoko.util;

import yondoko.struct.Color3d;

import javax.vecmath.*;

public class ObjectAllocator
{
    private static ThreadLocal<ObjectAllocator> threadObjectAllocator = new ThreadLocal<ObjectAllocator>() {
        @Override
        protected ObjectAllocator initialValue()
        {
            return new ObjectAllocator();
        }
    };

    public static ObjectAllocator get()
    {
        return threadObjectAllocator.get();
    }

    public final static int DEFAULT_SIZE = 128;

    private final CheckedStack<Vector3f> Vector3fStack = new CheckedStack<Vector3f>(
            DEFAULT_SIZE, new Factory<Vector3f>() {
        @Override
        public Vector3f create()
        {
            return new Vector3f(0,0,0);
        }
    });

    public Vector3f getVector3f()
    {
        return Vector3fStack.pop();
    }

    public void put(Vector3f v)
    {
        Vector3fStack.push(v);
    }

    private final CheckedStack<Quat4f> Quat4fStack = new CheckedStack<Quat4f>(
            DEFAULT_SIZE, new Factory<Quat4f>() {
        @Override
        public Quat4f create()
        {
            return new Quat4f(0.0f,0.0f,0.0f,1.0f);
        }
    });

    public Quat4f getQuat4f()
    {
        return Quat4fStack.pop();
    }

    public void put(Quat4f v)
    {
        Quat4fStack.push(v);
    }

    private final CheckedStack<Point3f> Point3fStack = new CheckedStack<Point3f>(
            DEFAULT_SIZE, new Factory<Point3f>()
    {
        @Override
        public Point3f create()
        {
            return new Point3f(0,0,0);
        }
    });

    public Point3f getPoint3f()
    {
        return Point3fStack.pop();
    }

    public void put(Point3f v)
    {
        Point3fStack.push(v);
    }

    private final CheckedStack<Matrix4f> Matrix4fStack = new CheckedStack<Matrix4f>(
            DEFAULT_SIZE, new Factory<Matrix4f>()
    {
        @Override
        public Matrix4f create()
        {
            Matrix4f m = new Matrix4f();
            m.setIdentity();
            return m;
        }
    });

    public Matrix4f getMatrix4f()
    {
        return Matrix4fStack.pop();
    }

    public void put(Matrix4f v)
    {
        Matrix4fStack.push(v);
    }

    private final CheckedStack<AxisAngle4f> AxisAngle4fStack = new CheckedStack<AxisAngle4f>(
            DEFAULT_SIZE, new Factory<AxisAngle4f>()
    {
        @Override
        public AxisAngle4f create()
        {
            return new AxisAngle4f();
        }
    });

    public AxisAngle4f getAxisAngle4f()
    {
        return AxisAngle4fStack.pop();
    }

    public void put(AxisAngle4f v)
    {
        AxisAngle4fStack.push(v);
    }

    private final CheckedStack<Vector3d> Vector3dStack = new CheckedStack<Vector3d>(DEFAULT_SIZE, new Factory<Vector3d>()
    {
        @Override
        public Vector3d create()
        {
            return new Vector3d(0, 0, 0);
        }
    });

    public Vector3d getVector3d()
    {
        return Vector3dStack.pop();
    }

    public void put(Vector3d v)
    {
        Vector3dStack.push(v);
    }
    private final CheckedStack<Point3d> Point3dStack = new CheckedStack<Point3d>(DEFAULT_SIZE, new Factory<Point3d>()
    {
        @Override
        public Point3d create()
        {
            return new Point3d(0, 0, 0);
        }
    });

    public Point3d getPoint3d()
    {
        return Point3dStack.pop();
    }

    public void put(Point3d v)
    {
        Point3dStack.push(v);
    }

    private final CheckedStack<Color3d> Color3dStack = new CheckedStack<Color3d>(DEFAULT_SIZE, new Factory<Color3d>()
    {
        @Override
        public Color3d create()
        {
            return new Color3d(0, 0, 0);
        }
    });

    public Color3d getColor3d()
    {
        return Color3dStack.pop();
    }

    public void put(Color3d v)
    {
        Color3dStack.push(v);
    }

    private final CheckedStack<Vector2d> Vector2dStack = new CheckedStack<Vector2d>(DEFAULT_SIZE, new Factory<Vector2d>()
    {
        @Override
        public Vector2d create()
        {
            return new Vector2d(0,0);
        }
    });

    public Vector2d getVector2d()
    {
        return Vector2dStack.pop();
    }

    public void put(Vector2d v)
    {
        Vector2dStack.push(v);
    }
}
