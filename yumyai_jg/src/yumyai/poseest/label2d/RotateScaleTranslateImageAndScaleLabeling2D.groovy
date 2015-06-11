package yumyai.poseest.label2d

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import yondoko.struct.Aabb2f
import yondoko.util.ImageUtil

import javax.imageio.ImageIO
import javax.vecmath.Vector2f
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.image.BufferedImage

class RotateScaleTranslateImageAndScaleLabeling2D {
    public static void main(String[] _args) {
        if (_args.length < 1) {
            println("java yumyai.poseest.label2d.RotateScaleTranslateImageAndScaleLabeling2D <args-file>");
            System.exit(0);
        }

        def args = new JsonSlurper().parse(new File(_args[0]));
        def configs = new JsonSlurper().parse(new File(args["config_file"]));
        def settings = new JsonSlurper().parse(new File(args["settings_file"]));
        int indexStart = args["index_start"]
        String imageFilePrintf = args["image_file_printf"];
        String poseFilePrintf = args["pose_file_printf"];

        int targetWidth = settings["image_width"];
        int targetHeight = settings["image_height"];
        String backgroundMode = settings["background_mode"];

        String lastFile = null;
        BufferedImage image;
        Color backgroundColor;
        Point2D.Float[] corners = new Point2D.Float[4];
        for (int i = 0; i < configs.size(); i++) {
            def config = configs[i]
            if (config["image_file"] != lastFile) {
                lastFile = config["image_file"];
                image = ImageIO.read(new File(config["image_file"]))

                if (backgroundMode.equals("border")) {
                    backgroundColor = ImageUtil.getAverageBorderColor(image);
                } else if (backgroundMode.equals("black")) {
                    backgroundColor = Color.BLACK;
                } else {
                    throw new RuntimeException("background mode '" + backgroundMode + "' not supported");
                }
            }
            //int imageWidth = image.getWidth(null);
            //int imageHeight = image.getHeight(null);

            def points2D = new JsonSlurper().parse(new File(config["pose_file"]))["points_2d"];
            Aabb2f aabb = new Aabb2f();
            for (String jointName : points2D.keySet()) {
                def point = points2D[jointName];
                if (point != null) {
                    aabb.expandBy(point[0] as float, point[1] as float)
                }
            }
            float yExtent = aabb.getExtent(1);
            aabb.pMin.y -= settings["eye_to_head"]*yExtent;
            Vector2f oldCenter = new Vector2f();
            oldCenter.add(aabb.pMin);
            oldCenter.add(aabb.pMax);
            oldCenter.scale(0.5);
            float xExtent = aabb.getExtent(0);
            yExtent =aabb.getExtent(1);
            corners[0] = new Point2D.Float(-xExtent/2 as float, -yExtent/2 as float);
            corners[1] = new Point2D.Float( xExtent/2 as float, -yExtent/2 as float);
            corners[2] = new Point2D.Float( xExtent/2 as float,  yExtent/2 as float);
            corners[3] = new Point2D.Float(-xExtent/2 as float,  yExtent/2 as float);

            AffineTransform xform = new AffineTransform();
            xform.translate(-oldCenter.x, -oldCenter.y);

            double angleDegree = settings["rotate_min"] + config["rotation"]*(settings["rotate_max"] - settings["rotate_min"]);
            double angleRad = angleDegree * Math.PI / 180.0;

            xform.preConcatenate(AffineTransform.getRotateInstance(angleRad))

            Aabb2f rotatedAabb = new Aabb2f();
            for (int j = 0; j < 4; j++) {
                Point2D.Float p = corners[j];
                Point2D.Float pp = new Point2D.Float();
                xform.transform(p, pp);
                rotatedAabb.expandBy(pp.x as float, pp.y as float);
            }

            float scaleFactor = (settings["scale_min"] + config["scale"]*(settings["scale_max"]-settings["scale_min"])) as float;
            float width = rotatedAabb.getExtent(0) * scaleFactor;
            float height = rotatedAabb.getExtent(1) * scaleFactor;

            float ratio;
            if (width < height) {
                ratio = targetHeight * 1.0f / height;
            } else {
                ratio = targetWidth * 1.0f / width;
            }

            xform.preConcatenate(AffineTransform.getScaleInstance(ratio, ratio));

            width = ratio*width;
            height = ratio*height;
            double xDiff = targetWidth - width;
            double yDiff = targetHeight - height;

            double xShift = -xDiff/2 + xDiff*config["x_shift"];
            double yShift = -yDiff/2 + yDiff*config["y_shift"];

            xform.preConcatenate(AffineTransform.getTranslateInstance(
                    xShift + targetWidth/2.0, yShift + targetHeight/2.0));

            for (String jointName : points2D.keySet()) {
                def point = points2D[jointName];
                if (point == null)
                    continue;
                Point2D.Float p = new Point2D.Float(point[0] as float, point[1] as float);
                Point2D.Float pp = new Point2D.Float();
                xform.transform(p, pp);
                point[0] = pp.x;
                point[1] = pp.y;
            }

            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D outGraphics = outputImage.createGraphics();
            outGraphics.setBackground(backgroundColor);
            outGraphics.clearRect(0,0,targetWidth,targetHeight);
            outGraphics.drawImage(image, xform, null)
            outGraphics.dispose();

            String outputImageFileName = String.format(imageFilePrintf, i+indexStart);
            new File(outputImageFileName).parentFile.mkdirs();
            ImageIO.write(outputImage, "png", new File(outputImageFileName));

            String outputPoseFileName = String.format(poseFilePrintf, i+indexStart);
            def content = ["points_2d": points2D];
            new File(outputPoseFileName).withWriter({ fout ->
                fout.write(new JsonBuilder(content).toPrettyString());
            });

            if ((i+1) % 100 == 0)
                println("Processed up to ${indexStart+i+1} files ...");
        }
    }
}
