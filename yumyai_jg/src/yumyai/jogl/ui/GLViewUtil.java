package yumyai.jogl.ui;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

public class GLViewUtil {
    public static GLCapabilities getDefaultCapabilities() {
        GLProfile glProfile = GLProfile.getDefault();
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setAlphaBits(8);
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setNumSamples(1);
        return glCapabilities;
    }

    public static GLView create() {
        return create(getDefaultCapabilities(), 60);
    }

    public static GLView create(float initialFrameRate) {
        return create(getDefaultCapabilities(), initialFrameRate);
    }

    public static GLView create(GLCapabilities glCapabilities) {
        return create(glCapabilities, 60);
    }

    public static GLView create(GLCapabilities glCapabilities, float initialFrameRate) {
        GLView glView;
        if (System.getProperty("os.name").startsWith("Windows") || System.getProperty("os.name").startsWith("Linux")) {
            glView = new GLViewWindows(glCapabilities, initialFrameRate);
        } else {
            glView = new GLViewMac(glCapabilities, initialFrameRate);
        }
        return glView;
    }
}
