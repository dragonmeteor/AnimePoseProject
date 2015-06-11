package yumyai.poseest.label2d

import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder
import layout.TableLayout
import org.apache.commons.lang3.tuple.Pair
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

class ViewPoseLabeling2D extends GLFrameWithCamera2d implements ActionListener {
    String labelingFileName
    String configFileName
    ArrayList<PoseLabeling2D> labelings
    Pose2DConfig config
    JComboBox<String> imageComboBox
    int lastImageIndex = -1
    Texture2D imageTexture = null
    int imageWidth, imageHeight
    boolean dirty = false
    JPanel jointPanel
    ButtonGroup jointButtonGroup
    HashMap<String, JRadioButton> jointButtons = new HashMap<String, JRadioButton>()

    public static void main(String[] args) {
        if (args.length < 1) {
            println("Usage: java yumyai.poseest.label2d.ViewPoseLabeling2D <args-file>")
            System.exit(0);
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (Exception e) {
            // NOP
        }

        def argsObj = new JsonSlurper().parse(new FileReader(args[0]))

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ViewPoseLabeling2D(argsObj["labeling_2d_file"], argsObj["pose_2d_config_file"]).run();
            }
        });
    }

    public ViewPoseLabeling2D(String labelingFileName, String configFileName) {
        super((float) (1.0f / 150), 0, 0)
        this.labelingFileName = labelingFileName;
        this.configFileName = configFileName;
    }

    void run() {
        labelings = PoseLabeling2D.load(labelingFileName)
        config = Pose2DConfig.load(configFileName)

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
        String lastItem = "";
        int countSinceLast = 0;
        for (int i = 0; i < labelings.size(); i++) {
            if (lastItem != labelings.get(i).fileName) {
                imageComboBox.addItem(String.format("[%08d] ",i) + labelings.get(i).fileName +
                    String.format(" %s", labelings.get(i).extraInfo));
                countSinceLast = 1;
            } else {
                imageComboBox.addItem(String.format("[%08d] ",i) + labelings.get(i).fileName +
                        String.format(" (%d)", countSinceLast+1) +
                        String.format(" %s", labelings.get(i).extraInfo));
                countSinceLast += 1;
            }
            lastItem = labelings.get(i).fileName
        }
        controlPanel.add(imageComboBox, String.format("3, %d, 7, %d", row, row));
        imageComboBox.setSelectedIndex(0);
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
                menuItem(text: "Exit", mnemonic: 'x', accelerator: shortcut('ctrl X'), actionPerformed: {
                    exitProgram()
                })
            }
            menu(text: 'Value', mnemonic: 'V') {
                menuItem(text: "Fit image to screen", accelerator: KeyStroke.getKeyStroke('F'), actionPerformed: {
                    fitImageToScreen()
                })
            }
            menu(text: 'Navigate', mnemonic: 'N') {
                menuItem(text: "Go to next file", accelerator: KeyStroke.getKeyStroke("S"), actionPerformed: {
                    if (getCurrentImageIndex() < labelings.size() - 1) {
                        changeCurrentIndex(getCurrentImageIndex() + 1)
                    }
                })
                menuItem(text: "Go to previous file", accelerator: KeyStroke.getKeyStroke("W"), actionPerformed: {
                    if (getCurrentImageIndex() > 0) {
                        changeCurrentIndex(getCurrentImageIndex() - 1)
                    }
                })
            }
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
            BufferedImage image = ImageIO.read(new File((String) labelings.get(imageComboBox.getSelectedIndex()).fileName));
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
            gl.glPointSize(7.0f);
            gl.glLineWidth(3.0f);
            gl.glColor3f(1, 0, 0);

            gl.glBegin(GL2.GL_POINTS);
            /*
            for (String jointName : labelings[currentImageIndex].points.keySet()) {
                drawPoint(gl, jointName);
            }
            */
            for (String jointName : config.boneNames) {
                drawPoint(gl, jointName);
            }
            gl.glEnd();

            gl.glBegin(GL2.GL_LINES);
            for (int i = 0; i < config.edges.size(); i++) {
                Pair<String, String> edge = config.edges[i];
                String parentName = edge.getLeft();
                String childName = edge.getRight();
                if (labelings[currentImageIndex].points.containsKey(parentName) &&
                        labelings[currentImageIndex].points.containsKey(childName)) {
                    if (labelings[getCurrentImageIndex()].points[parentName] != null &&
                            labelings[getCurrentImageIndex()].points[childName] != null) {
                        drawPoint(gl, parentName);
                        drawPoint(gl, childName);
                    }
                }
            }
            gl.glEnd();

            gl.glPopMatrix();

            imageTexture.unuse();
        }
    }

    void drawPoint(GL2 gl, String boneName) {
        Color3f color = config.boneColors[boneName]
        gl.glColor3f(color.x, color.y, color.z);
        Point2f p = labelings[getCurrentImageIndex()].points[boneName]
        if (p != null)
            gl.glVertex2f(p.x, (imageHeight - p.y) as float);
    }

    void exitProgram() {
        terminate();
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
}
