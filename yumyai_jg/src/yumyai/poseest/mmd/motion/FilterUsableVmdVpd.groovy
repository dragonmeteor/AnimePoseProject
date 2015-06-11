package yumyai.poseest.mmd.motion

class FilterUsableVmdVpd {
    public static void main(String[] args) {
        if (args.length < 2) {
            println("Usage: yumyai.poseest.mmd.motion.FilterUsableVmdVpd <input-list> <output-list>")
            System.exit(0)
        }

        new File(args[0]).withReader() { fin ->
            new File(args[1]).withWriter('utf-8') { fout ->
                def line = fin.readLine()
                def count = 0
                while (line != null) {
                    try {
                        def filename = line.trim()
                        def boneList = PrintVmdVpdBones.getBoneList(filename)
                        if (boneList.size() > 0)
                            fout.println(filename)
                        else
                            println(filename + " has 0 bones")
                    } catch (Exception e) {
                        e.printStackTrace()
                    }
                    line = fin.readLine()
                    count++
                    if (count % 100 == 0)
                        println("Processed ${count} files")
                }
            }
        }
    }
}
