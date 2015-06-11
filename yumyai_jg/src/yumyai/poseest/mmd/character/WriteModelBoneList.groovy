package yumyai.poseest.mmd.character

class WriteModelBoneList {
    public static void main(String[] args) {
        if (args.length < 2) {
            print("java yumyai.poseest.mmd.character.WriteModelBoneList <input-file> <output-file>")
            System.exit(-1)
        }

        new File(args[0]).withReader("UTF-8") { fin ->
            new File(args[1]).withWriter("UTF-8") { fout ->
                while (true) {
                    String line = fin.readLine()
                    if (line == null)
                        break
                    String fileName = line.substring(2).trim()
                    ArrayList<String> boneNames = PrintPmdPmxBones.getBoneList(fileName)
                    fout.write("${fileName}\n")
                    println(fileName)
                    for (String boneName : boneNames) {
                        fout.write("${boneName} ")
                    }
                    fout.write("\n")
                }
            }
        }
    }
}
