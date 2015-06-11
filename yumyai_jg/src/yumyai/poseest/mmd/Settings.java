package yumyai.poseest.mmd;

import javax.vecmath.Color3f;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class Settings {
    public static final String EXTRA_NOSE_TIP_NAME = "extra_noseTip";
    public static final String EXTRA_NOSE_ROOT_NAME = "extra_noseRoot";
    public static final String EXTRA_TIPTOE_RIGHT_NAME = "extra_tipToeRight";
    public static final String EXTRA_TIPTOE_LEFT_NAME = "extra_tipToeLeft";

    public static final float MORPH_DISPLACEMENT_THRESHOLD = 0.05f;
    public static final float DEFAULT_FOVY = 12.0f;
    public static final float DEFAULT_CAMERA_SCALE_FACTOR = 1.025f;

    public static final ArrayList<String> relevantBones = new ArrayList<String>();
    public static final ArrayList<String> displayBones = new ArrayList<String>();
    public static final ArrayList<String> bonesToLabel = new ArrayList<String>();
    public static final ArrayList<String> necessaryBones = new ArrayList<String>();
    public static final HashMap<String, Color3f> boneColors = new HashMap<String, Color3f>();
    public static final HashMap<String, String> boneParentNames = new HashMap<String, String>();
    public static final ArrayList<HashSet<String>> characterViewBones = new ArrayList<HashSet<String>>();
    public static final ArrayList<HashSet<String>> characterViewMorphs = new ArrayList<HashSet<String>>();
    public static final ArrayList<String> extraBoneNames = new ArrayList<String>();
    //public static final HashMap<String, String> extraBoneParentNames = new HashMap<String, String>();
    public static final HashMap<String, Color3f> extraBoneColor = new HashMap<String, Color3f>();
    public static final HashMap<String, String> displayBoneEnglishNames = new HashMap<String, String>();
    public static final HashMap<String, String> displayBoneJapaneseNames = new HashMap<String, String>();
    public static final HashMap<String, String> bonesToLabelParent = new HashMap<String, String>();

    static {
        relevantBones.addAll(Arrays.asList(
                "全ての親", "グループ", "センター",
                "上半身", "上半身２", "下半身", "首", "頭",
                "左目", "右目",
                "左肩", "左腕", "左ひじ", "左手首", "左親指１",
                "左足", "左ひざ", "左足首", "左つま先ＩＫ",
                "右肩", "右腕", "右ひじ", "右手首", "右親指１",
                "右足", "右ひざ", "右足首", "右つま先ＩＫ"
        ));

        displayBones.addAll(Arrays.asList(
                "上半身", "下半身", "首", "頭",
                "左肩", "左腕", "左ひじ", "左手首", "左親指１",
                "左足", "左ひざ", "左足首",
                "右肩", "右腕", "右ひじ", "右手首", "右親指１",
                "右足", "右ひざ", "右足首",
                EXTRA_NOSE_TIP_NAME,
                EXTRA_NOSE_ROOT_NAME,
                EXTRA_TIPTOE_RIGHT_NAME,
                EXTRA_TIPTOE_LEFT_NAME
        ));

        bonesToLabel.addAll(Arrays.asList(
                "上半身", "首", "頭", EXTRA_NOSE_TIP_NAME, EXTRA_NOSE_ROOT_NAME,
                "左腕", "左ひじ", "左手首", "左親指１",
                "右腕", "右ひじ", "右手首", "右親指１",
                "左足", "左ひざ", "左足首", EXTRA_TIPTOE_LEFT_NAME,
                "右足", "右ひざ", "右足首", EXTRA_TIPTOE_RIGHT_NAME
        ));

        bonesToLabelParent.put("上半身", null);
        bonesToLabelParent.put("首", "上半身");
        bonesToLabelParent.put("頭", "首");
        bonesToLabelParent.put(EXTRA_NOSE_TIP_NAME, "頭");
        bonesToLabelParent.put(EXTRA_NOSE_ROOT_NAME, "頭");
        bonesToLabelParent.put("左腕", "首");
        bonesToLabelParent.put("左ひじ", "左腕");
        bonesToLabelParent.put("左手首", "左ひじ");
        bonesToLabelParent.put("左親指１", "左手首");
        bonesToLabelParent.put("右腕", "首");
        bonesToLabelParent.put("右ひじ", "右腕");
        bonesToLabelParent.put("右手首", "右ひじ");
        bonesToLabelParent.put("右親指１", "右手首");
        bonesToLabelParent.put("左足", "上半身");
        bonesToLabelParent.put("左ひざ", "左足");
        bonesToLabelParent.put("左足首", "左ひざ");
        bonesToLabelParent.put(EXTRA_TIPTOE_LEFT_NAME, "左足首");
        bonesToLabelParent.put("右足", "上半身");
        bonesToLabelParent.put("右ひざ", "右足");
        bonesToLabelParent.put("右足首", "右ひざ");
        bonesToLabelParent.put(EXTRA_TIPTOE_RIGHT_NAME, "右足首");

        displayBoneEnglishNames.put("上半身", "body_upper");
        displayBoneEnglishNames.put("下半身", "body_lower");
        displayBoneEnglishNames.put("首", "neck");
        displayBoneEnglishNames.put("頭", "head");
        displayBoneEnglishNames.put("左肩", "shoulder_left");
        displayBoneEnglishNames.put("左腕", "arm_left");
        displayBoneEnglishNames.put("左ひじ", "elbow_left");
        displayBoneEnglishNames.put("左手首", "wrist_left");
        displayBoneEnglishNames.put("左親指１", "thumb_left");
        displayBoneEnglishNames.put("左足", "leg_left");
        displayBoneEnglishNames.put("左ひざ", "knee_left");
        displayBoneEnglishNames.put("左足首", "ankle_left");
        displayBoneEnglishNames.put("右肩", "shoulder_right");
        displayBoneEnglishNames.put("右腕", "arm_right");
        displayBoneEnglishNames.put("右ひじ", "elbow_right");
        displayBoneEnglishNames.put("右手首", "wrist_right");
        displayBoneEnglishNames.put("右親指１", "thumb_right");
        displayBoneEnglishNames.put("右足", "leg_right");
        displayBoneEnglishNames.put("右ひざ", "knee_right");
        displayBoneEnglishNames.put("右足首", "ankle_right");
        displayBoneEnglishNames.put(EXTRA_NOSE_TIP_NAME, "nose_tip");
        displayBoneEnglishNames.put(EXTRA_NOSE_ROOT_NAME, "nose_root");
        displayBoneEnglishNames.put(EXTRA_TIPTOE_RIGHT_NAME, "tiptoe_right");
        displayBoneEnglishNames.put(EXTRA_TIPTOE_LEFT_NAME, "tiptoe_left");

        displayBoneJapaneseNames.put("body_upper", "上半身");
        displayBoneJapaneseNames.put("body_lower", "下半身");
        displayBoneJapaneseNames.put("neck", "首");
        displayBoneJapaneseNames.put("head", "頭");
        displayBoneJapaneseNames.put("shoulder_left", "左肩");
        displayBoneJapaneseNames.put("arm_left", "左腕");
        displayBoneJapaneseNames.put("elbow_left", "左ひじ");
        displayBoneJapaneseNames.put("wrist_left", "左手首");
        displayBoneJapaneseNames.put("thumb_left", "左親指１");
        displayBoneJapaneseNames.put("leg_left", "左足");
        displayBoneJapaneseNames.put("knee_left", "左ひざ");
        displayBoneJapaneseNames.put("ankle_left", "左足首");
        displayBoneJapaneseNames.put("shoulder_right", "右肩");
        displayBoneJapaneseNames.put("arm_right", "右腕");
        displayBoneJapaneseNames.put("elbow_right", "右ひじ");
        displayBoneJapaneseNames.put("wrist_right", "右手首");
        displayBoneJapaneseNames.put("thumb_right", "右親指１");
        displayBoneJapaneseNames.put("leg_right", "右足");
        displayBoneJapaneseNames.put("knee_right", "右ひざ");
        displayBoneJapaneseNames.put("ankle_right", "右足首");
        displayBoneJapaneseNames.put("nose_tip", EXTRA_NOSE_TIP_NAME);
        displayBoneJapaneseNames.put("nose_root", EXTRA_NOSE_ROOT_NAME);
        displayBoneJapaneseNames.put("tiptoe_right", EXTRA_TIPTOE_RIGHT_NAME);
        displayBoneJapaneseNames.put("tiptoe_left", EXTRA_TIPTOE_LEFT_NAME);

        necessaryBones.addAll(Arrays.asList(
                "センター",
                "上半身", "下半身", "首", "頭",
                "左目", "右目", "両目",
                "左肩", "左腕", "左ひじ", "左手首", "左親指１",
                "左足", "左ひざ", "左足首", "左つま先ＩＫ",
                "右肩", "右腕", "右ひじ", "右手首", "右親指１",
                "右足", "右ひざ", "右足首", "右つま先ＩＫ"
        ));

        extraBoneNames.addAll(Arrays.asList(
                EXTRA_NOSE_TIP_NAME,
                EXTRA_NOSE_ROOT_NAME,
                EXTRA_TIPTOE_RIGHT_NAME,
                EXTRA_TIPTOE_LEFT_NAME
        ));
        /*
        extraBoneParentNames.put(EXTRA_NOSE_TIP_NAME, "頭");
        extraBoneParentNames.put(EXTRA_NOSE_ROOT_NAME, "頭");
        extraBoneParentNames.put(EXTRA_TIPTOE_RIGHT_NAME, "右足首");
        extraBoneParentNames.put(EXTRA_TIPTOE_LEFT_NAME, "左足首");
        */

        extraBoneColor.put(EXTRA_NOSE_TIP_NAME, new Color3f(1,0,0));
        extraBoneColor.put(EXTRA_NOSE_ROOT_NAME, new Color3f(0,1,0));
        extraBoneColor.put(EXTRA_TIPTOE_RIGHT_NAME, new Color3f(1,0,1));
        extraBoneColor.put(EXTRA_TIPTOE_LEFT_NAME, new Color3f(0,1,1));

        // Full body
        characterViewBones.add(new HashSet<String>(Arrays.asList(
                "上半身", "下半身", "首", "頭",
                "左目", "右目",
                "左肩", "左腕", "左ひじ", "左手首", "左親指１",
                "左足", "左ひざ", "左足首",
                "右肩", "右腕", "右ひじ", "右手首", "右親指１",
                "右足", "右ひざ", "右足首"
                //"左つま先",
                //"右つま先"
        )));
        characterViewMorphs.add(new HashSet<String>());
        // Everything but things lower than knee
        characterViewBones.add(new HashSet<String>(Arrays.asList(
                "上半身", "首", "頭",
                "左目", "右目",
                "左肩", "左腕", "左ひじ", "左手首", "左親指１",
                "左足",
                "右肩", "右腕", "右ひじ", "右手首", "右親指１",
                "右足"
        )));
        characterViewMorphs.add(new HashSet<String>());
        // Upper body
        characterViewBones.add(new HashSet<String>(Arrays.asList(
                "上半身", "首", "頭",
                "左目", "右目",
                "左肩", "右肩"
        )));
        characterViewMorphs.add(new HashSet<String>());
        // Head
        characterViewBones.add(new HashSet<String>(Arrays.asList(
                "首", "頭",
                "左目", "右目"
        )));
        characterViewMorphs.add(new HashSet<String>());
        // Face
        characterViewBones.add(new HashSet<String>(Arrays.asList(
                "左目", "右目"
        )));
        characterViewMorphs.add(new HashSet<String>(Arrays.asList("あ", "まばたき")));

        boneColors.put("全ての親", new Color3f(0, 0, 0));
        boneColors.put("グループ", new Color3f(0, 0, 0));
        boneColors.put("センター", new Color3f(0.5f, 0.5f, 0.5f));
        boneColors.put("上半身", new Color3f(0.5f, 0, 0));
        boneColors.put("上半身2", new Color3f(0, 0, 0));
        boneColors.put("首", new Color3f(0.75f, 0, 0));
        boneColors.put("頭", new Color3f(1, 0, 0));
        boneColors.put("下半身", new Color3f(0.5f, 0.5f, 0.5f));
        boneColors.put("左目", new Color3f(0, 0, 0));
        boneColors.put("右目", new Color3f(0, 0, 0));
        boneColors.put("左肩", new Color3f(0, 0, 0.25f));
        boneColors.put("左腕", new Color3f(0, 0, 0.5f));
        boneColors.put("左ひじ", new Color3f(0, 0, 0.75f));
        boneColors.put("左手首", new Color3f(0, 0, 1));
        boneColors.put("左親指１", new Color3f(0.5f, 0.5f, 1));
        boneColors.put("左足", new Color3f(0.25f, 0, 0.25f));
        boneColors.put("左ひざ", new Color3f(0.5f, 0, 0.5f));
        boneColors.put("左足首", new Color3f(0.75f, 0, 0.75f));
        boneColors.put("左つま先ＩＫ", new Color3f(0, 0, 0));
        boneColors.put(EXTRA_TIPTOE_LEFT_NAME, new Color3f(1, 0, 1));
        boneColors.put("右肩", new Color3f(0, 0.25f, 0));
        boneColors.put("右腕", new Color3f(0, 0.5f, 0));
        boneColors.put("右ひじ", new Color3f(0, 0.75f, 0));
        boneColors.put("右手首", new Color3f(0, 1, 0));
        boneColors.put("右親指１", new Color3f(0.5f, 1, 0.5f));
        boneColors.put("右足", new Color3f(0.25f, 0.25f, 0));
        boneColors.put("右ひざ", new Color3f(0.5f, 0.5f, 0));
        boneColors.put("右足首", new Color3f(0.75f, 0.75f, 0));
        boneColors.put("右つま先ＩＫ", new Color3f(0, 0, 0));
        boneColors.put(EXTRA_TIPTOE_RIGHT_NAME, new Color3f(1, 1, 0));
        boneColors.put(EXTRA_NOSE_TIP_NAME, new Color3f(1, 0.25f, 0.25f));
        boneColors.put(EXTRA_NOSE_ROOT_NAME, new Color3f(1, 0.5f, 0.5f));

        boneParentNames.put("全ての親", null);
        boneParentNames.put("グループ", null);
        boneParentNames.put("センター", null);
        boneParentNames.put("上半身", null);
        boneParentNames.put("上半身2", null);
        boneParentNames.put("首", "上半身");
        boneParentNames.put("頭", "首");
        boneParentNames.put("左目", "頭");
        boneParentNames.put("右目", "頭");
        boneParentNames.put("下半身", null);
        boneParentNames.put("左肩", "首");
        boneParentNames.put("左腕", "左肩");
        boneParentNames.put("左ひじ", "左腕");
        boneParentNames.put("左手首", "左ひじ");
        boneParentNames.put("左親指１", "左手首");
        boneParentNames.put("左足", "下半身");
        boneParentNames.put("左ひざ", "左足");
        boneParentNames.put("左足首", "左ひざ");
        boneParentNames.put("左つま先ＩＫ", "左足首");
        boneParentNames.put(EXTRA_TIPTOE_LEFT_NAME, "左足首");
        boneParentNames.put("右肩", "首");
        boneParentNames.put("右腕", "右肩");
        boneParentNames.put("右ひじ", "右腕");
        boneParentNames.put("右手首", "右ひじ");
        boneParentNames.put("右親指１", "右手首");
        boneParentNames.put("右足", "下半身");
        boneParentNames.put("右ひざ", "右足");
        boneParentNames.put("右足首", "右ひざ");
        boneParentNames.put("右つま先ＩＫ", "右足首");
        boneParentNames.put(EXTRA_TIPTOE_RIGHT_NAME, "右足首");
        boneParentNames.put(EXTRA_NOSE_TIP_NAME, "頭");
        boneParentNames.put(EXTRA_NOSE_ROOT_NAME, "頭");
    }
}
