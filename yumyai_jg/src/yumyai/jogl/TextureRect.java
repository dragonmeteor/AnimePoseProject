package yumyai.jogl;


import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TextureRect extends TextureTwoDim {

    public TextureRect(GL2 gl) {
        super(gl, GL2.GL_TEXTURE_RECTANGLE, GL2.GL_RGBA, false);
    }

    public TextureRect(GL2 gl, int internalFormat) {
        super(gl, GL2.GL_TEXTURE_RECTANGLE, internalFormat, false);
    }

    public TextureRect(GL2 gl, String filename) throws IOException {
        this(gl, filename, GL2.GL_RGBA);
    }

    public TextureRect(GL2 gl, String filename, int internalFormat) throws IOException {
        this(gl, new File(filename), internalFormat);
    }

    public TextureRect(GL2 gl, File file) throws IOException {
        this(gl, file, GL2.GL_RGBA);
    }

    public TextureRect(GL2 gl, File file, int internalFormat) throws IOException {
        super(gl, GL2.GL_TEXTURE_2D, internalFormat, false);
        TextureData data = null;
        if (FilenameUtils.getExtension(file.getAbsolutePath()).toLowerCase().equals("png")) {
            BufferedImage image = ImageIO.read(file);
            ImageUtil.flipImageVertically(image);
            data = AWTTextureIO.newTextureData(GLProfile.getDefault(), image, false);
        } else {
            data = TextureIO.newTextureData(GLProfile.getDefault(), file, false, null);
            if (data.getMustFlipVertically()) {
                BufferedImage image = ImageIO.read(file);
                ImageUtil.flipImageVertically(image);
                data = AWTTextureIO.newTextureData(GLProfile.getDefault(), image, false);
            }
        }
        setImage(data);
    }

    @Override
    public void allocate(int width, int height, int format, int type) {
        super.allocate(width, height, format, type);
    }
}