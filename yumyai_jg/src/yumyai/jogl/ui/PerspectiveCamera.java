package yumyai.jogl.ui;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

public class PerspectiveCamera extends Camera
{
    private static final float DEFAULT_FOVY = 45;
    /**
     * The Y field of view of the camera in degrees
     */
    public float fovy;
    public GLU glu = new GLU();
    public float dollyFactor = 6;

    /**
     * Required for IO
     */
    public PerspectiveCamera()
    {
    }

    public PerspectiveCamera(float newNear, float newFar)
    {
        this(newNear, newFar, DEFAULT_FOVY);
    }

    public PerspectiveCamera(float newNear, float newFar, float newFovy)
    {
        super(newNear, newFar);
        fovy = newFovy;
    }

    public PerspectiveCamera(Point3f eyePos, Point3f target, Vector3f up, float newNear, float newFar, float newFovy)
    {
        super(eyePos, target, up, newNear, newFar);
        fovy = newFovy;
    }

    public PerspectiveCamera(PerspectiveCamera copy)
    {
        super(new Point3f(copy.getEye()), new Point3f(copy.getTarget()), new Vector3f(copy.getUp()), copy.near, copy.far);
        fovy = copy.fovy;
    }

    /**
     * Perform an orbit move of this camera
     */
    public void orbit(Vector2f mouseDelta)
    {
        Vector3f negGaze = new Vector3f(eye);
        negGaze.sub(target);
        float dist = negGaze.length();
        negGaze.normalize();

        float azimuth = (float) Math.atan2(negGaze.x, negGaze.z);
        float elevation = (float) Math.atan2(negGaze.y, Math.sqrt(negGaze.x * negGaze.x + negGaze.z * negGaze.z));
        azimuth = (azimuth - mouseDelta.x) % (float) (2 * Math.PI);
        elevation = (float) Math.max(-Math.PI * 0.495, Math.min(Math.PI * 0.495, (elevation - mouseDelta.y)));

        negGaze.set((float) (Math.sin(azimuth) * Math.cos(elevation)), (float) Math.sin(elevation), (float) (Math.cos(azimuth) * Math.cos(elevation)));
        negGaze.normalize();

        eye.scaleAdd(dist, negGaze, target);

        updateFrame();
    }

    /**
     * Creates the camera from the current camera parameters
     */
    public void updateFrame()
    {
        negGaze.set(eye);
        negGaze.sub(target);
        negGaze.normalize();

        up.normalize();
        right.cross(VERTICAL, negGaze);
        right.normalize();
        up.cross(negGaze, right);
    }

    public void updateFrameNotUsingVertical() {
        super.updateFrame();
    }

    public void getViewMatrixNotUsingVertical(Matrix4f M) {
        super.updateFrame();

        M.m00 = right.x;
        M.m10 = right.y;
        M.m20 = right.z;
        M.m30 = 0.0f;

        M.m01 = up.x;
        M.m11 = up.y;
        M.m21 = up.z;
        M.m31 = 0.0f;

        M.m02 = negGaze.x;
        M.m12 = negGaze.y;
        M.m22 = negGaze.z;
        M.m32 = 0.0f;

        M.m03 = eye.x;
        M.m13 = eye.y;
        M.m23 = eye.z;
        M.m33 = 1.0f;
    }

    public void getViewMatrixInverseNotUsingVertical(Matrix4f M) {
        getViewMatrixNotUsingVertical(M);
        M.invert();
    }

    /**
     * Returns the height of the viewing fustrum, evaluated at the target point.
     */
    public float getHeight()
    {
        float dist = eye.distance(target);
        return (float) (Math.tan(Math.toRadians(fovy / 2.0)) * dist);
    }

   public void doProjection(GLAutoDrawable d)
    {
        glu.gluPerspective(fovy, aspect, near, far);
    }

    /**
     *
     * Zoom the camera to distance d.
     */
    public void zoom(float d)
    {
        dolly(-d);
        updateFrame();
    }

    /**
     * Dolly this camera
     */
    public Vector3f dolly(float d)
    {
        Vector3f gaze = new Vector3f(target);
        gaze.sub(eye);
        double dist = gaze.length();
        gaze.normalize();
        d *= dollyFactor;

        if (dist + d > 0.01)
        {
            eye.scaleAdd(-d, gaze, eye);
        }

        gaze.scale(-d);

        return gaze;
    }

    @Override
    public void getLineThroughNDC(Vector2f ndc, Vector3f p0,
            Vector3f p1)
    {
        p0.set(getEye());

        p1.set(getTarget());
        p1.scaleAdd(ndc.x * aspect * getHeight(), getRight(), p1);
        p1.scaleAdd(ndc.y * getHeight(), getUp(), p1);
    }

    @Override
    public Vector3f NDCToWorldAt(Vector2f ndc, Vector3f planePosition)
    {
        Vector3f gaze = new Vector3f();
        gaze.set(getTarget());
        gaze.sub(eye);
        gaze.normalize();

        Vector3f eyeToPlanePosition = new Vector3f();
        eyeToPlanePosition.set(planePosition);
        eyeToPlanePosition.sub(eye);

        float dist = eyeToPlanePosition.dot(gaze);

        float height = (float) (Math.tan(Math.toRadians(fovy / 2.0)) * dist);

        gaze.scale(dist);
        Vector3f out = new Vector3f();
        out.scale(ndc.x * aspect * height, right);
        out.scaleAdd(ndc.y * height, up, out);
        out.add(gaze);
        return out;
    }
}