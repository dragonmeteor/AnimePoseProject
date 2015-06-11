package yumyai.poseest.mmd.character

import com.jogamp.opengl.util.awt.Screenshot
import groovyjarjarcommonscli.HelpFormatter
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.Options
import org.apache.commons.cli.PosixParser
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import yondoko.struct.Aabb3f
import yumyai.gfx.ver01.MeshV1
import yumyai.gfx.ver01.mesh.PmdMeshV1
import yumyai.gfx.ver01.mesh.PmdToMeshV1Converter
import yumyai.gfx.ver01.mesh.PmxMeshV1
import yumyai.gfx.ver01.mesh.PmxToMeshV1Converter
import yumyai.mmd.pmd.PmdModel
import yumyai.mmd.pmx.PmxModel
import yumyai.mmd.pmx.PmxVertex
import yumyai.poseest.ui.RenderingFrame

import javax.imageio.ImageIO
import javax.media.opengl.GL2
import javax.media.opengl.GLAutoDrawable
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.vecmath.Matrix4f
import javax.vecmath.Vector3f
import java.awt.image.BufferedImage

class RenderRestPositionModelFromList extends RenderingFrame {
    ArrayList<String> fileNames;
    String outputDir
    int currentIndex = 0

    public static void main(String[] args) {
        CommandLineProcessor cmdProc = new CommandLineProcessor()
        cmdProc.run(args)

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // NOP
        }

