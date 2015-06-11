package yumyai.poseest.mmd.motion

import yondoko.util.ArgumentProcessor
import yumyai.poseest.mmd.MmdMotionInfo

class CreateConsecutivelyLargerMotionPool {
    String motionInfoFile;
    String outputFilePrintf;
    ArrayList<Integer> counts = new ArrayList<Integer>();
    ArrayList<MmdMotionInfo> motionInfos;

    public static void main(String[] args) {
        new CreateConsecutivelyLargerMotionPool().run(args);
    }

    public void run(String[] args) {
        if (args.length < 3) {
            println("Usage: java yumyai.poseest.mmd.motion.CreateConsecutivelyLargerMotionPool <motion-info-file> " +
                    "<format-string> <pose-count-1> ... <pose-count-n>");
            System.exit(0);
        }

        ArgumentProcessor argProc = new ArgumentProcessor(args);
        motionInfoFile = argProc.getString();
        outputFilePrintf = argProc.getString();
        while (argProc.getNumRemainingArguments() > 0) {
            counts.add(argProc.getInt());
        }
        for (int i=0;i<counts.size()-1;i++) {
            if (counts[i] >= counts[i+1]) {
                println("Pose counts should be strictly increasing!");
                System.exit(-1);
            }
        }

        motionInfos = MmdMotionInfo.load(motionInfoFile);
        println("number of motions = " + motionInfos.size())
        println();

        int totalFrameCount = 0
        ArrayList<Integer> frameStarts = new ArrayList<Integer>()
        for (MmdMotionInfo info : motionInfos) {
            totalFrameCount += info.frames.size()
            frameStarts.add(totalFrameCount)
        }

        ArrayList<Integer> frameIndices = new ArrayList<Integer>();
        for (int i = 0; i < totalFrameCount; i++) {
            frameIndices.add(i);
        }
        // Permute the frames randomly.
        Random random = new Random();
        for (int i=totalFrameCount-1;i>0;i--) {
            int j = random.nextInt(i+1);
            int temp = frameIndices[j];
            frameIndices[j] = frameIndices[i];
            frameIndices[i] = temp;
        }
        int lastCount = counts[counts.size()-1];
        while (frameIndices.size() > lastCount) {
            frameIndices.removeAt(frameIndices.size()-1);
        }

        for (int i = 0; i < counts.size(); i++) {
            int count = counts[i];
            String fileName = String.format(outputFilePrintf, i);
            ArrayList<Integer> indices = new ArrayList<Integer>();
            for (int j = 0; j < count; j++) {
                indices.add(frameIndices[j]);
            }
            Collections.sort(indices);
            print("Writing ${fileName} ... ")
            writeOutputFile(fileName, indices);
            println("DONE!")
        }
    }

    public void writeOutputFile(String fileName, ArrayList<Integer> indices) {
        ArrayList<MmdMotionInfo> outputs = new ArrayList<MmdMotionInfo>();
        int currentMotionInfoIndex = -1;
        int currentLastFrameIndex = 0;
        int currentLastFrameStart = 0;
        MmdMotionInfo lastMotionInfo = null;
        for (int i = 0; i < indices.size(); i++) {
            while (currentLastFrameIndex < indices[i]) {
                currentLastFrameStart = currentLastFrameIndex;
                currentMotionInfoIndex += 1;
                currentLastFrameIndex += motionInfos[currentMotionInfoIndex].frames.size();
            }
            MmdMotionInfo currentMotionInfo = motionInfos[currentMotionInfoIndex];
            if (currentMotionInfo != lastMotionInfo) {
                MmdMotionInfo newMotionInfo = new MmdMotionInfo();
                newMotionInfo.fileName = currentMotionInfo.fileName;
                newMotionInfo.requiredBones = currentMotionInfo.requiredBones;
                outputs.add(newMotionInfo);
                lastMotionInfo = currentMotionInfo;
            }
            outputs[outputs.size()-1].frames.add(indices[i]-currentLastFrameStart);
        }
        MmdMotionInfo.save(fileName, outputs);
    }
}
