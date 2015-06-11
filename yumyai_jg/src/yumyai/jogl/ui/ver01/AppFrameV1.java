/*
 */
package yumyai.jogl.ui.ver01;

import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import layout.TableLayout;
import yumyai.gfx.ver01.RenderEngineV1;
import yumyai.gfx.ver01.material.PhongMaterialV1;
import yumyai.gfx.ver01.material.TexturedPhongMaterialV1;
import yumyai.ui.BasicAction;
import yumyai.jogl.ui.CameraController;
import yumyai.jogl.ui.GLSceneDrawer;
import yumyai.jogl.ui.GLViewPanel;
import yumyai.jogl.ui.GLViewPanelWithCameraControl;
import yumyai.jogl.ui.PerspectiveCamera;
import yumyai.jogl.ui.PerspectiveCameraController;

public class AppFrameV1 extends JFrame implements GLSceneDrawer, ActionListener {
    protected static final String EXIT_TEXT = "Exit";
    /**
     * Flag infrastructure
     */
    protected HashMap<String, Boolean> flags = new HashMap<String, Boolean>();
    protected HashMap<String, JCheckBoxMenuItem> flagMenus = new HashMap<String, JCheckBoxMenuItem>();
    /**
     * Graphics related fields
     */
    protected GLViewPanel glViewPanel;
    protected PerspectiveCamera camera;
    protected PerspectiveCameraController cameraController;
    protected RenderEngineV1 renderEngine;
    /**
     * UI Elements
     */
    protected JPanel controlPanel;
    protected float cellSize = 5;
    protected JMenuBar menuBar;
    protected JMenu fileMenu;
    protected JMenu flagMenu;

    protected void initializeFlags() {
        // NOP
    }

