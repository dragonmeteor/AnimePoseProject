package yumyai.mmd.pmd;

import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.GdxNativesLoader;
import yondoko.math.VectorMathUtil;
import yondoko.util.ObjectAllocator;

public final class PmdModelPhysics {
    public final PmdModel model;
    public final List<btRigidBody> rigidBodies = new ArrayList<btRigidBody>();
    public final List<Matrix4f> bone2RigidBodyXforms = new ArrayList<Matrix4f>();
    public final List<btDefaultMotionState> motionStates = new ArrayList<btDefaultMotionState>();
    public final List<btTypedConstraint> constraints = new ArrayList<btTypedConstraint>();
    private btDiscreteDynamicsWorld physicsWorld;
    private final boolean[] updated;
    private final int[] associatedRigidBody;

    static {
        GdxNativesLoader.load();
        Bullet.init();
    }

    public PmdModelPhysics(PmdModel model) {
        this.model = model;
        this.updated = new boolean[model.bones.size()];
        this.associatedRigidBody = new int[model.bones.size()];
        for (int i = 0; i < model.bones.size(); i++) {
            this.associatedRigidBody[i] = -1;
        }
        createPhysicsWorld();
        createRigidBodies();
        createConstraints();
    }

    private void createPhysicsWorld() {
        btDefaultCollisionConfiguration config = new btDefaultCollisionConfiguration();
        btCollisionDispatcher dispatcher = new btCollisionDispatcher(config);
        //btAxisSweep3 pairCache = new btAxisSweep3(new Vector3(-3000, -3000, -3000), new Vector3(3000, 3000, 3000), 1024, null);
        btDbvtBroadphase pairCache = new btDbvtBroadphase();
        btSequentialImpulseConstraintSolver solver = new btSequentialImpulseConstraintSolver();
        physicsWorld = new btDiscreteDynamicsWorld(dispatcher, pairCache, solver, config);
        physicsWorld.setGravity(new Vector3(0, -9.8f*2.5f, 0));
    }

    public void setGravity(float x, float y, float z) {
        physicsWorld.setGravity(new Vector3(x, y, z));
    }

    private static Matrix4 convertToGdxMatrix(Matrix4f m) {
        Matrix4 result = new Matrix4();
        float[] mm = VectorMathUtil.matrixToArrayColumnMajor(m);
        result.set(mm);
        return result;
    }

    private void createRigidBodies() {
        ObjectAllocator allocator = ObjectAllocator.get();
        Matrix4f tempMat = allocator.getMatrix4f();
        Quat4f rotation = allocator.getQuat4f();
        PmdPose pose = new PmdPose(model);
        pose.clear();

        for (int rbIndex = 0; rbIndex < model.rigidBodies.size(); rbIndex++) {
            PmdRigidBody pmdRb = model.rigidBodies.get(rbIndex);
            if (pmdRb.boneIndex >= 0) {
                associatedRigidBody[pmdRb.boneIndex] = rbIndex;
            }

            Matrix4f b2rXform = new Matrix4f();
            b2rXform.setIdentity();
            VectorMathUtil.yawPitchRollToQuaternion(pmdRb.rotation.y, pmdRb.rotation.x, pmdRb.rotation.z, rotation);
            b2rXform.setRotation(rotation);
            b2rXform.setTranslation(pmdRb.position);
            bone2RigidBodyXforms.add(b2rXform);

            //System.out.println(pmdRb.name);
            //System.out.println(pmdRb.position);
            //System.out.println(pmdRb.boneIndex);

            btDefaultMotionState motionState = new btDefaultMotionState();
            if (pmdRb.boneIndex < 0) {
                int centerIndex = 0;
                tempMat.setIdentity();
                if (centerIndex >= 0) {
                    pose.getBoneTransform(centerIndex, tempMat);
                }
                tempMat.mul(b2rXform);
                motionState.setWorldTransform(convertToGdxMatrix(tempMat));
            } else {
                pose.getBoneTransform(pmdRb.boneIndex, tempMat);
                tempMat.mul(b2rXform);
                motionState.setWorldTransform(convertToGdxMatrix(tempMat));
            }
            motionStates.add(motionState);

            btCollisionShape collisionShape;
            if (pmdRb.shape == PmdRigidBodyShapeType.Box) {
                collisionShape = new btBoxShape(new Vector3(pmdRb.width, pmdRb.height, pmdRb.depth));
            } else if (pmdRb.shape == PmdRigidBodyShapeType.Sphere) {
                collisionShape = new btSphereShape(pmdRb.width);
            } else {
                collisionShape = new btCapsuleShape(pmdRb.width, pmdRb.height);
            }

            Vector3 localInertia = new Vector3(0, 0, 0);
            if (pmdRb.type != PmdRigidBodyType.FollowBone) {
                collisionShape.calculateLocalInertia(pmdRb.mass, localInertia);
            }

            btRigidBody rb;
            if (pmdRb.type == PmdRigidBodyType.FollowBone) {
                rb = new btRigidBody(0, motionState, collisionShape, localInertia);
            } else {
                rb = new btRigidBody(pmdRb.mass, motionState, collisionShape, localInertia);
            }

            rb.setDamping(pmdRb.positionDamping, pmdRb.rotationDamping);
            rb.setFriction(pmdRb.friction);
            rb.setRestitution(pmdRb.restitution);

            if (pmdRb.type == PmdRigidBodyType.FollowBone) {
                rb.setActivationState(Collision.DISABLE_DEACTIVATION);
                rb.setCollisionFlags(btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT | rb.getCollisionFlags());
            }

            short group = (short) Math.pow(2, pmdRb.groupIndex);
            //System.out.printf("groupIndex = %d, group = %x, flag = %x\n", pmdRb.groupIndex, group, (short)pmdRb.hitWithGroupFlags);
            physicsWorld.addRigidBody(rb, group, (short) pmdRb.hitWithGroupFlags);
            rigidBodies.add(rb);
        }

        allocator.put(rotation);
        allocator.put(tempMat);
    }

