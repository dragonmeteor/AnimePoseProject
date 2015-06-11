package yumyai.poseest.label2d

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import yondoko.util.ImageUtil

import javax.imageio.ImageIO
import javax.vecmath.Color3f
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

class ResizeImageAndLabeling2D {
    public static void main(String[] _args) {
        if (_args.length < 1) {
            println("Usage: java yumyai.poseest.label2d.ResizeIamgeAndLabeling2D <args-file>");
        }

        def args = new JsonSlurper().parse(new FileReader(_args[0]))
        def dataList = new JsonSlurper().parse(new File(args["data_list"]));
        int dataCount = args["data_count"];
        int indexStart = args["index_start"];
        int indexEnd = args["index_end"];
        int targetWidth = args["target_width"];
        int targetHeight = args["target_height"];
        int limit = args["limit"];
        String backgroundMode = args["background_mode"].toString().toLowerCase();
        String dirPrintf = args["dir_printf"];
        String imageFilePrintf = args["image_file_printf"];
        String poseFilePrintf = args["pose_file_printf"];

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

                float ratio;
                if (imageWidth < imageHeight) {
                    ratio = targetHeight * 1.0f / imageHeight;
                } else {
                    ratio = targetWidth * 1.0f / imageWidth;
                }

                int scaledWidth = (int)(ratio * imageWidth);
                int scaledHeight = (int)(ratio * imageHeight);

                int xOffset = (targetWidth - scaledWidth) / 2;
                int yOffset = (targetHeight - scaledHeight) / 2;

                BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D outGraphics = outputImage.createGraphics();
                outGraphics.setBackground(backgroundColor);
                outGraphics.clearRect(0,0,targetWidth,targetHeight);
                outGraphics.drawImage(image, xOffset, yOffset, scaledWidth, scaledHeight, null)
                outGraphics.dispose();

                String outputImageFileBaseName = String.format(imageFilePrintf, i);
                String outputImageFileName = String.format("%s/%s", dirName, outputImageFileBaseName);
                ImageIO.write(outputImage, "png", new File(outputImageFileName));

                def pose = new JsonSlurper().parse(new FileReader(sourcePoseFile));
                def points2D = pose["points_2d"];
                for (String jointName : points2D.keySet()) {
                    def point = points2D[jointName];
                    if (point != null) {
                        point[0] = (point[0]*1.0f*scaledWidth / imageWidth) + xOffset;
                        point[1] = (point[1]*1.0f*scaledHeight / imageHeight) + yOffset;
                    }
                }
                String content = new JsonBuilder(pose).toPrettyString();

                String outputPoseFileBaseName = String.format(poseFilePrintf, i);
                String outputPoseFileName = String.format("%s/%s", dirName, outputPoseFileBaseName);
                new File(outputPoseFileName).withWriter({ fout ->
                    fout.write(content);
                });

                if ((i+1) % 100 == 0) {
                    println("Processed ${i+1} files ...");
                }

            } catch (Exception e) {
                println("There are some error processing Example ${i}");
                println("Image file: ${sourceImageFile}");
                println("Pose file: ${sourcePoseFile}");
                println("Error message: ${e.getMessage()}")
            }

        }
    }
}
