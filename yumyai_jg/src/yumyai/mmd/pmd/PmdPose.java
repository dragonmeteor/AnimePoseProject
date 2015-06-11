package yumyai.mmd.pmd;

import java.nio.FloatBuffer;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import yondoko.math.VectorMathUtil;
import yumyai.mmd.vpd.VpdPose;
import yondoko.util.ObjectAllocator;

public class PmdPose {
    final FloatBuffer boneDisplacements;
    final FloatBuffer boneRotations;
    final FloatBuffer morphWeights;
    final PmdModel model;

    public PmdPose(PmdModel model) {
        this.model = model;
        boneDisplacements = FloatBuffer.allocate(model.bones.size() * 3);
        boneRotations = FloatBuffer.allocate(model.bones.size() * 4);
        morphWeights = FloatBuffer.allocate(model.morphs.size());
    }

    public void setPoseBoneDisplacement(int boneIndex, float x, float y, float z) {
        boneDisplacements.put(boneIndex * 3 + 0, x);
        boneDisplacements.put(boneIndex * 3 + 1, y);
        boneDisplacements.put(boneIndex * 3 + 2, z);
    }

    public void setPoseBoneDisplacement(int boneIndex, Vector3f v) {
        boneDisplacements.put(boneIndex * 3 + 0, v.x);
        boneDisplacements.put(boneIndex * 3 + 1, v.y);
        boneDisplacements.put(boneIndex * 3 + 2, v.z);
    }

    public void getPoseBoneDisplacement(int boneIndex, Vector3f v) {
        v.x = boneDisplacements.get(boneIndex * 3 + 0);
        v.y = boneDisplacements.get(boneIndex * 3 + 1);
        v.z = boneDisplacements.get(boneIndex * 3 + 2);
    }

    public void setPoseBoneRotation(int boneIndex, float x, float y, float z, float w) {
        boneRotations.put(boneIndex * 4 + 0, x);
        boneRotations.put(boneIndex * 4 + 1, y);
        boneRotations.put(boneIndex * 4 + 2, z);
        boneRotations.put(boneIndex * 4 + 3, w);
    }

    public void setPoseBoneRotation(int boneIndex, Quat4f q) {
        boneRotations.put(boneIndex * 4 + 0, q.x);
        boneRotations.put(boneIndex * 4 + 1, q.y);
        boneRotations.put(boneIndex * 4 + 2, q.z);
        boneRotations.put(boneIndex * 4 + 3, q.w);
    }

    public void getPoseBoneRotation(int boneIndex, Quat4f q) {
        q.x = boneRotations.get(boneIndex * 4 + 0);
        q.y = boneRotations.get(boneIndex * 4 + 1);
        q.z = boneRotations.get(boneIndex * 4 + 2);
        q.w = boneRotations.get(boneIndex * 4 + 3);
    }

    public void setPoseMorphWeight(int morphIndex, float value) {
        morphWeights.put(morphIndex, value);
    }

    public float getPoseMorphWeight(int morphIndex) {
        return morphWeights.get(morphIndex);
    }

    public int boneCount() {
        return model.bones.size();
    }

    public int morphCount() {
        return model.morphs.size();
    }

    public void clear() {
        for (int i = 0; i < boneCount(); i++) {
            setPoseBoneDisplacement(i, 0, 0, 0);
            setPoseBoneRotation(i, 0, 0, 0, 1);
        }
        for (int i = 0; i < morphCount(); i++) {
            setPoseMorphWeight(i, 0);
        }
    }

    public void copy(VpdPose pose) {
        clear();
        if (pose == null) {
            return;
        }

        ObjectAllocator allocator = ObjectAllocator.get();

        Vector3f displacement = allocator.getVector3f();
        Quat4f rotation = allocator.getQuat4f();

        for (String boneName : pose.boneNames()) {
            pose.getBonePose(boneName, displacement, rotation);
            int index = model.getBoneIndex(boneName);
            if (index < 0) {
                continue;
            }
            setPoseBoneDisplacement(index, displacement);
            setPoseBoneRotation(index, rotation);
        }
        for (String morphName : pose.morphNames()) {
            float weight = pose.getMorphWeight(morphName);
            int index = model.getMorphIndex(morphName);
            if (index < 0) {
                continue;
            }
            setPoseMorphWeight(index, weight);
        }

        allocator.put(rotation);
        allocator.put(displacement);
    }

