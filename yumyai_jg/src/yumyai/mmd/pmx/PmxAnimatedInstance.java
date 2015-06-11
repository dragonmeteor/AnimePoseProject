package yumyai.mmd.pmx;

import yumyai.mmd.pmd.PmdPose;
import yumyai.mmd.vpd.VpdPose;

public class PmxAnimatedInstance {
    public final PmxModel model;
    public final PmxPose pose;
    public boolean physicsEnabled;
    public PmxIkSolver ikSolver;
    public PmxModelPhysics physics;
    boolean disposed = false;

    public PmxAnimatedInstance(PmxModel model) {
        this.model = model;
        this.pose = new PmxPose(model);
        physicsEnabled = false;
        ikSolver = new PmxIkSolver();
        physics = new PmxModelPhysics(model);
    }

    public void setPmxPose(PmxPose inPose) {
        pose.copy(inPose);
        ikSolver.solve(pose);
        pose.copyBonePoses();
    }

    public void setVpdPose(VpdPose inPose) {
        pose.copy(inPose);
        //ikSolver.solve(pose);
        //pose.copyBonePoses();
    }

    public void setVpdPose(VpdPose inPose, boolean verbose) {
        pose.copy(inPose);
        //ikSolver.solve(pose, verbose);
        //pose.copyBonePoses();
    }

    public void update(float elaspedTimeInSeconds) {
        for (int i = 0; i < model.getBoneCount(); i++) {
            PmxBone bone = model.getBoneByOrder(i);
            if (bone.transformAfterPhysics())
                break;
            if (bone.copyTranslation() || bone.copyRotation()) {
                pose.copyBonePose(bone.boneIndex);
            } else if (bone.isIk()) {
                ikSolver.solve(pose, bone.boneIndex);
            }
        }

        if (physicsEnabled) {
            physics.update(pose, elaspedTimeInSeconds);
        }

        for (int i = 0; i < model.getBoneCount(); i++) {
            PmxBone bone = model.getBoneByOrder(i);
            if (!bone.transformAfterPhysics())
                continue;
            if (bone.copyTranslation() || bone.copyRotation()) {
                pose.copyBonePose(bone.boneIndex);
            } else if (bone.isIk()) {
                ikSolver.solve(pose, bone.boneIndex);
            }
        }

    }

    public void getPmxPose(PmxPose pose) {
        pose.copy(this.pose);
    }

    public void enablePhysics(boolean enabled) {
        this.physicsEnabled = enabled;
    }

    public boolean isPhysicsEnabled() {
        return physicsEnabled;
    }

    public PmxModel getModel() {
        return model;
    }

    public void dispose() {
        if (!disposed) {
            physics.dispose();
            physics = null;
            disposed = true;
        }
    }

    public void resetPhysics(PmxPose inPose) {
        pose.copy(inPose);
        ikSolver.solve(this.pose);
        physics.resetPhysicsWithPose(pose);
        physics.update(pose, 1);
        physics.resetPhysicsWithPose(pose);
        physics.update(pose, 1);
        physics.resetPhysicsWithPose(pose);
        physics.update(pose, 1);
    }

    public PmxModelPhysics getPhysics() {
        return physics;
    }
}
