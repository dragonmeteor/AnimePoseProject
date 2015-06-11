package yondoko.math;

import javax.vecmath.Vector2d;

public class Quadratic
{
    /**
     * Solve the quadratic equation A*x^2 + B*x + C = 0.
     *
     * @param A the coefficient of x^2
     * @param B the coefficient of x
     * @param C the glConstant term
     * @param roots the vectors containing the two real roots (if any)
     * @return the number of real roots
     */
    public static int solve(double A, double B, double C, Vector2d roots)
    {
        double D = B * B - 4 * A * C;

        if (A == 0)
        {
            roots.x = -C / B;
            return 1;
        }
        else if (D < 0)
        {
            return 0;
        }
        else if (D == 0)
        {
            roots.x = -B / (2 * A);
            return 1;
        }
        else if (D > 0 && B != 0)
        {
            double DD = Math.sqrt(D);
            double t;
            if (B > 0)
            {
                t = -0.5 * (B + DD);
            }
            else
            {
                t = -0.5 * (B - DD);
            }
            roots.x = t / A;
            roots.y = C / t;
            return 2;
        }
        else
        {
            double DD = Math.sqrt(D);
            roots.x = DD / (2 * A);
            roots.y = -DD / (2 * A);
            return 2;
        }
    }
}
