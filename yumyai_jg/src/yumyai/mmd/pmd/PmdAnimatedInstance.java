package yumyai.mmd.pmd;

import yumyai.mmd.vpd.VpdPose;

public class PmdAnimatedInstance {
    private final PmdModel model;
    private final PmdPose prePhysicsPose;
    private final PmdPose postPhysicsPose;
    private PmdModelPhysics physics;
    private boolean physicsEnabled;
    private boolean ikEnabled;
    private boolean disposed = false;

    public PmdAnimatedInstance(PmdModel model) {
        this(model, true, true);
    }

    public PmdAnimatedInstance(PmdModel model, boolean ikEnabled, boolean physicsEnabled) {
        this.model = model;
        this.prePhysicsPose = new PmdPose(model);
        this.postPhysicsPose = new PmdPose(model);
        this.physics = new PmdModelPhysics(model);
        this.physicsEnabled = physicsEnabled;
        this.ikEnabled = ikEnabled;
    }

    public void setPmdPose(PmdPose pose) {
        prePhysicsPose.copy(pose);
        prePhysicsPose.solveIk(physicsEnabled);
    }

    public void setVpdPose(VpdPose pose) {
        if (pose == null) {
            prePhysicsPose.clear();
        }
        prePhysicsPose.copy(pose);
        if (ikEnabled)
            prePhysicsPose.solveIk(physicsEnabled);
    }

    public void update(float elapsedTimeInSeconds) {
        postPhysicsPose.copy(prePhysicsPose);
        if (physicsEnabled) {
            physics.setPose(prePhysicsPose);
            physics.timeStep(elapsedTimeInSeconds);
            physics.getPose(postPhysicsPose);
        }
    }

    public void getPmdPose(PmdPose pose) {
        pose.copy(postPhysicsPose);
    }

    public void enablePhysics(boolean enabled) {
        this.physicsEnabled = enabled;
    }

    public void resetPhysics(PmdPose pose) {
        prePhysicsPose.copy(pose);
        prePhysicsPose.solveIk(false);
        physics.resetPhysicsWithPose(prePhysicsPose);
        physics.timeStep(1);
        physics.resetPhysicsWithPose(prePhysicsPose);
        physics.timeStep(1);
        physics.resetPhysicsWithPose(prePhysicsPose);
        physics.timeStep(1);
    }

    public PmdModelPhysics getPhysics() {
        return physics;
    }

    @Override
    public void finalize() {
        if (!disposed) {
            dispose();
        }
    }

    public void dispose() {
        physics.dispose();
        physics = null;
        disposed = true;
    }
}
