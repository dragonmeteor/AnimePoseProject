package yumyai.mmd.pmx;

import yondoko.math.VectorMathUtil;
import yondoko.util.ObjectAllocator;
import yumyai.mmd.vpd.VpdPose;

import javax.vecmath.*;

public class PmxPose {
    final PmxModel model;
    final Vector3f[] boneDisplacements;
    final Quat4f[] boneRotations;
    final float[] morphWeights;

    public PmxPose(PmxModel model) {
        this.model = model;
        boneDisplacements = new Vector3f[model.getBoneCount()];
        boneRotations = new Quat4f[model.getBoneCount()];
        for (int i = 0; i < model.getBoneCount(); i++) {
            boneDisplacements[i] = new Vector3f();
            boneRotations[i] = new Quat4f();
        }
        morphWeights = new float[model.getMorphCount()];
    }

    public PmxModel getModel() {
        return model;
    }

    public void clear() {
        for (int i = 0; i < model.getBoneCount(); i++)
        {
            boneDisplacements[i].set(0,0,0);
            boneRotations[i].set(0,0,0,1);
        }
        for (int i = 0; i < model.getMorphCount(); i++) {
            morphWeights[i] = 0;
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
            if (model.hasBone(boneName)) {
                pose.getBonePose(boneName, displacement, rotation);
                PmxBone bone = model.getBone(boneName);
                boneDisplacements[bone.boneIndex].set(displacement);
                boneRotations[bone.boneIndex].set(rotation);
            }
        }
        for (String morphName : pose.morphNames()) {
            if (model.hasMorph(morphName)) {
                float weight = pose.getMorphWeight(morphName);
                PmxMorph morph = model.getMorph(morphName);
                morphWeights[morph.morphIndex] = weight;
            }
        }

        //copyBonePoses();

        allocator.put(rotation);
        allocator.put(displacement);
    }

    public void copy(PmxPose other) {
        if (other.model != this.model) {
            throw new RuntimeException("model are not the same");
        }
        for (int i = 0; i < model.getBoneCount(); i++) {
            boneDisplacements[i].set(other.boneDisplacements[i]);
            boneRotations[i].set(other.boneRotations[i]);
        }
        for (int i = 0; i < model.getMorphCount(); i++) {
            morphWeights[i] = other.morphWeights[i];
        }
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
            PmxBone bone = model.getBone(current);

            xform.setIdentity();
            if (!VectorMathUtil.isNaN(boneRotations[current]))
                xform.setRotation(boneRotations[current]);
            if (!VectorMathUtil.isNaN(boneDisplacements[current]))
                boneDisp.set(boneDisplacements[current]);
            boneDisp.add(bone.displacementFromParent);
            xform.setTranslation(boneDisp);
            output.mul(xform, output);

            if (current == bone.parentIndex)
                break;
            current = bone.parentIndex;
        }

