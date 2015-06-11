package yumyai.mmd.pmd;

import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;

public class PmdBulletMotionState extends btMotionState
{
    /*
    public final btTransform xform = new btTransform();

    @Override
    public btTransform getWorldTransform(btTransform t)
    {
        t.setBasis(xform.getBasis());
        t.setOrigin(xform.getOrigin());
        return xform;
    }

    @Override
    public void setWorldTransform(btTransform t)
    {
        xform.set(t);
    }
    
    public void set(Matrix4f m)
    {
        assignMatrix4fToBulletTransform(xform, m);
    }
    
    public void get(Matrix4f m)
    {
        assignBulletTransformToMatrix4f(m, xform);
    }
    
    public static void assignMatrix4fToBulletTransform(btTransform xform, Matrix4f m)
    {
        xform.origin.x = m.m03;
        xform.origin.y = m.m13;
        xform.origin.z = m.m23;
        
        xform.basis.m00 = m.m00; xform.basis.m01 = m.m01; xform.basis.m02 = m.m02;        
        xform.basis.m10 = m.m10; xform.basis.m11 = m.m11; xform.basis.m12 = m.m12;        
        xform.basis.m20 = m.m20; xform.basis.m21 = m.m21; xform.basis.m22 = m.m22;
    }
    
    public static void assignBulletTransformToMatrix4f(Matrix4f m, btTransform xform)
    {
        m.setIdentity();
        
        m.m03 = xform.origin.x;
        m.m13 = xform.origin.y;
        m.m23 = xform.origin.z;
        
        m.m00 = xform.basis.m00; m.m01 = xform.basis.m01; m.m02 = xform.basis.m02;
        m.m10 = xform.basis.m10; m.m11 = xform.basis.m11; m.m12 = xform.basis.m12;
        m.m20 = xform.basis.m20; m.m21 = xform.basis.m21; m.m22 = xform.basis.m22;
    }
    */
}
