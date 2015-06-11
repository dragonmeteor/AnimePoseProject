package yumyai.poseest.mmd.character

import groovy.swing.SwingBuilder
import layout.TableLayout
import org.apache.commons.io.FilenameUtils
import yondoko.struct.Aabb3f
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
import yumyai.poseest.mmd.MmdCharInfo
import yumyai.poseest.mmd.PoseEstUtil
import yumyai.poseest.mmd.Settings

import javax.media.opengl.GL2
import javax.media.opengl.GLAutoDrawable
import javax.swing.ButtonGroup
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.vecmath.Color3f
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

class DetermineExtraBones extends JFrame implements GLSceneDrawer, ActionListener {
    final static float AXIS_LENGTH = 50.0F;
    final static float CELL_WIDTH = AXIS_LENGTH / 10.0F;

    ArrayList<MmdCharInfo> data;
    String outputFileName

    PerspectiveCamera camera
    javax.swing.Timer timer
    PerspectiveCameraController cameraController
    GLViewPanel glViewPanel
    RenderEngineV1 renderEngine

    Vector3f lightAmbient = new Vector3f(0.7f, 0.7f, 0.7f)
    Vector3f lightDiffuse = new Vector3f(0.5f, 0.5f, 0.5f)
    Vector3f lightSpecular = new Vector3f(1.0f, 1.0f, 1.0f)
    Vector3f lightDirection = new Vector3f(0, -1, -1)
    Matrix4f viewInverse = new Matrix4f()

    JPanel controlPanel
    JComboBox<String> fileNameComboBox
    JRadioButton[] extraBoneRadioButtons
    JCheckBox processedCheckBox

    int currentIndex = -1
    MeshV1 mesh = null
    boolean dirty = false
    int currentMaterialIndex = 0
    boolean showExtraBonePoints = true
    boolean showAllVertices = false
    boolean showWireframe = false

    ArrayList<Aabb3f> charViewAabbs = PoseEstUtil.createCharacterViewAabbs()

