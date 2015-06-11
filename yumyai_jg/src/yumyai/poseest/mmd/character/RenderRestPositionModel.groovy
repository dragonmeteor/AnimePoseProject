package yumyai.poseest.mmd.character

import com.jogamp.opengl.util.awt.Screenshot
import groovyjarjarcommonscli.HelpFormatter
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.Options
import org.apache.commons.cli.PosixParser
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
import javax.vecmath.Vector3f
import java.awt.image.BufferedImage

class RenderRestPositionModel extends RenderingFrame {
    MeshV1 mesh
    String outputFileName

    public static void main(String[] args) {
        CommandLineProcessor cmdProc = new CommandLineProcessor()
        cmdProc.run(args)

        String extension = FilenameUtils.getExtension(cmdProc.inputFileName)
        MeshV1 mesh = null
        try {
            if (extension.equals("pmd")) {
                PmdModel model = PmdModel.load(cmdProc.inputFileName)
                mesh = PmdToMeshV1Converter.createMesh(model);
            } else if (extension.equals("pmx")) {
                PmxModel model = PmxModel.load(cmdProc.inputFileName)
                mesh = PmxToMeshV1Converter.createMesh(model)
            } else {
                throw new RuntimeException("Unsupported extension '${extension}'.")
            }
        } catch (Exception e) {
            e.printStackTrace()
            System.exit(-1)
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // NOP
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new RenderRestPositionModel(mesh,
                        cmdProc.screenWidth, cmdProc.screenHeight,
                        cmdProc.outputFileName).run()
            }
        });
    }

    public RenderRestPositionModel(MeshV1 mesh, int screenWidth, int screenHeight,
                                   String outputFileName) {
        super("Render Rest Position Model", screenWidth, screenHeight)
        this.mesh = mesh
        this.outputFileName = outputFileName
    }

    @Override
    void init(GLAutoDrawable glAutoDrawable) {
        super.init(glAutoDrawable)

        Aabb3f aabb = new Aabb3f()
        if (mesh instanceof PmdMeshV1) {
            PmdModel model = ((PmdMeshV1)mesh).getModel()
            for (int i = 0; i < model.positions.capacity()/3; i++) {
                float x = model.positions.get(3*i+0);
                float y = model.positions.get(3*i+1);
                float z = model.positions.get(3*i+2);
                aabb.expandBy(x,y,z);
            }
        } else if (mesh instanceof PmxMeshV1) {
            PmxModel model = ((PmxMeshV1)mesh).getModel()
            for (int i = 0; i < model.getVertexCount(); i++) {
                PmxVertex vertex = model.getVertex(i)
                aabb.expandBy(vertex.position)
            }
        }

        float middleY = (aabb.pMax.y + aabb.pMin.y) / 2
        float extent = (aabb.pMax.y - aabb.pMin.y) / 2 * 1.3f
        float distance = (float)(extent / Math.tan(camera.fovy/2*Math.PI/180))

        camera.eye.set(0,middleY,distance);
        camera.target.set(0,middleY,0);
    }

    Vector3f lightAmbient = new Vector3f(1.0f, 1.0f, 1.0f);
    Vector3f lightDiffuse = new Vector3f(0.5f, 0.5f, 0.5f);
    Vector3f lightSpecular = new Vector3f(1.0f, 1.0f, 1.0f);
    Vector3f lightPosition = new Vector3f(100, 100, 100);

    @Override
    void draw(GLAutoDrawable glAutoDrawable) {
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
            mesh = null
        }

        gl.glFlush()

        try {
            File file = new File(outputFileName)
            BufferedImage screenShot = Screenshot.readToBufferedImage(0,0, screenWidth, screenHeight, true);
            ImageIO.write(screenShot, "png", file)
        } catch (Exception e) {
            e.printStackTrace()
        }

        renderEngine.popBindingFrame()
        renderEngine.garbageCollect()

        gl.glPopMatrix();

        System.exit(0);
    }

    static class CommandLineProcessor {
        Options options
        int screenWidth = 400
        int screenHeight = 400
        String inputFileName
        String outputFileName

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
                outputFileName = parsedArgs[1]
            } catch (Exception e) {
                printErrorAndExit("Command line parsing error!", e)
            }
        }

        void displayHelp(Options options) {
            HelpFormatter formatter = new HelpFormatter()
            formatter.printHelp("java yumyai.poseest.mmd.character.RenderRestPositionModel <model-name> <output-name>", options)
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
