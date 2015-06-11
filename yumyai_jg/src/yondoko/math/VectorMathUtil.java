package yondoko.math;

import com.badlogic.gdx.math.Matrix4;
import yondoko.util.ObjectAllocator;

import javax.vecmath.*;

public class VectorMathUtil {
    public static final Point3f ORIGIN = new Point3f(0, 0, 0);
    public static final Vector3f ZERO_VECTOR = new Vector3f(0, 0, 0);
    public static final Vector3f X_AXIS = new Vector3f(1, 0, 0);
    public static final Vector3f Y_AXIS = new Vector3f(0, 1, 0);
    public static final Vector3f Z_AXIS = new Vector3f(0, 0, 01);
    public static final Quat4f QUAT_IDENTITY = new Quat4f(0, 0, 0, 1);

    public static void quaternionToEuler(Quat4f q, Vector3f result) {
        float x2 = q.x + q.x;
        float y2 = q.y + q.y;
        float z2 = q.z + q.z;
        float xz2 = q.x * z2;
        float wy2 = q.w * y2;
        float temp = -(xz2 - wy2);

        if (temp >= 1) {
            temp = 1;
        } else if (temp <= -1) {
            temp = -1;
        }

        float yRadian = (float) Math.asin(temp);

        float xx2 = q.x * x2;
        float xy2 = q.x * y2;
        float zz2 = q.z * z2;
        float wz2 = q.w * z2;

        if (yRadian < 3.1415926f * 0.5f) {
            if (yRadian > -3.1415926f * 0.5f) {
                float yz2 = q.y * z2;
                float wx2 = q.w * x2;
                float yy2 = q.y * y2;
                result.x = (float) Math.atan2((yz2 + wx2), (1 - (xx2 + yy2)));
                result.y = yRadian;
                result.z = (float) Math.atan2((xy2 + wz2), (1 - (yy2 + zz2)));
            } else {
                result.x = -(float) Math.atan2((xy2 - wz2), (1 - (xx2 + zz2)));
                result.y = yRadian;
                result.z = 0;
            }
        } else {
            result.x = (float) Math.atan2((xy2 - wz2), (1 - (xx2 + zz2)));
            result.y = yRadian;
            result.z = 0;
        }
    }

    public static void eulerToQuaternion(Vector3f euler, Quat4f result) {
        float xRadian = euler.x * 0.5f;
        float yRadian = euler.y * 0.5f;
        float zRadian = euler.z * 0.5f;
        float sinX = (float) Math.sin(xRadian);
        float cosX = (float) Math.cos(xRadian);
        float sinY = (float) Math.sin(yRadian);
        float cosY = (float) Math.cos(yRadian);
        float sinZ = (float) Math.sin(zRadian);
        float cosZ = (float) Math.cos(zRadian);

        result.x = sinX * cosY * cosZ - cosX * sinY * sinZ;
        result.y = cosX * sinY * cosZ + sinX * cosY * sinZ;
        result.z = cosX * cosY * sinZ - sinX * sinY * cosZ;
        result.w = cosX * cosY * cosZ + sinX * sinY * sinZ;
    }

    public static void yawPitchRollToQuaternion(float yaw, float pitch, float roll, Quat4f quaternion) {
        quaternion.x = (((float) Math.cos((double) (yaw * 0.5f)) * (float) Math.sin((double) (pitch * 0.5f))) * (float) Math.cos((double) (roll * 0.5f))) + (((float) Math.sin((double) (yaw * 0.5f)) * (float) Math.cos((double) (pitch * 0.5f))) * (float) Math.sin((double) (roll * 0.5f)));
        quaternion.y = (((float) Math.sin((double) (yaw * 0.5f)) * (float) Math.cos((double) (pitch * 0.5f))) * (float) Math.cos((double) (roll * 0.5f))) - (((float) Math.cos((double) (yaw * 0.5f)) * (float) Math.sin((double) (pitch * 0.5f))) * (float) Math.sin((double) (roll * 0.5f)));
        quaternion.z = (((float) Math.cos((double) (yaw * 0.5f)) * (float) Math.cos((double) (pitch * 0.5f))) * (float) Math.sin((double) (roll * 0.5f))) - (((float) Math.sin((double) (yaw * 0.5f)) * (float) Math.sin((double) (pitch * 0.5f))) * (float) Math.cos((double) (roll * 0.5f)));
        quaternion.w = (((float) Math.cos((double) (yaw * 0.5f)) * (float) Math.cos((double) (pitch * 0.5f))) * (float) Math.cos((double) (roll * 0.5f))) + (((float) Math.sin((double) (yaw * 0.5f)) * (float) Math.sin((double) (pitch * 0.5f))) * (float) Math.sin((double) (roll * 0.5f)));
    }

