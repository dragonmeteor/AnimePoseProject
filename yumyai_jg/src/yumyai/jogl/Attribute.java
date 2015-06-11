/*
 */
package yumyai.jogl;

import javax.media.opengl.GL2;

public class Attribute
{
    private GL2 gl;
    private Program program;
    private String name;
    private int size;
    private int type;
    private int location;
    private boolean enabled;
    
    public Attribute(GL2 glContext, Program prog, int index)
    {
        this.program = prog;
        this.gl = glContext;

        byte[] attribName = new byte[512];

        int[] attribNameLength = new int[1];
        int[] attribSize = new int[1];
        int[] attribType = new int[1];

        // Get the uniform info (name, type, size)
        this.gl.glGetActiveAttrib(this.program.getId(), index,
                attribName.length, attribNameLength, 0, attribSize, 0,
                attribType, 0, attribName, 0);

        this.name = new String(attribName, 0, attribNameLength[0]);
        this.size = attribSize[0];
        this.type = attribType[0];

        // Get the uniform location within the program
        this.location = this.gl.glGetAttribLocation(this.program.getId(), this.name);
        
        this.enabled = false;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getSize()
    {
        return size;
    }

    public void setSize(int size)
    {
        this.size = size;
    }

    public int getType()
    {
        return type;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public int getLocation()
    {
        return location;
    }

    public void setLocation(int location)
    {
        this.location = location;
    }
    
    public void enable()
    {
        gl.glEnableVertexAttribArray(location);
        enabled = true;
    }
    
    public void disable()
    {
        gl.glEnableVertexAttribArray(location);
        enabled = false;
    }
    
    public boolean isEnabled()
    {
        return enabled;
    }
}