    public void copy(PmdPose other) {
        if (other.model != this.model) {
            throw new RuntimeException("model are not the same");
        }

        for (int i = 0; i < model.bones.size(); i++) {
            /*
            if (physics && model.bones.get(i).controlledByPhysics)
            {
                continue;
            }
            */
            boneDisplacements.put(3 * i + 0, other.boneDisplacements.get(3 * i + 0));
            boneDisplacements.put(3 * i + 1, other.boneDisplacements.get(3 * i + 1));
            boneDisplacements.put(3 * i + 2, other.boneDisplacements.get(3 * i + 2));
            boneRotations.put(4 * i + 0, other.boneRotations.get(4 * i + 0));
            boneRotations.put(4 * i + 1, other.boneRotations.get(4 * i + 1));
            boneRotations.put(4 * i + 2, other.boneRotations.get(4 * i + 2));
            boneRotations.put(4 * i + 3, other.boneRotations.get(4 * i + 3));
        }

        for (int i = 0; i < morphWeights.capacity(); i++) {
            morphWeights.put(i, other.morphWeights.get(i));
        }
    }

    public void getVertexTransform(int boneIndex, Matrix4f output) {
        ObjectAllocator allocator = ObjectAllocator.get();

        output.setIdentity();
        // Set tempVec to the negative of bone's head position.
        Vector3f negHead = allocator.getVector3f();
        negHead.set(model.bones.get(boneIndex).headPosition);
        negHead.negate();
        // Set the translation to be by the negative of bone's head position.
        output.setTranslation(negHead);

        Vector3f poseDisp = allocator.getVector3f();
        Vector3f boneDisp = allocator.getVector3f();
        Quat4f rotation = allocator.getQuat4f();
        Matrix4f xform = allocator.getMatrix4f();

        int current = boneIndex;
        while (current >= 0) {
            PmdBone bone = model.bones.get(current);

            xform.setIdentity();
            getPoseBoneRotation(current, rotation);
            xform.setRotation(rotation);

            getPoseBoneDisplacement(current, poseDisp);
            bone.getDisplacement(boneDisp);
            poseDisp.add(boneDisp);
            xform.setTranslation(poseDisp);
            output.mul(xform, output);

            current = bone.parentIndex;
        }

        allocator.put(xform);
        allocator.put(rotation);
        allocator.put(boneDisp);
        allocator.put(poseDisp);
        allocator.put(negHead);
    }

    public void getBoneTransform(int boneIndex, Matrix4f output) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Vector3f poseDisp = allocator.getVector3f();
        Vector3f boneDisp = allocator.getVector3f();
        Quat4f rotation = allocator.getQuat4f();
        Matrix4f xform = allocator.getMatrix4f();

        output.setIdentity();
        int current = boneIndex;
        while (current >= 0) {
            PmdBone bone = model.bones.get(current);

            xform.setIdentity();
            getPoseBoneRotation(current, rotation);
            xform.setRotation(rotation);
            getPoseBoneDisplacement(current, poseDisp);
            bone.getDisplacement(boneDisp);
            poseDisp.add(boneDisp);
            xform.setTranslation(poseDisp);
            output.mul(xform, output);

            current = bone.parentIndex;
        }