    public static void coordinateSystem(Vector3f normal, Vector3f tangent, Vector3f binormal) {
        normal.normalize();

        float nx = Math.abs(normal.x);
        float ny = Math.abs(normal.y);
        float nz = Math.abs(normal.z);

        if (nx > ny && nx > nz) {
            tangent.set(-ny, nx, 0);
        } else if (ny > nx && ny > nz) {
            tangent.set(0, -nz, ny);
        } else {
            tangent.set(nz, 0, -nx);
        }
        tangent.normalize();

        binormal.cross(normal, tangent);
        binormal.normalize();
    }

    public static void coordinateSystemGivenZandY(Vector3f x, Vector3f y, Vector3f z) {
        z.normalize();
        x.cross(y, z);
        x.normalize();
        y.cross(z, x);
        y.normalize();
    }

    public static Matrix4f matrixFromFrame(Tuple3f x, Tuple3f y, Tuple3f z) {
        Matrix4f M = new Matrix4f();
        M.setIdentity();

        M.m00 = x.x;
        M.m10 = x.y;
        M.m20 = x.z;

        M.m01 = y.x;
        M.m11 = y.y;
        M.m21 = y.z;

        M.m02 = z.x;
        M.m12 = z.y;
        M.m22 = z.z;

        return M;
    }

    public static float[] matrixToArrayColumnMajor(Matrix4f m) {
        float[] result = new float[16];

        result[0] = m.m00;
        result[1] = m.m10;
        result[2] = m.m20;
        result[3] = m.m30;

        result[4] = m.m01;
        result[5] = m.m11;
        result[6] = m.m21;
        result[7] = m.m31;

        result[8] = m.m02;
        result[9] = m.m12;
        result[10] = m.m22;
        result[11] = m.m32;

        result[12] = m.m03;
        result[13] = m.m13;
        result[14] = m.m23;
        result[15] = m.m33;

        return result;
    }

    public static void gdxToMatrix4f(Matrix4 gdx, Matrix4f m) {
        float[] values = gdx.getValues();

        m.m00 = values[0];
        m.m10 = values[1];
        m.m20 = values[2];
        m.m30 = values[3];

        m.m01 = values[4];
        m.m11 = values[5];
        m.m21 = values[6];
        m.m31 = values[7];

        m.m02 = values[8];
        m.m12 = values[9];
        m.m22 = values[10];
        m.m32 = values[11];

        m.m03 = values[12];
        m.m13 = values[13];
        m.m23 = values[14];
        m.m33 = values[15];
    }

    public static void matrix4ftoGdx(Matrix4f m, Matrix4 gdx) {
        float[] values = matrixToArrayColumnMajor(m);
        m.set(values);
    }

    public static float getCompenent(Tuple3f p, int dim) {
        if (dim == 0)
            return p.x;
        else if (dim == 1)
            return p.y;
        else if (dim == 2)
            return p.z;
        else
            throw new RuntimeException("dim was neither 0, 1, or 2");
    }

    public static int getCompenent(Tuple3i p, int dim) {
        if (dim == 0)
            return p.x;
        else if (dim == 1)
            return p.y;
        else if (dim == 2)
            return p.z;
        else
            throw new RuntimeException("dim was neither 0, 1, or 2");
    }

    public static boolean factorQuaternionZXY(Quat4f input, Tuple3f out) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Quat4f inputQ = allocator.getQuat4f();
        inputQ.set(input.x, input.y, input.z, input.w);
        inputQ.normalize();

        Matrix4f rot = allocator.getMatrix4f();
        rot.setIdentity();
        rot.setRotation(inputQ);
        rot.transpose();

