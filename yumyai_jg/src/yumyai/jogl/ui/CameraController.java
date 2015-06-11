package yumyai.jogl.ui;

import java.awt.event.MouseEvent;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.vecmath.Tuple2f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

public abstract class CameraController extends BlankGLController
{
    public static final int NO_MODE = 0;
    public static final int ROTATE_MODE = 1;
    public static final int TRANSLATE_MODE = 2;
    public static final int ZOOM_MODE = 3;
    protected Camera camera;
    protected int top;
    protected int left;
    protected int width;
    protected int height;
    protected final Vector2f lastMousePosition = new Vector2f();
    protected final Vector2f currentMousePosition = new Vector2f();
    protected final Vector2f mouseDelta = new Vector2f();
    protected final Vector3f worldMotion = new Vector3f();
    protected int mode;
    protected int captureFrameNumber = -1;
    protected boolean captureNextFrame = false;
    protected GLSceneDrawer drawer;
    protected ViewsCoordinator coordinator;
    protected int viewId;
    protected static int nFrames = 0;

    public CameraController(Camera camera, GLSceneDrawer drawer)
    {
        this(camera, drawer, null, 0);
    }

    public CameraController(Camera camera, GLSceneDrawer drawer, ViewsCoordinator coordinator, int viewId)
    {
        this.camera = camera;
        this.drawer = drawer;
        this.coordinator = coordinator;
        this.viewId = viewId;
        initializeCameraController();
    }

    protected void initializeCameraController()
    {
        camera.updateFrame();
        mode = NO_MODE;
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
        drawer.init(drawable, this);
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        camera.updateFrame();

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        camera.doProjection(drawable);
        camera.doView(drawable);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        drawer.draw(drawable, this);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        final GL2 gl = drawable.getGL().getGL2();

        // Sometimes we get an invalid width or height.
        // Quick fix: ignore it
        if (width <= 0 || height <= 0)
        {
            return;
        }

        gl.glViewport(0, 0, width, height);
        camera.setAspect(width * 1.0f / height);

        this.left = x;
        this.top = y;
        this.width = width;
        this.height = height;
    }

    public void windowToViewport(Tuple2f p)
    {
        int w = width;
        int h = height;
        p.set((2 * p.x - w) / w, (2 * (h - p.y - 1) - h) / h);
    }

    protected boolean isFlagSet(MouseEvent e, int flag)
    {
        return (e.getModifiersEx() & flag) == flag;
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        lastMousePosition.set(e.getX(), e.getY());
        windowToViewport(lastMousePosition);

        if (!isFlagSet(e, MouseEvent.BUTTON1_DOWN_MASK)
                && !isFlagSet(e, MouseEvent.BUTTON2_DOWN_MASK)
                && isFlagSet(e, MouseEvent.BUTTON3_DOWN_MASK))
        {
            if (isFlagSet(e, MouseEvent.ALT_DOWN_MASK))
            {
                mode = TRANSLATE_MODE;
            }
            else if (isFlagSet(e, MouseEvent.CTRL_DOWN_MASK))
            {
                mode = ZOOM_MODE;
            }
            else if (isFlagSet(e, MouseEvent.SHIFT_DOWN_MASK))
            {
                mode = ROTATE_MODE;
            }
            else
            {
                mode = NO_MODE;
            }
        }
        else
        {
            mode = NO_MODE;
        }

        drawer.mousePressed(e, this);
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        mode = NO_MODE;
        drawer.mouseReleased(e, this);
    }

    protected abstract void processMouseDragged(MouseEvent e);

    @Override
    public void mouseDragged(MouseEvent e)
    {
        currentMousePosition.set(e.getX(), e.getY());
        windowToViewport(currentMousePosition);
        mouseDelta.sub(currentMousePosition, lastMousePosition);

        processMouseDragged(e);
        drawer.mouseDragged(e, this);

        lastMousePosition.set(e.getX(), e.getY());
        windowToViewport(lastMousePosition);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        drawer.mouseMoved(e, this);
    }
    
    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        drawer.dispose(drawable, this);
    }

    public Camera getCamera()
    {
        return camera;
    }

    public GLSceneDrawer getDrawer()
    {
        return drawer;
    }

    public Vector2f getCurrentMousePosition()
    {
        return currentMousePosition;
    }

    public Vector2f getMouseDelta()
    {
        return mouseDelta;
    }

    public Vector2f getLastMousePosition()
    {
        return lastMousePosition;
    }

    public void captureNextFrame()
    {
        this.captureNextFrame = true;
    }

    public void setViewId(int viewId)
    {
        this.viewId = viewId;
    }

    public void setCoordinator(ViewsCoordinator coordinator)
    {
        this.coordinator = coordinator;
    }
    
    public int getScreenWidth()
    {
        return this.width;
    }
    
    public int getScreenHeight()
    {
        return this.height;
    }
}