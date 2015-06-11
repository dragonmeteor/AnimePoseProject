package yumyai.poseest.mmd.background

import yondoko.util.ArgumentProcessor
import yondoko.util.ImageUtil
import yumyai.poseest.mmd.example.GenRandomTrainingExampleModelMotionBackground

import javax.imageio.ImageIO
import java.awt.Image
import java.awt.image.BufferedImage

class GenRandomSubImages {
    String imageFileName
    int imageSize
    int imageCount
    String outputFileFormat
    ArrayList<String> imageFiles;

    public static void main(String[] args) {
        new GenRandomSubImages().run(args)
    }

    public void run(String[] args) {
        if (args.length < 4) {
            println("Usage: java yumyai.poseest.mmd.background.GenRandomSubImages " +
                    "<image-list> <image-size> <count> <output-file-format>")
            System.exit(-1)
        }

        ArgumentProcessor argProc = new ArgumentProcessor(args)
        imageFileName = argProc.getString()
        imageSize = argProc.getInt()
        imageCount = argProc.getInt()
        outputFileFormat = argProc.getString()

        imageFiles = GenRandomTrainingExampleModelMotionBackground.loadBackgroundFile(imageFileName)

        Random random = new Random();
        for (int i = 0; i < imageCount; i++) {
            int imageIndex = random.nextInt(imageFiles.size());

            BufferedImage rawBg = ImageIO.read(new File(imageFiles[imageIndex]))
            int rawSize = Math.min(rawBg.getWidth(null), rawBg.getHeight(null));
            int size = (int) (rawSize * 0.5f + rawSize * 0.5f * random.nextFloat())
            int x = (int) ((rawBg.getWidth() - size) * random.nextFloat())
            int y = (int) ((rawBg.getHeight() - size) * random.nextFloat())
            BufferedImage subBg = rawBg.getSubimage(x, y, size, size)
            Image scaledBg = subBg.getScaledInstance(imageSize, imageSize, Image.SCALE_SMOOTH)
            BufferedImage outputImage = ImageUtil.toBufferedImage(scaledBg)


            String outputFileName = String.format(outputFileFormat, i)
            File file = new File(outputFileName)
            ImageIO.write(outputImage, "png", file)
            System.out.println("Written ${outputFileName} ... ")
        }
    }
}
