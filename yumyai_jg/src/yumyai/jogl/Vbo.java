/*
 */
package yumyai.jogl;

import java.nio.ByteBuffer;
import javax.media.opengl.GL2;

public class Vbo
{    
    private int id;
    private GL2 gl;
    private VboTarget target;
    private boolean disposed;    
    
    public Vbo(GL2 gl, VboTarget target)
    {
        this.gl = gl;        
        this.target = target;
        int[] idv = new int[1];
        gl.glGenBuffers(1, idv, 0);
        id = idv[0];
    }
    
    public GL2 getGL()
    {
        return gl;
    }
    
    public void bind()
    {
        if (target.getBoundVbo() != null)
        {
            target.getBoundVbo().unbind();
        }                
        gl.glBindBuffer(target.getConstant(), id);
        target.setBoundVbo(this);
    }
    
    public void unbind()
    {
        if (target.getBoundVbo() == this)
        {
            gl.glBindBuffer(target.getConstant(), 0);
            target.setBoundVbo(null);
        }
    }       
    
    public void use()
    {
        bind();
    }
    
    public void unuse()
    {
        unbind();
    }
    
    public boolean isBound()
    {
        return target.getBoundVbo() == this;
    }
    
    public int getId()
    {
        return id;
    }
    
    public void setData(ByteBuffer buffer)
    {        
        bind();
        buffer.rewind();
        gl.glBufferData(target.getConstant(), buffer.capacity(), buffer, GL2.GL_STATIC_DRAW);
        unbind();
    }
    
    public void dispose()
    {
        if (isBound())
        {
            throw new RuntimeException("cannot dispose a bound vertex array buffer");
        }
        
        if (!disposed)
        {
            int idv[] = new int[1];
            idv[0] = id;
            gl.glDeleteBuffers(1, idv, 0);
            disposed = true;
        }
    }
}
