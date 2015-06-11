package yumyai.gfx.ver01.mesh;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;
import yondoko.math.Util;
import yondoko.util.PathUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class TextureCopier {
    org.slf4j.Logger logger = LoggerFactory.getLogger(TextureCopier.class);
    String sourceDirectory;
    String destinationDirectory;
    HashMap<String, String> processedFileMap;
    boolean copyTextures;
    boolean convertToPng;

    public TextureCopier(String sourceFileName, String destinationFileName, boolean copyTextures, boolean convertToPng) {
        destinationDirectory = FilenameUtils.normalize(new File(destinationFileName).getParent(), true);
        sourceDirectory = FilenameUtils.normalize(new File(sourceFileName).getParent(), true);
        processedFileMap = new HashMap<String, String>();
        this.copyTextures = copyTextures;
        this.convertToPng = convertToPng;
    }

    public int[] getImageSize(String sourceFileName) {
        String cmdLine = "bin\\identify -format \"%[fx:w] %[fx:h]\" \"" + sourceFileName + "\"";
        String line;
        String output = "";
        try {
            Process p = Runtime.getRuntime().exec(cmdLine);
            BufferedReader input = new BufferedReader
                    (new InputStreamReader(p.getInputStream()));
            BufferedReader error = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));
            line = input.readLine();
            while (line != null) {
                output += (line + "\n");
                line = input.readLine();
            }
            line = error.readLine();
            while (line != null) {
                System.out.println(line);
                line = error.readLine();
            }
            input.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println(cmdLine);
        output = output.trim();
        System.out.println(output);
        String[] comps = output.split(" ");
        int[] result = new int[2];
        result[0] = Integer.valueOf(comps[0]);
        result[1] = Integer.valueOf(comps[1]);
        return result;
    }

    public String getActualTextureFileName(String sourceFileName) {
        sourceFileName = FilenameUtils.normalize(new File(sourceFileName).getAbsolutePath());
        File sourceFile = new File(sourceFileName);
        if (!sourceFile.exists()) {
            sourceFileName = sourceFileName + ".png";
        }

        if (!copyTextures) {
            return sourceFileName;
        }

        String targetExtension = FilenameUtils.getExtension(sourceFileName);
        if (convertToPng)
            targetExtension = "png";

        String sourceDir = new File(sourceDirectory).getAbsolutePath();
        if (!processedFileMap.containsKey(sourceFileName)) {
            System.out.println(sourceFileName.length());
            System.out.println(sourceFileName.trim().length());
            String relPath = PathUtil.relativizeSecondToFirst(sourceDir, sourceFileName.trim());
            String destFile = destinationDirectory + "/" + relPath;
            if (!FilenameUtils.getExtension(destFile).toLowerCase().trim().equals("png"))
                destFile = destFile + ".png";
            //destFile = FilenameUtils.normalize(FilenameUtils.removeExtension(destFile) + "." + targetExtension);
            processedFileMap.put(sourceFileName, destFile);
        } else {
            return processedFileMap.get(sourceFileName);
        }
        String destinationFileName = processedFileMap.get(sourceFileName);

        int[] imageSize = getImageSize(sourceFileName);
        boolean needResize = !Util.isPowerOfTwo(imageSize[0]) || !Util.isPowerOfTwo(imageSize[1]) ||
                imageSize[0] < 64 || imageSize[1] < 64;
        imageSize[0] = Math.max(Util.getClosestPowerOfTwo(imageSize[0]),64);
        imageSize[1] = Math.max(Util.getClosestPowerOfTwo(imageSize[1]),64);
        String line = null;
        if (needResize) {
            line = "bin\\convert \"" + sourceFileName +
                    "\" -resize " + imageSize[0] + "x" + imageSize[1] + "! \"" +
                    destinationFileName + "\"";
        } else {
            line = "bin\\convert \"" + sourceFileName + "\" \"" + destinationFileName + "\"";
        }
        String destDir = new File(destinationFileName).getParent();
        try {
            FileUtils.forceMkdir(new File(destDir));
            //logger.info("Convert " + sourceFileName + " to " + destinationFileName);
            logger.info(line);
            CommandLine cmdLine = CommandLine.parse(line);
            DefaultExecutor executor = new DefaultExecutor();
            int exitCode;
            try {
                exitCode = executor.execute(cmdLine, EnvironmentUtils.getProcEnvironment());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (exitCode != 0)
                throw new RuntimeException("Cannot convert " + sourceFileName + " to " + destinationFileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return FilenameUtils.separatorsToUnix(destinationFileName);

        /*
        boolean createNewFile = false;
        if (convertToPng) {
            String extension = FilenameUtils.getExtension(sourceFileName).toLowerCase();
            if (extension.equals("png")) {
                createNewFile = false;
            } else {
                String pngFile = FilenameUtils.removeExtension(sourceFileName) + ".png";
                logger.info("Using ImageMagick to convert " + sourceFileName + " to " + pngFile);
                String line = "bin\\convert \"" + sourceFileName + "\" \"" + pngFile + "\"";
                CommandLine cmdLine = CommandLine.parse(line);
                DefaultExecutor executor = new DefaultExecutor();
                int exitCode;
                try {
                    exitCode = executor.execute(cmdLine, EnvironmentUtils.getProcEnvironment());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (exitCode != 0)
                    throw new RuntimeException("Cannot convert " + sourceFileName + " to PNG file");
                createNewFile = true;
                sourceFileName = pngFile;
            }
        }

        String result = null;
        if (!copyTextures) {
            result = sourceFileName;
        } else {
            sourceFileName = FilenameUtils.normalize(new File(sourceFileName).getAbsolutePath());
            if (!processedFileMap.containsKey(sourceFileName)) {
                if (!sourceFileName.startsWith(sourceDirectory)) {
                    String fileName = FilenameUtils.getName(sourceFileName);
                    String baseName = FilenameUtils.getBaseName(sourceFileName);
                    String extension = FilenameUtils.getExtension(fileName);
                    String destFile = destinationDirectory + "/" + fileName;
                    int index = 0;
                    while (processedFileMap.containsValue(destFile)) {
                        destFile = destinationDirectory + "/" + baseName + String.format("_%05d", index) + "." + extension;
                        index++;
                    }
                    processedFileMap.put(sourceFileName, destFile);
                } else {
                    String relPath = PathUtil.relativizeSecondToFirstDir(sourceDirectory, sourceFileName);
                    String destFile = destinationDirectory + "/" + relPath;
                    processedFileMap.put(sourceFileName, destFile);
                }
                String destFile = processedFileMap.get(sourceFileName);
                String destDir = new File(destFile).getParent();
                try {
                    FileUtils.forceMkdir(new File(destDir));
                    logger.info("Copy " + sourceFileName + " to " + destFile);
                    FileUtils.copyFile(new File(sourceFileName), new File(destFile));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            result = processedFileMap.get(sourceFileName);
        }

        if (createNewFile) {
            try {
                FileUtils.forceDelete(new File(sourceFileName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
        */
    }
}
