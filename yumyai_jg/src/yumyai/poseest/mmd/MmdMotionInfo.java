package yumyai.poseest.mmd;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class MmdMotionInfo {
    public String fileName;
    public ArrayList<Integer> frames = new ArrayList<Integer>();
    public HashSet<String> requiredBones = new HashSet<String>();

    public String getFrameIntervalsString() {
        String result = "";
        int lastIntervalStart = -2;
        int lastIntervalEnd = -2;
        for (int frame : frames) {
            if (frame > lastIntervalEnd+1) {
                if (lastIntervalEnd >= 0) {
                    result += getIntervalString(lastIntervalStart, lastIntervalEnd) + " ";
                }
                lastIntervalStart = frame;
                lastIntervalEnd = frame;
            } else {
                lastIntervalEnd = frame;
            }
        }
        result += getIntervalString(lastIntervalStart, lastIntervalEnd);
        return result;
    }

    private String getIntervalString(int start, int end) {
        if (start == end) {
            return Integer.toString(start);
        } else {
            return String.format("%d-%d", start, end);
        }
    }

    public static ArrayList<MmdMotionInfo> load(String fName) throws IOException {
        ArrayList<MmdMotionInfo> motionInfos = new ArrayList<MmdMotionInfo>();

        FileInputStream inputStream = new FileInputStream(fName);
        InputStreamReader fileReader = new InputStreamReader(inputStream, "UTF-8");
        BufferedReader f = new BufferedReader(fileReader);

        String line = f.readLine();
        while (line != null) {
            MmdMotionInfo motionInfo = new MmdMotionInfo();
            motionInfo.fileName = line.trim();
            line = f.readLine().trim();
            String[] comps = line.split(" ");
            for (int i = 0; i < comps.length; i++) {
                if (comps[i].contains("-")) {
                    String[] c = comps[i].split("-");
                    int start = Integer.valueOf(c[0]);
                    int end = Integer.valueOf(c[1]);
                    for (int j = start; j <= end; j++) {
                        motionInfo.frames.add(j);
                    }
                } else {
                    motionInfo.frames.add(Integer.valueOf(comps[i]));
                }
            }
            line = f.readLine();
            comps = line.split(" ");
            for (String boneName : comps) {
                motionInfo.requiredBones.add(boneName);
            }
            motionInfos.add(motionInfo);
            line = f.readLine();
        }

        f.close();

        return motionInfos;
    }

    public static void save(String fName, Collection<MmdMotionInfo> motionInfos) throws IOException {
        OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(fName), "UTF-8");
        BufferedWriter fout = new BufferedWriter(fileWriter);

        for(MmdMotionInfo motionInfo : motionInfos) {
            fout.write(motionInfo.fileName + "\n");
            fout.write(motionInfo.getFrameIntervalsString() + "\n");
            for (String boneName : motionInfo.requiredBones) {
                fout.write(boneName + " ");
            }
            fout.write("\n");
        }

        fout.close();
    }
}