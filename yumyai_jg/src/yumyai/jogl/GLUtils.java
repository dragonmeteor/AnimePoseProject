package yumyai.jogl;

import javax.media.opengl.GL2;
import javax.vecmath.Matrix4f;
import javax.vecmath.Tuple3f;

public class GLUtils
{
    private static float[] xformArray = new float[16];

    public static void glTransform(GL2 gl, Matrix4f m)
    {
        xformArray[ 0] = m.m00;
        xformArray[ 1] = m.m10;
        xformArray[ 2] = m.m20;
        xformArray[ 3] = m.m30;

        xformArray[ 4] = m.m01;
        xformArray[ 5] = m.m11;
        xformArray[ 6] = m.m21;
        xformArray[ 7] = m.m31;

        xformArray[ 8] = m.m02;
        xformArray[ 9] = m.m12;
        xformArray[10] = m.m22;
        xformArray[11] = m.m32;

        xformArray[12] = m.m03;
        xformArray[13] = m.m13;
        xformArray[14] = m.m23;
        xformArray[15] = m.m33;

        gl.glMultMatrixf(xformArray, 0);
    }

    /**
     * Draw a coordinate system and the Euclidean xz-plane.
     * @param gl GL2 context object
     * @param cellSize the size of each cell
     * @param halfCellCount half the number of cells on each side of the grid
     */
    public static void drawCoordinateAxesAndPlanes(GL2 gl, float cellSize, int halfCellCount)
    {                
        gl.glLineWidth(2.0f);

        gl.glBegin(GL2.GL_LINES);
        {
            gl.glColor3f(1, 0, 0);
            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f(cellSize * halfCellCount, 0, 0);

            gl.glColor3f(0, 1, 1);
            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f(-cellSize * halfCellCount, 0, 0);

            gl.glColor3f(0, 1, 0);
            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f(0, cellSize * halfCellCount, 0);

            gl.glColor3f(1, 0, 1);
            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f(0, -cellSize * halfCellCount, 0);

            gl.glColor3f(0, 0, 1);
            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f(0, 0, cellSize * halfCellCount);

            gl.glColor3f(1, 1, 0);
            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f(0, 0, -cellSize * halfCellCount);
        }
        gl.glEnd();

        gl.glLineWidth(1.0f);
        gl.glColor3f(0.5f, 0.5f, 0.5f);
        gl.glBegin(GL2.GL_LINES);
        {
            for (int i = -halfCellCount; i <= halfCellCount; i++)
            {
                gl.glVertex3f(cellSize * i, 0, -cellSize * halfCellCount);
                gl.glVertex3f(cellSize * i, 0, cellSize * halfCellCount);
            }

            for (int i = -halfCellCount; i <= halfCellCount; i++)
            {
                gl.glVertex3f(-cellSize * halfCellCount, 0, cellSize * i);
                gl.glVertex3f(cellSize * halfCellCount, 0, cellSize * i);
            }
        }
        gl.glEnd();
    }

    public static void glVertex(GL2 gl, Tuple3f v) {
        gl.glVertex3f(v.x, v.y, v.z);
    }
}
