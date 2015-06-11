package yumyai.jogl.ui;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.media.opengl.GLAutoDrawable;

public abstract class BlankGLController implements GLController
{
    @Override
    public void display(GLAutoDrawable drawable)
    {
        // NOP
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        // NOP
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
        // NOP
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width,
            int height)
    {
        // NOP
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        // NOP
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        // NOP
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        // NOP
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        // NOP
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        // NOP
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        // NOP
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        // NOP
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        // NOP
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        // NOP
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
        // NOP
    }
}
