package yumyai.poseest.mmd.motion

import yumyai.poseest.mmd.Settings

class WriteMotionBoneList {
    public static void main(String[] args) {
        if (args.length < 2) {
            print("java yumyai.poseest.mmd.motion.WriteMotionBoneList <input-file> <output-file>")
            System.exit(-1)
        }

        new File(args[0]).withReader("UTF-8") { fin ->
            new File(args[1]).withWriter("UTF-8") { fout ->
                while (true) {
                    String fileName = fin.readLine()
                    if (fileName == null)
                        break
                    else
                        fileName = fileName.trim()
                    println(fileName)
                    ArrayList<String> motionBoneList = PrintVmdVpdBones.getBoneList(fileName)
                    String frameLine = fin.readLine().trim()
                    fout.write("${fileName}\n")
                    fout.write("${frameLine}\n")
                    for(String boneName : Settings.relevantBones) {
                        if (motionBoneList.contains(boneName)) {
                            fout.write("${boneName} ")
                        }
                    }
                    fout.write("\n")
                }
            }
        }
    }
}
