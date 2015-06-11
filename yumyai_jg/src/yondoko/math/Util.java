package yondoko.math;

import javax.vecmath.Tuple3f;

public class Util {
    public static boolean between(float x, float lower, float upper) {
        return (x >= lower) && (x <= upper);
    }

    public static boolean between(double x, double lower, double upper) {
        return (x >= lower) && (x <= upper);
    }

    public static float clamp(float x, float lower, float upper) {
        return Math.max(Math.min(upper, x), lower);
    }

    public static double clamp(double x, double lower, double upper) {
        return Math.max(Math.min(upper, x), lower);
    }

    public static void clamp(Tuple3f v, Tuple3f lower, Tuple3f upper) {
        v.x = clamp(v.x, lower.x, upper.x);
        v.y = clamp(v.y, lower.y, upper.y);
        v.z = clamp(v.z, lower.z, upper.z);
    }

    public static int getClosestPowerOfTwo(int x) {
        if (x < 0)
            return 1;
        else {
            int y = 1;
            while (y < x) {
                y = y*2;
            }
            if (Math.abs(y-x) < Math.abs(y/2-x)) {
                return y;
            } else {
                return y/2;
            }
        }
    }

    public static boolean isPowerOfTwo(int x) {
        x = Math.abs(x);
        if (x == 0)
            return false;
        else if (x == 1)
            return true;
        else {
            while (x > 1) {
                if ((x & 1) == 1)
                    return false;
                else
                    x = (x >> 1);
            }
            return x == 1;
        }
    }
}
