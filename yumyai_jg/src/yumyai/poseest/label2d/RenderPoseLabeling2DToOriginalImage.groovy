package yumyai.poseest.label2d

import groovy.json.JsonSlurper
import org.apache.commons.lang3.tuple.Pair

import javax.imageio.ImageIO
import javax.vecmath.Color3f
import javax.vecmath.Point2f
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.image.BufferedImage

class RenderPoseLabeling2DToOriginalImage {
    public static void main(String[] _args) {
        if (_args.length < 1) {
            println("Usage: java yumyai.poseest.label2d.RenderPoseLabeling2DToOriginalImage <args-file>");
            System.exit(-1);
        }

        def args = new JsonSlurper().parse(new File(_args[0]));
        String labelingFileName = args["labeling_2d_file"];
        String originalDataListFileName = args["original_data_list_file"]
        String configFileName = args["pose_2d_config_file"];
        int resizedImageWidth = args["resized_image_width"];
        int resizedImageHeight = args["resized_image_height"];
        String outputFilePrintf = args["output_file_printf"];
        float lineWidth = 3;
        if (args.containsKey("line_width")) {
            lineWidth = args["line_width"];
        }
        float pointWidth = 7;
        if (args.containsKey("point_width")) {
            pointWidth = args["point_width"];
        }

        def originalDataList = new JsonSlurper().parse(new File(originalDataListFileName));
        ArrayList<PoseLabeling2D> labelings = PoseLabeling2D.load(labelingFileName);
        if (labelings.size() != originalDataList.size()) {
            println("Labeling size != original data list size!!!");
            System.exit(-1);
        }
        Pose2DConfig config = Pose2DConfig.load(configFileName);

        for (int i = 0; i < labelings.size(); i++) {
            PoseLabeling2D labeling = labelings[i];
            BufferedImage sourceImage = ImageIO.read(new File(originalDataList[i][0]));
            BufferedImage outputImage = new BufferedImage(sourceImage.getWidth(null), sourceImage.getHeight(null),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = outputImage.getGraphics();
            g.drawImage(sourceImage, 0, 0, null);
            g.setStroke(new BasicStroke(lineWidth));

            double factorX = sourceImage.getWidth(null) * 1.0  / resizedImageWidth;
            double factorY = sourceImage.getHeight(null) * 1.0 / resizedImageHeight;
            double factor = Math.max(factorX, factorY);

            for(Pair<String, String> edge : config.edges) {
                String parent = edge.left;
                String child = edge.right;
                if (labeling.points.containsKey(parent) && labeling.points.containsKey(child)) {
                    Color3f color3f = config.boneColors[child];
                    Color color = new Color(color3f.x, color3f.y, color3f.z);
                    g.setColor(color);

                    Point2f parentPos = labeling.points[parent].clone();
                    Point2f childPos = labeling.points[child].clone();

                    parentPos.x = ((parentPos.x - resizedImageWidth * 0.5f) * factor + sourceImage.getWidth(null)*0.5f) as float;
                    parentPos.y = ((parentPos.y - resizedImageHeight * 0.5f) * factor + sourceImage.getHeight(null)*0.5f) as float;
                    childPos.x = ((childPos.x - resizedImageWidth * 0.5f) * factor + sourceImage.getWidth(null)*0.5f) as float;
                    childPos.y = ((childPos.y - resizedImageHeight * 0.5f) * factor + sourceImage.getHeight(null)*0.5f) as float;

                    g.draw(new Line2D.Double(parentPos.x, parentPos.y, childPos.x, childPos.y));
                }
            }

            g.setStroke(new BasicStroke(1));
            for(String boneName : config.boneNames) {
                if (labeling.points.containsKey(boneName)) {
                    Color3f color3f = config.boneColors[boneName];
                    Color color = new Color(color3f.x, color3f.y, color3f.z);
                    g.setColor(color);
                    g.setPaint(color);
                    Point2f pos = labeling.points[boneName].clone();
                    pos.x = ((pos.x - resizedImageWidth * 0.5f) * factor + sourceImage.getWidth(null)*0.5f) as float;
                    pos.y = ((pos.y - resizedImageHeight * 0.5f) * factor + sourceImage.getHeight(null)*0.5f) as float;
                    g.fill(new Ellipse2D.Double(pos.x - pointWidth/2, pos.y - pointWidth/2, pointWidth, pointWidth));
                }
            }

            ImageIO.write(outputImage, "png", new File(String.format(outputFilePrintf, i)));

            if ((i+1) % 100 == 0) {
                println("Processed ${i+1} files ...");
            }
        }
    }
}
