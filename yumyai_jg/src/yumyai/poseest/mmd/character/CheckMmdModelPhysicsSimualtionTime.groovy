package yumyai.poseest.mmd.character

import org.apache.commons.io.FilenameUtils
import yumyai.mmd.pmd.PmdAnimatedInstance
import yumyai.mmd.pmd.PmdModel
import yumyai.mmd.pmx.PmxAnimatedInstance
import yumyai.mmd.pmx.PmxModel
import yumyai.mmd.vmd.VmdMotion
import yumyai.mmd.vpd.VpdPose

class CheckMmdModelPhysicsSimualtionTime {
    public static void main(String[] args) {
        if (args.length < 2) {
            println("Usage: java yumyai.poseest.mmd.character.CheckMmdModelPhysicsSimualtionTime " +
                    "<input-file> <output-file>")
        }

        def inputMap = CheckMmdModelTexture.readFileUsage(args[0])
        def outputMap = CheckMmdModelTexture.readFileUsage(args[1])

        VmdMotion vmdMotion = VmdMotion.load("data/vmd/kissme_right.vmd")
        VpdPose inVpdPose = new VpdPose();
        inVpdPose.clear();
        vmdMotion.getPose(0, inVpdPose)

        VpdPose restPose = new VpdPose();

        int count = 0
        int modelsWithTooLongSimualtionTime = 0
        long start = System.nanoTime()
        for (String fileName : inputMap.keySet()) {
            outputMap[fileName] = inputMap[fileName]
            if (inputMap[fileName] == 1)
                continue
            println(fileName)
            String extension = FilenameUtils.getExtension(fileName).toLowerCase()
            def instance = null
            if (extension.equals("pmd")) {
                try {
                    PmdModel model = PmdModel.load(fileName)
                    instance = new PmdAnimatedInstance(model)
                    instance.getPhysics()setGravity(0, -100, 0)
                    instance.enablePhysics(true)
                } catch (Exception e) {
                    println("Cannot open because: " + e.getMessage())
                    e.printStackTrace();
                    inputMap[fileName] = 1;
                }
            } else if (extension.equals("pmx")) {
                try {
                    PmxModel model = PmxModel.load(fileName)
                    instance = new PmxAnimatedInstance(model)
                    instance.getPhysics()setGravity(0, -100, 0)
                    instance.enablePhysics(true)
                } catch (Exception e) {
                    println("Cannot open because: " + e.getMessage())
                    e.printStackTrace()
                    inputMap[fileName] = 1;
                }
            }

            if (inputMap[fileName] != 1)
            {
                VpdPose interpPose = new VpdPose();

                int startInterval = 150;
                long startTime = System.nanoTime()
                long origTime = startTime;
                long elaspedTime;
                for (int i = 0; i < startInterval; i++) {
                    float alpha = i * 1.0f / startInterval
                    interpPose.clear()
                    VpdPose.interpolate(restPose, inVpdPose, alpha, interpPose)

                    if (instance instanceof PmdAnimatedInstance) {
                        instance.setVpdPose(interpPose)
                        instance.update(i / 30.0)
                    } else if (instance instanceof PmxAnimatedInstance) {
                        instance.setVpdPose(interpPose)
                        instance.update(i / 30.0)
                    }

                    elaspedTime = System.nanoTime() - startTime;
                    if (elaspedTime * 1e-9 > 1) {
                        println("Simulation elasped time = " + (elaspedTime * 1e-9) + " seconds")
                        startTime = System.nanoTime()
                    }
                }
                elaspedTime = System.nanoTime() - origTime;
                if (elaspedTime * 1e-9 > 1) {
                    println("Simulation elasped time = " + (elaspedTime * 1e-9) + " seconds")
                }
                if (elaspedTime*1e-9 > 10) {
                    outputMap[fileName] = 1
                    modelsWithTooLongSimualtionTime++
                    println("This model takes too long time to simulate");
                }
                println();
            }
            if (instance != null)
                instance.dispose()

            count++
            if (count % 100 == 0) {
                println("Finished ${count} of ${inputMap.size()}")
                println("Elapsed time = ${(System.nanoTime() - start) * 1e-9} seconds")
                println();
            }
        }

        CheckMmdModelTexture.writeMap(outputMap, args[1], args[0])
        println()
        println("#models without too long simulation time = " + modelsWithTooLongSimualtionTime)
    }
}
