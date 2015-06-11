package yumyai.mmd.pmx;

import yondoko.math.Util;
import yondoko.math.VectorMathUtil;
import yondoko.util.ObjectAllocator;

import javax.vecmath.*;

public class PmxIkSolver {
    public void solve(PmxPose pose) {
        PmxModel model = pose.getModel();
        for (int i = 0; i < model.getBoneCount(); i++) {
            PmxBone targetBone = model.getBone(i);
            if (!targetBone.isIk())
                continue;
            if (targetBone.ikTargetBoneIndex < 0)
                continue;
            for (int j = 0; j < targetBone.ikLoopCount; j++) {
                for (int k = 0; k < targetBone.ikLinks.size(); k++) {
                    solve(pose, targetBone, k);
                }
            }
        }
    }

    public void solve(PmxPose pose, int boneIndex) {
        PmxModel model = pose.getModel();
        PmxBone targetBone = model.getBone(boneIndex);
        if (!targetBone.isIk())
            return;
        if (targetBone.ikTargetBoneIndex < 0)
            return;
        for (int j = 0; j < targetBone.ikLoopCount; j++) {
            for (int k = 0; k < targetBone.ikLinks.size(); k++) {
                solve(pose, targetBone, k);
            }
        }
    }

    public void solve(PmxPose pose, PmxBone targetBone, int k) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Point3f effectorGlobal = allocator.getPoint3f();
        Point3f targetGlobal = allocator.getPoint3f();
        Matrix4f boneInverseXform = allocator.getMatrix4f();
        Point3f targetLocal = allocator.getPoint3f();
        Point3f effectorLocal = allocator.getPoint3f();
        Vector3f boneToTarget = allocator.getVector3f();
        Vector3f boneToEffector = allocator.getVector3f();
        Vector3f rotationAxis = allocator.getVector3f();
        Quat4f rotation = allocator.getQuat4f();
        AxisAngle4f axisAngle = allocator.getAxisAngle4f();
        Vector3f axisRot = allocator.getVector3f();

        PmxModel model = pose.getModel();
        PmxBone effectorBone = model.getBone(targetBone.ikTargetBoneIndex);
        pose.getBoneWorldPosition(targetBone.boneIndex, targetGlobal);

        pose.getBoneWorldPosition(effectorBone.boneIndex, effectorGlobal);
        PmxIkLink ikLink = targetBone.ikLinks.get(k);
        PmxBone ikLinkBone = model.getBone(ikLink.boneIndex);

        pose.getInverseBoneTransform(ikLink.boneIndex, boneInverseXform);
        boneInverseXform.transform(effectorGlobal, effectorLocal);
        boneToEffector.set(effectorLocal);
        boneToEffector.normalize();
        boneInverseXform.transform(targetGlobal, targetLocal);
        boneToTarget.set(targetLocal);
        boneToTarget.normalize();

        float dot = boneToTarget.dot(boneToEffector);
        if (dot > 1) dot = 1;
        float rotationAngle = Util.clamp((float) Math.acos(dot),
                -targetBone.ikAngleLimit, targetBone.ikAngleLimit);
        if (Float.isNaN(rotationAngle)) {
            allocator.put(axisRot);
            allocator.put(axisAngle);
            allocator.put(rotation);
            allocator.put(rotationAxis);
            allocator.put(boneToEffector);
            allocator.put(boneToTarget);
            allocator.put(effectorLocal);
            allocator.put(targetLocal);
            allocator.put(boneInverseXform);
            allocator.put(targetGlobal);
            allocator.put(effectorGlobal);
            return;
        }
        if (Math.abs(rotationAngle) < 1e-3) {
            allocator.put(axisRot);
            allocator.put(axisAngle);
            allocator.put(rotation);
            allocator.put(rotationAxis);
            allocator.put(boneToEffector);
            allocator.put(boneToTarget);
            allocator.put(effectorLocal);
            allocator.put(targetLocal);
            allocator.put(boneInverseXform);
            allocator.put(targetGlobal);
            allocator.put(effectorGlobal);
            return;
        }

        rotationAxis.cross(boneToEffector, boneToTarget);
        rotationAxis.normalize();
        axisAngle.set(rotationAxis.x, rotationAxis.y, rotationAxis.z, rotationAngle);
        rotation.set(axisAngle);
        pose.boneRotations[ikLink.boneIndex].mul(rotation);

