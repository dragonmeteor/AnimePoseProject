package yumyai.jogl.ui;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.swing.Icon;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

public abstract class Camera {
    //Default position of the camera
    protected static final Point3f DEFAULT_EYE = new Point3f(0, 0, 10);
    protected static final Point3f DEFAULT_TARGET = new Point3f(0, 0, 0);
    protected static final Vector3f DEFAULT_UP = new Vector3f(0, 1, 0);
    protected static final Vector3f VERTICAL = new Vector3f(0, 1, 0);
    //All the camera parameters
    public float aspect = 1;
    public float near;
    public float far;
    public final Point3f eye = new Point3f();
    public final Point3f target = new Point3f();
    public final Vector3f up = new Vector3f();
    public final Vector3f right = new Vector3f();
    public final Vector3f negGaze = new Vector3f();
    private GLU glu;
    //Icon to use in the tree display
    protected transient Icon icon = null;

    /**
     * Required by IO
     */
    public Camera() {
    }

    public Camera(float newNear, float newFar) {
        this(DEFAULT_EYE, DEFAULT_TARGET, DEFAULT_UP, newNear, newFar);
    }

    /**
     * Creates a camera from input parameters
     */
    public Camera(Point3f newEye, Point3f newTarget, Vector3f newUp, float newNear, float newFar) {
        up.set(newUp);
        eye.set(newEye);
        target.set(newTarget);
        near = newNear;
        far = newFar;

        glu = new GLU();

        updateFrame();
    }

    /**
     * Creates the camera from the current camera parameters
     */
    public void updateFrame() {
        negGaze.set(eye);
        negGaze.sub(target);
        negGaze.normalize();

        up.normalize();
        right.cross(up, negGaze);
        right.normalize();
        up.cross(negGaze, right);
    }

    public void getViewMatrix(Matrix4f M) {
        updateFrame();

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

    public void getViewMatrixInverse(Matrix4f M) {
        getViewMatrix(M);
        M.invert();
    }

    /**
     * Return the eye point
     *
     * @return
     */
    public Point3f getEye() {

        return eye;
    }

    /**
     * Return the target point
     *
     * @return
     */
    public Point3f getTarget() {

        return target;
    }

    /**
     * Return the up vector
     *
     * @return
     */
    public Vector3f getUp() {

        return up;
    }

    /**
     * Return the right vector
     *
     * @return
     */
    public Vector3f getRight() {

        return right;
    }

    /**
     * Returns the height of this camera's image
     *
     * @return
     */
    public abstract float getHeight();

    /**
     * Set the aspect ratio
     *
     * @param d
     */
    public void setAspect(float d) {

        aspect = d;
    }

    /**
     * Move the camera eye and target by the given vector
     *
     * @param delta
     */
    public void translate(Vector3f delta) {
        eye.add(delta);
        target.add(delta);
    }

    /*
     * Zoom the camera to distance d.
     */
    public abstract void zoom(float d);

    /**
     * Convert displacement delta in the view plane to world space.
     *
     * @param delta
     * @param output
     */
    public void convertMotion(Vector2f delta, Vector3f output) {
        // note: this method is approximate.  Loses accuracy away from the center of the viewport
        output.scale(-delta.x * aspect * getHeight(), right);
        output.scaleAdd(-delta.y * getHeight(), up, output);
    }

    /**
     * Flips the camera
     */
    public void flip() {
        Vector3f offset = new Vector3f();
        offset.sub(eye, target);
        offset.negate();
        eye.add(target, offset);
    }

    /**
     * Set the OpenGL project matrix for this camera
     */
    public abstract void doProjection(GLAutoDrawable d);

    /**
     * Set the model view matrix for this camera
     */
    public void doView(GLAutoDrawable d) {
        glu.gluLookAt(eye.x, eye.y, eye.z, target.x, target.y, target.z, up.x, up.y, up.z);
    }

    /**
     * Return two points that defines a line from the eye to a point with the
     * given normalized device coordinate.
     */
    public abstract void getLineThroughNDC(Vector2f ndc, Vector3f p0, Vector3f p1);

    /**
     * Return the point corresponding to the given normalized device coordinate
     * on the plane perpendiculat to the image plane that contains
     * planePosition.
     */
    public abstract Vector3f NDCToWorldAt(Vector2f ndc, Vector3f planePosition);
}