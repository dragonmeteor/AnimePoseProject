package yumyai.poseest.mmd.motion

import yumyai.poseest.mmd.MmdMotionInfo

class CountMotion {
    public static void main(String[] args) {
        if (args.length < 2) {
            println("java yumyai.poseest.mmd.motion.CountMotion <motion-info-file> <output-file-name>");
            System.exit(0);
        }

        ArrayList<MmdMotionInfo> info = MmdMotionInfo.load(args[0]);
        int motionCount = info.size();
        int frameCount = 0;
        for (MmdMotionInfo item : info) {
            frameCount += item.frames.size();
        }
        new File(args[1]).withWriter { fout ->
            fout.write(String.format("%d\n%d\n", motionCount, frameCount));
        }
    }
}
