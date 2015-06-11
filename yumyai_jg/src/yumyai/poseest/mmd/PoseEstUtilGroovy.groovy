package yumyai.poseest.mmd

class PoseEstUtilGroovy {
    public static ArrayList<String> readFileUsageListAsArray(String fileName) {
        ArrayList<String> result = new ArrayList<String>()
        File file = new File(fileName)
        if (!file.exists()) {
            return result
        }
        file.withReader("UTF-8") { fin ->
            def lines = fin.readLines()
            for (int i = 0; i < lines.size(); i++) {
                if (lines[i].length() > 0) {
                    String fName = lines[i].substring(2).trim()
                    result.add(fName)
                }
            }
        }
        return result
    }

    /*
    public static HashMap<String, HashMap<String, Integer>> readExtraJointInfo(String fileName) {
        HashMap<String, HashMap<String, Integer>> result = new HashMap<String, HashMap<String, Integer>>();
        File file = new File(fileName)
        if (!file.exists()) {
            return result
        }
        file.withReader("UTF-8") { fin ->
            int jointCount = Integer.valueOf(fin.readLine())
            while (true) {
                String line = fin.readLine()
                if (line == null)
                    break
                String modelFileName = line.trim()
                HashMap<String, Integer> info = new HashMap<String, Integer>();
                for(String extraBoneName : Settings.extraBoneNames) {
                    info[extraBoneName] = -1
                }
                for (int i = 0; i < jointCount; i++) {
                    line = fin.readLine()
                    String[] comps = line.split()
                    String boneName = comps[0]
                    int vertexIndex = Integer.valueOf(comps[1])
                    info[boneName] = vertexIndex
                }
                result[modelFileName] = info
            }
        }
        return result;
    }

    public static void saveExtraJointInfo(String fileName, ArrayList<String> modelFileNames,
                                          HashMap<String, HashMap<String, Integer>> extratJointInfo) {
        new File(fileName).withWriter("UTF-8") { fout ->
            fout.write("${Settings.extraBoneNames.size()}\n")
            for(String modelFileName : modelFileNames) {
                fout.write("${modelFileName}\n")
                HashMap<String, Integer> info = extratJointInfo[modelFileName]
                for(String extraBoneName : Settings.extraBoneNames) {
                    fout.write("${extraBoneName} ${info[extraBoneName]}\n")
                }
            }
        }
    }
    */
}
