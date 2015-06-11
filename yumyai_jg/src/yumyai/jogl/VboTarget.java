/*
 */
package yumyai.jogl;

import javax.media.opengl.GL2;

public class VboTarget
{
    private final int constant;
    private static Vbo boundVbo = null;
    
    public static VboTarget ARRAY_BUFFER = new VboTarget(GL2.GL_ARRAY_BUFFER);
    public static VboTarget ELEMENT_ARRAY_BUFFER = new VboTarget(GL2.GL_ELEMENT_ARRAY_BUFFER);

    private VboTarget(int constant)
    {
        this.constant = constant;
    }

    public int getConstant()
    {
        return constant;
    }
    
    public Vbo getBoundVbo()
    {
        return boundVbo;
    }
    
    public void setBoundVbo(Vbo vbo)
    {
        boundVbo = vbo;
    }
    
    public void unbindVbo()
    {
        if (boundVbo != null)
        {
            boundVbo.unbind();
        }
    }
}
