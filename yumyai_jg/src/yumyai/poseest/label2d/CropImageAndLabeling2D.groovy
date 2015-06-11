package yumyai.poseest.label2d

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import javax.imageio.ImageIO
import java.awt.Graphics2D
import java.awt.image.BufferedImage

class CropImageAndLabeling2D {
    public static void main(String[] _args) {
        if (_args.length < 1) {
            println("Usage: java yumyai.poseest.label2d.CropImageAndLabeling2D <args-file>");
        }

        def args = new JsonSlurper().parse(new FileReader(_args[0]))
        def dataList = new JsonSlurper().parse(new File(args["data_list"]));
        int dataCount = args["data_count"];
        int indexStart = args["index_start"];
        int indexEnd = args["index_end"];
        int targetWidth = args["target_width"];
        int targetHeight = args["target_height"];
        int limit = args["limit"];
        String dirPrintf = args["dir_printf"];
        String imageFilePrintf = args["image_file_printf"];
        String poseFilePrintf = args["pose_file_printf"];

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

                if (imageWidth < targetWidth) {
                    throw new RuntimeException("imageWidth < targetWidth");
                }
                if (imageHeight < targetHeight) {
                    throw new RuntimeException("imageHeight < targetHeight");
                }

                int leftThrowOut = (imageWidth - targetWidth) / 2;
                int topThrowOut = (imageHeight - targetHeight) / 2;

                BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D outGraphics = outputImage.createGraphics();
                outGraphics.drawImage(image, -leftThrowOut, -topThrowOut, null)
                outGraphics.dispose();

                String outputImageFileBaseName = String.format(imageFilePrintf, i);
                String outputImageFileName = String.format("%s/%s", dirName, outputImageFileBaseName);
                ImageIO.write(outputImage, "png", new File(outputImageFileName));

                def pose = new JsonSlurper().parse(new FileReader(sourcePoseFile));
                def points2D = pose["points_2d"];
                for (String jointName : points2D.keySet()) {
                    def point = points2D[jointName];
                    if (point != null) {
                        point[0] -= leftThrowOut;
                        point[1] -= topThrowOut;
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