        allocator.put(xform);
        allocator.put(rotation);
        allocator.put(boneDisp);
        allocator.put(poseDisp);
    }

    public void getInverseBoneTransform(int boneIndex, Matrix4f output) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Vector3f poseDisp = allocator.getVector3f();
        Vector3f boneDisp = allocator.getVector3f();
        Quat4f rotation = allocator.getQuat4f();
        Matrix4f xform = allocator.getMatrix4f();

        output.setIdentity();
        int current = boneIndex;
        while (current >= 0) {
            PmdBone bone = model.bones.get(current);

            //if (current != boneIndex)
            //{
            getPoseBoneRotation(current, rotation);
            rotation.inverse();
            xform.setIdentity();
            xform.setRotation(rotation);
            output.mul(xform);
            //}

            getPoseBoneDisplacement(current, poseDisp);
            poseDisp.negate();
            bone.getDisplacement(boneDisp);
            poseDisp.sub(boneDisp);
            xform.setIdentity();
            xform.setTranslation(poseDisp);
            output.mul(xform);

            current = bone.parentIndex;
        }

        allocator.put(xform);
        allocator.put(rotation);
        allocator.put(boneDisp);
        allocator.put(poseDisp);
    }

    public void getBoneWorldPosition(int boneIndex, Point3f output) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Vector3f poseDisp = allocator.getVector3f();
        Vector3f boneDisp = allocator.getVector3f();
        Quat4f rotation = allocator.getQuat4f();
        Matrix4f xform = allocator.getMatrix4f();

        output.set(0, 0, 0);
        int current = boneIndex;
        while (current >= 0) {
            xform.setIdentity();

            PmdBone bone = model.bones.get(current);
            getPoseBoneRotation(current, rotation);
            xform.setRotation(rotation);
            xform.transform(output);

            bone.getDisplacement(boneDisp);
            output.add(boneDisp);
            getPoseBoneDisplacement(current, poseDisp);
            output.add(poseDisp);

            current = bone.parentIndex;
        }

        allocator.put(xform);
        allocator.put(rotation);
        allocator.put(boneDisp);
        allocator.put(poseDisp);
    }

    public void solveIk(boolean physics) {
        for (PmdIkChain chain : model.ikChains) {
            if (physics) {
                PmdBone bone = model.bones.get(chain.boneIndex);
                if (!bone.controlledByPhysics) {
                    solveIkChain(chain);
                }
            } else {
                solveIkChain(chain);
            }
        }
    }

    private final float TOLERANCE = 1e-4f;

    void solveIkChain(PmdIkChain chain) {
        ObjectAllocator allocator = ObjectAllocator.get();

        Point3f targetPosition = allocator.getPoint3f();
        Point3f endEffectorPosition = allocator.getPoint3f();

        getBoneWorldPosition(chain.boneIndex, targetPosition);
        getBoneWorldPosition(chain.targetBoneIndex, endEffectorPosition);

        if (targetPosition.distance(endEffectorPosition) > TOLERANCE) {
            //if (false)
            if (chain.isLeg) {
                Point3f femurPosition = allocator.getPoint3f();
                getBoneWorldPosition(chain.chainBoneIndices[1], femurPosition);
                float targetLength = femurPosition.distance(targetPosition);

                setPoseBoneRotation(chain.chainBoneIndices[0], 0, 0, 0, 1);

                /*
                 if (chain.isLeftLeg)
                 {
                 limitAngleLeftLeg(chain);
                 }
                 else if (chain.isRightLeg)
                 {
                 limitAngleRightLeg(chain);
                 }
                 */

                getBoneWorldPosition(chain.targetBoneIndex, endEffectorPosition);
                adjustBone(chain.chainBoneIndices[1], endEffectorPosition, targetPosition, chain, false);
                adjustLeg(chain, targetLength);

                allocator.put(femurPosition);
            } else {
                boolean done = false;
                for (int iterationCount = 0; iterationCount < chain.iteration; iterationCount++) {
                    for (int boneOrder = 0; boneOrder < chain.chainBoneIndices.length; boneOrder++) {
                        int boneIndex = chain.chainBoneIndices[boneOrder];
                        adjustBone(boneIndex, endEffectorPosition, targetPosition, chain, true);
                        getBoneWorldPosition(chain.targetBoneIndex, endEffectorPosition);
                        //System.out.println("targetPosition = " + targetPosition);
                        //System.out.println("endEffectorPosition = " + endEffectorPosition);
                        if (endEffectorPosition.distance(targetPosition) < TOLERANCE) {
                            done = true;
                            break;
                        }
                    }
                    if (done) {
                        break;
                    }
                }
            }
        }

        allocator.put(endEffectorPosition);
        allocator.put(targetPosition);
    }

    void limitAngleLeftLeg(PmdIkChain chain) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Quat4f q = allocator.getQuat4f();
        Vector3f angles = allocator.getVector3f();

        getPoseBoneRotation(chain.chainBoneIndices[1], q);
        VectorMathUtil.quaternionToEuler(q, angles);
        angles.x = 0;
        angles.z = 0;
        if (angles.y < -0.1f) {
            angles.y = -0.1f;
        }
        if (angles.y > Math.PI / 2) {
            angles.y = (float) (Math.PI / 2);
        }
        VectorMathUtil.eulerToQuaternion(angles, q);
        setPoseBoneRotation(chain.chainBoneIndices[1], q);

        allocator.put(angles);
        allocator.put(q);
    }

    void limitAngleRightLeg(PmdIkChain chain) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Quat4f q = allocator.getQuat4f();
        Vector3f angles = allocator.getVector3f();

        getPoseBoneRotation(chain.chainBoneIndices[1], q);
        VectorMathUtil.quaternionToEuler(q, angles);
        angles.x = 0;
        angles.z = 0;
        if (angles.y > 0.1f) {
            angles.y = -0.1f;
        }
        if (angles.y < -Math.PI / 2) {
            angles.y = (float) (-Math.PI / 2);
        }
        VectorMathUtil.eulerToQuaternion(angles, q);
        setPoseBoneRotation(chain.chainBoneIndices[1], q);

        allocator.put(angles);
        allocator.put(q);
    }

    void adjustLeg(PmdIkChain chain, float targetLength) {
        float femurLength = model.bones.get(chain.chainBoneIndices[1]).headPosition.distance(
                model.bones.get(chain.chainBoneIndices[0]).headPosition);
        float tibiaLength = model.bones.get(chain.chainBoneIndices[0]).headPosition.distance(
                model.bones.get(chain.targetBoneIndex).headPosition);
        float legLength = femurLength + tibiaLength;

        float femurAngle = (float) Math.acos(
                (targetLength * targetLength + femurLength * femurLength - tibiaLength * tibiaLength) / (2 * targetLength * femurLength));
        float kneeAngle = (float) (Math.PI - Math.acos((femurLength * femurLength + tibiaLength * tibiaLength - targetLength * targetLength) / (2 * femurLength * tibiaLength)));

        if (legLength <= targetLength) {
            return;
        }

        ObjectAllocator allocator = ObjectAllocator.get();
        Quat4f femurFix = allocator.getQuat4f();
        Quat4f kneeFix = allocator.getQuat4f();
        AxisAngle4f femurAa = allocator.getAxisAngle4f();
        AxisAngle4f kneeAa = allocator.getAxisAngle4f();
        Quat4f boneRotation = allocator.getQuat4f();

        femurAa.set(VectorMathUtil.X_AXIS, femurAngle);
        femurFix.set(femurAa);
        getPoseBoneRotation(chain.chainBoneIndices[1], boneRotation);
        femurFix.mul(boneRotation, femurFix);
        setPoseBoneRotation(chain.chainBoneIndices[1], femurFix);

        kneeAa.set(VectorMathUtil.X_AXIS, -kneeAngle);
        kneeFix.set(kneeAa);
        setPoseBoneDisplacement(chain.chainBoneIndices[0], VectorMathUtil.ZERO_VECTOR);
        setPoseBoneRotation(chain.chainBoneIndices[0], kneeFix);

        allocator.put(boneRotation);
        allocator.put(kneeAa);
        allocator.put(femurAa);
        allocator.put(kneeFix);
        allocator.put(femurFix);
    }

    void adjustBone(int boneIndex, Point3f endEffectorPosition, Point3f targetPosition, PmdIkChain chain, boolean limitAngle) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Matrix4f invBoneXform = allocator.getMatrix4f();
        getInverseBoneTransform(boneIndex, invBoneXform);

        boolean skip = false;

        Point3f endEffectInBoneSpace = allocator.getPoint3f();
        Vector3f b2e = allocator.getVector3f();
        if (!skip) {
            invBoneXform.transform(endEffectorPosition, endEffectInBoneSpace);
            b2e.set(endEffectInBoneSpace);
            if (b2e.lengthSquared() < TOLERANCE) {
                skip = true;
            } else {
                b2e.normalize();
            }
        }

        Point3f targetInBoneSpace = allocator.getPoint3f();
        Vector3f b2t = allocator.getVector3f();
        if (!skip) {
            invBoneXform.transform(targetPosition, targetInBoneSpace);
            b2t.set(targetInBoneSpace);
            if (b2t.lengthSquared() < TOLERANCE) {
                skip = true;
            }
            b2t.normalize();
        }

        float angle = 0;
        if (!skip) {
            float dotProd = b2e.dot(b2t);
            if (dotProd > 1) {
                dotProd = 1;
            }
            if (dotProd < -1) {
                dotProd = -1;
            }
            angle = (float) Math.acos(dotProd);
            if (Math.abs(angle) < TOLERANCE) {
                skip = true;
            }
        }

        if (!skip) {
            if (limitAngle) {
                if (angle > Math.PI * chain.controlWeight) {
                    angle = (float) (Math.PI * chain.controlWeight);
                }
                if (angle < -Math.PI * chain.controlWeight) {
                    angle = (float) (-Math.PI * chain.controlWeight);
                }
            }
        }

        Vector3f axis = allocator.getVector3f();
        if (!skip) {
            axis.cross(b2e, b2t);
            if (axis.lengthSquared() < TOLERANCE) {
                skip = true;
            }
            axis.normalize();
        }

        if (!skip) {
            Quat4f fix = allocator.getQuat4f();
            AxisAngle4f axisAngle = allocator.getAxisAngle4f();
            axisAngle.set(axis, angle);
            fix.set(axisAngle);
            //if (model.bones.get(boneIndex).isKnee)
            //{
            //    limitKneeAngle(fix);
            //}
            Quat4f old = allocator.getQuat4f();
            getPoseBoneRotation(boneIndex, old);
            fix.mul(old, fix);
            setPoseBoneRotation(boneIndex, fix);
            allocator.put(old);
            allocator.put(axisAngle);
            allocator.put(fix);
        }

        allocator.put(axis);
        allocator.put(targetInBoneSpace);
        allocator.put(b2t);
        allocator.put(endEffectInBoneSpace);
        allocator.put(b2e);
        allocator.put(invBoneXform);
    }

    void limitKneeAngle(Quat4f q) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Vector3f angles = allocator.getVector3f();

        VectorMathUtil.quaternionToEuler(q, angles);
        if (angles.x > 3.14159f) {
            angles.x = 3.14159f;
        }
        if (angles.x < 0.002f) {
            angles.x = 0.002f;
        }
        angles.y = 0;
        angles.z = 0;
        VectorMathUtil.eulerToQuaternion(angles, q);

        allocator.put(angles);
    }

    public PmdModel getModel() {
        return model;
    }


}
