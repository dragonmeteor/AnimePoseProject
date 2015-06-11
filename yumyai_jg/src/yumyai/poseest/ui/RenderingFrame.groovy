package yumyai.poseest.ui

import yumyai.gfx.ver01.RenderEngineV1
import yumyai.gfx.ver01.material.PhongMaterialV1
import yumyai.gfx.ver01.material.TexturedPhongMaterialV1
import yumyai.jogl.ui.GLController
import yumyai.jogl.ui.GLViewPanel
import yumyai.jogl.ui.PerspectiveCamera
import yumyai.poseest.mmd.Settings

import javax.media.opengl.GL2
import javax.media.opengl.GLAutoDrawable
import javax.swing.JFrame
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

abstract class RenderingFrame extends JFrame implements GLController {
    int screenWidth, screenHeight
    RenderEngineV1 renderEngine
    PerspectiveCamera camera
    GLViewPanel glViewPanel

    public RenderingFrame(String title, int screenWidth, int screenHeight) {
        super(title)
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
    }

    @Override
    void init(GLAutoDrawable glAutoDrawable) {
        final GL2 gl = glAutoDrawable.getGL().getGL2();

        gl.glClearColor(1, 1, 1, 1);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        PhongMaterialV1.createProgram(gl);
        TexturedPhongMaterialV1.createProgram(gl);

        renderEngine = new RenderEngineV1(gl);
        renderEngine.getTexturePool().setCapacity(64);

        camera = new PerspectiveCamera(0.1f, Float.MAX_VALUE, Settings.DEFAULT_FOVY);
        camera.up.set(0,1,0);
        camera.setAspect((float)(screenWidth * 1.0f / screenHeight))
    }

    void run() {
        getContentPane().setLayout(new BorderLayout());

        glViewPanel = new GLViewPanel(30) {};
        glViewPanel.addGLController(this);
        glViewPanel.setMinimumSize(new Dimension(screenWidth, screenHeight))
        glViewPanel.setPreferredSize(new Dimension(screenWidth, screenHeight))
        glViewPanel.setSize(screenWidth, screenHeight)
        getContentPane().add(glViewPanel, BorderLayout.CENTER)

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

        pack()
        setLocationRelativeTo(null)
        setVisible(true)
    }

    @Override
    void dispose(GLAutoDrawable glAutoDrawable) {
        renderEngine.dispose()
        PhongMaterialV1.destroyProgram()
        TexturedPhongMaterialV1.destroyProgram()
    }

    @Override
    void display(GLAutoDrawable glAutoDrawable) {
        final GL2 gl = glAutoDrawable.getGL().getGL2();

        gl.glClearColor(0,0,0,0)
        gl.glClearDepth(1)
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT)

        draw(glAutoDrawable)
    }

    abstract void draw(GLAutoDrawable glAutoDrawable);

    @Override
    void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
        final GL2 gl = glAutoDrawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        screenWidth = width;
        screenHeight = height;
        camera.setAspect((float)(width * 1.0f / height));
    }

    @Override
    void keyTyped(KeyEvent e) {

    }

    @Override
    void keyPressed(KeyEvent e) {

    }

    @Override
    void keyReleased(KeyEvent e) {

    }

    @Override
    void mouseClicked(MouseEvent e) {

    }

    @Override
    void mousePressed(MouseEvent e) {

    }

    @Override
    void mouseReleased(MouseEvent e) {

    }

    @Override
    void mouseEntered(MouseEvent e) {

    }

    @Override
    void mouseExited(MouseEvent e) {

    }

    @Override
    void mouseDragged(MouseEvent e) {

    }

    @Override
    void mouseMoved(MouseEvent e) {

    }
}
