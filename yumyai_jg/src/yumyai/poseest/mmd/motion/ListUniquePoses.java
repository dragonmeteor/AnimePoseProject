package yumyai.poseest.mmd.motion;

import org.apache.commons.io.FilenameUtils;
import yumyai.mmd.vmd.VmdMotion;
import yumyai.mmd.vpd.VpdPose;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.ArrayList;

public class ListUniquePoses {
    VmdMotion motion;
    String[] importantBoneNames = {
            "センター",
            "左腕",
            "左手首",
            "左ひじ",
            "上半身",
            "左肩",
            "首",
            "右腕",
            "右手首",
            "右ひじ",
            "左親指１",
            "左親指２",
            "左中指１",
            "左中指２",
            "右足",
            "左中指３",
            "左足",
            "右足ＩＫ",
            "左足ＩＫ",
            "左人指１",
            "左人指３",
            "左薬指１",
            "左薬指２",
            "左人指２",
            "左薬指３",
            "左小指２",
            "左小指１",
            "下半身",
            "左小指３",
            "頭",
            "右肩",
            "左足首",
            "右足首",
            "右親指１",
            "右中指１",
            "右中指２",
            "右中指３",
            "右親指２",
            "右人指１",
            "右人指３",
            "右薬指２",
            "右薬指１",
            "右人指２",
            "右薬指３",
            "右小指２",
            "右小指１",
            "右小指３",
            "右つま先ＩＫ",
            "左つま先ＩＫ",
            "左ひざ",
            "右ひざ",
            "右手捩",
            "左手捩",
            "右腕捩",
            "左腕捩",
            "両目",
            "右目",
            "左目",
            "左つま先",
            "右つま先",
            "全ての親",
            "上半身2",
            "グルーブ"
    };

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java yumyai.poseest.mmd.motion.ListUniquePoses <motion-file>");
            System.exit(0);
        }

        ArrayList<Integer> times = getUniquePoseTimes(args[0]);

        for (Integer i : times) {
            System.out.println(i);
        }
    }

    public static ArrayList<Integer> getUniquePoseTimes(String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        if (extension.equals("vpd")) {
            ArrayList<Integer> result = new ArrayList<Integer>();
            result.add(0);
            return result;
        } else {
            try {
                VmdMotion motion = VmdMotion.load(fileName);
                System.out.println("There are " + motion.getMaxFrame() + " frames");
                return new ListUniquePoses(motion).extractUniquePoses();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ListUniquePoses(VmdMotion motion) {
        this.motion = motion;
    }

    public ArrayList<Integer> extractUniquePoses() {
        int maxFrame = motion.getMaxFrame();
        ArrayList<VpdPose> poses = new ArrayList<VpdPose>();
        ArrayList<Integer> times = new ArrayList<Integer>();

        VpdPose p0 = new VpdPose();
        motion.getPose(0, p0);
        poses.add(p0);
        times.add(0);

        float threshold = 1e-3f;
        for (int i = 1; i <= maxFrame; i++) {
            VpdPose pi = new VpdPose();
            motion.getPose(i, pi);
            boolean found = false;
            //float minDistance = Float.POSITIVE_INFINITY;
            //int minIndex = 0;
            for (int j = 0; j < poses.size(); j++) {
                VpdPose pj = poses.get(j);
                float distance = computeDistance(pi, pj);
                //if (distance < minDistance)
                //    minIndex = j;
                //minDistance = Math.min(minDistance, distance);
                if (distance < threshold) {
                    found = true;
                    break;
                }
            }
            //System.out.println(i + " minDistance = " + minDistance + ", minIndex = " + minIndex);
            if (!found) {
                poses.add(pi);
                times.add(i);
            }

            if (times.size() > 300) {
                break;
            }
        }

        if (times.size() > 300) {
            times.clear();
            for (int i = 0; i <= maxFrame; i++) {
                times.add(i);
            }
        }

        return times;
    }

    Vector3f d0 = new Vector3f();
    Vector3f d1 = new Vector3f();
    Quat4f q0 = new Quat4f();
    Quat4f q1 = new Quat4f();
    Vector3f tempD = new Vector3f();
    Quat4f tempQ = new Quat4f();

    public float computeDistance(VpdPose p0, VpdPose p1) {
        float maxDistance = Float.NEGATIVE_INFINITY;
        for (String boneName : importantBoneNames) {
            p0.getBonePose(boneName, d0, q0);
            p1.getBonePose(boneName, d1, q1);
            tempD.sub(d0,d1);
            float dd = tempD.length();
            tempQ.sub(q0, q1);
            float dq = (float)Math.sqrt(tempQ.x*tempQ.x + tempQ.y*tempQ.y + tempQ.z*tempQ.z + tempQ.w*tempQ.w);
            if (!boneName.equals("全ての親") && !boneName.equals("センター"))
                maxDistance = Math.max(maxDistance, dd);
            maxDistance = Math.max(maxDistance, dq);
            //System.out.println(boneName + " " + p0.hasBone(boneName) + " " + p1.hasBone(boneName));
            //System.out.println("dd = " + dd + ", dq = " + dq);
        }
        return maxDistance;
    }
}
