package yumyai.jogl.ui;

import com.jogamp.newt.event.InputEvent;
import yumyai.ui.JSpinnerSlider;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.*;
import java.awt.event.MouseEvent;

public class GLFrameWithCamera2d extends GLFrameWithControlPanel {
    protected JSpinnerSlider zoomController;
    protected Camera2dController camera2dController;
    protected int cameraTranslateMouseButton = MouseEvent.BUTTON3;
    protected int cameraTranslateMouseModifiers = 0;

    public GLFrameWithCamera2d(float initialScalingFactor,
                               float initialCenterX, float initialCenterY) {
        super();
        camera2dController = new Camera2dController(initialScalingFactor, initialCenterX, initialCenterY);
    }

    protected void createZoomController() {
        JLabel zoomLabel = new JLabel("Zoom:");
        controlPanel.add(zoomLabel, "1, 1, 1, 1");
        zoomController = new JSpinnerSlider(-6, 6, 100, 0);
        controlPanel.add(zoomController, "3, 1, 7, 1");
    }

    @Override
    protected void initControlPanel() {
        super.initControlPanel();
        createZoomController();
    }

    public void seCameraTranslatetMouseButton(int button) {
        if (button != MouseEvent.BUTTON1 && button != MouseEvent.BUTTON2
                && button != MouseEvent.BUTTON3) {
            throw new RuntimeException("invalid mouse button specified");
        } else {
            this.cameraTranslateMouseButton = button;
        }
    }

    public void setCameraTranslateMouseModifiers(int modifiers) {
        int allValidBits = (InputEvent.ALT_MASK | InputEvent.CTRL_MASK |
                InputEvent.SHIFT_MASK | InputEvent.META_MASK);
        int allInvalidBits = ~allValidBits;
        if ((modifiers & allInvalidBits) != 0) {
            throw new RuntimeException("invalid mouse modifiers specified");
        } else {
            this.cameraTranslateMouseModifiers = modifiers;
        }
    }

    @Override
    public void display(GLAutoDrawable glad) {
        super.display(glad);

        final GL2 gl = glad.getGL().getGL2();

        setupCamera(gl);
    }

    public void setupCamera(GL2 gl) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        camera2dController.doProjection(gl);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        camera2dController.updateScalingFactor((float) zoomController.getValue());
        camera2dController.doModelView(gl);
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        final GL2 gl = glad.getGL().getGL2();

        camera2dController.reshape(x, y, width, height);
    }

    private boolean allBitsAreOne(int x) {
        return x == -1;
    }

    private boolean checkAllCameraTranslationModifiersBitsAreOne(int modifiers) {
        return allBitsAreOne(modifiers | (~cameraTranslateMouseModifiers));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == cameraTranslateMouseButton
                && checkAllCameraTranslationModifiersBitsAreOne(e.getModifiers())) {
            camera2dController.mousePressed(e.getX(), e.getY());
        } else {
            camera2dController.mouseReleased();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        camera2dController.mouseReleased();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        camera2dController.updateScalingFactor((float) zoomController.getValue());
        camera2dController.mouseDragged(e.getX(), e.getY());
    }
}
