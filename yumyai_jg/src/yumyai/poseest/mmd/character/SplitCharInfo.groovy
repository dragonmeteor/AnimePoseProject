package yumyai.poseest.mmd.character

import yondoko.util.ArgumentProcessor
import yumyai.poseest.mmd.MmdCharInfo

class SplitCharInfo {
    String charInfoFile;
    String outputFileFormat;
    ArrayList<Float> portions = new ArrayList<Float>();

    public static void main(String[] args) {
        new SplitCharInfo().run(args);
    }

    void run(String[] args) {
        if (args.length < 3) {
            println("Usage: java yumyai.poseest.mmd.character.SplitCharInfo <char-info-file> " +
                "<format-string> <portion-1> ... <portion-n>");
            System.exit(-1);
        }

        ArgumentProcessor argProc = new ArgumentProcessor(args);
        charInfoFile = argProc.getString();
        outputFileFormat = argProc.getString();
        while (argProc.getNumRemainingArguments() > 0) {
            portions.add(argProc.getInt());
        }

        // Read the character info and split them into groups based on directories
        def charInfos = MmdCharInfo.load(charInfoFile);
        def groups = new ArrayList<ArrayList<MmdCharInfo>>();
        def lastGroup = new ArrayList<MmdCharInfo>();
        lastGroup.add(charInfos[0]);
        String lastDir = new File(charInfos[0].fileName).getParent();
        for (int i = 1; i < charInfos.size(); i++) {
            def charInfo = charInfos[i];
            String dir = new File(charInfo.fileName).getParent();
            if (lastDir.equals(dir)) {
                lastGroup.add(charInfo)
            } else {
                groups.add(lastGroup);
                lastGroup = new ArrayList<MmdCharInfo>();
                lastGroup.add(charInfo);
                lastDir = dir;
            }
        }
        groups.add(lastGroup);
        println("number of groups = " + groups.size())
        println();

        // Shuffle the groups
        Random random = new Random();
        for (int i = 1; i < groups.size(); i++) {
            int j = random.nextInt(i+1);
            if (j != i) {
                def temp = groups[i]
                groups[i] = groups[j]
                groups[j] = temp
            }
        }

        // Distribute the groups
        def partitions = new ArrayList<ArrayList<MmdCharInfo>>();
        def goalSize = new int[portions.size()];
        float totalPortion = 0;
        for (int i = 0; i < portions.size(); i++) {
            totalPortion += portions[i];
        }
        int currentIndex = 0;
        for (int i = 0; i < portions.size(); i++) {
            goalSize[i] = (int)Math.round(portions[i] / totalPortion * charInfos.size());
            println("partition ${i} target = " + goalSize[i])
            def partition = new ArrayList<MmdCharInfo>()
            int count = 0;
            while (count < goalSize[i] && currentIndex < groups.size()) {
                partition.addAll(groups[currentIndex])
                count += groups[currentIndex].size()
                currentIndex++
            }
            println("partition ${i} actual = " + partition.size())
            partitions.add(partition);
            println()
        }
        while (currentIndex < groups.size()) {
            partitions[random.nextInt(partitions.size())].addAll(groups[currentIndex])
            currentIndex++
        }

        for (int i = 0; i < partitions.size(); i++) {
            def fileName = String.format(outputFileFormat, i);
            MmdCharInfo.save(fileName, partitions[i])
        }
    }
}
