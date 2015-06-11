package yumyai.jogl.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLProfile;
import javax.swing.JPanel;
import javax.swing.Timer;

public abstract class GLViewPanel extends JPanel
        implements GLController, ActionListener {
    private static final long serialVersionUID = 1L;
    protected int initialFrameRate;
    protected GLView glView;
    protected Timer timer;

    public GLViewPanel(int frameRate) {
        super(new BorderLayout());

        initialFrameRate = frameRate;

        GLProfile glProfile = GLProfile.getDefault();
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setAlphaBits(8);
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setNumSamples(8);

        glView = GLViewUtil.create();
        glView.addGLController(this);

        timer = new Timer(1000 / initialFrameRate, this);

        add((Component)glView, BorderLayout.CENTER);
    }

    public GLView getGlView() {
        return glView;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        timer.start();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        // NOP
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
        // NOP
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        ((Component) drawable).setMinimumSize(new Dimension(0, 0));
    }

    @Override
    public void keyTyped(KeyEvent key) {
        // NOP
    }

    @Override
    public void keyPressed(KeyEvent key) {
        // NOP
    }

    @Override
    public void keyReleased(KeyEvent key) {
        // NOP
    }

    @Override
    public void mouseClicked(MouseEvent mouse) {
        // NOP
    }

    @Override
    public void mousePressed(MouseEvent mouse) {
        // NOP
    }

    @Override
    public void mouseReleased(MouseEvent mouse) {
        // NOP
    }

    @Override
    public void mouseEntered(MouseEvent mouse) {
        // NOP
    }

    @Override
    public void mouseExited(MouseEvent mouse) {
        // NOP
    }

    @Override
    public void mouseDragged(MouseEvent mouse) {
        // NOP
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // NOP
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // NOP
    }

    public void startAnimation() {
        timer.start();
    }

    public void stopAnimation() {
        timer.stop();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == timer) {
            glView.repaint();
        }
    }

    public void immediatelyRepaint() {
        glView.repaint();
    }

    public void addGLController(GLController controller) {
        glView.addGLController(controller);
    }
}