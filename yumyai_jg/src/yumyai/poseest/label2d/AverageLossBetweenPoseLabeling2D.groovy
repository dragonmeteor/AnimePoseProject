package yumyai.poseest.label2d

import javax.vecmath.Point2f

class AverageLossBetweenPoseLabeling2D {
    public static void main(String[] args) {
        if (args.length < 5) {
            println("Usage: java yumyai.poseest.label2d.AverageLossBetweenPoseLabeling2D <labeling-file-1> <labeling-file-2> <config-file> <image-width> <image-height>")
            System.exit(0);
        }

        ArrayList<PoseLabeling2D> labelings0 = PoseLabeling2D.load(args[0])
        ArrayList<PoseLabeling2D> labelings1 = PoseLabeling2D.load(args[1])
        Pose2DConfig config = Pose2DConfig.load(args[2])
        float imageWidth = Integer.valueOf(args[3])
        float imageHeight = Integer.valueOf(args[4])

        double total = 0
        int count = Math.min(labelings0.size(), labelings1.size());
        for (int i = 0; i < count; i++) {
            PoseLabeling2D l0 = labelings0[i]
            PoseLabeling2D l1 = labelings1[i]
            double sum = 0
            for (String boneName : config.boneNames) {
                Point2f p0 = l0.points[boneName]
                Point2f p1 = l1.points[boneName]
                float dx = (p0.x - p1.x) / imageWidth
                float dy = (p0.y - p1.y) / imageHeight
                sum += (dx * dx + dy * dy) / 2
            }
            total += sum
        }
        System.out.println(total / count)
    }
}