    public AppFrameV1() {
        super();
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // NOP
        }
    }

    protected void createFlag(String command, int key, int mask, boolean initialValue) {
        flags.put(command, initialValue);
        BasicAction action = new BasicAction(command, this);
        action.setAcceleratorKey(key, mask);
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem(action);
        menu.setSelected(initialValue);
        flags.put(command, initialValue);
        flagMenus.put(command, menu);
    }

    protected void initializeControlPanel() {
        controlPanel = new JPanel();
        LayoutManager controlPanelLayout = createControlPanelLayout();
        controlPanel.setLayout(controlPanelLayout);

        getContentPane().add(controlPanel, BorderLayout.SOUTH);
    }

    protected void addLabeledComponent(String text, JComponent component, int fromCol, int toCol, int row) {
        JLabel label = new JLabel(text);
        controlPanel.add(label, String.format("%d, %d, %d, %d", fromCol, row, fromCol, row));
        controlPanel.add(component, String.format("%d, %d, %d, %d", fromCol + 2, row, toCol, row));
    }

    protected LayoutManager createControlPanelLayout() {
        double tableLayoutSizes[][] =
                {
                        {
                                5, TableLayout.MINIMUM,
                                5, TableLayout.FILL,
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

    protected void initializeMenuBar() {
        BasicAction exit = new BasicAction(EXIT_TEXT, this);

        menuBar = new JMenuBar();
        JMenu menu;
        menu = new JMenu("File");
        menu.setMnemonic('F');
        menu.add(new JMenuItem(exit));
        menuBar.add(menu);
        fileMenu = menu;

        menu = new JMenu("Flags");
        menu.setMnemonic('F');
        for (JCheckBoxMenuItem menuItem : flagMenus.values()) {
            menu.add(menuItem);
        }
        menuBar.add(menu);
        flagMenu = menu;

        setJMenuBar(menuBar);
    }

    public void run() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                terminate();
            }
        });

        camera = new PerspectiveCamera(0.1f, 10000, 45);
        camera.eye.set(0, 50, 80);
        camera.target.set(0, 30, 0);
        camera.dollyFactor = 24;
        cameraController = new PerspectiveCameraController(camera, this);
        glViewPanel = new GLViewPanelWithCameraControl(60, cameraController);
        getContentPane().add(glViewPanel, BorderLayout.CENTER);

        glViewPanel.getGlView().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                glViewKeyTyped(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                glViewKeyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                glViewKeyReleased(e);
            }
        });

        initializeControlPanel();
        initializeFlags();
        initializeMenuBar();

        setSize(1024, 768);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    protected void glViewKeyTyped(KeyEvent e) {
        // NOP
    }

    protected void glViewKeyPressed(KeyEvent e) {
        //  NOP
    }

    protected void glViewKeyReleased(KeyEvent e) {
        // NOP
    }

    public void terminate() {
        new Thread() {
            @Override
            public void run() {
                glViewPanel.stopAnimation();
            }
        }.start();

        renderEngine.dispose();
        PhongMaterialV1.destroyProgram();
        TexturedPhongMaterialV1.destroyProgram();

        dispose();
        System.exit(0);
    }

    @Override
    public void init(GLAutoDrawable drawable, CameraController controller) {
        GL2 gl = drawable.getGL().getGL2();
        renderEngine = new RenderEngineV1(gl);
        gl.glClearColor(1, 1, 1, 1);

        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    @Override
    public void draw(GLAutoDrawable drawable, CameraController controller) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glEnable(GL2.GL_DEPTH_TEST);

        drawCoordinateAxesAndPlanes(gl);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd == null) {
            // NOP
        } else if (cmd.equals(EXIT_TEXT)) {
            terminate();
        } else if (flags.keySet().contains(cmd)) {
            boolean oldVal = flags.get(cmd);
            flags.put(cmd, !oldVal);
            flagMenus.get(cmd).setSelected(!oldVal);
        }
    }

    /**
     * Displays an exception in a window
     *
     * @param e
     */
    protected void showExceptionDialog(Exception e) {
        String str = "The following exception was thrown: " + e.toString() + ".\n\n" + "Would you like to see the stack trace?";
        int choice = JOptionPane.showConfirmDialog(this, str, "Exception Thrown", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            e.printStackTrace();
        }
    }

    @Override
    public void mousePressed(MouseEvent e, CameraController controller) {
        // NOP
    }

    @Override
    public void mouseReleased(MouseEvent e, CameraController controller) {
        // NOP
    }

    @Override
    public void mouseDragged(MouseEvent e, CameraController controller) {
        // NOP
    }

    @Override
    public void mouseMoved(MouseEvent e, CameraController controller) {
        // NOP
    }

    protected void drawCoordinateAxesAndPlanes(GL2 gl) {
        gl.glLineWidth(2.0f);

        gl.glBegin(GL2.GL_LINES);
        {
            gl.glColor3f(1, 0, 0);
            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f(cellSize * 10, 0, 0);

            gl.glColor3f(0, 1, 1);
            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f(-cellSize * 10, 0, 0);

            gl.glColor3f(0, 1, 0);
            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f(0, cellSize * 10, 0);

            gl.glColor3f(1, 0, 1);
            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f(0, -cellSize * 10, 0);

            gl.glColor3f(0, 0, 1);
            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f(0, 0, cellSize * 10);

            gl.glColor3f(1, 1, 0);
            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f(0, 0, -cellSize * 10);
        }
        gl.glEnd();

        gl.glLineWidth(1.0f);
        gl.glColor3f(0.5f, 0.5f, 0.5f);
        gl.glBegin(GL2.GL_LINES);
        {
            for (int i = -10; i <= 10; i++) {
                gl.glVertex3f(cellSize * i, 0, -cellSize * 10);
                gl.glVertex3f(cellSize * i, 0, cellSize * 10);
            }

            for (int i = -10; i <= 10; i++) {
                gl.glVertex3f(-cellSize * 10, 0, cellSize * i);
                gl.glVertex3f(cellSize * 10, 0, cellSize * i);
            }
        }
        gl.glEnd();
    }

    @Override
    public void dispose(GLAutoDrawable drawable, CameraController controller) {
        // NOP
    }
}
