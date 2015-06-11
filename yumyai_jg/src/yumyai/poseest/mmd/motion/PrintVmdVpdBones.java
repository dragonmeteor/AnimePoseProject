package yumyai.poseest.mmd.motion;

import org.apache.commons.io.FilenameUtils;
import yumyai.mmd.vmd.VmdMotion;
import yumyai.mmd.vpd.VpdPose;

import java.util.ArrayList;

public class PrintVmdVpdBones {
    public static ArrayList<String> getBoneList(String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        ArrayList<String> result = new ArrayList<String>();
        try {
            if (extension.equals("vpd")) {
                VpdPose pose = VpdPose.load(fileName);
                for (String boneName : pose.boneNames()) {
                    result.add(boneName);
                }
            } else if (extension.equals("vmd")) {
                VmdMotion motion = VmdMotion.load(fileName);
                for (String boneName : motion.boneMotions.keySet()) {
                    result.add(boneName);
                }
            } else {
                throw new RuntimeException("Unsupported extension: '" + extension + "'");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java yumyai.poseest.mmd.motion.PrintVmdVpdBones <file-name>");
            System.exit(0);
        }

        ArrayList<String> bones = getBoneList(args[0]);

        for (String bone : bones) {
            System.out.println(bone);
        }
    }
}
