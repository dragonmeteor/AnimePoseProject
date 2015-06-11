package yumyai.poseest.mmd.character

import groovy.swing.SwingBuilder
import layout.TableLayout
import org.apache.commons.io.FilenameUtils
import yumyai.gfx.ver01.MeshV1
import yumyai.gfx.ver01.RenderEngineV1
import yumyai.gfx.ver01.material.PhongMaterialV1
import yumyai.gfx.ver01.material.TexturedPhongMaterialV1
import yumyai.gfx.ver01.mesh.PmdMeshV1
import yumyai.gfx.ver01.mesh.PmdToMeshV1Converter
import yumyai.gfx.ver01.mesh.PmxMeshV1
import yumyai.gfx.ver01.mesh.PmxToMeshV1Converter
import yumyai.jogl.ui.CameraController
import yumyai.jogl.ui.GLSceneDrawer
import yumyai.jogl.ui.GLViewPanel
import yumyai.jogl.ui.GLViewPanelWithCameraControl
import yumyai.jogl.ui.PerspectiveCamera
import yumyai.jogl.ui.PerspectiveCameraController
import yumyai.mmd.pmd.PmdModel
import yumyai.mmd.pmx.PmxModel

import javax.media.opengl.GL2
import javax.media.opengl.GLAutoDrawable
import javax.swing.ButtonGroup
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.vecmath.Matrix4f
import javax.vecmath.Vector3f
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.LayoutManager
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

class ViewMmdModelListV1 extends JFrame implements GLSceneDrawer, ActionListener {
    final static float AXIS_LENGTH = 50.0F;
    final static float CELL_WIDTH = AXIS_LENGTH / 10.0F;

    String inputFileName
    ArrayList<String> fileNames
    ArrayList<Integer> fileFlags

    PerspectiveCamera camera
    javax.swing.Timer timer
    PerspectiveCameraController cameraController
    GLViewPanel glViewPanel
    RenderEngineV1 renderEngine;

    JPanel controlPanel;
    JComboBox<String> fileNameComboBox;

    Vector3f lightAmbient = new Vector3f(0.7f, 0.7f, 0.7f);
    Vector3f lightDiffuse = new Vector3f(0.5f, 0.5f, 0.5f);
    Vector3f lightSpecular = new Vector3f(1.0f, 1.0f, 1.0f);
    Vector3f lightPosition = new Vector3f(0, 100, 100);
    Vector3f lightDirection = new Vector3f(0,-1,-1);
    Matrix4f viewInverse = new Matrix4f();

    JRadioButton[] usageRadioButtons
    ButtonGroup modelUsageGroup

