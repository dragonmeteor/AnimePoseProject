package yumyai.jogl.ui;

import java.awt.event.MouseEvent;

import javax.media.opengl.GLAutoDrawable;

public class OrthographicCameraController extends CameraController
{
    protected OrthographicCamera orthographicCamera;

    public OrthographicCameraController(OrthographicCamera camera, GLSceneDrawer drawer)
    {
        this(camera, drawer, null, 0);
    }

    public OrthographicCameraController(OrthographicCamera camera, GLSceneDrawer drawer,
            ViewsCoordinator coordinator, int viewId)
    {
        super(camera, drawer, coordinator, viewId);
        orthographicCamera = camera;
    }

    protected void processMouseDragged(MouseEvent e)
    {
        if (mode == TRANSLATE_MODE)
        {
            orthographicCamera.convertMotion(mouseDelta, worldMotion);
            orthographicCamera.translate(worldMotion);
        }
        else if (mode == ZOOM_MODE)
        {
            orthographicCamera.zoom(mouseDelta.y);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        super.display(drawable);

        if (coordinator != null)
        {
            coordinator.setViewUpdated(viewId);
        }
    }

    public OrthographicCamera getOrthographicCamera()
    {
        return orthographicCamera;
    }
}
