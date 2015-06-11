package yumyai.poseest.mmd.example

import groovy.json.JsonBuilder
import yondoko.util.ArgumentProcessor
import yumyai.poseest.mmd.MmdCharInfo
import yumyai.poseest.mmd.MmdMotionInfo

class GenRandomTrainingExampleModelMotionBackground {
    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("java yumyai.poseest.app.GenRandomTrainingExampleConfigs " +
                    "<model-file> <motion-file> <background-file> <count> " +
                    "<output-file-limit> <output-file-prefix> <output-file-extension>")
            System.exit(-1)
        }

        ArgumentProcessor argProc = new ArgumentProcessor(args)
        String modelFile = argProc.getString()
        String motionFile = argProc.getString()
        String backgroundFile = argProc.getString()
        int count = argProc.getInt()
        int limit = argProc.getInt()
        String outputFilePrefix = argProc.getString()
        String outputFileExtension = argProc.getString()

        ArrayList<MmdCharInfo> modelInfos = MmdCharInfo.load(modelFile)
        ArrayList<MmdMotionInfo> motionInfos = MmdMotionInfo.load(motionFile)
        ArrayList<String> backgroundFileNames = loadBackgroundFile(backgroundFile)

        int totalFrameCount = 0
        ArrayList<Integer> frameStarts = new ArrayList<Integer>()
        for (MmdMotionInfo info : motionInfos) {
            totalFrameCount += info.frames.size()
            frameStarts.add(totalFrameCount)
        }

        Random random = new Random()
        def result = []
        for (int i = 0; i < count; i++) {
        //for (int i = 0; i < modelInfos.size(); i++) {
            def item = [:]
            int modelIndex = random.nextInt(modelInfos.size())
            //int modelIndex = i;
            item["model"] = modelInfos[modelIndex].fileName
            int frameIndex = 0;
            int motionIndex = 0;
            while (true) {
                frameIndex = random.nextInt(totalFrameCount)+1
                motionIndex = Collections.binarySearch(frameStarts, frameIndex)
                if (motionIndex < 0) {
                    motionIndex = -(motionIndex+1);
                }
                MmdMotionInfo motionInfo = motionInfos[motionIndex]
                MmdCharInfo modelInfo = modelInfos[modelIndex]
                if (modelInfo.boneNames.containsAll(motionInfo.requiredBones)) {
                    break;
                }
            }
            item["motion"] = motionInfos[motionIndex].fileName
            if (motionIndex > 0)
                frameIndex -= frameStarts[motionIndex-1]+1

            item["frame"] = motionInfos[motionIndex].frames[frameIndex]

            item["background"] = backgroundFileNames[random.nextInt(backgroundFileNames.size())]
            item["background_size"] = random.nextFloat()
            item["background_x"] = random.nextFloat()
            item["background_y"] = random.nextFloat()

            item["light_theta"] = random.nextFloat()
            item["light_phi"] = random.nextFloat()
            item["light_fraction"] = random.nextFloat()

            item["view"] = random.nextFloat()

            item["camera_theta"] = random.nextFloat()
            item["camera_phi"] = random.nextFloat()
            result.add(item)

            item["camera_rotation"] = random.nextFloat()
            item["camera_shift_x"] = random.nextFloat()
            item["camera_shift_y"] = random.nextFloat()

            if ((i+1) % 10000 == 0) {
                println("Generated ${i+1} items ...");
            }
        }

        result.sort(new Comparator() {
            @Override
            int compare(Object o1, Object o2) {
                def r = o1["model"].compareTo(o2["model"])
                if (result != 0)
                    return r
                else {
                    return o1["motion"].compareTo(o2["motion"])
                }
            }
        })
        for (int i = 0; i < result.size(); i++) {
            result[i]["index"] = i
        }

        int currentIndex = 0;
        int currentFileLimit = 0;
        int fileIndex = -1;
        String outputFileName = ""
        while (currentIndex < count) {
            if (currentFileLimit <= currentIndex) {
                currentFileLimit += limit
                fileIndex++;
                outputFileName = "${outputFilePrefix}-${String.format("%05d", fileIndex)}.${outputFileExtension}";
            }
            new File(outputFileName).withWriter("UTF-8") { fout ->
                fout.write("[\n")
                while (currentIndex < currentFileLimit && currentIndex < count) {
                    def i = currentIndex
                    def builder = new JsonBuilder()
                    builder(result[i])
                    fout.write(builder.toPrettyString())
                    if (i < currentFileLimit-1 && i < count-1)
                        fout.write(",\n")
                    if ((i+1) % 10000 == 0)
                        println "Written ${i+1} examples ..."
                    currentIndex++;
                }
                fout.write("]\n")
            }
        }
    }

    public static ArrayList<String> loadBackgroundFile(String fileName) {
        ArrayList<String> fileNames = new ArrayList<String>()
        new File(fileName).withReader("UTF-8") { f ->
            def line = f.readLine()
            while (line != null) {
                fileNames.add(line.trim())
                line = f.readLine()
            }
        }
        return fileNames
    }
}
