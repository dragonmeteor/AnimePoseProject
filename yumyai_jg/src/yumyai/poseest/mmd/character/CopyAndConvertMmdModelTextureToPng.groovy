package yumyai.poseest.mmd.character

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.apache.commons.cli.PosixParser
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import yumyai.gfx.ver01.mesh.TextureCopier
import yumyai.mmd.pmd.PmdMaterial
import yumyai.mmd.pmd.PmdModel
import yumyai.mmd.pmx.PmxMaterial
import yumyai.mmd.pmx.PmxModel

class CopyAndConvertMmdModelTextureToPng {
    private org.slf4j.Logger logger = LoggerFactory.getLogger(CopyAndConvertMmdModelTextureToPng.class);

    public static final int NO = 0;
    public static final int YES = 1;
    public static final int FILE = 2;

    public String inputFileName;
    public String outputFileName;
    public String inputFormat = "";
    public HashSet<String> supportedFormats;
    public HashMap<Integer, String> yesNoFileToString;

    Options options;

    public static void main(String[] args) {
        new CopyAndConvertMmdModelTextureToPng().run(args);
    }

    public CopyAndConvertMmdModelTextureToPng() {
        initializeOptions();

        supportedFormats = new HashSet<String>();
        supportedFormats.add("pmd");
        supportedFormats.add("pmx");

        yesNoFileToString = new HashMap<Integer, String>();
        yesNoFileToString.put(YES, "yes");
        yesNoFileToString.put(NO, "no");
        yesNoFileToString.put(FILE, "file");
    }

    private void initializeOptions() {
        options = new Options();
        options.addOption("h", "help", false, "display help");
        options.addOption("i", "input", true, "input format (pmd, pmx)");
    }

    public void copyTexture(TextureCopier textureCopier, String textureName) {
        File file = new File(textureName)
        if (file.exists())
            textureCopier.getActualTextureFileName(textureName)
        else {
            file = new File(textureName + ".png")
            if (file.exists()) {
                textureCopier.getActualTextureFileName(textureName + ".png")
            }
        }
    }

    public void run(String[] args) {
        processCommandLineArguments(args);

        println inputFileName
        println outputFileName
        TextureCopier textureCopier = new TextureCopier(inputFileName, outputFileName, true, true);
        if (inputFormat.equals("pmd")) {
            try {
                PmdModel pmd = PmdModel.load(inputFileName);
                for (int i = 0; i < pmd.materials.size(); i++) {
                    PmdMaterial material = pmd.materials.get(i);
                    if (!material.textureFileName.equals("")) {
                        copyTexture(textureCopier, material.textureFileName)
                    }
                    if (!material.sphereMapFileName.equals("")) {
                        copyTexture(textureCopier, material.sphereMapFileName)
                    }
                    if (material.toonIndex >= 0) {
                        copyTexture(textureCopier, pmd.toonFileNames.get(material.toonIndex))
                    }
                }
            } catch (IOException e) {
                printErrorAndExit("Cannot load PMD file", e);
            }
        } else if (inputFormat.equals("pmx")) {
            try {
                PmxModel pmx = PmxModel.load(inputFileName);
                for (int i = 0; i < pmx.getMaterialCount(); i++) {
                    PmxMaterial material = pmx.getMaterial(i);
                    if (material.textureIndex >= 0) {
                        copyTexture(textureCopier, pmx.getAbsoluteTextureFileName(material.textureIndex))
                    }
                    if (material.toonTextureIndex >= 0) {
                        if (!material.useSharedToonTexture) {
                            String textureName = pmx.getAbsoluteTextureFileName(material.toonTextureIndex);
                            copyTexture(textureCopier, textureName)
                        }
                    }
                    if (material.sphereMode > 0 && material.sphereTextureIndex >= 0) {
                        String textureName = pmx.getAbsoluteTextureFileName(material.sphereTextureIndex);
                        copyTexture(textureCopier, textureName)
                    }
                }
            } catch (IOException e) {
                printErrorAndExit("Cannot load PMX file", e);
            }
        } else {
            logger.error("unsupported format: " + inputFormat);
            System.exit(-1);
        }

        File outputFile = new File(outputFileName);
        try {
            FileUtils.forceMkdir(outputFile.getParentFile());
            FileUtils.copyFile(new File(inputFileName), outputFile);
        } catch (IOException e) {
            printErrorAndExit("Cannot create directory + '" + outputFile.getParent() + "'.", e);
        }
    }

    public void displayHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java yumyai.poseest.app.ConvertMMDModelTextureToPng <input-file> <output-file>", options);
    }

    public void processCommandLineArguments(String[] args) {
        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String[] parsedArgs = cmd.getArgs();

            if (parsedArgs.length < 2) {
                displayHelp(options);
                System.exit(0);
            }
            if (cmd.hasOption("h")) {
                displayHelp(options);
                System.exit(0);
            }
            if (cmd.hasOption("i")) {
                inputFormat = cmd.getOptionValue("i");
            }

            inputFileName = parsedArgs[0];
            outputFileName = parsedArgs[1];

            if (inputFormat.equals("")) {
                inputFormat = FilenameUtils.getExtension(inputFileName).toLowerCase();
            }
            if (!supportedFormats.contains(inputFormat)) {
                logger.error("unsupported format: " + inputFormat);
                System.exit(-1);
            }
        } catch (ParseException e) {
            printErrorAndExit("Command line parsing error!", e);
        }
    }

    private void printErrorAndExit(String message, Exception e) {
        if (message != null) {
            logger.error(message);
        }
        if (e != null) {
            logger.error(e.toString());
        }
        System.exit(0);
    }

    private void printInputs() {
        logger.info("inputFileName = %s\n", inputFileName);
        logger.info("outputFileName = %s\n", outputFileName);
        logger.info("inputFormat = %s\n", (inputFormat.equals("")) ? "unspecified" : inputFormat);
        logger.info("copyTextures = %s\n", (copyTextures) ? "yes" : "no");
    }
}
