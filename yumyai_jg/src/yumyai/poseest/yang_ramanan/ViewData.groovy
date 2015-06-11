package yumyai.poseest.yang_ramanan

import layout.TableLayout
import yondoko.math.Util
import yondoko.util.ImageUtil
import yumyai.jogl.Texture2D
import yumyai.jogl.ui.GLFrameWithCamera2d

import javax.imageio.ImageIO
import javax.media.opengl.GL2
import javax.media.opengl.GLAutoDrawable
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.vecmath.Point2f
import java.awt.Image
import java.awt.LayoutManager
import java.awt.Rectangle
import java.awt.event.ActionListener
import java.awt.image.BufferedImage

class ViewData extends GLFrameWithCamera2d implements ActionListener {
    YangRamananData data;
    JComboBox<String> imageComboBox;
    int lastImageIndex = -1
    Texture2D imageTexture = null
    int imageWidth
    int imageHeight

    public static void main(String[] args) {
        if (args.length < 1) {
            println("Usage: java yumyui.poseest.yang_ramanan.ViewData <input-file>")
            System.exit(-1)
        }

        final YangRamananData data = YangRamananData.load(args[0]);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (Exception e) {
            // NOP
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ViewData(data).run();
            }
        });
    }

    public ViewData(YangRamananData data) {
        super((float) (1.0f / 150), 0, 0)
        this.data = data;
    }

    void run() {
        setTitle("View Yang & Ramanan data")
        initExtraControls()

        setSize(1024, 768);
        setVisible(true);
    }

    private void initExtraControls() {
        createImageControls(3)
    }

    private void createImageControls(int row) {
        JLabel imageLabel = new JLabel("Image:");
        controlPanel.add(imageLabel, String.format("1, %d, 1, %d", row, row));

        imageComboBox = new JComboBox<String>();
        for (int i = 0; i < data.items.size(); i++) {
            imageComboBox.addItem(data.items.get(i).imageName);
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

    @Override
    public void init(GLAutoDrawable glad) {
        super.init(glad);
        final GL2 gl = glad.getGL().getGL2()
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
            BufferedImage image = ImageIO.read(new File((String)imageComboBox.getSelectedItem()));
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
            int newWidth = Util.getClosestPowerOfTwo(imageWidth);
            int newHeight = Util.getClosestPowerOfTwo(imageHeight);
            Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
            BufferedImage scaledBufferedImage = ImageUtil.toBufferedImage(scaledImage)
            com.jogamp.opengl.util.awt.ImageUtil.flipImageVertically(scaledBufferedImage);

            imageTexture = new Texture2D(gl, scaledBufferedImage);
            lastImageIndex = imageComboBox.getSelectedIndex();
        }

        if (imageTexture != null) {
            imageTexture.use()

            gl.glPushMatrix();
            gl.glColor3f(1,1,1);
            gl.glEnable(GL2.GL_TEXTURE_2D);

            gl.glTranslated(-imageWidth*1.0/2, -imageHeight*1.0/2, 0);

            gl.glBegin(GL2.GL_POLYGON);

            gl.glTexCoord2f(0, 0);
            gl.glVertex2f(0,0);

            gl.glTexCoord2f(1, 0);
            gl.glVertex2f(imageWidth, 0);

            gl.glTexCoord2f(1, 1);
            gl.glVertex2f(imageWidth, imageHeight);

            gl.glTexCoord2f(0, 1);
            gl.glVertex2f(0, imageHeight);

            gl.glEnd();

            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glPointSize(5.0f);
            gl.glColor3f(1,0,0);
            YangRamananData.Item item = data.items.get(imageComboBox.getSelectedIndex());
            gl.glBegin(GL2.GL_POINTS);
            for (int i = 0; i < item.points.size(); i++) {
                Point2f p = item.points.get(i);
                gl.glVertex2f(p.x, (float)(imageHeight-p.y));
            }
            gl.glEnd();

            gl.glPopMatrix();

            imageTexture.unuse();
        }
    }
}