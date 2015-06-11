package yondoko.util;

import javax.vecmath.Tuple3f;
import javax.vecmath.Tuple3i;
import javax.vecmath.Tuple4f;
import javax.vecmath.Tuple4i;

public class VectorUtil {
    public static int getComponent(Tuple4i v, int i) {
        switch (i) {
            case 0:
                return v.x;
            case 1:
                return v.y;
            case 2:
                return v.z;
            case 3:
                return v.w;
            default:
                throw new RuntimeException("invalid index");
        }
    }

    public static void setComponent(Tuple4i v, int i, int x) {
        switch (i) {
            case 0:
                v.x = x;
                break;
            case 1:
                v.y = x;
                break;
            case 2:
                v.z = x;
                break;
            case 3:
                v.w = x;
                break;
            default:
                throw new RuntimeException("invalid index");
        }
    }

    public static float getComponent(Tuple4f v, int i) {
        switch (i) {
            case 0:
                return v.x;
            case 1:
                return v.y;
            case 2:
                return v.z;
            case 3:
                return v.w;
            default:
                throw new RuntimeException("invalid index");
        }
    }

    public static void setComponent(Tuple4f v, int i, float x) {
        switch (i) {
            case 0:
                v.x = x;
                break;
            case 1:
                v.y = x;
                break;
            case 2:
                v.z = x;
                break;
            case 3:
                v.w = x;
                break;
            default:
                throw new RuntimeException("invalid index");
        }
    }

    public static int getComponent(Tuple3i v, int i) {
        switch (i) {
            case 0:
                return v.x;
            case 1:
                return v.y;
            case 2:
                return v.z;
            default:
                throw new RuntimeException("invalid index");
        }
    }

    public static void setComponent(Tuple3i v, int i, int x) {
        switch (i) {
            case 0:
                v.x = x;
                break;
            case 1:
                v.y = x;
                break;
            case 2:
                v.z = x;
                break;
            default:
                throw new RuntimeException("invalid index");
        }
    }

    public static float getComponent(Tuple3f v, int i) {
        switch (i) {
            case 0:
                return v.x;
            case 1:
                return v.y;
            case 2:
                return v.z;
            default:
                throw new RuntimeException("invalid index");
        }
    }

    public static void setComponent(Tuple3f v, int i, float x) {
        switch (i) {
            case 0:
                v.x = x;
                break;
            case 1:
                v.y = x;
                break;
            case 2:
                v.z = x;
                break;
            default:
                throw new RuntimeException("invalid index");
        }
    }
}
