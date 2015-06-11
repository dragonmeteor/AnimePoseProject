package yumyai.jogl;

import javax.media.opengl.GL2;
import javax.media.opengl.GLException;

public abstract class Texture
{
    public int minFilter;
    public int magFilter;
    public int wrapS;
    public int wrapT;
    public int wrapR;
    private boolean disposed;
    protected int id;
    protected int target;
    protected GL2 gl;
    protected TextureUnit boundTextureUnit;
    protected int internalFormat;
    
    public Texture(GL2 gl, int target, int internalFormat)
    {
        int idv[] = new int[1];
        gl.glGenTextures(1, idv, 0);
        this.gl = gl;
        this.id = idv[0];
        this.target = target;
        this.boundTextureUnit = null;
        this.internalFormat = internalFormat;
        this.disposed = false;

        minFilter = GL2.GL_NEAREST;
        magFilter = GL2.GL_NEAREST;
        /*
        wrapS = GL2.GL_CLAMP;
        wrapT = GL2.GL_CLAMP;
        wrapR = GL2.GL_CLAMP;
        */
        wrapS = GL2.GL_REPEAT;
        wrapT = GL2.GL_REPEAT;
        wrapR = GL2.GL_REPEAT;
    }

    public boolean isDisposed()
    {
        return disposed;
    }

    public boolean isBound()
    {
        return boundTextureUnit != null;
    }

    public int getId()
    {
        return id;
    }

    public int getTarget()
    {
        return target;
    }

    public void bind()
    {
        bindTo(TextureUnit.getActiveTextureUnit(gl));
    }

    public void bindTo(TextureUnit textureUnit)
    {
        if (isDisposed())
        {
            throw new GLException("program tries to bind a disposed texture");
        }

        textureUnit.bindTexture(this);
        boundTextureUnit = textureUnit;
    }

    public void unbind()
    {
        if (isBound())
        {
            if (isDisposed())
            {
                throw new GLException("program tries to unbind a disposed texture");
            }

            boundTextureUnit.unbindTexture(this);
            boundTextureUnit = null;
        }
    }

    protected void setTextureParameters()
    {
        gl.glTexParameteri(target, GL2.GL_TEXTURE_MAG_FILTER, magFilter);
        gl.glTexParameteri(target, GL2.GL_TEXTURE_MIN_FILTER, minFilter);
        gl.glTexParameteri(target, GL2.GL_TEXTURE_WRAP_S, wrapS);
        gl.glTexParameteri(target, GL2.GL_TEXTURE_WRAP_T, wrapT);
        gl.glTexParameteri(target, GL2.GL_TEXTURE_WRAP_R, wrapR);
    }

    public void use()
    {
        enable();
        bind();
        setTextureParameters();
    }

    public void unuse()
    {
        unbind();
        disable();
    }

    protected void finalize()
    {
        if (isBound())
        {
            unbind();
        }
        dispose();
    }

    public void dispose()
    {
        if (isBound())
        {
            throw new GLException("program tries to dispose a texture before unbinding it");
        }

        if (!disposed)
        {
            int idv[] = new int[1];
            idv[0] = id;
            gl.glDeleteTextures(1, idv, 0);
            disposed = true;
        }
    }

    public void enable()
    {
        gl.glEnable(target);
    }

    public void disable()
    {
        gl.glDisable(target);
    }    
}
