/*
 */
package yumyai.jogl;

import javax.media.opengl.GL2;

public class Fbo {
    /**
     * Static members.
     */
    private static boolean staticInitialized = false;
    private static int numColorAttachements;
    private Fbo boundFbo = null;
    /**
     * Constants for different color attachments.
     */
    private static int[] COLOR_ATTACHMENTS =
            {
                    GL2.GL_COLOR_ATTACHMENT0,
                    GL2.GL_COLOR_ATTACHMENT1,
                    GL2.GL_COLOR_ATTACHMENT2,
                    GL2.GL_COLOR_ATTACHMENT3,
                    GL2.GL_COLOR_ATTACHMENT4,
                    GL2.GL_COLOR_ATTACHMENT5,
                    GL2.GL_COLOR_ATTACHMENT6,
                    GL2.GL_COLOR_ATTACHMENT7,
                    GL2.GL_COLOR_ATTACHMENT8,
                    GL2.GL_COLOR_ATTACHMENT9,
                    GL2.GL_COLOR_ATTACHMENT10,
                    GL2.GL_COLOR_ATTACHMENT11,
                    GL2.GL_COLOR_ATTACHMENT12,
                    GL2.GL_COLOR_ATTACHMENT13,
                    GL2.GL_COLOR_ATTACHMENT14,
                    GL2.GL_COLOR_ATTACHMENT15
            };
    /**
     * Instance members
     */
    private GL2 gl;
    private int id;
    private boolean disposed = false;
    private Texture[] colorAttachements;
    private Texture depthAttachment;
    private Texture stencilAttachment;
    private boolean bound = false;

    public static void staticInitialize(GL2 gl) {
        if (!staticInitialized) {
            int[] temp = new int[1];
            gl.glGetIntegerv(GL2.GL_MAX_COLOR_ATTACHMENTS, temp, 0);
            numColorAttachements = temp[0];
            System.out.println("num color attachement = " + temp[0]);

            staticInitialized = true;
        }
    }

    public Fbo(GL2 gl) {
        staticInitialize(gl);

        this.gl = gl;
        int[] ids = new int[1];
        gl.glGenFramebuffers(1, ids, 0);
        this.id = ids[0];

        colorAttachements = new Texture[numColorAttachements];
    }

    public int getId() {
        return id;
    }

    public void bind() {
        if (boundFbo != null) {
            boundFbo.unbind();
        }
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, id);
        boundFbo = this;
        bound = true;
    }

    public void unbind() {
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
        boundFbo = null;
        bound = false;
    }

    public boolean isBound() {
        return bound;
    }

    protected void finalize() {
        dispose();
    }

    public void dispose() {
        unbind();

        if (!disposed) {
            int idv[] = new int[1];
            idv[0] = id;
            gl.glDeleteFramebuffers(1, idv, 0);
            disposed = true;
        }
    }

    public Texture getColorAttachements(int index) {
        return colorAttachements[index];
    }

    public Texture getDepthAttachement() {
        return depthAttachment;
    }

    public Texture getStencilAttachment() {
        return stencilAttachment;
    }

    public Fbo getBoundFbo() {
        return boundFbo;
    }

    public void checkBound() {
        if (!isBound()) {
            throw new RuntimeException("the fbo is not bound");
        }
    }

    public void attachColorBuffer(int index, Texture texture) {
        checkBound();
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, COLOR_ATTACHMENTS[index], texture.getTarget(), texture.getId(), 0);
        colorAttachements[index] = texture;
    }

    public void detachColorBuffer(int index) {
        checkBound();
        if (colorAttachements[index] != null) {
            gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, COLOR_ATTACHMENTS[index], colorAttachements[index].getTarget(), 0, 0);
            colorAttachements[index] = null;
        }
    }

    public void detachColorBuffer(int index, int target) {
        checkBound();
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, COLOR_ATTACHMENTS[index], target, 0, 0);
        colorAttachements[index] = null;
    }

    public void attachDepthBuffer(Texture texture) {
        checkBound();
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, texture.getTarget(), texture.getId(), 0);
        depthAttachment = texture;
    }

    public void detachDepthTexture() {
        checkBound();
        if (depthAttachment != null) {
            gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, depthAttachment.getTarget(), 0, 0);
            depthAttachment = null;
        }
    }

    public void detachDepthTexture(int target) {
        checkBound();
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, target, 0, 0);
        depthAttachment = null;
    }

    public void attachStencilBuffer(Texture texture) {
        checkBound();
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_STENCIL_ATTACHMENT, texture.getTarget(), texture.getId(), 0);
        stencilAttachment = texture;
    }

    public void detachStencilTexture() {
        checkBound();
        if (stencilAttachment != null) {
            gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_STENCIL_ATTACHMENT, stencilAttachment.getTarget(), 0, 0);
            stencilAttachment = null;
        }
    }

    public void detachStencilTexture(int target) {
        checkBound();
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, target, 0, 0);
        stencilAttachment = null;
    }

    public void drawToNone() {
        checkBound();
        gl.glDrawBuffer(GL2.GL_NONE);
    }

    public void readFromNone() {
        checkBound();
        gl.glReadBuffer(GL2.GL_NONE);
    }

    public void drawTo(int start, int count) {
        checkBound();
        gl.glDrawBuffers(count, COLOR_ATTACHMENTS, start);
    }

    public void readFrom(int index) {
        checkBound();
        gl.glReadBuffer(COLOR_ATTACHMENTS[index]);
    }

    public void drawTo(Texture t0) {
        checkBound();
        attachColorBuffer(0, t0);
        drawTo(0, 1);
    }

    public void drawTo(Texture t0, Texture t1) {
        checkBound();
        attachColorBuffer(0, t0);
        attachColorBuffer(1, t1);
        drawTo(0, 2);
    }

    public void drawTo(Texture t0, Texture t1, Texture t2) {
        checkBound();
        attachColorBuffer(0, t0);
        attachColorBuffer(1, t1);
        attachColorBuffer(2, t2);
        drawTo(0, 3);
    }

    public void drawTo(Texture t0, Texture t1, Texture t2, Texture t3) {
        checkBound();
        attachColorBuffer(0, t0);
        attachColorBuffer(1, t1);
        attachColorBuffer(2, t2);
        attachColorBuffer(3, t3);
        drawTo(0, 4);
    }

    public void detachAllColorBuffers() {
        checkBound();
        for (int i = 0; i < numColorAttachements; i++) {
            if (colorAttachements[i] != null) {
                detachColorBuffer(i);
            }
        }
    }

    public void detachAll() {
        checkBound();
        detachAllColorBuffers();
    }

    public static void checkStatus(GL2 gl) {
        int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
        switch (status) {
            case GL2.GL_FRAMEBUFFER_COMPLETE:
                return;
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                throw new RuntimeException("frame buffer incomplete: incomplete attachement");
            case GL2.GL_FRAMEBUFFER_UNSUPPORTED:
                throw new RuntimeException("Unsupported frame buffer format");
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                throw new RuntimeException("frame buffer incomplete: missing attachment");
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
                throw new RuntimeException("frame buffer incomplete: attached images must have same dimensions");
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
                throw new RuntimeException("frame buffer incomplete: attached images must have same format");
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                throw new RuntimeException("frame buffer incomplete: missing draw buffer");
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                throw new RuntimeException("frame buffer incomplete: missing read buffer");
        }
    }
}