    private void createConstraints() {
        ObjectAllocator allocator = ObjectAllocator.get();
        Matrix4f tempMat = allocator.getMatrix4f();
        //Matrix4f xform0Inv = allocator.getMatrix4f();
        //Matrix4f xform1Inv = allocator.getMatrix4f();
        Quat4f rotation = allocator.getQuat4f();

        for (int jointIndex = 0; jointIndex < model.joints.size(); jointIndex++) {
            PmdJoint pmdJoint = model.joints.get(jointIndex);

            Matrix4f jointXform4f = new Matrix4f();

            jointXform4f.setIdentity();
            //System.out.println("name = " + pmdJoint.name + ", rotationConstraint = " + pmdJoint.angularLowerLimit + " -- " + pmdJoint.angularUpperLimit);
            //VectorMathUtil.yawPitchRollToQuaternion(-pmdJoint.rotation.y, pmdJoint.rotation.x, pmdJoint.rotation.z, rotation);
            VectorMathUtil.yawPitchRollToQuaternion(pmdJoint.rotation.y, pmdJoint.rotation.x, pmdJoint.rotation.z, rotation);
            jointXform4f.setRotation(rotation);
            jointXform4f.setTranslation(pmdJoint.position);

            Matrix4 jointXform = convertToGdxMatrix(jointXform4f);

            Matrix4 xform0 = new Matrix4();
            Matrix4 xform1 = new Matrix4();

            motionStates.get(pmdJoint.rigidBodies[0]).getWorldTransform(xform0);
            motionStates.get(pmdJoint.rigidBodies[1]).getWorldTransform(xform1);

            Matrix4 xform0Inv = xform0.inv();
            Matrix4 xform1Inv = xform1.inv();

            xform0 = xform0Inv.mul(jointXform);
            xform1 = xform1Inv.mul(jointXform);

            btGeneric6DofSpringConstraint constraint = new btGeneric6DofSpringConstraint(
                    rigidBodies.get(pmdJoint.rigidBodies[0]),
                    rigidBodies.get(pmdJoint.rigidBodies[1]),
                    xform0,
                    xform1,
                    true);

            //System.out.println("linearLowerLimit = " + pmdJoint.linearLowerLimit);
            //System.out.println("linearUppserLimit = " + pmdJoint.linearUpperLimit);

            constraint.setLinearLowerLimit(new Vector3(
                    pmdJoint.linearLowerLimit.x, pmdJoint.linearLowerLimit.y, pmdJoint.linearLowerLimit.z));
            constraint.setLinearUpperLimit(new Vector3(
                    pmdJoint.linearUpperLimit.x, pmdJoint.linearUpperLimit.y, pmdJoint.linearUpperLimit.z));

            constraint.setAngularLowerLimit(new Vector3(
                    pmdJoint.angularLowerLimit.x, pmdJoint.angularLowerLimit.y, pmdJoint.angularLowerLimit.z));
            constraint.setAngularUpperLimit(new Vector3(
                    pmdJoint.angularUpperLimit.x, pmdJoint.angularUpperLimit.y, pmdJoint.angularUpperLimit.z));

            for (int j = 0; j < 3; j++) {
                constraint.setStiffness(j, pmdJoint.springLinearStiffness[j]);
                constraint.enableSpring(j, true);
                constraint.setStiffness(j + 3, pmdJoint.springAngularStiffness[j]);
                constraint.enableSpring(j + 3, true);
            }

            constraint.calculateTransforms();
            constraint.setEquilibriumPoint();

            physicsWorld.addConstraint(constraint);
            constraints.add(constraint);
        }

        allocator.put(rotation);
        //allocator.put(xform1);
        //allocator.put(xform0);
        allocator.put(tempMat);
    }

