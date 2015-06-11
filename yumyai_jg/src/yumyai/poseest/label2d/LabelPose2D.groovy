package yumyai.poseest.label2d

import groovy.swing.SwingBuilder
import layout.TableLayout
import yondoko.math.Util
import yondoko.util.ImageUtil
import yumyai.jogl.Texture2D
import yumyai.jogl.ui.GLFrameWithCamera2d
import yumyai.poseest.mmd.Settings

import javax.imageio.ImageIO
import javax.media.opengl.GL2
import javax.media.opengl.GLAutoDrawable
import javax.swing.*
import javax.vecmath.Color3f
import javax.vecmath.Point2f
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage

class LabelPose2D extends GLFrameWithCamera2d implements ActionListener {
    ArrayList<PoseLabeling2D> labelings
    JComboBox<String> imageComboBox
    int lastImageIndex = -1
    Texture2D imageTexture = null
    int imageWidth, imageHeight
    boolean dirty = false
    String fileName
    JPanel jointPanel
    ButtonGroup jointButtonGroup
    HashMap<String, JRadioButton> jointButtons = new HashMap<String, JRadioButton>()

    public static void main(String[] args) {
        if (args.length < 1) {
            println("Usage: java yumyai.poseest.label2d.LabelPose2D <labeling-file>")
            System.exit(0);
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (Exception e) {
            // NOP
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LabelPose2D(args[0]).run();
            }
        });
    }

    public LabelPose2D(String fileName) {
        super((float) (1.0f / 150), 0, 0)
        this.fileName = fileName;
    }

    void run() {
        labelings = PoseLabeling2D.load(fileName)

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitProgram()
            }
        })

        setTitle("Label 2D Poses")
        initExtraControls()
        initializeMenuBar()
        initializeBonePanel()

        setSize(1024, 768);
        setVisible(true);
    }

    void initExtraControls() {
        initImageControls(3)
    }

    void initImageControls(int row) {
        JLabel imageLabel = new JLabel("Image:");
        controlPanel.add(imageLabel, String.format("1, %d, 1, %d", row, row));

        imageComboBox = new JComboBox<String>();
        for (int i = 0; i < labelings.size(); i++) {
            imageComboBox.addItem(labelings.get(i).fileName);
        }
        controlPanel.add(imageComboBox, String.format("3, %d, 7, %d", row, row));
        imageComboBox.setSelectedIndex(0);
    }

    void initializeBonePanel() {
        jointPanel = new JPanel()
        JScrollPane scrollPane = new JScrollPane()
        scrollPane.setViewportView(jointPanel);
        getContentPane().add(scrollPane, BorderLayout.EAST)

        jointButtonGroup = new ButtonGroup()
        jointPanel.setLayout(new BoxLayout(jointPanel, BoxLayout.PAGE_AXIS))
        for (String name : Settings.bonesToLabel) {
            JRadioButton radioButton = new JRadioButton(Settings.displayBoneEnglishNames[name]);
            jointPanel.add(radioButton)
            jointButtonGroup.add(radioButton)
            jointButtons.put(name, radioButton)
        }
        jointButtons[Settings.bonesToLabel[0]].setSelected(true)
    }

    @Override
    protected LayoutManager createControlPanelLayout() {
        double[][] tableLayoutSizes =
                [
                        [
                                5, TableLayout.MINIMUM, 5, TableLayout.FILL,
                                5, TableLayout.MINIMUM, 5, TableLayout.MINIMUM,
                                5
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
            menu(text: 'Value', mnemonic: 'V') {
                menuItem(text: "Fit image to screen", accelerator: KeyStroke.getKeyStroke('F'), actionPerformed: {
                    fitImageToScreen()
                })
            }
            /*
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
            */
            menu(text: 'Navigate', mnemonic: 'N') {
                menuItem(text: "Go to next file", accelerator: KeyStroke.getKeyStroke("S"), actionPerformed: {
                    if (getCurrentImageIndex() < labelings.size() - 1) {
                        changeCurrentIndex(getCurrentImageIndex() + 1)
                        //fitImageToScreen()
                    }
                })
                menuItem(text: "Go to previous file", accelerator: KeyStroke.getKeyStroke("W"), actionPerformed: {
                    if (getCurrentImageIndex() > 0) {
                        changeCurrentIndex(getCurrentImageIndex() - 1)
                        //fitImageToScreen()
                    }
                })
                separator()
                menuItem(text: "Go to next bone", accelerator: KeyStroke.getKeyStroke("A"), actionPerformed: {
                    setCurrentBoneIndex(getCurrentBoneIndex() + 1)
                })
                menuItem(text: "Go to previous bone", accelerator: KeyStroke.getKeyStroke("Q"), actionPerformed: {
                    setCurrentBoneIndex(getCurrentBoneIndex() - 1)
                })
                /*
                menuItem(text: "Go to first 'Unprocessed' file", accelerator: KeyStroke.getKeyStroke("D"), actionPerformed: {
                    int first = -1;
                    for (int i = 0; i < data.size(); i++) {
                        if (!data[i].processed) {
                            first = i
                            break
                        }
                    }
                    if (first != -1) {
                        changeCurrentIndex(first)
                        fileNameComboBox.setSelectedIndex(first)
                    } else {
                        println("No unprocessed files.")
                    }
                })
                */
            }
            /*
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
            */
        }

        setJMenuBar(menuBar);
    }

    @Override
    public void display(GLAutoDrawable glad) {
        final GL2 gl = glad.getGL().getGL2();
        super.display(glad);

        boolean update = lastImageIndex != imageComboBox.getSelectedIndex()
        if (update) {
            if (imageTexture != null) {
                imageTexture.dispose()
            }
            BufferedImage image = ImageIO.read(new File((String) imageComboBox.getSelectedItem()));
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
            int newWidth = Util.getClosestPowerOfTwo(imageWidth);
            int newHeight = Util.getClosestPowerOfTwo(imageHeight);
            Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
            BufferedImage scaledBufferedImage = ImageUtil.toBufferedImage(scaledImage)
            com.jogamp.opengl.util.awt.ImageUtil.flipImageVertically(scaledBufferedImage);

            imageTexture = new Texture2D(gl, scaledBufferedImage);
            lastImageIndex = imageComboBox.getSelectedIndex();

            fitImageToScreen()
        }

        setupCamera(gl);

        if (imageTexture != null) {
            imageTexture.use()

            gl.glPushMatrix();
            gl.glColor3f(1, 1, 1);
            gl.glEnable(GL2.GL_TEXTURE_2D);

            gl.glTranslated(-imageWidth * 1.0 / 2, -imageHeight * 1.0 / 2, 0);

            gl.glBegin(GL2.GL_POLYGON);

            gl.glTexCoord2f(0, 0);
            gl.glVertex2f(0, 0);

            gl.glTexCoord2f(1, 0);
            gl.glVertex2f(imageWidth, 0);

            gl.glTexCoord2f(1, 1);
            gl.glVertex2f(imageWidth, imageHeight);

            gl.glTexCoord2f(0, 1);
            gl.glVertex2f(0, imageHeight);

            gl.glEnd();

            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glPointSize(5.0f);
            gl.glColor3f(1, 0, 0);

            gl.glBegin(GL2.GL_POINTS);
            for (String jointName : labelings[currentImageIndex].points.keySet()) {
                drawPoint(gl, jointName);
            }
            gl.glEnd();

            gl.glBegin(GL2.GL_LINES);
            for (String jointName : labelings[currentImageIndex].points.keySet()) {
                String japaneseName = Settings.displayBoneJapaneseNames[jointName];
                String parentName = Settings.bonesToLabelParent[japaneseName]
                if (parentName != null) {
                    String parentEnglishName = Settings.displayBoneEnglishNames[parentName]
                    if (labelings[getCurrentImageIndex()].points.containsKey(parentEnglishName)) {
                        drawPoint(gl, jointName);
                        drawPoint(gl, parentEnglishName);
                    }
                }
            }
            gl.glEnd();

            gl.glPopMatrix();

            imageTexture.unuse();
        }
    }

    void drawPoint(GL2 gl, String jointName) {
        String japaneseName = Settings.displayBoneJapaneseNames[jointName]
        Color3f color = Settings.boneColors[japaneseName]
        gl.glColor3f(color.x, color.y, color.z);
        Point2f p = labelings[getCurrentImageIndex()].points[jointName]
        gl.glVertex2f(p.x, (imageHeight - p.y) as float);
    }

    void save() {
        PoseLabeling2D.save(fileName, labelings)
        dirty = false
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

    void terminate() {
        new Thread() {
            @Override
            public void run() {
                glViewPanel.stopAnimation();
            }
        }.start();
        System.exit(0);
    }

    void fitImageToScreen() {
        if (imageTexture != null) {
            int panelWidth = glViewPanel.getWidth()
            int panelHeight = glViewPanel.getHeight()
            float aspect = panelWidth * 1.0f / panelHeight;

            if (aspect > 1) {
                float widthFactor = (1.8f / imageWidth * aspect) as float;
                float heightFactor = (1.8f / imageHeight) as float;
                camera2dController.setBaseScalingFactor(Math.min(widthFactor, heightFactor))
            } else {
                float widthFactor = (1.8f / imageWidth) as float;
                float heightFactor = (1.8f / imageHeight / aspect) as float;
                camera2dController.setBaseScalingFactor(Math.min(widthFactor, heightFactor))
            }

            zoomController.setValue(0)
        }
    }

    def changeCurrentIndex(int newIndex) {
        if (newIndex != imageComboBox.getSelectedIndex()) {
            imageComboBox.setSelectedIndex(newIndex)
        }
    }

    def getCurrentImageIndex() {
        return imageComboBox.getSelectedIndex()
    }

    int getCurrentBoneIndex() {
        for (int i = 0; i < Settings.bonesToLabel.size(); i++) {
            String name = Settings.bonesToLabel[i]
            if (jointButtons[name].isSelected()) {
                return i;
            }
        }
    }

    void setCurrentBoneIndex(int newIndex) {
        if (newIndex >= 0 && newIndex < jointButtons.size())
            jointButtons[Settings.bonesToLabel[newIndex]].setSelected(true)
    }

    private boolean dragging = false;

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            Point2f p = getImageSpacePosition(e.getX(), e.getY());
            setJointPosition(p);
            dragging = true;
        } else {
            super.mousePressed(e);
        }
    }

    void setJointPosition(Point2f p) {
        int currentBoneIndex = getCurrentBoneIndex()
        int currentImageIndex = getCurrentImageIndex()
        String name = Settings.bonesToLabel[currentBoneIndex]
        String englishName = Settings.displayBoneEnglishNames[name];
        labelings[currentImageIndex].points[englishName] = p;
        if (name.equals("上半身")) {
            labelings[currentImageIndex].points[Settings.displayBoneEnglishNames["下半身"]] = p;
        }
        dirty = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        dragging = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragging) {
            Point2f p = getImageSpacePosition(e.getX(), e.getY());
            setJointPosition(p);
            dragging = true;
        } else {
            super.mouseDragged(e);
        }
    }

    Point2f getImageSpacePosition(int ix, int iy) {
        int panelWidth = glViewPanel.getWidth()
        int panelHeight = glViewPanel.getHeight()
        float x = (ix * 1.0f / panelWidth - 0.5f) * 2 as float;
        float y = (iy * 1.0f / panelHeight - 0.5f) * 2 as float;
        float aspect = panelWidth * 1.0f / panelHeight;
        if (aspect > 1) {
            x *= aspect;
        } else {
            y /= aspect;
        }
        float scalingFactor = camera2dController.getScalingFactor()
        x /= scalingFactor;
        y /= scalingFactor;
        x += imageWidth / 2.0f + camera2dController.getCenterX();
        y += imageHeight / 2.0f - camera2dController.getCenterY();
        return new Point2f(x, y);
    }
}
