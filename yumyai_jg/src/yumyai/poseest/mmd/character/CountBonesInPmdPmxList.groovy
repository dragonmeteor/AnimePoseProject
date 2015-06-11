package yumyai.poseest.mmd.character

import yumyai.poseest.mmd.character.PrintPmdPmxBones

class CountBonesInPmdPmxList {
    public static void main(String[] args) {
        if (args.length < 2) {
            print("Usage: java yumyai.poseest.mmd.character.CountBonesInPmdPmxList <list-file> <output-file>")
            System.exit(0)
        }

        HashMap<String, Integer> boneCount = new HashMap<String, Integer>()
        def count = 0
        new File(args[0]).withReader { f ->
            def line = f.readLine()
            while (line != null) {
                try {
                    def fileName = line.substring(2).trim()
                    //println("Reading ${fileName} ...")
                    ArrayList<String> bones = PrintPmdPmxBones.getBoneList(fileName)
                    for (String boneName : bones) {
                        if (!boneCount.containsKey(boneName)) {
                            boneCount[boneName] = 0
                        }
                        boneCount[boneName] += 1
                    }
                } catch (Exception e) {
                    e.printStackTrace()
                }
                line = f.readLine()
                count++
                if (count % 100 == 0)
                    println("Processed ${count} files")
            }
        }

        ArrayList<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>();
        for (def entry : boneCount.entrySet()) {
            entries.add(entry)
        }
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.value - o1.value
            }
        })
        new File(args[1]).withWriter("UTF-8") { f ->
            for (def entry : entries) {
                f.write(entry.key + " " + entry.value + "\n")
            }
        }
    }
}