    private void updateBoneXformAttachedToRigidBody(int i, PmdPose pose, ObjectAllocator allocator) {
        PmdRigidBody pmdRb = model.rigidBodies.get(i);

        if (pmdRb.type == PmdRigidBodyType.FollowBone) {
            return;
        }
        if (pmdRb.boneIndex == 65535) {
            return;
        }

        Matrix4f boneXform = allocator.getMatrix4f();
        Matrix4f parentBoneXform = allocator.getMatrix4f();
        Matrix4f rbXform = allocator.getMatrix4f();
        Vector3f translation = allocator.getVector3f();
        Quat4f rotation = allocator.getQuat4f();
        Matrix4f temp = allocator.getMatrix4f();
        Vector3f displacement = allocator.getVector3f();

        Matrix4 rbXformGdx = new Matrix4();
        motionStates.get(i).getWorldTransform(rbXformGdx);
        VectorMathUtil.gdxToMatrix4f(rbXformGdx, rbXform);

        PmdBone bone = model.bones.get(pmdRb.boneIndex);
        if (bone.parent != null) {
            pose.getInverseBoneTransform(bone.parentIndex, parentBoneXform);
        } else {
            parentBoneXform.setIdentity();
        }
        //pose.getInverseBoneTransform(pmdRb.boneIndex, boneXform);
        boneXform.set(parentBoneXform);
        temp.invert(bone2RigidBodyXforms.get(i));

        boneXform.mul(rbXform);
        boneXform.mul(temp);
        //PmdBulletMotionState.assignMatrix4fToBulletTransform(tempXform, boneXform);
        //tempXform.getRotation(rotation); 
        //rotation.x = -rotation.x;
        //rotation.y = -rotation.y;
        //translation.x = tempXform.origin.x;
        //translation.y = tempXform.origin.y;
        //translation.z = -tempXform.origin.z;
        boneXform.get(translation);
        boneXform.get(rotation);
        bone.getDisplacement(displacement);
        translation.sub(displacement);

        pose.setPoseBoneDisplacement(pmdRb.boneIndex, translation);
        pose.setPoseBoneRotation(pmdRb.boneIndex, rotation);

        allocator.put(displacement);
        allocator.put(temp);
        allocator.put(translation);
        allocator.put(rotation);
        allocator.put(rbXform);
        allocator.put(parentBoneXform);
        allocator.put(boneXform);
    }

    private void updateBoneHelper(int boneIndex, PmdPose pose, ObjectAllocator allocator) {
        if (boneIndex < 0)
            return;
        PmdBone bone = model.bones.get(boneIndex);
        if (boneIndex != 0) {
            updateBoneHelper(bone.parentIndex, pose, allocator);
        }
        if (!updated[boneIndex] && bone.controlledByPhysics) {
            updateBoneXformAttachedToRigidBody(associatedRigidBody[boneIndex], pose, allocator);
            updated[boneIndex] = true;
        }
    }

    public void getPose(PmdPose pose) {
        ObjectAllocator allocator = ObjectAllocator.get();
        for (int i = 0; i < model.bones.size(); i++) {
            updated[i] = false;
        }
        for (int i = 0; i < rigidBodies.size(); i++) {
            PmdRigidBody rb = model.rigidBodies.get(i);
            if (rb.boneIndex != 65535 && rb.boneIndex >= 0) {
                updateBoneHelper(rb.boneIndex, pose, allocator);
            }
        }
    }
    /*
     public void getPose(PmdPose pose)
     {
     ObjectAllocator allocator = ObjectAllocator.get();
     Matrix4f boneXform = allocator.getMatrix4f();
     Matrix4f rbXform = allocator.getMatrix4f();
     Vector3f translation = allocator.getVector3f();
     Quat4f rotation = allocator.getQuat4f();
     Matrix4f temp = allocator.getMatrix4f();

     for (int i = 0; i < rigidBodies.size(); i++)
     {
     PmdRigidBody pmdRb = model.rigidBodies.get(i);

     if (pmdRb.type != PmdRigidBodyType.FollowBone)
     {
     if (pmdRb.boneIndex == 65535)
     {
     continue;
     }

     motionStates.get(i).get(rbXform);
     pose.getInverseBoneTransform(pmdRb.boneIndex, boneXform);
     temp.invert(bone2RigidBodyXforms.get(i));

     boneXform.mul(rbXform);
     boneXform.mul(temp);
     boneXform.get(translation);
     boneXform.get(rotation);

     pose.setPoseBoneDisplacement(pmdRb.boneIndex, translation);
     pose.setPoseBoneRotation(pmdRb.boneIndex, rotation);
     }
     }

     allocator.put(temp);
     allocator.put(translation);
     allocator.put(rotation);
     allocator.put(rbXform);
     allocator.put(boneXform);
     }
     */

