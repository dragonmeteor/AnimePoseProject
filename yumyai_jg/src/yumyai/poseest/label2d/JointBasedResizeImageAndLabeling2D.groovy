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

class JointBasedResizeImageAndLabeling2D {
    public static void main(String[] _args) {
        if (_args.length < 1) {
            println("Usage: java yumyai.poseest.label2d.JointBasedResizeIamgeAndLabeling2D <args-file>");
        }

        def args = new JsonSlurper().parse(new FileReader(_args[0]))
        def dataList = new JsonSlurper().parse(new File(args["data_list"]));
        int dataCount = args["data_count"];
        int indexStart = args["index_start"];
        int indexEnd = args["index_end"];
        int targetWidth = args["target_width"];
        int targetHeight = args["target_height"];
        int limit = args["limit"];
        float scaleFactor = args["scale_factor"];
        String dirPrintf = args["dir_printf"];
        String imageFilePrintf = args["image_file_printf"];
        String poseFilePrintf = args["pose_file_printf"];
        String backgroundMode = args["background_mode"].toString().toLowerCase();
        float eyeToHead = args["eye_to_head"];

        if (targetWidth != targetHeight) {
            println("targetWidth != targetHeight not supported");
            System.exit(0);
        }

        for (int i = indexStart; i < indexEnd; i++) {
            if (i >= dataCount) {
                break;
            }

            String dirName = String.format(dirPrintf, (int)(i / limit));
            File dir = new File(dirName);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String sourceImageFile = dataList[i][0];
            String sourcePoseFile = dataList[i][1];
            try {
                BufferedImage image = ImageIO.read(new File(sourceImageFile))
                int imageWidth = image.getWidth(null);
                int imageHeight = image.getHeight(null);

                Color backgroundColor;
                if (backgroundMode.equals("border")) {
                    backgroundColor = ImageUtil.getAverageBorderColor(image);
                } else if (backgroundMode.equals("black")) {
                    backgroundColor = Color.BLACK;
                } else {
                    throw new RuntimeException("background mode '" + backgroundMode + "' not supported");
                }

                def pose = new JsonSlurper().parse(new FileReader(sourcePoseFile));
                def points2D = pose["points_2d"];
                Aabb2f aabb = new Aabb2f();
                for (String jointName : points2D.keySet()) {
                    def point = points2D[jointName];
                    if (point != null) {
                        aabb.expandBy(point[0] as float, point[1] as float)
                    }
                }
                float yExtent = aabb.getExtent(1);
                aabb.pMin.y -= eyeToHead*yExtent;
                Vector2f oldCenter = new Vector2f();
                oldCenter.add(aabb.pMin);
                oldCenter.add(aabb.pMax);
                oldCenter.scale(0.5);

                AffineTransform xform = new AffineTransform();
                xform.translate(-oldCenter.x, -oldCenter.y);

                float width = aabb.getExtent(0) * scaleFactor;
                float height = aabb.getExtent(1) * scaleFactor;

                float ratio;
                if (width < height) {
                    ratio = targetHeight * 1.0f / height;
                } else {
                    ratio = targetWidth * 1.0f / width;
                }

                xform.preConcatenate(AffineTransform.getScaleInstance(ratio, ratio));

                xform.preConcatenate(AffineTransform.getTranslateInstance(targetWidth/2.0, targetHeight/2.0));

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
                ImageIO.write(outputImage, "png", new File(dirName + File.separator + outputImageFileName));

                String outputPoseFileName = dirName + File.separator + String.format(poseFilePrintf, i+indexStart);
                def content = ["points_2d": points2D];
                new File(outputPoseFileName).withWriter({ fout ->
                    fout.write(new JsonBuilder(content).toPrettyString());
                });

                if ((i+1) % 100 == 0)
                    println("Processed up to ${indexStart+i+1} files ...");

            } catch (Exception e) {
                throw(e);
                println("There are some error processing Example ${i}");
                println("Image file: ${sourceImageFile}");
                println("Pose file: ${sourcePoseFile}");
                println("Error message: ${e.getMessage()}")
            }
        }
    }
}
