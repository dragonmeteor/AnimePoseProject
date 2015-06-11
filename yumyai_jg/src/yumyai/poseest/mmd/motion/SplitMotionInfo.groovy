package yumyai.poseest.mmd.motion

import yondoko.util.ArgumentProcessor
import yumyai.poseest.mmd.MmdMotionInfo

class SplitMotionInfo {
    String motionInfoFile;
    String outputFileFormat;
    ArrayList<Float> portions = new ArrayList<Float>();

    public static void main(String[] args) {
        new SplitMotionInfo().run(args);
    }

    void run(String[] args) {
        if (args.length < 3) {
            println("Usage: java yumyai.poseest.mmd.motion.SplitCharInfo <motion-info-file> " +
                    "<format-string> <portion-1> ... <portion-n>");
            System.exit(-1);
        }

        ArgumentProcessor argProc = new ArgumentProcessor(args);
        motionInfoFile = argProc.getString();
        outputFileFormat = argProc.getString();
        while (argProc.getNumRemainingArguments() > 0) {
            portions.add(argProc.getInt());
        }

        // Read the character info and split them into groups
        def motionInfos = MmdMotionInfo.load(motionInfoFile);
        println("number of motions = " + motionInfos.size())
        println();

        // Shuffle the motions
        Random random = new Random();
        for (int i = 1; i < motionInfos.size(); i++) {
            int j = random.nextInt(i+1);
            if (j != i) {
                def temp = motionInfos[i]
                motionInfos[i] = motionInfos[j]
                motionInfos[j] = temp
            }
        }

        // Distribute the motions
        def partitions = new ArrayList<ArrayList<MmdMotionInfo>>();
        def goalSize = new int[portions.size()];
        float totalPortion = 0;
        for (int i = 0; i < portions.size(); i++) {
            totalPortion += portions[i];
        }
        int currentIndex = 0;
        for (int i = 0; i < portions.size(); i++) {
            goalSize[i] = (int)Math.round(portions[i] / totalPortion * motionInfos.size());
            println("partition ${i} target = " + goalSize[i])
            def partition = new ArrayList<MmdMotionInfo>()
            int count = 0;
            while (count < goalSize[i] && currentIndex < motionInfos.size()) {
                partition.add(motionInfos[currentIndex])
                count++
                currentIndex++
            }
            println("partition ${i} actual = " + partition.size())
            partitions.add(partition);
            println()
        }
        while (currentIndex < motionInfos.size()) {
            partitions[random.nextInt(partitions.size())].addAll(motionInfos[currentIndex])
            currentIndex++
        }

        for (int i = 0; i < partitions.size(); i++) {
            def fileName = String.format(outputFileFormat, i);
            MmdMotionInfo.save(fileName, partitions[i])
        }
    }
}