    public void setPose(PmdPose pose) {
        if (pose.model != model) {
            throw new RuntimeException("pose's model is not the same as physics's model");
        }

        ObjectAllocator allocator = ObjectAllocator.get();
        Matrix4f boneXform = allocator.getMatrix4f();
        Matrix4f rbXform = allocator.getMatrix4f();

        for (int i = 0; i < rigidBodies.size(); i++) {
            PmdRigidBody pmdRb = model.rigidBodies.get(i);
            if (pmdRb.type == PmdRigidBodyType.FollowBone) {
                pose.getBoneTransform(pmdRb.boneIndex, boneXform);
                rbXform.mul(boneXform, bone2RigidBodyXforms.get(i));
                Matrix4 gdx = convertToGdxMatrix(rbXform);
                motionStates.get(i).setWorldTransform(gdx);
            }
        }

        allocator.put(rbXform);
        allocator.put(boneXform);
    }

    public void timeStep(float elapsedTimeInSeconds) {
        physicsWorld.stepSimulation(elapsedTimeInSeconds);
    }

    public void setPoseAndTimeStep(float elapsedTimeInSeconds, PmdPose pose) {
        setPose(pose);
        timeStep(elapsedTimeInSeconds);
    }

    public void resetPhysicsWithPose(PmdPose pose) {
        if (pose.model != model) {
            throw new RuntimeException("pose's model is not the same as physics's model");
        }

        ObjectAllocator allocator = ObjectAllocator.get();
        Matrix4f boneXform = allocator.getMatrix4f();
        Matrix4f rbXform = allocator.getMatrix4f();

        for (int i = 0; i < constraints.size(); i++) {
            physicsWorld.removeConstraint(constraints.get(i));
        }

        for (int i = 0; i < rigidBodies.size(); i++) {
            physicsWorld.removeRigidBody(rigidBodies.get(i));
        }

        for (int i = 0; i < rigidBodies.size(); i++) {
            PmdRigidBody pmdRb = model.rigidBodies.get(i);
            if (pmdRb.boneIndex != 65535) {
                pose.getBoneTransform(pmdRb.boneIndex, boneXform);
            } else {
                int centerIndex = model.getBoneIndex("センター");
                pose.getBoneTransform(centerIndex, boneXform);
            }
            rbXform.mul(boneXform, bone2RigidBodyXforms.get(i));
            motionStates.get(i).setWorldTransform(convertToGdxMatrix(rbXform));

            rigidBodies.get(i).setMotionState(rigidBodies.get(i).getMotionState());
            rigidBodies.get(i).clearForces();
            rigidBodies.get(i).setLinearVelocity(Vector3.Zero);
            rigidBodies.get(i).setAngularVelocity(Vector3.Zero);
            Matrix4 transform = new Matrix4();
            motionStates.get(i).getWorldTransform(transform);
            rigidBodies.get(i).setWorldTransform(transform);
        }

        for (int i = 0; i < rigidBodies.size(); i++) {
            PmdRigidBody pmdRb = model.rigidBodies.get(i);
            btRigidBody rb = rigidBodies.get(i);
            short group = (short) Math.pow(2, pmdRb.groupIndex);
            physicsWorld.addRigidBody(rb, group, (short) pmdRb.hitWithGroupFlags);
        }

        for (int i = 0; i < constraints.size(); i++) {
            physicsWorld.addConstraint(constraints.get(i));
        }

        for (int i = 0; i < rigidBodies.size(); i++) {
            rigidBodies.get(i).activate();
        }

        physicsWorld.clearForces();
        physicsWorld.getConstraintSolver().reset();
        /*
        btOverlappingPairCache pairCache = physicsWorld.getBroadphase().getOverlappingPairCache();
        btBroadphasePairArray pairArray = pairCache.getOverlappingPairArray();
        for (int i = 0; i < pairArray.size(); i++)
        {
            btBroadphasePair pair = pairArray.at(i);
            pairCache.cleanOverlappingPair(pair, physicsWorld.getDispatcher());
        }
        */

        allocator.put(rbXform);
        allocator.put(boneXform);
    }

    public void dispose() {
        for (btTypedConstraint contraint : constraints) {
            physicsWorld.removeConstraint(contraint);
        }
        constraints.clear();
        for (btRigidBody rb : rigidBodies) {
            physicsWorld.removeRigidBody(rb);
        }
        rigidBodies.clear();
        physicsWorld.dispose();
        physicsWorld = null;
    }
}