        try {
            //ヨー(X軸周りの回転)を取得
            if (rot.m21 > 1 - 1.0e-4 || rot.m21 < -1 + 1.0e-4) {//ジンバルロック判定
                out.x = (float) (rot.m21 < 0 ? Math.PI / 2 : -Math.PI / 2);
                out.z = 0;
                out.y = (float) Math.atan2(-rot.m02, rot.m00);
                return false;
            }
            out.x = -(float) Math.asin(rot.m21);
            //ロールを取得
            out.z = (float) Math.asin(rot.m01 / Math.cos(out.x));
            if (Float.isNaN(out.z)) {//漏れ対策
                out.x = (float) (rot.m21 < 0 ? Math.PI / 2 : -Math.PI / 2);
                out.z = 0;
                out.y = (float) Math.atan2(-rot.m02, rot.m00);
                return false;
            }
            if (rot.m11 < 0)
                out.z = (float) (Math.PI - out.z);
            //ピッチを取得
            out.y = (float) Math.atan2(rot.m20, rot.m22);
            return true;
        } finally {
            allocator.put(inputQ);
            allocator.put(rot);
        }
    }

    public static boolean factorQuaternionXYZ(Quat4f input, Tuple3f out) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Quat4f inputQ = allocator.getQuat4f();
        inputQ.set(input.x, input.y, input.z, input.w);
        inputQ.normalize();

        Matrix4f rot = allocator.getMatrix4f();
        rot.setIdentity();
        rot.setRotation(inputQ);
        rot.transpose();

        try {
            //Y軸回りの回転を取得
            if (rot.m02 > 1 - 1.0e-4 || rot.m02 < -1 + 1.0e-4) {//ジンバルロック判定
                out.x = 0;
                out.y = (float) (rot.m02 < 0 ? Math.PI / 2 : -Math.PI / 2);
                out.z = -(float) Math.atan2(-rot.m10, rot.m11);
                return false;
            }
            out.y = -(float) Math.asin(rot.m02);
            //X軸回りの回転を取得
            out.x = (float) Math.asin(rot.m12 / Math.cos(out.y));
            if (Float.isNaN(out.x)) {//ジンバルロック判定(漏れ対策)
                out.x = 0;
                out.y = (float) (rot.m02 < 0 ? Math.PI / 2 : -Math.PI / 2);
                out.z = -(float) Math.atan2(-rot.m10, rot.m11);
                return false;
            }
            if (rot.m22 < 0)
                out.x = (float) (Math.PI - out.x);
            //Z軸回りの回転を取得
            out.z = (float) Math.atan2(rot.m01, rot.m00);
            return true;
        } finally {
            allocator.put(inputQ);
            allocator.put(rot);
        }
    }

    public static boolean factorQuaternionYZX(Quat4f input, Tuple3f out) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Quat4f inputQ = allocator.getQuat4f();
        inputQ.set(input.x, input.y, input.z, input.w);
        inputQ.normalize();

        Matrix4f rot = allocator.getMatrix4f();
        rot.setIdentity();
        rot.setRotation(inputQ);
        rot.transpose();

        try {
            //Z軸回りの回転を取得
            if (rot.m10 > 1 - 1.0e-4 || rot.m10 < -1 + 1.0e-4) {//ジンバルロック判定
                out.y = 0;
                out.z = (float) (rot.m10 < 0 ? Math.PI / 2 : -Math.PI / 2);
                out.x = -(float) Math.atan2(-rot.m21, rot.m22);
                return false;
            }
            out.z = -(float) Math.asin(rot.m10);
            //Y軸回りの回転を取得
            out.y = (float) Math.asin(rot.m20 / Math.cos(out.z));
            if (Float.isNaN(out.y)) {//ジンバルロック判定(漏れ対策)
                out.y = 0;
                out.z = (float) (rot.m10 < 0 ? Math.PI / 2 : -Math.PI / 2);
                out.x = -(float) Math.atan2(-rot.m21, rot.m22);
                return false;
            }
            if (rot.m00 < 0)
                out.y = (float) (Math.PI - out.y);
            //X軸回りの回転を取得
            out.x = (float) Math.atan2(rot.m12, rot.m11);
            return true;
        } finally {
            allocator.put(inputQ);
            allocator.put(rot);
        }
    }

    public static void eulerXYZToQuaternion(Vector3f euler, Quat4f q) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Quat4f qx = allocator.getQuat4f();
        Quat4f qy = allocator.getQuat4f();
        Quat4f qz = allocator.getQuat4f();
        AxisAngle4f aax = allocator.getAxisAngle4f();
        AxisAngle4f aay = allocator.getAxisAngle4f();
        AxisAngle4f aaz = allocator.getAxisAngle4f();

        aax.set(1, 0, 0, euler.x);
        aay.set(0, 1, 0, euler.y);
        aaz.set(0, 0, 1, euler.z);
        qx.set(aax);
        qy.set(aay);
        qz.set(aaz);

        q.set(qz);
        q.mul(qy);
        q.mul(qx);

        allocator.put(aaz);
        allocator.put(aay);
        allocator.put(aax);
        allocator.put(qz);
        allocator.put(qy);
        allocator.put(qx);
    }

    public static void eulerYZXToQuaternion(Vector3f euler, Quat4f q) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Quat4f qx = allocator.getQuat4f();
        Quat4f qy = allocator.getQuat4f();
        Quat4f qz = allocator.getQuat4f();
        AxisAngle4f aax = allocator.getAxisAngle4f();
        AxisAngle4f aay = allocator.getAxisAngle4f();
        AxisAngle4f aaz = allocator.getAxisAngle4f();

        aax.set(1, 0, 0, euler.x);
        aay.set(0, 1, 0, euler.y);
        aaz.set(0, 0, 1, euler.z);
        qx.set(aax);
        qy.set(aay);
        qz.set(aaz);

        q.set(qx);
        q.mul(qz);
        q.mul(qy);

        allocator.put(aaz);
        allocator.put(aay);
        allocator.put(aax);
        allocator.put(qz);
        allocator.put(qy);
        allocator.put(qx);
    }

    public static void eulerZXYToQuaternion(Vector3f euler, Quat4f q) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Quat4f qx = allocator.getQuat4f();
        Quat4f qy = allocator.getQuat4f();
        Quat4f qz = allocator.getQuat4f();
        AxisAngle4f aax = allocator.getAxisAngle4f();
        AxisAngle4f aay = allocator.getAxisAngle4f();
        AxisAngle4f aaz = allocator.getAxisAngle4f();

        aax.set(1, 0, 0, euler.x);
        aay.set(0, 1, 0, euler.y);
        aaz.set(0, 0, 1, euler.z);
        qx.set(aax);
        qy.set(aay);
        qz.set(aaz);

        q.set(qy);
        q.mul(qx);
        q.mul(qz);

        allocator.put(aaz);
        allocator.put(aay);
        allocator.put(aax);
        allocator.put(qz);
        allocator.put(qy);
        allocator.put(qx);
    }

    public static void normalizeEulerAngle(Tuple3f source) {
        while (!Util.between(source.x, (float) -Math.PI, (float) Math.PI)) {
            if (source.x > Math.PI) {
                source.x -= (float) Math.PI * 2;
            } else {
                source.x += (float) Math.PI * 2;
            }
        }
        /*
        while (!Util.between(source.y, (float) -Math.PI * 0.5f, (float) Math.PI * 0.5f)) {
            if (source.y > Math.PI * 0.5f) {
                source.y -= (float) Math.PI * 2;
            } else {
                source.y += (float) Math.PI * 2;
            }
        }
        */
        while (!Util.between(source.y, (float) -Math.PI, (float) Math.PI)) {
            if (source.y > Math.PI) {
                source.y -= (float) Math.PI * 2;
            } else {
                source.y += (float) Math.PI * 2;
            }
        }
        while (!Util.between(source.z, (float) -Math.PI, (float) Math.PI)) {
            if (source.z > Math.PI) {
                source.z -= (float) Math.PI * 2;
            } else {
                source.z += (float) Math.PI * 2;
            }
        }
    }

    public static Matrix4f createProjectionMatrix(float fovy, float aspect, float near, float far) {
        float f = (float)(1.0 / Math.tan(Math.toRadians(fovy / 2.0)));
        Matrix4f M = new Matrix4f();
        M.setZero();
        M.m00 = f / aspect;
        M.m11 = f;
        M.m22 = (far + near)/(near - far);
        M.m23 = (2*far*near) /(near - far);
        M.m32 = -1;
        return M;
    }

    public static boolean isNaN(Tuple4f t) {
        return Float.isNaN(t.x) || Float.isNaN(t.y) || Float.isNaN(t.z) || Float.isNaN(t.w);
    }

    public static boolean isNaN(Tuple3f t) {
        return Float.isNaN(t.x) || Float.isNaN(t.y) || Float.isNaN(t.z);
    }
}
