package yumyai.jogl;

import com.jogamp.opengl.util.texture.TextureData;

import java.nio.Buffer;
import javax.media.opengl.GL2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextureTwoDim extends Texture {
    private static Logger logger = LoggerFactory.getLogger(TextureTwoDim.class);
    protected int width;
    protected int height;
    protected boolean allocated;
    protected boolean textureHasMipmap;

    public TextureTwoDim(GL2 gl, int target, int internalFormat) {
        this(gl, target, internalFormat, true);
    }

    public TextureTwoDim(GL2 gl, int target, int internalFormat, boolean hasMipmap) {
        super(gl, target, internalFormat);
        allocated = false;
        if (hasMipmap) {
            minFilter = GL2.GL_LINEAR_MIPMAP_LINEAR;
            magFilter = GL2.GL_LINEAR;
        } else {
            minFilter = GL2.GL_NEAREST;
            magFilter = GL2.GL_NEAREST;
        }
        this.textureHasMipmap = hasMipmap;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setImage(int width, int height, int format, int type, Buffer buffer) {
        this.width = width;
        this.height = height;

        Texture oldTexture = TextureUnit.getActiveTextureUnit(gl).getBoundTexture();
        if (oldTexture != this) {
            bind();
        }

        logger.debug("width = " + width);
        logger.debug("height = " + height);
        logger.debug("format = " + format);
        logger.debug("type = " + type);

        buffer.rewind();
        logger.debug("buffer size = " + buffer.capacity());
        gl.glTexImage2D(target, 0, internalFormat, width, height, 0, format, type, buffer);
        if (textureHasMipmap)
            gl.glGenerateMipmap(target);

        if (oldTexture == null) {
            unbind();
        } else if (oldTexture != this) {
            oldTexture.bind();
        }

        allocated = true;
    }

    public void setImage(TextureData data) {
        setImage(data.getWidth(), data.getHeight(), data.getPixelFormat(), data.getPixelType(), data.getBuffer());
    }

    protected void allocate(int width, int height, int format, int type) {
        setImage(width, height, format, type, null);
    }

    public boolean isAllocated() {
        return allocated;
    }

    public boolean hasMipmap() { return textureHasMipmap; }
}
