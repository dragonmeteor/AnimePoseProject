package yumyai.mmd.pmx;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btGeneric6DofSpringConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.GdxNativesLoader;
import yondoko.math.VectorMathUtil;
import yondoko.util.ObjectAllocator;
import yumyai.mmd.pmd.PmdRigidBodyShapeType;
import yumyai.mmd.pmd.PmdRigidBodyType;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

public class PmxModelPhysics {
    public PmxModel model;
    public btDiscreteDynamicsWorld dynamicsWorld;
    public btDefaultCollisionConfiguration collisionConfiguration;
    public btCollisionDispatcher dispatcher;
    public btBroadphaseInterface overlappingPairCache;
    public btSequentialImpulseConstraintSolver solver;
    public Matrix4f[] rb2Bone;
    public Matrix4f[] bone2Rb;
    public btRigidBody[] rigidBodies;
    public btGeneric6DofSpringConstraint[] constraints;
    public btCollisionShape[] collisionShapes;

    static {
        GdxNativesLoader.load();
        Bullet.init();
    }

    public PmxModelPhysics(PmxModel model) {
        this.model = model;
        createDynamicsWorld();
        createRigidBodies();
        createConstraints();
    }

    private void createDynamicsWorld() {
        collisionConfiguration = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfiguration);
        overlappingPairCache = new btDbvtBroadphase();
        solver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, overlappingPairCache, solver, collisionConfiguration);
        dynamicsWorld.setGravity(new Vector3(0, -9.8f*10.0f, 0));
    }

    public void setGravity(float x, float y, float z) {
        dynamicsWorld.setGravity(new Vector3(x, y, z));
    }

    public void setGravity(Vector3f gravity) {
        dynamicsWorld.setGravity(new Vector3(gravity.x, gravity.y, gravity.z));
    }

    public void dispose() {
        for (int i = 0; i < model.getJointCount(); i++) {
            dynamicsWorld.removeConstraint(constraints[i]);
            constraints[i].dispose();
            constraints[i] = null;
        }
        for (int i = 0; i < model.getRigidBodyCount(); i++) {
            dynamicsWorld.removeRigidBody(rigidBodies[i]);
            if (rigidBodies[i].getMotionState() != null) {
                rigidBodies[i].getMotionState().dispose();
            }
            rigidBodies[i].dispose();
            rigidBodies[i] = null;
            collisionShapes[i].dispose();
            collisionShapes[i] = null;
        }

        dynamicsWorld.dispose();
        solver.dispose();
        overlappingPairCache.dispose();
        dispatcher.dispose();
        collisionConfiguration.dispose();

        solver = null;
        overlappingPairCache = null;
        dispatcher = null;
        collisionConfiguration = null;
        dynamicsWorld = null;
    }

    private static Matrix4 convertToGdxMatrix(Matrix4f m) {
        Matrix4 result = new Matrix4();
        float[] mm = VectorMathUtil.matrixToArrayColumnMajor(m);
        result.set(mm);
        return result;
    }

    private void createRigidBodies() {
        ObjectAllocator allocator = ObjectAllocator.get();
        Matrix4f rbMatrix = allocator.getMatrix4f();
        Quat4f rotation = allocator.getQuat4f();
        Vector3f translation = allocator.getVector3f();

        rb2Bone = new Matrix4f[model.getRigidBodyCount()];
        bone2Rb = new Matrix4f[model.getRigidBodyCount()];
        rigidBodies = new btRigidBody[model.getRigidBodyCount()];
        collisionShapes = new btCollisionShape[model.getRigidBodyCount()];
        for (int i = 0; i < model.getRigidBodyCount(); i++) {
            PmxRigidBody pmxRb = model.getRigidBody(i);

            rb2Bone[i] = new Matrix4f();
            rb2Bone[i].setIdentity();
            VectorMathUtil.yawPitchRollToQuaternion(
                    pmxRb.rotation.y, pmxRb.rotation.x, pmxRb.rotation.z, rotation);
            rb2Bone[i].setRotation(rotation);
            translation.set(pmxRb.position);
            if (pmxRb.boneIndex >= 0) {
                translation.sub(model.getBone(pmxRb.boneIndex).position);
            } else {
                //translation.sub(model.getBone(0).position);
            }
            rb2Bone[i].setTranslation(translation);
            bone2Rb[i] = new Matrix4f();
            bone2Rb[i].invert(rb2Bone[i]);

            btCollisionShape collisionShape = null;
            if (pmxRb.shape == PmdRigidBodyShapeType.Sphere) {
                collisionShape = new btSphereShape(pmxRb.width);
            } else if (pmxRb.shape == PmdRigidBodyShapeType.Box) {
                collisionShape = new btBoxShape(new Vector3(pmxRb.width, pmxRb.height, pmxRb.depth));
            } else if (pmxRb.shape == PmdRigidBodyShapeType.Capsule) {
                collisionShape = new btCapsuleShape(pmxRb.width, pmxRb.height);
            } else {
                throw new RuntimeException("Invalid rigid body shape type");
            }
            collisionShapes[i] = collisionShape;

            float mass = (pmxRb.type == PmdRigidBodyType.FollowBone) ? 0 : pmxRb.mass;
            Vector3 localInertia = new Vector3();
            collisionShape.calculateLocalInertia(mass, localInertia);

            rbMatrix.setIdentity();
            /*
            if (pmxRb.boneIndex >= 0)
                translation.set(model.getBone(pmxRb.boneIndex).position);
            else {
                translation.sub(model.getBone(0).position);
            }
            rbMatrix.setTranslation(translation);
            rbMatrix.mul(rb2Bone[i]);
            */
            rbMatrix.setTranslation(pmxRb.position);
            rbMatrix.setRotation(rotation);
            btDefaultMotionState motionState = new btDefaultMotionState(convertToGdxMatrix(rbMatrix));

            btRigidBody rigidBody = new btRigidBody(mass, motionState, collisionShape, localInertia);
            rigidBody.setRestitution(pmxRb.restitution);
            rigidBody.setFriction(pmxRb.friction);
            rigidBody.setDamping(pmxRb.positionDamping, pmxRb.rotationDamping);
            if (pmxRb.type == PmdRigidBodyType.FollowBone) {
                rigidBody.setActivationState(Collision.DISABLE_DEACTIVATION);
                rigidBody.setCollisionFlags(btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT | rigidBody.getCollisionFlags());
            }
            short group = (short) Math.pow(2, pmxRb.groupIndex);
            dynamicsWorld.addRigidBody(rigidBody, group, (short) pmxRb.hitWithGroupFlags);
            rigidBodies[i] = rigidBody;
        }

        allocator.put(translation);
        allocator.put(rotation);
        allocator.put(rbMatrix);
    }

    private void createConstraints() {
        ObjectAllocator allocator = ObjectAllocator.get();
        Matrix4f bodyAWorldInv = allocator.getMatrix4f();
        Matrix4f bodyBWorldInv = allocator.getMatrix4f();
        Matrix4f xformA = allocator.getMatrix4f();
        Matrix4f xformB = allocator.getMatrix4f();

        constraints = new btGeneric6DofSpringConstraint[model.getJointCount()];
        for (int i = 0; i < model.getJointCount(); i++) {
            PmxJoint pmxJoint = model.getJoint(i);

            int bodyAIndex = pmxJoint.rigidBodies[0];
            if (bodyAIndex < 0)
                continue;
            btRigidBody bodyA = rigidBodies[bodyAIndex];
            VectorMathUtil.gdxToMatrix4f(bodyA.getWorldTransform(), bodyAWorldInv);
            bodyAWorldInv.invert();

            int bodyBIndex = pmxJoint.rigidBodies[1];
            if (bodyBIndex < 0)
                continue;
            btRigidBody bodyB = rigidBodies[bodyBIndex];
            VectorMathUtil.gdxToMatrix4f(bodyB.getWorldTransform(), bodyBWorldInv);
            bodyBWorldInv.invert();

            Quat4f jointRotation =  new Quat4f();
            VectorMathUtil.yawPitchRollToQuaternion(
                    pmxJoint.rotation.y, pmxJoint.rotation.x, pmxJoint.rotation.z, jointRotation);
            Matrix4f jointXform4f = new Matrix4f();
            jointXform4f.setIdentity();
            jointXform4f.setRotation(jointRotation);
            jointXform4f.setTranslation(pmxJoint.position);

            xformA.mul(bodyAWorldInv, jointXform4f);
            xformB.mul(bodyBWorldInv, jointXform4f);

            btGeneric6DofSpringConstraint constraint = new btGeneric6DofSpringConstraint(
                    bodyA, bodyB, convertToGdxMatrix(xformA), convertToGdxMatrix(xformB), true);

            constraint.setLinearLowerLimit(new Vector3(
                    pmxJoint.linearLowerLimit.x, pmxJoint.linearLowerLimit.y, pmxJoint.linearLowerLimit.z));
            constraint.setLinearUpperLimit(new Vector3(
                    pmxJoint.linearUpperLimit.x, pmxJoint.linearUpperLimit.y, pmxJoint.linearUpperLimit.z));

            constraint.setAngularLowerLimit(new Vector3(
                    pmxJoint.angularLowerLimit.x, pmxJoint.angularLowerLimit.y, pmxJoint.angularLowerLimit.z));
            constraint.setAngularUpperLimit(new Vector3(
                    pmxJoint.angularUpperLimit.x, pmxJoint.angularUpperLimit.y, pmxJoint.angularUpperLimit.z));

            for (int j = 0; j < 3; j++) {
                constraint.setStiffness(j, pmxJoint.springLinearStiffness[j]);
                constraint.enableSpring(j, true);
                constraint.setStiffness(j + 3, pmxJoint.springAngularStiffness[j]);
                constraint.enableSpring(j + 3, true);
            }

            constraint.calculateTransforms();
            constraint.setEquilibriumPoint();
            dynamicsWorld.addConstraint(constraint);
            constraints[i] = constraint;
        }

        allocator.put(xformB);
        allocator.put(xformA);
        allocator.put(bodyBWorldInv);
        allocator.put(bodyAWorldInv);
    }

    public void setPose(PmxPose pose) {
        if (pose.model != model) {
            throw new RuntimeException("pose's model is not the same as physics's model");
        }

        ObjectAllocator allocator = ObjectAllocator.get();
        Matrix4f boneXform = allocator.getMatrix4f();
        Matrix4f rbXform = allocator.getMatrix4f();

        for (int i = 0; i < rigidBodies.length; i++) {
            PmxRigidBody pmxRb = model.getRigidBody(i);
            if (pmxRb.type == PmdRigidBodyType.FollowBone) {
                pose.getBoneTransform(pmxRb.boneIndex, boneXform);
                rbXform.mul(boneXform, rb2Bone[i]);
                Matrix4 gdx = convertToGdxMatrix(rbXform);
                rigidBodies[i].getMotionState().setWorldTransform(gdx);
            }
        }

        allocator.put(rbXform);
        allocator.put(boneXform);
    }

    public void update(PmxPose pose, float elapsedTimeInSeconds) {
        setPose(pose);
        dynamicsWorld.stepSimulation(elapsedTimeInSeconds);
        getPose(pose);
        alignRigidBodies(pose);
    }

    private void getPose(PmxPose pose) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Matrix4f boneXform = allocator.getMatrix4f();
        Matrix4f parentBoneXformInv = allocator.getMatrix4f();
        Matrix4f rbXform = allocator.getMatrix4f();
        Vector3f translation = allocator.getVector3f();
        Quat4f rotation = allocator.getQuat4f();
        Matrix4 gdxWorldXform = new Matrix4();

        for (int i = 0; i < rigidBodies.length; i++) {
            PmxRigidBody pmxRb = model.getRigidBody(i);
            if (pmxRb.boneIndex == -1) continue;
            if (pmxRb.type == PmdRigidBodyType.FollowBone) continue;
            PmxBone bone = model.getBone(pmxRb.boneIndex);

            rigidBodies[i].getMotionState().getWorldTransform(gdxWorldXform);
            if (Float.isNaN(gdxWorldXform.val[Matrix4.M00])) continue;
            VectorMathUtil.gdxToMatrix4f(gdxWorldXform, rbXform);

            if (bone.parentIndex != -1) {
                pose.getInverseBoneTransform(bone.parentIndex, parentBoneXformInv);
            } else {
                parentBoneXformInv.setIdentity();
            }

            boneXform.set(parentBoneXformInv);
            boneXform.mul(rbXform);
            boneXform.mul(bone2Rb[i]);

            boneXform.get(translation);
            boneXform.get(rotation);
            translation.sub(bone.displacementFromParent);

            pose.boneDisplacements[pmxRb.boneIndex].set(translation);
            pose.boneRotations[pmxRb.boneIndex].set(rotation);
        }

        allocator.put(rotation);
        allocator.put(translation);
        allocator.put(rbXform);
        allocator.put(parentBoneXformInv);
        allocator.put(boneXform);
    }

    private void alignRigidBodies(PmxPose pose) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Point3f bonePosition = allocator.getPoint3f();
        Point3f rbPosition = allocator.getPoint3f();
        Matrix4 m = new Matrix4();

        for (int i = 0; i < rigidBodies.length; i++) {
            PmxRigidBody pmxRb = model.getRigidBody(i);
            if (pmxRb.boneIndex != -1 && pmxRb.type == PmdRigidBodyType.PhysicsWithBone) {
                pose.getBoneWorldPosition(pmxRb.boneIndex, bonePosition);
                rbPosition.set(pmxRb.position);
                rbPosition.sub(model.getBone(pmxRb.boneIndex).position);
                rbPosition.add(bonePosition);
                rigidBodies[i].getMotionState().getWorldTransform(m);
                m.setTranslation(rbPosition.x, rbPosition.y, rbPosition.z);
                rigidBodies[i].getMotionState().setWorldTransform(m);
            }
        }

        allocator.put(rbPosition);
        allocator.put(bonePosition);
    }

    public void resetPhysicsWithPose(PmxPose pose) {
        if (pose.model != model) {
            throw new RuntimeException("pose's model is not the same as physics's model");
        }

        ObjectAllocator allocator = ObjectAllocator.get();
        Matrix4f boneXform = allocator.getMatrix4f();
        Matrix4f rbXform = allocator.getMatrix4f();

        for (int i = 0; i < constraints.length; i++) {
            dynamicsWorld.removeConstraint(constraints[i]);
        }

        for (int i = 0; i < rigidBodies.length; i++) {
            dynamicsWorld.removeRigidBody(rigidBodies[i]);
        }

        for (int i = 0; i < rigidBodies.length; i++) {
            rigidBodies[i].getMotionState().setWorldTransform(convertToGdxMatrix(rb2Bone[i]));
            rigidBodies[i].clearForces();
            rigidBodies[i].setLinearVelocity(Vector3.Zero);
            rigidBodies[i].setAngularVelocity(Vector3.Zero);
        }

        for (int i = 0; i < rigidBodies.length; i++) {
            PmxRigidBody pmxRb = model.getRigidBody(i);
            btRigidBody rb = rigidBodies[i];
            short group = (short) Math.pow(2, pmxRb.groupIndex);
            dynamicsWorld.addRigidBody(rb, group, (short) pmxRb.hitWithGroupFlags);
        }

        for (int i = 0; i < constraints.length; i++) {
            dynamicsWorld.addConstraint(constraints[i]);
        }

        for (int i = 0; i < rigidBodies.length; i++) {
            rigidBodies[i].activate();
        }

        dynamicsWorld.clearForces();
        dynamicsWorld.getConstraintSolver().reset();
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
}
