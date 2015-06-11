package yumyai.jogl.ui;

import java.awt.event.MouseEvent;

import javax.media.opengl.GLAutoDrawable;

public interface GLSceneDrawer
{
    public void init(GLAutoDrawable drawable, CameraController controller);

    public void draw(GLAutoDrawable drawable, CameraController controller);
    
    public void dispose(GLAutoDrawable drawable, CameraController controller);

    public void mousePressed(MouseEvent e, CameraController controller);

    public void mouseReleased(MouseEvent e, CameraController controller);

    public void mouseDragged(MouseEvent e, CameraController controller);

    public void mouseMoved(MouseEvent e, CameraController controller);
}
