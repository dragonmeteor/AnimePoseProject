package yondoko.util;

public class FloatUtil {
    public static boolean isFinite(float x) {
        return !Float.isNaN(x) && !Float.isInfinite(x);
    }
}