    public static void main(String[] args) {
        if (args.length < 1) {
            println("Usage: java yumyai.poseest.app.DetermineExtraJoints <file>")
            System.exit(-1)
        }

        final ArrayList<MmdCharInfo> data = MmdCharInfo.load(args[0])

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new DetermineExtraBones(data, args[0]).run();
            }
        });
    }


    public DetermineExtraBones(ArrayList<MmdCharInfo> data,
                               String outputFileName) {
        super("Determine Extra Joint Info")
        this.data = data;
        this.outputFileName = outputFileName

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (Exception e) {
            // NOP
        }
    }

    void run() {
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

        changeCurrentFile(0)
        resetCamera()

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

    def initializeViewPanel() {
        camera = new PerspectiveCamera(0.1f, 1000.0f, Settings.DEFAULT_FOVY);
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
        for (int i = 0; i < data.size(); i++) {
            fileNameComboBox.addItem(data[i].fileName);
        }
        controlPanel.add(fileNameComboBox, "3,1,3,1");
        fileNameComboBox.addActionListener(this);

        JPanel radioButtonPanel = new JPanel();
        radioButtonPanel.setLayout(new FlowLayout())

        extraBoneRadioButtons = new JRadioButton[Settings.extraBoneNames.size()]
        for (int i = 0; i < Settings.extraBoneNames.size(); i++) {
            extraBoneRadioButtons[i] = new JRadioButton(Settings.extraBoneNames[i])
        }
        ButtonGroup extraBoneRadioButtonGroup = new ButtonGroup()
        for (int i = 0; i < extraBoneRadioButtons.length; i++) {
            radioButtonPanel.add(extraBoneRadioButtons[i])
            extraBoneRadioButtonGroup.add(extraBoneRadioButtons[i])
            extraBoneRadioButtons[i].addActionListener(this)
        }

        controlPanel.add(radioButtonPanel, "1,5,3,5")

        JLabel processedLabel = new JLabel("Processed:");
        controlPanel.add(processedLabel, "1,3,1,3");
        processedCheckBox = new JCheckBox();
        controlPanel.add(processedCheckBox, "3,3,3,3");

        getContentPane().add(controlPanel, BorderLayout.SOUTH);

        setCurrentExtraBone(0)
        changeCurrentFile(0)
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
        for (int i = 0; i < extraBoneRadioButtons.length; i++) {
            if (e.getSource() == extraBoneRadioButtons[i]) {
                setCurrentExtraBone(i)
                return
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
        gl.glEnable(GL2.GL_DEPTH_TEST)
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        drawCoordinatePlane(gl);

        gl.glPushMatrix();
        gl.glScaled(1, 1, -1);

        renderEngine.pushBindingFrame();
        renderEngine.setVariable("sys_eyePosition", camera.getEye());
        renderEngine.setVariable("sys_lightAmbient", lightAmbient);
        renderEngine.setVariable("sys_lightDiffuse", lightDiffuse);
        renderEngine.setVariable("sys_lightSpecular", lightSpecular);
        renderEngine.setVariable("sys_lightDirection", lightDirection);
        camera.getViewMatrixInverse(viewInverse);
        renderEngine.setVariable("sys_viewMatrixInverse", viewInverse);

        try {
            if (mesh != null) {
                int materialCount = mesh.getMaterialCount()
                if (currentMaterialIndex > materialCount)
                    currentMaterialIndex = 0
                if (currentMaterialIndex == 0) {
                    mesh.draw(renderEngine);
                } else {
                    mesh.drawWithMaterialIndex(renderEngine, currentMaterialIndex - 1);
                }

                if (showWireframe) {
                    gl.glEnable(GL2.GL_DEPTH_TEST)
                    gl.glColor3f(0,0,0)
                    PoseEstUtil.drawWireframe(gl, mesh, currentMaterialIndex)
                }

                if (showAllVertices) {
                    gl.glDisable(GL2.GL_DEPTH_TEST)
                    gl.glPointSize(3)
                    gl.glColor3f(1, 1, 0)
                    PoseEstUtil.drawAllVertices(gl, mesh)
                }

                if (showExtraBonePoints) {
                    gl.glDisable(GL2.GL_DEPTH_TEST)
                    gl.glPointSize(9)
                    String fileName = data[currentIndex].fileName
                    HashMap<String, Integer> boneInfo = data[currentIndex].extraBoneVertexIndex
                    gl.glBegin(GL2.GL_POINTS)
                    for (String boneName : boneInfo.keySet()) {
                        Color3f color = Settings.extraBoneColor[boneName]
                        //println(boneName + " " + boneInfo[boneName])
                        gl.glColor3f(color.x, color.y, color.z)
                        if (mesh instanceof PmdMeshV1) {
                            int vertexIndex = boneInfo[boneName];
                            if (vertexIndex < 0)
                                continue
                            float x = mesh.positions.get(3 * vertexIndex + 0)
                            float y = mesh.positions.get(3 * vertexIndex + 1)
                            float z = mesh.positions.get(3 * vertexIndex + 2)
                            gl.glVertex3f(x, y, z)
                        } else if (mesh instanceof PmxMeshV1) {
                            int vertexIndex = boneInfo[boneName];
                            if (vertexIndex < 0)
                                continue
                            float x = mesh.positions.get(3 * vertexIndex + 0)
                            float y = mesh.positions.get(3 * vertexIndex + 1)
                            float z = mesh.positions.get(3 * vertexIndex + 2)
                            gl.glVertex3f(x, y, z)
                        }
                    }
                    gl.glEnd()
                }
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
    }

    @Override
    void mousePressed(MouseEvent e, CameraController controller) {
        if (e.button == MouseEvent.BUTTON1) {
            if (mesh != null) {
                int x = e.getX();
                int y = e.getY();
                int index = PoseEstUtil.findClickedVertex(x, y, camera,
                        cameraController.getScreenWidth(),
                        cameraController.getScreenHeight(),
                        mesh, currentMaterialIndex)
                //println("clicked index = ${index}")
                if (index >= 0) {
                    int currentExtraBoneIndex = getCurrentExtraBoneIndex()
                    data[currentIndex].extraBoneVertexIndex[Settings.extraBoneNames[currentExtraBoneIndex]] = index
                    glViewPanel.repaint()
                }
            }
        }
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

    private void initializeMenuBar() {
        def builder = new SwingBuilder()
        def menuBar = builder.menuBar() {
            menu(text: 'File', mnemonic: 'f') {
                menuItem(text: "Save", mnemonic: 's', accelerator: shortcut('ctrl S'), actionPerformed: {
                    save()
                    println("Saved.")
                })
                menuItem(text: "Exit", mnemonic: 'x', accelerator: shortcut('ctrl X'), actionPerformed: {
                    exitProgram()
                })
            }
            menu(text: 'Data', mnemonic: 'B') {
                menuItem(text: "Toggle processed flag", accelerator: KeyStroke.getKeyStroke('Z'), actionPerformed: {
                    toggleProcessedFlag()
                })
                separator()
                for (int i = 0; i < Settings.extraBoneNames.size(); i++) {
                    { index ->
                        String boneName = Settings.extraBoneNames[index]
                        menuItem(text: "Switch attention to '" + boneName + "' bone",
                                accelerator: KeyStroke.getKeyStroke("${index + 1}"),
                                actionPerformed: {
                                    setCurrentExtraBone(index)
                                })
                    }(i)
                }
            }
            menu(text: 'Navigate', mnemonic: 'N') {
                menuItem(text: "Go to next file", accelerator: KeyStroke.getKeyStroke("A"), actionPerformed: {
                    if (currentIndex < data.size()-1) {
                        changeCurrentFile(currentIndex+1)
                    }
                })
                menuItem(text: "Go to previous file", accelerator: KeyStroke.getKeyStroke("Q"), actionPerformed: {
                    if (currentIndex > 0) {
                        changeCurrentFile(currentIndex-1)
                    }
                })
                menuItem(text: "Go to first 'Unprocessed' file", accelerator: KeyStroke.getKeyStroke("D"), actionPerformed: {
                    int first = -1;
                    for (int i = 0; i < data.size(); i++) {
                        if (!data[i].processed) {
                            first = i
                            break
                        }
                    }
                    if (first != -1) {
                        changeCurrentFile(first)
                        fileNameComboBox.setSelectedIndex(first)
                    } else {
                        println("No unprocessed files.")
                    }
                })
            }
            menu(text: 'View', mnemonic: 'V') {
                menuItem(text: "Go to next material", accelerator: KeyStroke.getKeyStroke('S'), actionPerformed: {
                    currentMaterialIndex = (currentMaterialIndex + 1) % (mesh.getMaterialCount() + 1)
                })
                menuItem(text: "Go to previous material", accelerator: KeyStroke.getKeyStroke('W'), actionPerformed: {
                    currentMaterialIndex = (currentMaterialIndex - 1) % (mesh.getMaterialCount() + 1)
                    if (currentMaterialIndex < 0)
                        currentMaterialIndex += mesh.getMaterialCount() + 1
                })
                menuItem(text: "Show all materials", accelerator: KeyStroke.getKeyStroke('X'), actionPerformed: {
                    currentMaterialIndex = 0
                })
                separator()
                menuItem(text: "Reset camera", accelerator: KeyStroke.getKeyStroke('ctrl R'), actionPerformed: {
                    resetCamera()
                })
                for (int i = 0; i < Settings.extraBoneNames.size(); i++) {
                    { index ->
                        menuItem(text: "Set camera to " + Settings.extraBoneNames[index],
                                accelerator: shortcut("ctrl " + (index + 1)),
                                actionPerformed: {
                                    PoseEstUtil.setCameraToViewExtraBone(Settings.extraBoneNames[index], mesh, camera)
                                })
                    }(i)
                }
                separator()
                menuItem(text: "Toggle showing extra bone points", accelerator: shortcut('ctrl B'),
                        actionPerformed: {
                            showExtraBonePoints = !showExtraBonePoints
                        })
                menuItem(text: "Toggle showing all vertices", accelerator: shortcut('ctrl V'),
                        actionPerformed: {
                            showAllVertices = !showAllVertices
                        })
                menuItem(text: "Toggle display wireframe", accelerator: shortcut('ctrl W'),
                        actionPerformed: {
                            showWireframe = !showWireframe
                        })
            }
            menu(text: 'Info', mnemonic: 'I') {
                menuItem(text: "Print statistics", accelerator: shortcut("ctrl Z"), actionPerformed: {
                    int completeCount = 0;
                    int incompleteCount = 0;
                    for (int i = 0; i < data.size(); i++) {
                        if (isInfoProcessed(i))
                            completeCount++;
                        else
                            incompleteCount++;
                    }
                    println()
                    println("Complete: " + completeCount)
                    println("Incomplete: " + incompleteCount)
                    println()

                })
            }
        }

        setJMenuBar(menuBar);
    }

    void setCurrentExtraBone(int index) {
        println("Set current extras bone to " + Settings.extraBoneNames[index])
        extraBoneRadioButtons[index].setSelected(true)
    }

    int getCurrentExtraBoneIndex() {
        for (int i = 0; i < extraBoneRadioButtons.length; i++) {
            if (extraBoneRadioButtons[i].isSelected()) {
                return i
            }
        }
    }

    void resetCamera() {
        Aabb3f aabb = charViewAabbs[0]
        PoseEstUtil.makeCameraLookAtCenterFromPositiveZ(aabb, camera)
    }

    boolean isInfoProcessed(int index) {
        boolean complete = data[index].processed;
        return complete;
    }

    def changeCurrentFile(int newIndex) {
        if (newIndex != currentIndex) {
            currentIndex = newIndex
            loadMesh(data[currentIndex].fileName);
            PoseEstUtil.computeCharacterViewAabbs(mesh, null, charViewAabbs)
            processedCheckBox.setSelected(data[currentIndex].processed)
            fileNameComboBox.setSelectedIndex(currentIndex);
            //usageRadioButtons[fileFlags[currentIndex]].setSelected(true)
        }
    }

    def toggleProcessedFlag() {
        data[currentIndex].processed = !data[currentIndex].processed
        processedCheckBox.setSelected(data[currentIndex].processed)
        dirty = true
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
        MmdCharInfo.save(outputFileName, data)
        dirty = false
    }

    void terminate() {
        new Thread() {
            @Override
            public void run() {
                timer.stop();
                glViewPanel.stopAnimation();
            }
        }.start();
        renderEngine.dispose();
        System.exit(0);
    }


    void exitProgram() {
        if (dirty) {
            Object[] options = ["Save", "Exit without Saving", "Cancel"] as Object[];
            int response = JOptionPane.showOptionDialog(this,
                    "You have made some 1nges that you have not made. "
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
            gl.glVertex3f((float) (i * CELL_WIDTH), 0.0f, -AXIS_LENGTH);
            gl.glVertex3f((float) (i * CELL_WIDTH), 0.0f, AXIS_LENGTH);
        }

        for (int i = -10; i <= 10; i++) {
            gl.glVertex3f(-AXIS_LENGTH, 0.0f, (float) (i * CELL_WIDTH));
            gl.glVertex3f(AXIS_LENGTH, 0.0f, (float) (i * CELL_WIDTH));
        }
        gl.glEnd();
    }
}
