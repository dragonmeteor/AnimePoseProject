/*
 * Copyright 1997-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 */
package yondoko.struct;

import java.awt.Color;
import javax.vecmath.*;

public class Color3d extends Tuple3d
{
    /**
     * Constructs and initializes a Color3d from the specified xyz coordinates.
     *
     * @param x the red color value
     * @param y the green color value
     * @param z the blue color value
     */
    public Color3d(double x, double y, double z)
    {
        super(x, y, z);
    }

    /**
     * Constructs and initializes a Color3d from the array of length 3.
     *
     * @param c the array of length 3 containing r,g,b in order
     */
    public Color3d(double[] c)
    {
        super(c);
    }

    /**
     * Constructs and initializes a Color3d from the specified Tuple4f.
     *
     * @param t1 the Tuple3d containing the initialization r,g,b data
     */
    public Color3d(Tuple3d t1)
    {
        super(t1);
    }

    /**
     * Constructs and initializes a Color3f from the specified Tuple3d.
     *
     * @param t1 the Tuple3f containing the initialization r,g,b data
     */
    public Color3d(Tuple3f t1)
    {
        super(t1);
    }

    /**
     * Constructs and initializes a Color4f from the specified AWT Color object.
     * No conversion is done on the color to compensate for gamma correction.
     *     
     * @param color the AWT color with which to initialize this Color4f object
     */
    public Color3d(Color color)
    {
        super(color.getRed() / 255.0,
              color.getGreen() / 255.0,
              color.getBlue() / 255.0);
    }

    /**
     * Constructs and initializes a Color4f to (0.0, 0.0, 0.0, 0.0).
     */
    public Color3d()
    {
        super();
    }

    /**
     * Sets the r,g,b,a values of this Color4f object to those of the specified
     * AWT Color object. No conversion is done on the color to compensate for
     * gamma correction.
     */
    public final void set(Color color)
    {
        x = (float) color.getRed() / 255.0f;
        y = (float) color.getGreen() / 255.0f;
        z = (float) color.getBlue() / 255.0f;
    }

    /**
     * Returns a new AWT color object initialized with the r,g,b,a values of
     * this Color4f object.
     *     
     * @return a new AWT Color object
     */
    public final Color get()
    {
        int r = (int) Math.round(x * 255.0f);
        int g = (int) Math.round(y * 255.0f);
        int b = (int) Math.round(z * 255.0f);

        return new Color(r, g, b, 255);
    }
}
