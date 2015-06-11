package yumyai.poseest.mmd;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MmdCharInfo {
    public String fileName = null;
    public ArrayList<String> boneNames = new ArrayList<String>();
    public HashMap<String, Integer> extraBoneVertexIndex = new HashMap<String, Integer>();
    public boolean processed = false;

    public MmdCharInfo() {
        for (String extraBoneName : Settings.extraBoneNames) {
            extraBoneVertexIndex.put(extraBoneName, -1);
        }
    }

    public MmdCharInfo(String fileName) {
        this();
        this.fileName = fileName;
    }

    public int getVertexIndex(String extraBoneName) {
        if (!extraBoneVertexIndex.containsKey(extraBoneName)) {
            return -1;
        } else {
            return extraBoneVertexIndex.get(extraBoneName);
        }
    }

    public static ArrayList<MmdCharInfo> createExtraBoneInfoArray(List<String> fileNames) {
        ArrayList<MmdCharInfo> result = new ArrayList<MmdCharInfo>();
        for (String fileName : fileNames) {
            result.add(new MmdCharInfo(fileName));
        }
        return result;
    }

    public static void save(String outputFileName, ArrayList<MmdCharInfo> data) throws IOException {
        OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(outputFileName), "UTF-8");
        BufferedWriter fout = new BufferedWriter(fileWriter);

        fout.write(String.format("%d\n", Settings.extraBoneNames.size()));
        for (MmdCharInfo info : data) {
            fout.write(info.fileName + "\n");

            for (int i = 0; i < info.boneNames.size(); i++) {
                fout.write(info.boneNames.get(i));
                if (info.boneNames.size() - 1 > i) {
                    fout.write(" ");
                }
            }
            fout.write("\n");

            for (String extraBoneName : Settings.extraBoneNames) {
                fout.write(String.format("%s %d\n", extraBoneName, info.getVertexIndex(extraBoneName)));
            }
            fout.write(info.processed + "\n");
        }
        fout.close();
    }

    public static ArrayList<MmdCharInfo> load(String inputFileName) throws IOException {
        ArrayList<MmdCharInfo> result = new ArrayList<MmdCharInfo>();

        FileInputStream inputStream = new FileInputStream(inputFileName);
        InputStreamReader fileReader = new InputStreamReader(inputStream, "UTF-8");
        BufferedReader fin = new BufferedReader(fileReader);

        String line = fin.readLine();
        int extraBoneCount = Integer.valueOf(line);

        while (true) {
            line = fin.readLine();
            if (line != null) {
                String fileName = line.trim();
                MmdCharInfo info = new MmdCharInfo(fileName);

                String[] comps = fin.readLine().trim().split(" ");
                for (int i = 0; i < comps.length; i++) {
                    info.boneNames.add(comps[i].trim());
                }

                for (int i = 0; i < extraBoneCount; i++) {
                    line = fin.readLine();
                    comps = line.split(" ");
                    info.extraBoneVertexIndex.put(comps[0], Integer.valueOf(comps[1]));
                }
                info.processed = Boolean.valueOf(fin.readLine());
                result.add(info);
            } else {
                break;
            }
        }

        fin.close();

        return result;
    }
}
