package yumyai.poseest.mmd.character

import com.jogamp.opengl.util.awt.ImageUtil
import com.jogamp.opengl.util.texture.TextureData
import com.jogamp.opengl.util.texture.TextureIO
import com.jogamp.opengl.util.texture.awt.AWTTextureIO
import org.apache.commons.io.FilenameUtils
import yumyai.mmd.pmd.PmdMaterial
import yumyai.mmd.pmd.PmdModel
import yumyai.mmd.pmx.PmxMaterial
import yumyai.mmd.pmx.PmxModel

import javax.imageio.ImageIO
import javax.media.opengl.GLProfile
import java.awt.image.BufferedImage

class CheckMmdModelTexture {
    static def powersOfTwo = []

    static {
        for (int i = 0; i < 16; i++) {
            powersOfTwo.add(1<<i)
        }
    }

    public static HashMap<String, Integer> readFileUsage(String fileName) {
        HashMap<String, Integer> result = new HashMap<String, Integer>()
        File file = new File(fileName)
        if (!file.exists()) {
            return result
        }
        file.withReader() { fin ->
            def lines = fin.readLines()
            for (int i = 0; i < lines.size(); i++) {
                if (lines[i].length() > 0) {
                    String fName = lines[i].substring(2).trim()
                    int flag = Integer.valueOf(lines[i].substring(0, 1))
                    result[fName] = flag
                }
            }
        }
        return result
    }

    public static void writeMap(HashMap<String, Integer> outputMap, String outputFileName, String inputFileName) {
        new File(outputFileName).withWriter("UTF-8") {fout ->
            File file = new File(inputFileName)
            file.withReader() { fin ->
                def lines = fin.readLines()
                for (int i = 0; i < lines.size(); i++) {
                    if (lines[i].length() > 0) {
                        String fName = lines[i].substring(2).trim()
                        fout.write("${outputMap[fName]} ${fName}\n")
                    }
                }
            }
        }

    }