        allocator.put(xform);
        allocator.put(rotation);
        allocator.put(boneDisp);
        allocator.put(poseDisp);
    }

    public void getBoneWorldPosition(int boneIndex, Tuple3f output) {
        if (boneIndex < 0)
            output.set(0,0,0);

        ObjectAllocator allocator = ObjectAllocator.get();
        Vector3f poseDisp = allocator.getVector3f();
        Vector3f boneDisp = allocator.getVector3f();
        Quat4f rotation = allocator.getQuat4f();
        Point3f pos = allocator.getPoint3f();
        Matrix4f xform = allocator.getMatrix4f();

        pos.set(0, 0, 0);
        int current = boneIndex;
        while (current >= 0) {
            PmxBone bone = model.getBone(current);

            xform.setIdentity();
            xform.setRotation(boneRotations[current]);
            xform.transform(pos);
            pos.add(boneDisplacements[current]);
            pos.add(bone.displacementFromParent);

            if (current == bone.parentIndex)
                break;
            current = bone.parentIndex;
        }

        output.set(pos);

        allocator.put(pos);
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
            PmxBone bone = model.getBone(current);

            rotation.set(boneRotations[current]);
            rotation.inverse();
            xform.setIdentity();
            xform.setRotation(rotation);
            output.mul(xform);

            poseDisp.set(boneDisplacements[current]);
            poseDisp.negate();
            poseDisp.sub(bone.displacementFromParent);
            xform.setIdentity();
            xform.setTranslation(poseDisp);
            output.mul(xform);

            if (current == bone.parentIndex)
                break;
            current = bone.parentIndex;
        }

        allocator.put(xform);
        allocator.put(rotation);
        allocator.put(boneDisp);
        allocator.put(poseDisp);
    }

    public float getMorphWeights(int i) {
        return morphWeights[i];
    }

    public void getBoneWorldRotation(int boneIndex, Quat4f output) {
        output.set(0,0,0,1);
        if (boneIndex == -1) {
            output.set(0, 0, 0, 1);
            return;
        }
        int current = boneIndex;
        while (current >= 0) {
            PmxBone bone = model.getBone(current);
            output.mul(boneRotations[current], output);
            if (current == bone.parentIndex)
                break;
            current = bone.parentIndex;
        }
    }

    public void copyBonePose(int boneIndex) {
        PmxBone bone = model.getBone(boneIndex);
        if (bone.copyRotation() && bone.copyParentBoneIndex >= 0) {
            Quat4f copyParentBoneRotation = new Quat4f();
            Quat4f identity = new Quat4f(0,0,0,1);
            copyParentBoneRotation.interpolate(identity, boneRotations[bone.copyParentBoneIndex], bone.copyRate);
            boneRotations[bone.boneIndex].mul(copyParentBoneRotation);
        }
        if (bone.copyTranslation()  && bone.copyParentBoneIndex >= 0) {
            Point3f copyParentBonePosition = new Point3f();
            copyParentBonePosition.scale(bone.copyRate, boneDisplacements[bone.copyParentBoneIndex]);
            boneDisplacements[bone.boneIndex].add(copyParentBonePosition);
        }
    }

    public void copyBonePoses() {
        Quat4f currentBoneRotation = new Quat4f();
        Quat4f parentBoneRotation = new Quat4f();
        Quat4f copyParentBoneRotation = new Quat4f();

        Point3f currentBonePosition = new Point3f();
        Point3f parentBonePosition = new Point3f();
        Point3f copyParentBonePosition = new Point3f();

        Matrix4f parentTransform = new Matrix4f();
        AxisAngle4f axisAngle = new AxisAngle4f();
        Quat4f identity = new Quat4f(0,0,0,1);

        for (int i = 0; i < model.getBoneCount(); i++) {
            PmxBone bone = model.getBone(i);
            if (bone.copyRotation() && bone.copyParentBoneIndex >= 0) {
                copyParentBoneRotation.interpolate(identity, boneRotations[bone.copyParentBoneIndex], bone.copyRate);
                boneRotations[bone.boneIndex].mul(copyParentBoneRotation);

                /*
                getBoneWorldRotation(bone.boneIndex, currentBoneRotation);
                getBoneWorldRotation(bone.parentIndex, parentBoneRotation);
                getBoneWorldRotation(bone.copyParentBoneIndex, copyParentBoneRotation);
                if (bone.copyRate < 0) {
                    axisAngle.set(copyParentBoneRotation);
                    if (bone.copyRate < -1)
                        axisAngle.angle *= 1;
                    else
                        axisAngle.angle *= Math.abs(bone.copyRate);
                    copyParentBoneRotation.set(axisAngle);
                    currentBoneRotation.mul(copyParentBoneRotation);
                } else {
                    currentBoneRotation.interpolate(currentBoneRotation, copyParentBoneRotation,
                            Math.min(Math.abs(bone.copyRate), 1));
                }
                parentBoneRotation.inverse();
                boneRotations[bone.boneIndex].mul(parentBoneRotation, currentBoneRotation);
                */
            }
            if (bone.copyTranslation()  && bone.copyParentBoneIndex >= 0) {
                getBoneWorldPosition(bone.boneIndex, currentBonePosition);
                getBoneWorldPosition(bone.parentIndex, parentBonePosition);
                getBoneWorldPosition(bone.copyParentBoneIndex, copyParentBonePosition);

                currentBonePosition.scale(1-bone.copyRate);
                currentBonePosition.scaleAdd(bone.copyRate, copyParentBonePosition, currentBonePosition);

                getBoneTransform(bone.parentIndex, parentTransform);
                parentTransform.invert();
                parentTransform.transform(currentBonePosition);
                currentBonePosition.sub(bone.displacementFromParent);

                boneDisplacements[bone.boneIndex].set(currentBonePosition);
            }
        }
    }

    public static void interpolate(PmxPose p0, PmxPose p1, float alpha, PmxPose out) {
        if (p1.getModel() != p0.getModel() || p1.getModel() != out.getModel()) {
            throw new RuntimeException("poses to interpolate must be the same!");
        }
        for (int i = 0; i < p0.boneDisplacements.length; i++) {
            out.boneDisplacements[i].scale(1-alpha, p0.boneDisplacements[i]);
            out.boneDisplacements[i].scaleAdd(alpha, p1.boneDisplacements[i], out.boneDisplacements[i]);
            out.boneRotations[i].interpolate(p0.boneRotations[i], p1.boneRotations[i], alpha);
        }
        for (int i = 0; i < p0.morphWeights.length; i++) {
            out.morphWeights[i] = p0.morphWeights[i]*(1-alpha) + p1.morphWeights[i]*alpha;
        }
    }
}
