package yumyai.jogl.ui;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.swing.event.EventListenerList;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.jogamp.opengl.util.GLBuffers;

public class PickingController implements GLController
{
    private final int SELECT_BUFFER_SIZE = 65536;
    protected CameraController cameraController;
    protected final EventListenerList listenerList = new EventListenerList();
    private final int[] viewport = new int[4];
    private final double[] modelView = new double[16];
    private final double[] projection = new double[16];
    private final double[] pickPosition = new double[3];
    private final IntBuffer selectBuffer = GLBuffers.newDirectIntBuffer(SELECT_BUFFER_SIZE);
    private GLU glu;
    private final Set<Integer> prioritizedObjectIds = new HashSet<Integer>();
    protected boolean pickingRequested = false;
    protected final Vector2f mousePosition = new Vector2f();

    public PickingController(CameraController controller)
    {
        this.cameraController = controller;
    }

    public Camera getCamera()
    {
        return cameraController.getCamera();
    }

    public CameraController getCameraController()
    {
        return cameraController;
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        final GL2 gl = drawable.getGL().getGL2();

        if (pickingRequested)
        {
            Camera camera = cameraController.getCamera();
            camera.updateFrame();

            gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);

            fireStartPickingMode();
            gl.glSelectBuffer(selectBuffer.capacity(), selectBuffer);
            gl.glRenderMode(GL2.GL_SELECT);


            gl.glCullFace(GL2.GL_BACK);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);

            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            glu.gluPickMatrix(mousePosition.x, viewport[3] - 1 - mousePosition.y, 2, 2, viewport, 0);
            camera.doProjection(drawable);

            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();
            camera.doView(drawable);

            gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelView, 0);
            gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projection, 0);

            gl.glInitNames();
            gl.glPushName(0);
            cameraController.getDrawer().draw(drawable, cameraController);

            int numHits = gl.glRenderMode(GL2.GL_RENDER);
            fireStopPickingMode();

            processHits(gl, numHits);

            pickingRequested = false;
        }

        cameraController.display(drawable);
    }

    private double convertDepth(long depth)
    {
        if (depth < 0)
        {
            depth += 0x100000000L;
        }
        double retVal = ((double) depth) / 0x100000000L;
        return retVal;
    }

    private void processHits(GL2 gl, int numHits)
    {
        boolean picked = false;

        double closestZ = Integer.MAX_VALUE;
        int closestId = 0;

        // Check for any prioritized object IDs first.
        for (int i_hit = numHits - 1; i_hit >= 0; i_hit--)
        {
            int idx = i_hit * 4;
            int pickId = selectBuffer.get(idx + 3);
            long depth = selectBuffer.get(idx + 1);
            double pickZ = convertDepth(depth);

            if (prioritizedObjectIds.contains(new Integer(pickId)))
            {
                closestZ = pickZ;
                closestId = pickId;
                picked = true;
                break;
            }
        }

        // Then objects
        if (!picked)
        {
            closestZ = Integer.MAX_VALUE;
            for (int i_hit = 0; i_hit < numHits; i_hit++)
            {
                int idx = i_hit * 4;

                int pickId = selectBuffer.get(idx + 3);
                long depth = selectBuffer.get(idx + 1);
                double pickZ = convertDepth(depth);
                picked = true;

                if (pickZ < closestZ)
                {
                    closestZ = pickZ;
                    closestId = pickId;
                }
            }
        }

        if (picked)
        {
            glu.gluUnProject(mousePosition.x, viewport[3] - 1 - mousePosition.y, closestZ,
                    modelView, 0,
                    projection, 0,
                    viewport, 0,
                    pickPosition, 0);

            fireObjectPicked(closestId, new Vector3f(
                    (float) pickPosition[0], (float) pickPosition[1], (float) pickPosition[2]),
                    mousePosition);
        }
    }

    @Override
    public void dispose(GLAutoDrawable arg0)
    {
        cameraController.dispose(arg0);
    }

    @Override
    public void init(GLAutoDrawable arg0)
    {
        cameraController.init(arg0);
        glu = new GLU();
    }

    @Override
    public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3,
            int arg4)
    {
        cameraController.reshape(arg0, arg1, arg2, arg3, arg4);
    }

    protected boolean isFlagSet(MouseEvent e, int flag)
    {
        return (e.getModifiersEx() & flag) == flag;
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        cameraController.mouseClicked(e);
    }

    @Override
    public void mouseEntered(MouseEvent arg0)
    {
        cameraController.mouseEntered(arg0);
    }

    @Override
    public void mouseExited(MouseEvent arg0)
    {
        cameraController.mouseExited(arg0);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        mousePosition.set(e.getX(), e.getY());

        if (isFlagSet(e, MouseEvent.BUTTON1_DOWN_MASK)
                && !isFlagSet(e, MouseEvent.BUTTON2_DOWN_MASK)
                && !isFlagSet(e, MouseEvent.BUTTON3_DOWN_MASK))
        {
            if (!isFlagSet(e, MouseEvent.ALT_DOWN_MASK)
                    && !isFlagSet(e, MouseEvent.CTRL_DOWN_MASK)
                    && !isFlagSet(e, MouseEvent.SHIFT_DOWN_MASK))
            {
                pickingRequested = true;
            }
        }
        else
        {
            pickingRequested = false;
        }

        cameraController.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent arg0)
    {
        pickingRequested = false;
        cameraController.mouseReleased(arg0);
    }

    @Override
    public void mouseDragged(MouseEvent arg0)
    {
        cameraController.mouseDragged(arg0);
    }

    @Override
    public void mouseMoved(MouseEvent arg0)
    {
        cameraController.mouseMoved(arg0);
    }

    @Override
    public void keyPressed(KeyEvent arg0)
    {
        cameraController.keyPressed(arg0);
    }

    @Override
    public void keyReleased(KeyEvent arg0)
    {
        cameraController.keyReleased(arg0);
    }

    @Override
    public void keyTyped(KeyEvent arg0)
    {
        cameraController.keyTyped(arg0);
    }

    public void addPickingEventListener(PickingEventListener listener)
    {
        listenerList.add(PickingEventListener.class, listener);
    }

    public void removePickingEventListener(PickingEventListener listener)
    {
        listenerList.remove(PickingEventListener.class, listener);
    }

    protected void fireObjectPicked(int objectId, Vector3f pickLocation, Vector2f mousePosition)
    {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2)
        {
            if (listeners[i] == PickingEventListener.class)
            {
                ((PickingEventListener) listeners[i + 1]).objectPicked(this, objectId, pickLocation, mousePosition);
            }
        }
    }

    protected void fireStartPickingMode()
    {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2)
        {
            if (listeners[i] == PickingEventListener.class)
            {
                ((PickingEventListener) listeners[i + 1]).startPickingMode(this);
            }
        }
    }

    protected void fireStopPickingMode()
    {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2)
        {
            if (listeners[i] == PickingEventListener.class)
            {
                ((PickingEventListener) listeners[i + 1]).stopPickingMode(this);
            }
        }
    }

    public void addPrioritizedObjectId(int id)
    {
        prioritizedObjectIds.add(new Integer(id));
    }

    public void removePrioritizedObjectId(int id)
    {
        prioritizedObjectIds.remove(new Integer(id));
    }
}
