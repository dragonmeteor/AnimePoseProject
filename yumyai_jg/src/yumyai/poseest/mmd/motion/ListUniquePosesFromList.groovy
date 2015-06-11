package yumyai.poseest.mmd.motion

class ListUniquePosesFromList {
    public static void main(String[] args) {
        if (args.length < 2) {
            println("java yumyai.poseest.mmd.motion.ListUniquePosesFromList <input-file> <output-file>")
            System.exit(0)
        }

        ArrayList<String> fileNames = new ArrayList<String>();
        new File(args[0]).withReader() { fin ->
            def line = fin.readLine()
            while (line != null) {
                fileNames.add(line.trim())
                line = fin.readLine()
            }
        }

        ArrayList<ArrayList<Integer>> uniqueFrames = new ArrayList<ArrayList<Integer>>();
        int count = 0
        for(String fileName : fileNames) {
            print(fileName + "\n")
            uniqueFrames.add(ListUniquePoses.getUniquePoseTimes(fileName));
            count++
            print("${uniqueFrames.last().size()} unique frame(s) [${count} files so far]\n")
        }

        new File(args[1]).withWriter("UTF-8") { fout ->
            for (int i = 0; i < fileNames.size(); i++) {
                fout.write(fileNames[i] + "\n")
                def compactList = compactifyNumberList(uniqueFrames[i])
                for (int j = 0; j < compactList.size(); j++) {
                    if (j > 0)
                        fout.write(" ")
                    fout.write(compactList[j])
                }
                fout.write("\n")
            }
        }
    }

    public static String intervalString(int start, int last) {
        if (start == last)
            return "${start}"
        else
            return "${start}-${last}"
    }

    public static ArrayList<String> compactifyNumberList(ArrayList<Integer> a) {
        int last = -2
        int start = -2;
        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < a.size(); i++) {
            int x = a[i];
            if (x != last+1) {
                if (last >= 0)
                    result.add(intervalString(start,last))
                start = x
            }
            last = x
        }
        if (last >= 0)
            result.add(intervalString(start, last))
        return result
    }
}
