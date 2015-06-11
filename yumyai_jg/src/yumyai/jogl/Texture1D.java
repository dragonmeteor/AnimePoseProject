package yumyai.jogl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import java.nio.Buffer;

public class Texture1D extends Texture {
    private static Logger logger = LoggerFactory.getLogger(Texture1D.class);
    private int width;
    private boolean allocated = false;

    public Texture1D(GL2 gl) {
        super(gl, GL2.GL_TEXTURE_1D, GL2.GL_RGBA);
    }

    public Texture1D(GL2 gl, int internalFormat) {
        super(gl, GL2.GL_TEXTURE_1D, internalFormat);
    }

    public void setData(int width, int format, int type, Buffer buffer) {
        this.width = width;

        Texture oldTexture = TextureUnit.getActiveTextureUnit(gl).getBoundTexture();
        if (oldTexture != this) {
            bind();
        }

        /*
        logger.debug("width = " + width);
        logger.debug("format = " + format);
        logger.debug("type = " + type);
        */

        if (buffer != null) {
            buffer.rewind();
            //logger.debug("buffer size = " + buffer.capacity());
        }
        gl.glTexImage1D(target, 0, internalFormat, width, 0, format, type, buffer);

        if (oldTexture == null) {
            unbind();
        } else if (oldTexture != this) {
            oldTexture.bind();
        }

        allocated = true;
    }

    public void allocate(int width, int format, int type) {
        setData(width, format, type, null);
    }
}
