package yumyai.poseest.yang_ramanan;

import javax.vecmath.Point2f;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class YangRamananData {
    public ArrayList<String> jointNames = new ArrayList<String>();
    public ArrayList<Item> items = new ArrayList<Item>();

    public static class Item {
        public String imageName = null;
        public ArrayList<Point2f> points = new ArrayList<Point2f>();
    }

    public static YangRamananData load(String fileName) throws IOException {
        YangRamananData result = new YangRamananData();

        File file = new File(fileName);
        FileReader fileReader = new FileReader(file);
        BufferedReader fin = new BufferedReader(fileReader);

        int jointCount = Integer.valueOf(fin.readLine());
        for (int i = 0; i < jointCount; i++) {
            String jointName = fin.readLine().trim();
            result.jointNames.add(jointName);
        }

        int itemCount = Integer.valueOf(fin.readLine());
        for (int i = 0; i < itemCount; i++) {
            String imageName = fin.readLine().trim();
            Item item = new Item();
            item.imageName = imageName;
            for (int j = 0; j < jointCount; j++) {
                String[] comps = fin.readLine().split(" ");
                item.points.add(new Point2f(Float.valueOf(comps[0]), Float.valueOf(comps[1])));
            }
            result.items.add(item);
        }

        fin.close();

        return result;
    }
}
