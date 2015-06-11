package yumyai.poseest.mmd.character

import org.apache.commons.io.FilenameUtils
import yondoko.struct.Aabb3f
import yumyai.mmd.pmd.PmdModel
import yumyai.mmd.pmx.PmxModel
import yumyai.mmd.pmx.PmxVertex
import yumyai.poseest.mmd.Settings

class CheckMmdModelBonesAndAabb {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("java yumyai.poseest.app.CheckMmdModelBones <input-file> <output-file>");
            System.exit(0);
        }

        def inputMap = CheckMmdModelTexture.readFileUsage(args[0])
        def outputMap = CheckMmdModelTexture.readFileUsage(args[1])



        int count = 0
        int modelWithoutRightBones;
        long start = System.nanoTime()
        for (String fileName : inputMap.keySet()) {
            //System.out.println(fileName);
            boolean hasTheRightBones = false;
            String extension = FilenameUtils.getExtension(fileName).toLowerCase()
            Aabb3f aabb = new Aabb3f();
            aabb.reset()
            if (extension.equals("pmd")) {
                try {
                    PmdModel model = PmdModel.load(fileName)
                    hasTheRightBones = true;
                    for(String boneName : Settings.necessaryBones) {
                        hasTheRightBones = hasTheRightBones && (model.getBoneIndex(boneName) >= 0)
                    }
                    for (int i = 0; i < model.positions.capacity()/3; i++) {
                        float x = model.positions.get(3*i+0);
                        float y = model.positions.get(3*i+1);
                        float z = model.positions.get(3*i+2);
                        aabb.expandBy(x,y,z)
                    }
                } catch (Exception e) {
                    println("Cannot open because: " + e.getMessage())
                    e.printStackTrace();
                }
            } else if (extension.equals("pmx")) {
                try {
                    PmxModel model = PmxModel.load(fileName)
                    hasTheRightBones = true;
                    for(String boneName : Settings.necessaryBones) {
                        hasTheRightBones = hasTheRightBones && model.hasBone(boneName);
                    }
                    for (int i = 0; i < model.getVertexCount(); i++) {
                        PmxVertex vertex = model.getVertex(i)
                        aabb.expandBy(vertex.position)
                    }
                } catch (Exception e) {
                    println("Cannot open because: " + e.getMessage())
                }
            }

            boolean hasRightAabb = (aabb.pMin.x >= -1000) &&
                    (aabb.pMin.y >= -1000) &&
                    (aabb.pMin.z >= -1000) &&
                    (aabb.pMax.x <= 1000) &&
                    (aabb.pMax.y <= 1000) &&
                    (aabb.pMax.z <= 1000)
            if (!hasRightAabb) {
                println fileName
                println aabb
            }

            outputMap[fileName] = inputMap[fileName]
            if (!hasTheRightBones) {
                outputMap[fileName] = 1;
                modelWithoutRightBones++;
            }
            if (!hasRightAabb) {
                outputMap[fileName] = 1;
                modelWithoutRightBones++;
            }


            count++
            if (count % 100 == 0) {
                println("Finished ${count} of ${inputMap.size()}")
                println("Elapsed time = ${(System.nanoTime() - start) * 1e-9} seconds")
            }
        }

        CheckMmdModelTexture.writeMap(outputMap, args[1], args[0])
        println()
        println("#models without the right bones = " + modelWithoutRightBones)
    }
}