    int currentIndex = 0;
    MeshV1 mesh = null;
    boolean dirty = false

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java yumyai.app.mmd.ViewMmdModelList <list-file-name>")
            System.exit(0);
        }

        final def fileNames = new ArrayList<String>()
        final def fileFlags = new ArrayList<Integer>()
        new File(args[0]).withReader { fin ->
            def lines = fin.readLines()
            for (int i = 0; i < lines.size(); i++) {
                if (lines[i].length() > 0) {
                    String fileName = lines[i].substring(2).trim();
                    File file = new File(fileName);
                    if (file.exists()) {
                        fileNames.add(fileName);
                        fileFlags.add(Integer.valueOf(lines[i].substring(0, 1)))
                    }
                }
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ViewMmdModelListV1(args[0], fileNames, fileFlags).run();
            }
        });
    }

    public ViewMmdModelListV1(String inputFileName, ArrayList<String> fileNames, ArrayList<Integer> fileFlags) {
        super("View MMD Model List (Version 1)")
        this.fileNames = fileNames
        this.fileFlags = fileFlags
        this.inputFileName = inputFileName

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (Exception e) {
            // NOP
        }
    }

    def run() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitProgram()
            }
        })

        initializeViewPanel()
        initializeControlPanel()
        initializeMenuBar()

        mesh = loadMesh(fileNames[0])
        usageRadioButtons[fileFlags[0]].setSelected(true)

        timer = new javax.swing.Timer(30, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAnimation();
            }
        });
        timer.start();

        setSize(1024, 768)
        setLocationRelativeTo(null)
        setVisible(true)
    }

    private void initializeMenuBar() {
        def builder = new SwingBuilder()
        def menuBar = builder.menuBar() {
            menu(text: 'File', mnemonic: 'f') {
                menuItem(text: "Save", mnemonic: 's', accelerator: shortcut('ctrl S'), actionPerformed: {
                    save()
                    println("Saved.")
                })
                menuItem(text: "Exit", mnemonic: 'x', accelerator: shortcut('ctrl Q'), actionPerformed: {
                    exitProgram()
                })
            }
            menu(text: 'Usage', mnemonic: 'U') {
                menuItem(text: "Set flag to 'Use'", accelerator: KeyStroke.getKeyStroke('1'), actionPerformed: {
                    setCurrentFileUsage(0)
                })
                menuItem(text: "Set flag to 'Do not use'", accelerator: KeyStroke.getKeyStroke('2'), actionPerformed: {
                    setCurrentFileUsage(1)
                })
                menuItem(text: "Set flag to 'Undetermined'", accelerator: KeyStroke.getKeyStroke('3'), actionPerformed: {
                    setCurrentFileUsage(2)
                })
                menuItem(text: "Set flag to 'Resize or Convert to PNG", accelerator: KeyStroke.getKeyStroke('4'), actionPerformed: {
                    setCurrentFileUsage(3)
                })
            }
            menu(text: 'Navigate', mnemonic: 'N') {
                menuItem(text: "Go to first 'Undetermined' file", accelerator: shortcut("ctrl D"), actionPerformed: {
                    int first = -1;
                    for (int i = 0; i < fileNames.size(); i++) {
                        if (fileFlags[i] == 2) {
                            first = i
                            break
                        }
                    }
                    if (first != -1) {
                        changeCurrentFile(first)
                        fileNameComboBox.setSelectedIndex(first)
                    } else {
                        println("No files with 'Undetermined' flag.")
                    }
                })
            }
            menu(text: 'Info', mnemonic: 'I') {
                menuItem(text: "Print statistics", accelerator: shortcut("ctrl A"), actionPerformed: {
                    int useCount = 0
                    int dontUseCount = 0
                    int undeterminedCount = 0
                    for (int i = 0; i < fileNames.size(); i++) {
                        if (fileFlags[i] == 2)
                            undeterminedCount++
                        else if (fileFlags[i] == 0)
                            useCount++
                        else if (fileFlags[i] == 1)
                            dontUseCount++
                    }
                    println()
                    println("Use: " + useCount)
                    println("Don't use: " + dontUseCount)
                    println("Undetermined count: " + undeterminedCount)
                    println()
                })
            }
        }

        setJMenuBar(menuBar);
    }

    def setCurrentFileUsage(int usage) {
        println("Set usage of current model to: " + usage)
        fileFlags[currentIndex] = usage;
        usageRadioButtons[usage].setSelected(true)
        dirty = true;
    }

    def changeCurrentFile(int newIndex) {
        if (newIndex != currentIndex) {
            currentIndex = newIndex
            loadMesh(fileNames[currentIndex]);
            usageRadioButtons[fileFlags[currentIndex]].setSelected(true)
        }
    }


    def terminate() {
        new Thread() {
            @Override
            public void run() {
                timer.stop();
                glViewPanel.stopAnimation();
            }
        }.start();

        renderEngine.dispose();
        PhongMaterialV1.destroyProgram();
        TexturedPhongMaterialV1.destroyProgram();

        System.exit(0);
    }

    def initializeViewPanel() {
        camera = new PerspectiveCamera(0.1f, 1000, 45);
        camera.eye.set(0, 20, 30);
        camera.target.set(0, 10, 0);
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
    }

    private void initializeControlPanel() {
        controlPanel = new JPanel();
        LayoutManager controlPanelLayout = createControlPanelLayout();
        controlPanel.setLayout(controlPanelLayout);

        JLabel modelFileLabel = new JLabel("Model File:");
        controlPanel.add(modelFileLabel, "1,1,1,1");

        fileNameComboBox = new JComboBox<String>();
        for (int i = 0; i < fileNames.size(); i++) {
            fileNameComboBox.addItem(fileNames[i]);
        }
        controlPanel.add(fileNameComboBox, "3,1,3,1");
        fileNameComboBox.addActionListener(this);

        JLabel usageLabel = new JLabel("Usage:")
        controlPanel.add(usageLabel, "1,3,1,3")

        JPanel radioButtonPanel = new JPanel();
        radioButtonPanel.setLayout(new FlowLayout())

        usageRadioButtons = new JRadioButton[4]
        usageRadioButtons[0] = new JRadioButton("Use")
        usageRadioButtons[1] = new JRadioButton("Don't use")
        usageRadioButtons[2] = new JRadioButton("Undetermined")
        usageRadioButtons[3] = new JRadioButton("Resize or convert to PNG")

        modelUsageGroup = new ButtonGroup();
        for (int i = 0; i < 4; i++) {
            radioButtonPanel.add(usageRadioButtons[i])
            modelUsageGroup.add(usageRadioButtons[i])
            usageRadioButtons[i].addActionListener(this)
        }

        controlPanel.add(radioButtonPanel, "3,3,3,3")

        getContentPane().add(controlPanel, BorderLayout.SOUTH);
    }

    def LayoutManager createControlPanelLayout() {
        double[][] tableLayoutSizes =
                [
                    [
                        5, TableLayout.MINIMUM, 5, TableLayout.FILL,
                        5,
                    ],
                    [
                        5, TableLayout.MINIMUM,
                        5, TableLayout.MINIMUM,
                        5
                    ]
                ];
        TableLayout tableLayout = new TableLayout(tableLayoutSizes);
        return tableLayout;
    }


    def glViewKeyTyped(KeyEvent e) {
        // NOP
    }

    def glViewKeyPressed(KeyEvent e) {
        // NOP
    }

    def glViewKeyReleased(KeyEvent e) {
        // NOP
    }

    def updateAnimation() {
        // NOP
    }

    @Override
    void actionPerformed(ActionEvent e) {
        for (int i = 0; i < usageRadioButtons.length; i++) {
            if (e.getSource() == usageRadioButtons[i]) {
                setCurrentFileUsage(i)
                return;
            }
        }

        if (e.getSource() == fileNameComboBox) {
            int currentSelectedIndex = fileNameComboBox.getSelectedIndex();
            changeCurrentFile(currentSelectedIndex)
        }
    }

    @Override
    void init(GLAutoDrawable drawable, CameraController controller) {
        GL2 gl = drawable.getGL().getGL2();
        renderEngine = new RenderEngineV1(gl);
        gl.glClearColor(1, 1, 1, 1);

        gl.glEnable(GL2.GL_DEPTH_TEST);

        PhongMaterialV1.createProgram(gl);
        TexturedPhongMaterialV1.createProgram(gl);

        renderEngine.getTexturePool().setCapacity(64);
    }

    @Override
    void draw(GLAutoDrawable drawable, CameraController controller) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        drawCoordinatePlane(gl);

        gl.glPushMatrix();
        gl.glScaled(1, 1, -1);

        renderEngine.pushBindingFrame();
        renderEngine.setVariable("sys_eyePosition", camera.getEye());
        renderEngine.setVariable("sys_lightPosition", lightPosition);
        renderEngine.setVariable("sys_lightAmbient", lightAmbient);
        renderEngine.setVariable("sys_lightDiffuse", lightDiffuse);
        renderEngine.setVariable("sys_lightSpecular", lightSpecular);
        renderEngine.setVariable("sys_lightDirection", lightDirection);
        camera.getViewMatrixInverse(viewInverse);
        renderEngine.setVariable("sys_viewMatrixInverse", viewInverse);

        try {
            if (mesh != null) {
                mesh.draw(renderEngine);
            }
        } catch (Exception e) {
            if (e.getMessage().equals("TGADecoder Compressed True Color images not supported")) {
                System.out.println(e.getMessage());
            } else {
                e.printStackTrace();
            }
            mesh = null
        }

        renderEngine.popBindingFrame()
        renderEngine.garbageCollect()

        gl.glPopMatrix();
    }

    @Override
    void dispose(GLAutoDrawable drawable, CameraController controller) {
        renderEngine.dispose();
        PhongMaterialV1.destroyProgram();
        TexturedPhongMaterialV1.destroyProgram();
    }

    @Override
    void mousePressed(MouseEvent e, CameraController controller) {

    }

    @Override
    void mouseReleased(MouseEvent e, CameraController controller) {

    }

    @Override
    void mouseDragged(MouseEvent e, CameraController controller) {

    }

    @Override
    void mouseMoved(MouseEvent e, CameraController controller) {

    }

    def drawCoordinatePlane(GL2 gl) {
        gl.glLineWidth(2.0f);

        gl.glBegin(GL2.GL_LINES);
        gl.glColor3f(1, 0, 0);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(AXIS_LENGTH, 0, 0);

        gl.glColor3f(0, 1, 1);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(-AXIS_LENGTH, 0, 0);

        gl.glColor3f(0, 1, 0);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(0, AXIS_LENGTH, 0);

        gl.glColor3f(1, 0, 1);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(0, -AXIS_LENGTH, 0);

        gl.glColor3f(0, 0, 1);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(0, 0, AXIS_LENGTH);

        gl.glColor3f(1, 1, 0);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(0, 0, -AXIS_LENGTH);
        gl.glEnd();

        gl.glLineWidth(1.0f);
        gl.glColor3f(0.5f, 0.5f, 0.5f);
        gl.glBegin(GL2.GL_LINES);
        for (int i = -10; i <= 10; i++) {
            gl.glVertex3f((float)(i * CELL_WIDTH), 0.0f, -AXIS_LENGTH);
            gl.glVertex3f((float)(i * CELL_WIDTH), 0.0f, AXIS_LENGTH);
        }

        for (int i = -10; i <= 10; i++) {
            gl.glVertex3f(-AXIS_LENGTH, 0.0f, (float)(i * CELL_WIDTH));
            gl.glVertex3f(AXIS_LENGTH, 0.0f, (float)(i * CELL_WIDTH));
        }
        gl.glEnd();
    }

    def loadMesh(String fileName) {
        if (FilenameUtils.getExtension(fileName).equals("pmd")) {
            PmdModel pmd = PmdModel.load(fileName)
            PmdMeshV1 pmdMesh = PmdToMeshV1Converter.createMesh(pmd)
            pmdMesh.setPmdPose(null)
            pmdMesh.updateMeshWithPose()
            mesh = pmdMesh
        } else if (FilenameUtils.getExtension(fileName).equals("pmx")) {
            PmxModel pmx = PmxModel.load(fileName)
            PmxMeshV1 pmxMesh = PmxToMeshV1Converter.createMesh(pmx)
            pmxMesh.setPmxPose(null)
            pmxMesh.updateMeshWithPose()
            mesh = pmxMesh
        } else {
            mesh = null;
        }
    }

    def save() {
        new File(inputFileName).withWriter("UTF-8") { fout ->
            for (int i = 0; i < fileNames.size(); i++) {
                fout.write("${fileFlags[i]} ${fileNames[i]}\n")
            }
        }
        dirty = false
    }

    def exitProgram() {
        if (dirty) {
            Object[] options = ["Save", "Exit without Saving", "Cancel"] as Object[];
            int response = JOptionPane.showOptionDialog(this,
                    "You have made some changes that you have not made. "
                            + "Are you sure you want to create new data?",
                    "New Data Confirmation",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (response == 0) {
                save()
                terminate();
            } else if (response == 1) {
                terminate();
            }
        } else {
            terminate();
        }
    }
}
