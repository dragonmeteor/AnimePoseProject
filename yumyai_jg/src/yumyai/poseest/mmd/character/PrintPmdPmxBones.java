package yumyai.poseest.mmd.character;

import org.apache.commons.io.FilenameUtils;
import yumyai.mmd.pmd.PmdBone;
import yumyai.mmd.pmd.PmdModel;
import yumyai.mmd.pmx.PmxBone;
import yumyai.mmd.pmx.PmxModel;

import java.util.ArrayList;

public class PrintPmdPmxBones {
    public static ArrayList<String> getBoneList(String filename) {
        String extension = FilenameUtils.getExtension(filename).toLowerCase();
        ArrayList<String> result = new ArrayList<String>();
        if (extension.equals("pmd")) {
            try {
                PmdModel model = PmdModel.load(filename);
                for (PmdBone bone : model.bones) {
                    result.add(bone.japaneseName);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (extension.equals("pmx")) {
            try {
                PmxModel model = PmxModel.load(filename);
                for (int i = 0; i < model.getBoneCount(); i++) {
                    PmxBone bone = model.getBone(i);
                    result.add(bone.japaneseName);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Unsupported extension '" + extension + "'");
        }
        return result;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: yumyai.poseest.mmd.character.PrintPmdPmxBones <file>");
        }

        ArrayList<String> boneNames = getBoneList(args[0]);

        for(String boneName : boneNames) {
            System.out.println(boneName);
        }
    }
}