    static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("java yumyai.poseest.mmd.character.CheckMmdModelTexture <input-file> <output-file>");
            System.exit(0);
        }

        def inputMap = readFileUsage(args[0])
        def outputMap = readFileUsage(args[1])

        int count = 0
        long start = System.nanoTime()
        for (String fileName : inputMap.keySet()) {
            System.out.println(fileName);
            if (!outputMap.containsKey(fileName)) {
                boolean someOtherError = false
                ArrayList<String> textureList = null;
                String extension = FilenameUtils.getExtension(fileName).toLowerCase()
                if (extension.equals("pmd")) {
                    try {
                        PmdModel model = PmdModel.load(fileName)
                        textureList = getPmdTextureList(model)
                    } catch (Exception e) {
                        println("Cannot open because: " + e.getMessage())
                        e.printStackTrace();
                        someOtherError = true;
                    }
                } else if (extension.equals("pmx")) {
                    try {
                        PmxModel model = PmxModel.load(fileName)
                        textureList = getPmxTextureList(model)
                    } catch (Exception e) {
                        println("Cannot open because: " + e.getMessage())
                        someOtherError = true;
                    }
                }

                boolean textureNotExist = false
                boolean textureNeedConversion = false

                if (!someOtherError && textureList != null && textureList.size() > 0) {
                    for (String textureFileName : textureList) {
                        File textureFile = new File(textureFileName)
                        println("Try loading ${textureFile} ...")
                        if (!textureFile.exists()) {
                            //textureNotExist = true
                            continue;
                        }
                        try {
                            TextureData data = null;
                            if (FilenameUtils.getExtension(textureFile.absolutePath).toLowerCase().equals("png")) {
                                BufferedImage image = ImageIO.read(textureFile);
                                ImageUtil.flipImageVertically(image);
                                data = AWTTextureIO.newTextureData(GLProfile.getDefault(), image, false);
                            } else {
                                data = TextureIO.newTextureData(GLProfile.getDefault(), textureFile, false, null);
                                if (data.getMustFlipVertically()) {
                                    BufferedImage image = ImageIO.read(textureFile);
                                    ImageUtil.flipImageVertically(image);
                                    data = AWTTextureIO.newTextureData(GLProfile.getDefault(), image, false);
                                }
                            }
                            if (!isPowerOfTwo(data.getWidth()) || !isPowerOfTwo(data.getHeight()) ||
                                    data.getWidth() < 64 || data.getHeight() < 64) {
                                textureNeedConversion = true
                                println("Sizes (${data.getWidth()}, ${data.getHeight()}) are not powers of two.")
                            }

                        } catch (Exception e) {
                            if (e.getMessage().equals("TGADecoder Compressed True Color images not supported")) {
                                textureNeedConversion = true
                                println("Contains TGA file that cannot be opened by our system!")
                            } else if (e.getMessage().equals("TGADecoder Compressed Colormapped images not supported")) {
                                textureNeedConversion = true
                                println("Contains TGA file that cannot be opened by our system!")
                            } else if (e.getMessage().equals("PNG interlaced not supported by this library")) {
                                someOtherError = true
                                println("Contains interlaced PNG files")
                            } else if (e.getMessage().equals("PNGImage can only handle Lum/RGB/RGBA [1/3/4 bpp] images for now. BytesPerPixel 8")) {
                                someOtherError = true
                                println("Contains unsupported PNG files")
                            } else if (e instanceof jogamp.opengl.util.pngj.PngjInputException) {
                                someOtherError = true
                                println(e.getMessage())
                            } else if (e.getMessage().equals("Numbers of source Raster bands and source color space components do not match")) {
                                someOtherError = true
                                println(e.getMessage())
                            } else if (e instanceof java.io.IOException) {
                                someOtherError = true;
                                println(e.getMessage());
                            } else if (e instanceof java.lang.NullPointerException) {
                                someOtherError = true
                                println(e.getMessage())
                            } else {
                                e.printStackTrace()
                                System.exit(0)
                            }
                        }
                        if (someOtherError)
                            break
                    }
                }

                outputMap[fileName] = inputMap[fileName]
                if (textureNeedConversion)
                    outputMap[fileName] = 3;
                //if (textureNotExist)
                //    outputMap[fileName] = 1;
                if (someOtherError)
                    outputMap[fileName] = 1;
            }

            count++
            println("Finished ${count} of ${inputMap.size()}")
            println("Elapsed time = ${(System.nanoTime()-start)*1e-9} seconds")
            println()
        }

        writeMap(outputMap, args[1], args[0])

        int discardCount = 0
        int convertCount = 0
        for (String fileName : outputMap.keySet()) {
            if (outputMap[fileName] == 3) {
                convertCount++;
            }
            if (outputMap[fileName] == 1) {
                discardCount++;
            }
        }
        println("${discardCount} files to be discarded");
        println("${convertCount} files's textures have to be converted")
    }

    static boolean isPowerOfTwo(int x) {
        return powersOfTwo.contains(x)
    }

    static ArrayList<String> getPmdTextureList(PmdModel model) {
        ArrayList<String> result = new ArrayList<>()
        for (int i = 0; i < model.materials.size(); i++) {
            PmdMaterial material = model.materials.get(i);
            if (!material.textureFileName.equals("")) {
                if (!result.contains(material.textureFileName))
                    result.add(material.textureFileName)
            }
            if (material.toonIndex >= 0) {
                if (!result.contains(model.toonFileNames.get(material.toonIndex)))
                    result.add(model.toonFileNames.get(material.toonIndex));
            }
            if (!material.sphereMapFileName.equals("")) {
                if (!result.contains(material.sphereMapFileName))
                    result.add(material.sphereMapFileName);
            }

        }
        return result
    }

    static ArrayList<String> getPmxTextureList(PmxModel model) {
        ArrayList<String> result = new ArrayList<>()
        for (int i = 0; i < model.getMaterialCount(); i++) {
            PmxMaterial material = model.getMaterial(i);
            if (material.textureIndex >= 0) {
                String textureFile = model.getAbsoluteTextureFileName(material.textureIndex);
                if (!result.contains(textureFile))
                    result.add(textureFile);
            }
            if (!material.useSharedToonTexture) {
                if (material.toonTextureIndex >= 0) {
                    String textureFile = model.getAbsoluteTextureFileName(material.toonTextureIndex);
                    if (!result.contains(textureFile))
                        result.add(textureFile);
                }
            }
            if (material.sphereMode > 0) {
                if (material.sphereTextureIndex >= 0) {
                    String textureFile = model.getAbsoluteTextureFileName(material.sphereTextureIndex);
                    if (!result.contains(textureFile))
                        result.add(textureFile);
                }
            }
        }
        return result
    }
}
