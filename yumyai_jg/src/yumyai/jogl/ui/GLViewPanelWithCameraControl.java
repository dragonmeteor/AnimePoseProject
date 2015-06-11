package yumyai.jogl.ui;

import javax.media.opengl.GLContext;

public class GLViewPanelWithCameraControl extends GLViewPanel implements ViewsCoordinator {
    private static final long serialVersionUID = 1L;
    protected CameraController cameraController;
    protected PickingController pickingController;
    protected boolean viewsUpdated = false;

    public GLViewPanelWithCameraControl(int initialFrameRate, CameraController cameraController) {
        super(initialFrameRate);
        this.cameraController = cameraController;
        this.pickingController = new PickingController(cameraController);
        addGLController(pickingController);
    }

    public CameraController getCameraController() {
        return cameraController;
    }

    public PickingController getPickingController() {
        return pickingController;
    }

    public void addPickingEventListener(PickingEventListener listener) {
        pickingController.addPickingEventListener(listener);
    }

    public void removePickingEventListener(PickingEventListener listener) {
        pickingController.removePickingEventListener(listener);
    }

    public void addPrioritizedObjectId(int id) {
        pickingController.addPrioritizedObjectId(id);
    }

    public void removePrioritizedObjectId(int id) {
        pickingController.removePrioritizedObjectId(id);
    }

    public void resetUpdatedStatus() {
        viewsUpdated = false;
    }

    public boolean checkAllViewsUpdated() {
        return viewsUpdated;
    }

    public void setViewUpdated(int viewId) {
        viewsUpdated = true;
    }

    public void captureNextFrame() {
        cameraController.captureNextFrame();
    }
}
