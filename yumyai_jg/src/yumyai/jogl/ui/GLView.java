package yumyai.jogl.ui;

import java.awt.event.KeyListener;

public interface GLView {
    public void addGLController(GLController controller);
    public void removeGLController(GLController controller);
    public void startAnimation();
    public void stopAnimation();
    public void repaint();

    public void addKeyListener(KeyListener keyListener);
}