        if (!ikLink.angleLimited) {
            allocator.put(axisRot);
            allocator.put(axisAngle);
            allocator.put(rotation);
            allocator.put(rotationAxis);
            allocator.put(boneToEffector);
            allocator.put(boneToTarget);
            allocator.put(effectorLocal);
            allocator.put(targetLocal);
            allocator.put(boneInverseXform);
            allocator.put(targetGlobal);
            allocator.put(effectorGlobal);
            return;
        }

        int rotationType;
        if (VectorMathUtil.factorQuaternionXYZ(pose.boneRotations[ikLink.boneIndex], axisRot))
            rotationType = 0;
        else if (VectorMathUtil.factorQuaternionYZX(pose.boneRotations[ikLink.boneIndex], axisRot))
            rotationType = 1;
        else {
            VectorMathUtil.factorQuaternionZXY(pose.boneRotations[ikLink.boneIndex], axisRot);
            rotationType = 2;
        }
        VectorMathUtil.normalizeEulerAngle(axisRot);
        Util.clamp(axisRot, ikLink.angleLowerBound, ikLink.angleUpperBound);
        switch (rotationType) {
            case 0:
                VectorMathUtil.eulerXYZToQuaternion(axisRot, pose.boneRotations[ikLink.boneIndex]);
                break;
            case 1:
                VectorMathUtil.eulerYZXToQuaternion(axisRot, pose.boneRotations[ikLink.boneIndex]);
                break;
            case 2:
                VectorMathUtil.eulerZXYToQuaternion(axisRot, pose.boneRotations[ikLink.boneIndex]);
                break;
        }

