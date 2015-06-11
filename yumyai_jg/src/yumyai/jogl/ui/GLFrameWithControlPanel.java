package yumyai.jogl.ui;

import layout.TableLayout;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;

public class GLFrameWithControlPanel extends JFrame implements GLController,
        ChangeListener, ActionListener {
    protected GLViewPanel glViewPanel;
    protected JPanel controlPanel;

    public GLFrameWithControlPanel() {
        initGui();
    }

    protected void initGui() {
        getContentPane().setLayout(new BorderLayout());

        glViewPanel = new GLViewPanel(60) {
        };
        glViewPanel.addGLController(this);
        glViewPanel.setMinimumSize(new Dimension(800, 600));
        glViewPanel.setPreferredSize(new Dimension(1200, 800));
        getContentPane().add(glViewPanel, BorderLayout.CENTER);

        initControlPanel();

        getContentPane().add(controlPanel, BorderLayout.SOUTH);

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                glViewPanel.requestFocusInWindow();
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    protected void initControlPanel() {
        controlPanel = new JPanel();
        LayoutManager controlPanelLayout = createControlPanelLayout();
        controlPanel.setLayout(controlPanelLayout);
    }

    protected LayoutManager createControlPanelLayout() {
        double tableLayoutSizes[][] =
                {
                        {
                                5, TableLayout.MINIMUM, 5, TableLayout.FILL,
                                5, TableLayout.MINIMUM, 5, TableLayout.FILL,
                                5,
                        },
                        {
                                5, TableLayout.MINIMUM,
                                5
                        }
                };
        TableLayout tableLayout = new TableLayout(tableLayoutSizes);
        return tableLayout;
    }

    @Override
    public void init(GLAutoDrawable glad) {
        // NOP
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        // NOP
    }

    @Override
    public void display(GLAutoDrawable glad) {
        final GL2 gl = glad.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 0);
        gl.glClearDepth(1);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void reshape(GLAutoDrawable glad, int i, int i1, int i2, int i3) {
        // NOP
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // NOP
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // NOP
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // NOP
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // NOP
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // NOP
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // NOP
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // NOP
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // NOP
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // NOP
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // NOP
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        // NOP
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // NOP
    }
}

