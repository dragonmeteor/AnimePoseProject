package yumyai.jogl.ui;

import java.awt.event.MouseEvent;

import javax.media.opengl.GLAutoDrawable;

public class PerspectiveCameraController extends CameraController
{
    private PerspectiveCamera perspectiveCamera;

    public PerspectiveCameraController(PerspectiveCamera camera, GLSceneDrawer drawer)
    {
        this(camera, drawer, null, 0);
    }

    public PerspectiveCameraController(PerspectiveCamera camera, GLSceneDrawer drawer, ViewsCoordinator coordinator, int viewId)
    {
        super(camera, drawer, coordinator, viewId);
        this.perspectiveCamera = camera;
    }

    public void display(GLAutoDrawable drawable)
    {
        super.display(drawable);
        if (coordinator != null)
        {
            coordinator.setViewUpdated(viewId);
        }
    }

    protected void processMouseDragged(MouseEvent e)
    {
        if (mode == TRANSLATE_MODE)
        {
            perspectiveCamera.convertMotion(mouseDelta, worldMotion);
            perspectiveCamera.translate(worldMotion);
        }
        else if (mode == ZOOM_MODE)
        {
            perspectiveCamera.zoom(mouseDelta.y);
        }
        else if (mode == ROTATE_MODE)
        {
            perspectiveCamera.orbit(mouseDelta);
        }
    }
}