        ArrayList<String> fileNames = new ArrayList<String>()
        new File(cmdProc.inputFileName).withReader() { f ->
            def line = f.readLine()
            while (line != null) {
                fileNames.add(line.substring(2).trim())
                line = f.readLine()
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new RenderRestPositionModelFromList(fileNames,
                        cmdProc.screenWidth, cmdProc.screenHeight,
                        cmdProc.outputDir).run()
            }
        });
    }

    @Override
    void run() {
        FileUtils.forceMkdir(new File(outputDir))
        super.run()
    }

    public RenderRestPositionModelFromList(ArrayList<String> fileNames,
                                           int screenWidth, int screenHeight,
                                           String outputDir) {
        super("Render Rest Position Model Frame List", screenWidth, screenHeight)
        this.fileNames = fileNames
        this.outputDir = outputDir
    }

    Vector3f lightAmbient = new Vector3f(0.7f, 0.7f, 0.7f);
    Vector3f lightDiffuse = new Vector3f(0.5f, 0.5f, 0.5f);
    Vector3f lightSpecular = new Vector3f(1.0f, 1.0f, 1.0f)
    Vector3f lightPosition = new Vector3f(100, 100, 100)
    Vector3f lightDirection = new Vector3f(0,-1,-1)
    Matrix4f viewInverse = new Matrix4f()
    Aabb3f aabb = new Aabb3f()

    @Override
    void draw(GLAutoDrawable glAutoDrawable) {
        if (currentIndex >= fileNames.size())
            System.exit(0)
        boolean exist = true
        while(exist) {
            File file = new File(String.format(outputDir + File.separator + "%05d.png", currentIndex))
            exist = file.exists()
            if (exist)
                currentIndex++
        }
        if (currentIndex >= fileNames.size())
            System.exit(0)

        String fileName = fileNames[currentIndex]
        String extension = FilenameUtils.getExtension(fileName)
        MeshV1 mesh = null
        try {
            if (extension.equals("pmd")) {
                PmdModel model = PmdModel.load(fileName)
                mesh = PmdToMeshV1Converter.createMesh(model);
            } else if (extension.equals("pmx")) {
                PmxModel model = PmxModel.load(fileName)
                mesh = PmxToMeshV1Converter.createMesh(model)
                mesh.setPmxPose(null)
                mesh.updateMeshWithPose()
            } else {
                throw new RuntimeException("Unsupported extension '${extension}'.")
            }
        } catch (Exception e) {
            e.printStackTrace()
            System.exit(-1)
        }

        if (mesh == null) {
            currentIndex++
            return
        }

        aabb.reset()
        if (mesh instanceof PmdMeshV1) {
            PmdModel model = ((PmdMeshV1)mesh).getModel()
            print model.getVertexCount()
            for (int i = 0; i < model.positions.capacity()/3; i++) {
                if (!model.vertexUsed[i])
                    continue;
                float x = model.positions.get(3*i+0);
                float y = model.positions.get(3*i+1);
                float z = model.positions.get(3*i+2);
                aabb.expandBy(x,y,z)
            }
        } else if (mesh instanceof PmxMeshV1) {
            PmxModel model = ((PmxMeshV1)mesh).getModel()
            for (int i = 0; i < model.getVertexCount(); i++) {
                if (!model.isVertexUsed(i))
                    continue;
                PmxVertex vertex = model.getVertex(i)
                aabb.expandBy(vertex.position)
            }
        }

        float middleX = (aabb.pMax.x + aabb.pMin.x) / 2
        float middleY = (aabb.pMax.y + aabb.pMin.y) / 2
        float zCoord = -aabb.pMin.z
        float extent = (aabb.pMax.y - aabb.pMin.y) / 2 * 1.1f
        float distance = (float) (extent / Math.tan(camera.fovy / 2 * Math.PI / 180))

        camera.eye.set(middleX, middleY, (float)(zCoord+distance));
        camera.target.set(middleX, middleY, zCoord);
        camera.up.set(0, 1, 0);
        camera.setAspect((float) (screenWidth * 1.0f / screenHeight))

        final GL2 gl = glAutoDrawable.getGL().getGL2();

        gl.glEnable(GL2.GL_DEPTH_TEST)
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDepthFunc(GL2.GL_LEQUAL);

        camera.updateFrame();

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        camera.doProjection(glAutoDrawable);
        camera.doView(glAutoDrawable);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glPushMatrix();
        gl.glScaled(1, 1, -1);

        renderEngine.pushBindingFrame();
        renderEngine.setVariable("sys_eyePosition", camera.getEye());
        renderEngine.setVariable("sys_lightPosition", lightPosition);
        renderEngine.setVariable("sys_lightAmbient", lightAmbient);
        renderEngine.setVariable("sys_lightDiffuse", lightDiffuse);
        renderEngine.setVariable("sys_lightSpecular", lightSpecular);
        renderEngine.setVariable("sys_lightDirection", lightDirection);
        camera.getViewMatrixInverse(viewInverse);
        renderEngine.setVariable("sys_viewMatrixInverse", viewInverse);

        try {
            if (mesh != null) {
                mesh.draw(renderEngine);
            }
        } catch (Exception e) {
            if (e.getMessage().equals("TGADecoder Compressed True Color images not supported")) {
                System.out.println(e.getMessage());
            } else {
                e.printStackTrace();
            }
        }

        gl.glFlush()

        try {
            String outputFileName = String.format(outputDir + File.separator + "%05d.png", currentIndex)
            File file = new File(outputFileName)
            BufferedImage screenShot = Screenshot.readToBufferedImage(0,0, screenWidth, screenHeight, true);
            ImageIO.write(screenShot, "png", file)
            System.out.println()
            System.out.println("Written ${outputFileName} ... ")
            System.out.println()
        } catch (Exception e) {
            e.printStackTrace()
        }

        renderEngine.popBindingFrame()
        renderEngine.garbageCollect()

        gl.glPopMatrix();

        currentIndex++
    }

    static class CommandLineProcessor {
        Options options
        int screenWidth = 400
        int screenHeight = 400
        String inputFileName
        String outputDir

        public void run(String[] args) {
            initializeOptions();
            processCommandLineArguments(args);
        }

        void processCommandLineArguments(String[] args) {
            CommandLineParser parser = new PosixParser()
            try {
                CommandLine cmd = parser.parse(options, args)
                String[] parsedArgs = cmd.getArgs()

                if (parsedArgs.length < 2) {
                    displayHelp(options)
                    System.exit(0)
                }
                if (cmd.hasOption("h")) {
                    displayHelp(options)
                    System.exit(0)
                }
                if (cmd.hasOption("x")) {
                    screenWidth = Integer.valueOf(cmd.getOptionValue("x"))
                }
                if (cmd.hasOption("y")) {
                    screenHeight = Integer.valueOf(cmd.getOptionValue("y"))
                }

                inputFileName = parsedArgs[0]
                outputDir = parsedArgs[1]
            } catch (Exception e) {
                printErrorAndExit("Command line parsing error!", e)
            }
        }

        void displayHelp(Options options) {
            HelpFormatter formatter = new HelpFormatter()
            formatter.printHelp("java yumyai.poseest.mmd.character.RenderRestPositionModelFromList <list-file> <output-dir>",
                    options)
        }

        public void initializeOptions() {
            options = new Options()
            options.addOption("h", "help", false, "display help")
            options.addOption("x", "x-extent", true, "screen width")
            options.addOption("y", "y-extent", true, "screen height")
        }

        private void printErrorAndExit(String message, Exception e) {
            if (message != null)
                println(message)
            if (e != null)
                println(e.toString())
            System.exit(0)
        }
    }
}
