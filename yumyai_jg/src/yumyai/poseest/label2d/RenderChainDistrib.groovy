package yumyai.poseest.label2d

import groovy.json.JsonSlurper

import javax.imageio.ImageIO
import javax.vecmath.Vector2d
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D
import java.awt.image.BufferedImage

class RenderChainDistrib {
    public static void main(String[] _args) {
        if (_args.length < 1) {
            println("Usage: java yumyai.poseest.label2d.RenderChainDistrib <args-file>");
            System.exit(0);
        }

        def args = new JsonSlurper().parse(new File(_args[0]));
        def dataList = new JsonSlurper().parse(new File(args["data_list_file"]));
        def poseConfig = new JsonSlurper().parse(new File(args["pose_2d_config_file"]));
        def chain = args["chain"];
        int imageWidth = args["image_width"];
        int imageHeight = args["image_height"];
        String outputFileName = args["output_file"];
        def colors = args["colors"];
        int chainLength = chain.size();

        Vector2d[] chainPoints = new Vector2d[chainLength];
        for (int i = 0; i < chainLength; i++) {
            chainPoints[i] = new Vector2d();
        }

        BufferedImage output = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.getGraphics();
        graphics.setBackground(Color.WHITE);
        graphics.clearRect(0, 0, imageWidth, imageHeight);

        for (int i = 0; i < dataList.size(); i++) {
            String poseFile = dataList[i][1];
            def points = new JsonSlurper().parse(new File(poseFile))["points_2d"];

            for (int j = 0; j < chainLength; j++) {
                String jointName = chain[j];
                def p = points[jointName];
                chainPoints[j].x = p[0];
                chainPoints[j].y = p[1];
            }
            for (int j = 1; j < chainLength; j++) {
                chainPoints[j].x -= chainPoints[0].x - imageWidth / 2.0;
                chainPoints[j].y -= chainPoints[0].y - imageHeight / 2.0;
            }
            chainPoints[0].x = imageWidth / 2.0;
            chainPoints[0].y = imageHeight / 2.0;

            for (int j = 0; j < chainLength-1; j++) {
                String jointName = chain[j];
                def color = colors[j];
                graphics.setColor(new Color(color[0] as float, color[1] as float, color[2] as float));
                graphics.draw(new Line2D.Double(
                        chainPoints[j].x, chainPoints[j].y,
                        chainPoints[j+1].x, chainPoints[j+1].y));
            }

            if ((i+1) % 100 == 0) {
                println("Processed ${i+1} files ...");
            }
        }

        ImageIO.write(output, "png", new File(outputFileName));
    }
}