        allocator.put(axisRot);
        allocator.put(axisAngle);
        allocator.put(rotation);
        allocator.put(rotationAxis);
        allocator.put(boneToEffector);
        allocator.put(boneToTarget);
        allocator.put(effectorLocal);
        allocator.put(targetLocal);
        allocator.put(boneInverseXform);
        allocator.put(targetGlobal);
        allocator.put(effectorGlobal);
    }

    public void solve(PmxPose pose, boolean verbose) {
        ObjectAllocator allocator = ObjectAllocator.get();
        Point3f effectorGlobal = allocator.getPoint3f();
        Point3f targetGlobal = allocator.getPoint3f();
        Matrix4f boneInverseXform = allocator.getMatrix4f();
        Point3f targetLocal = allocator.getPoint3f();
        Point3f effectorLocal = allocator.getPoint3f();
        Vector3f boneToTarget = allocator.getVector3f();
        Vector3f boneToEffector = allocator.getVector3f();
        Vector3f rotationAxis = allocator.getVector3f();
        Quat4f rotation = allocator.getQuat4f();
        AxisAngle4f axisAngle = allocator.getAxisAngle4f();
        Vector3f axisRot = allocator.getVector3f();

        PmxModel model = pose.getModel();
        for (int i = 0; i < model.getBoneCount(); i++) {
            PmxBone targetBone = model.getBone(i);
            if (!targetBone.isIk())
                continue;
            if (targetBone.ikTargetBoneIndex < 0)
                continue;

            PmxBone effectorBone = model.getBone(targetBone.ikTargetBoneIndex);
            pose.getBoneWorldPosition(targetBone.boneIndex, targetGlobal);

            if (verbose) {
                System.out.println("============================");
                System.out.println("targetBone = " + targetBone.japaneseName);
                System.out.println("effectorBone = " + effectorBone.japaneseName);
                System.out.println("loopCount = " + targetBone.ikLoopCount);
                System.out.println("targetGlobal = " + targetGlobal);
            }
            for (int j = 0; j < targetBone.ikLoopCount; j++) {
                if (verbose) {
                    System.out.println("-------------------------");
                    System.out.println("Iteration " + j);
                }
                for (int k = 0; k < targetBone.ikLinks.size(); k++) {
                    pose.getBoneWorldPosition(effectorBone.boneIndex, effectorGlobal);
                    PmxIkLink ikLink = targetBone.ikLinks.get(k);
                    PmxBone ikLinkBone = model.getBone(ikLink.boneIndex);

                    pose.getInverseBoneTransform(ikLink.boneIndex, boneInverseXform);
                    boneInverseXform.transform(effectorGlobal, effectorLocal);
                    boneToEffector.set(effectorLocal);
                    boneToEffector.normalize();
                    boneInverseXform.transform(targetGlobal, targetLocal);
                    boneToTarget.set(targetLocal);
                    boneToTarget.normalize();

                    float dot = boneToTarget.dot(boneToEffector);
                    if (dot > 1) dot = 1;
                    float rotationAngle = Util.clamp((float) Math.acos(dot),
                            -targetBone.ikAngleLimit, targetBone.ikAngleLimit);
                    if (verbose) {
                        System.out.println();
                        System.out.println("targetGlobal = " + targetGlobal);
                        System.out.println("effectorGlobal = " + effectorGlobal);
                        System.out.println("ikLinkBone = " + ikLinkBone.japaneseName);
                        System.out.println("boneInverseXform:");
                        System.out.print(boneInverseXform);
                        System.out.println("boneToEffector = " + boneToEffector);
                        System.out.println("boneToTarget = " + boneToTarget);
                        System.out.println("dot = " + dot);
                        System.out.println("rotationAngle = " + (rotationAngle * 180 / Math.PI));
                        System.out.println("pose.boneRotations[" + ikLinkBone.japaneseName + "] (before) = " + pose.boneRotations[ikLink.boneIndex]);
                    }

                    if (Float.isNaN(rotationAngle)) continue;
                    if (Math.abs(rotationAngle) < 1e-3) continue;

                    rotationAxis.cross(boneToEffector, boneToTarget);
                    rotationAxis.normalize();
                    axisAngle.set(rotationAxis.x, rotationAxis.y, rotationAxis.z, rotationAngle);
                    rotation.set(axisAngle);
                    pose.boneRotations[ikLink.boneIndex].mul(rotation);

                    if (verbose) {
                        System.out.println("rotationAxis = " + rotationAxis);
                        System.out.println("rotation = " + rotation);
                        System.out.println("pose.boneRotations[" + ikLinkBone.japaneseName + "] (after) = " + pose.boneRotations[ikLink.boneIndex]);

                        Quat4f q = new Quat4f();
                        q.set(axisAngle);
                        Quat4f pp = new Quat4f(boneToEffector.x, boneToEffector.y, boneToEffector.z, 0);
                        Quat4f qq = new Quat4f();
                        qq.set(q);
                        qq.mul(pp);
                        q.inverse();
                        qq.mul(q);
                        System.out.println("rotated boneToEffector = " + qq);
                    }

                    if (!ikLink.angleLimited) continue;

                    int rotationType;
                    if (VectorMathUtil.factorQuaternionXYZ(pose.boneRotations[ikLink.boneIndex], axisRot))
                        rotationType = 0;
                    else if (VectorMathUtil.factorQuaternionYZX(pose.boneRotations[ikLink.boneIndex], axisRot))
                        rotationType = 1;
                    else {
                        VectorMathUtil.factorQuaternionZXY(pose.boneRotations[ikLink.boneIndex], axisRot);
                        rotationType = 2;
                    }

                    if (verbose) {
                        System.out.println("rotationType = " + rotationType);
                        System.out.println("axisRot = " + axisRot);
                    }

                    VectorMathUtil.normalizeEulerAngle(axisRot);

                    if (verbose) {
                        System.out.println("axisRot (after normalizing) = " + axisRot);
                    }

                    Util.clamp(axisRot, ikLink.angleLowerBound, ikLink.angleUpperBound);

                    if (verbose) {
                        System.out.println("axisRot (after clampling) = " + axisRot);
                    }

                    switch (rotationType) {
                        case 0:
                            VectorMathUtil.eulerXYZToQuaternion(axisRot, pose.boneRotations[ikLink.boneIndex]);
                            break;
                        case 1:
                            VectorMathUtil.eulerYZXToQuaternion(axisRot, pose.boneRotations[ikLink.boneIndex]);
                            break;
                        case 2:
                            VectorMathUtil.eulerZXYToQuaternion(axisRot, pose.boneRotations[ikLink.boneIndex]);
                            break;
                    }
                }
            }
        }

        allocator.put(axisRot);
        allocator.put(axisAngle);
        allocator.put(rotation);
        allocator.put(rotationAxis);
        allocator.put(boneToEffector);
        allocator.put(boneToTarget);
        allocator.put(effectorLocal);
        allocator.put(targetLocal);
        allocator.put(boneInverseXform);
        allocator.put(targetGlobal);
        allocator.put(effectorGlobal);
    }
}
