package yumyai.poseest.mmd.motion

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
import yumyai.mmd.pmd.PmdAnimatedInstance
import yumyai.mmd.pmd.PmdModel
import yumyai.mmd.pmd.PmdPose
import yumyai.mmd.pmx.PmxAnimatedInstance
import yumyai.mmd.pmx.PmxModel
import yumyai.mmd.pmx.PmxPose
import yumyai.mmd.vmd.VmdMotion
import yumyai.mmd.vpd.VpdPose
import yumyai.poseest.ui.RenderingFrame

import javax.imageio.ImageIO
import javax.media.opengl.GL2
import javax.media.opengl.GLAutoDrawable
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.vecmath.Matrix4f
import javax.vecmath.Quat4f
import javax.vecmath.Vector3f
import java.awt.image.BufferedImage


class RenderAnimationFramesNoPhysics extends RenderingFrame {
    MeshV1 mesh
    ArrayList<VpdPose> poses
    int currentIndex = 0
    String outputDir
    def animatedInstance
    def pose
    int speed

    public static void renderFrames(MeshV1 mesh,
                                    ArrayList<VpdPose> poses,
                                    String outputDir,
                                    int screenWidth, int screenHeight) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // NOP
        }

        FileUtils.forceMkdir(new File(outputDir))
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new RenderAnimationFramesNoPhysics(mesh, poses, outputDir, screenWidth, screenHeight).run()
            }
        });
    }

    Vector3f lightAmbient = new Vector3f(1.0f, 1.0f, 1.0f)
    Vector3f lightDiffuse = new Vector3f(0.5f, 0.5f, 0.5f)
    Vector3f lightSpecular = new Vector3f(1.0f, 1.0f, 1.0f)
    Vector3f lightPosition = new Vector3f(100, 100, 100)
    Vector3f lightDirection = new Vector3f(0,-1,-1)
    Matrix4f viewInverse = new Matrix4f()
    Aabb3f aabb = new Aabb3f()

    public static void main(String[] args) {
        CommandLineProcessor cmdProc = new CommandLineProcessor()
        cmdProc.run(args)

        MeshV1 mesh = null
        ArrayList<VpdPose> poses = new ArrayList<VpdPose>()
        try {
            String extension = FilenameUtils.getExtension(cmdProc.modelFileName)
            if (extension.equals("pmd")) {
                PmdModel model = PmdModel.load(cmdProc.modelFileName)
                mesh = PmdToMeshV1Converter.createMesh(model);
            } else if (extension.equals("pmx")) {
                PmxModel model = PmxModel.load(cmdProc.modelFileName)
                mesh = PmxToMeshV1Converter.createMesh(model)
            } else {
                throw new RuntimeException("Unsupported model extension '${extension}'.")
            }

            extension = FilenameUtils.getExtension(cmdProc.motionFileName)
            if (extension.equals("vpd")) {
                VpdPose pose = VpdPose.load(cmdProc.motionFileName)
                poses.add(pose)
            } else if (extension.equals("vmd")) {
                VmdMotion motion = VmdMotion.load(cmdProc.motionFileName)
                for (Integer frame : cmdProc.frames) {
                    VpdPose pose = new VpdPose()
                    motion.getPose(frame, pose)
                    poses.add(pose)
                }
            } else {
                throw new RuntimeException("Unsupported motion extension '${extension}'")
            }
        } catch (Exception e) {
            e.printStackTrace()
            System.exit(0)
        }

        renderFrames(mesh, poses, cmdProc.outputDir, cmdProc.screenWidth, cmdProc.screenHeight)
    }

    public RenderAnimationFramesNoPhysics(MeshV1 mesh,
                                          ArrayList<VpdPose> poses,
                                          String outputDir,
                                          int screenWidth, int screenHeight) {
        super("Render Animation Frames No Physics", screenWidth, screenHeight)
        this.mesh = mesh
        this.poses = poses
        this.outputDir = outputDir
        if (poses.size() <= 300) {
            speed = 1
        } else {
            speed = poses.size() / 300
        }

        if (mesh instanceof PmdMeshV1) {
            animatedInstance = new PmdAnimatedInstance(mesh.getModel(), true, false)
            pose = new PmdPose(mesh.getModel())
        } else if (mesh instanceof PmxMeshV1) {
            animatedInstance = new PmxAnimatedInstance(mesh.getModel())
            animatedInstance.enablePhysics(false)
            pose = new PmxPose(mesh.getModel())
        }
    }

    Vector3f displacement = new Vector3f();
    Quat4f rotation = new Quat4f();

    void nullifyPlaneMovement(VpdPose pose, String boneName) {
        if (pose.hasBone(boneName)) {
            pose.getBonePose(boneName, displacement, rotation)
            pose.setBoneDisplacement(boneName, 0, displacement.y, 0)
        }
    }

    void nullifyPlaneMovement(VpdPose pose) {
        //nullifyPlaneMovement(pose, "センター")
        nullifyPlaneMovement(pose, "全ての親")
    }

    @Override
    void draw(GLAutoDrawable glAutoDrawable)  {
        boolean exist = true
        while (currentIndex < poses.size() && exist) {
            File file = new File(String.format(outputDir + File.separator + "%05d.png", (int)(currentIndex / speed)))
            exist = file.exists()
            System.out.println("${exist ? "exists" : "not exists"}\t${file.absolutePath}\n")
            if (exist)
                currentIndex += speed
        }

        if (currentIndex >= poses.size()) {
            System.exit(0)
        }

        VpdPose currentPose = poses.get(currentIndex)
        nullifyPlaneMovement(currentPose)

        if (mesh instanceof PmdMeshV1) {
            PmdAnimatedInstance ai = (PmdAnimatedInstance) animatedInstance
            ai.setVpdPose(currentPose)
            ai.update(0)
            ai.getPmdPose(pose)
            mesh.setPmdPose(pose)
            mesh.updateMeshWithPose()
        } else if (mesh instanceof PmxMeshV1) {
            PmxAnimatedInstance ai = (PmxAnimatedInstance) animatedInstance
            ai.setVpdPose(currentPose)
            ai.update(0)
            ai.getPmxPose(pose)
            mesh.setPmxPose(pose)
            mesh.updateMeshWithPose()
        }

        aabb.reset()
        if (mesh instanceof PmdMeshV1) {
            PmdMeshV1 pmdMesh = (PmdMeshV1)mesh
            for (int i = 0; i < pmdMesh.positions.capacity() / 3; i++) {
                if (!mesh.getModel().vertexUsed[i])
                    continue;
                float x = pmdMesh.positions.get(3 * i + 0);
                float y = pmdMesh.positions.get(3 * i + 1);
                float z = pmdMesh.positions.get(3 * i + 2);
                if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
                    continue;
                aabb.expandBy(x, y, z);
            }
        } else if (mesh instanceof PmxMeshV1) {
            PmxMeshV1 pmxMesh = (PmxMeshV1)mesh
            for (int i = 0; i < pmxMesh.positions.capacity() / 3; i++) {
                if (!pmxMesh.getModel().isVertexUsed(i))
                    continue;
                float x = pmxMesh.positions.get(3 * i + 0);
                float y = pmxMesh.positions.get(3 * i + 1);
                float z = pmxMesh.positions.get(3 * i + 2);
                if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
                    continue;
                aabb.expandBy(x, y, z);
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

        drawCoordinatePlane(gl);

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
            String outputFileName = String.format(outputDir + File.separator + "%05d.png", (int)(currentIndex / speed))
            File file = new File(outputFileName)
            BufferedImage screenShot = Screenshot.readToBufferedImage(0, 0, screenWidth, screenHeight, true);
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

        currentIndex += speed
    }

    private final static float AXIS_LENGTH = 50.0F;
    private final static float CELL_WIDTH = AXIS_LENGTH / 10.0F;

    protected void drawCoordinatePlane(GL2 gl) {
        gl.glLineWidth(2.0f);

        gl.glBegin(GL2.GL_LINES);
        gl.glColor3f(1, 0, 0);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(AXIS_LENGTH, 0, 0);

        gl.glColor3f(0, 1, 1);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(-AXIS_LENGTH, 0, 0);

        gl.glColor3f(0, 1, 0);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(0, AXIS_LENGTH, 0);

        gl.glColor3f(1, 0, 1);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(0, -AXIS_LENGTH, 0);

        gl.glColor3f(0, 0, 1);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(0, 0, AXIS_LENGTH);

        gl.glColor3f(1, 1, 0);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(0, 0, -AXIS_LENGTH);
        gl.glEnd();

        gl.glLineWidth(1.0f);
        gl.glColor3f(0.5f, 0.5f, 0.5f);
        gl.glBegin(GL2.GL_LINES);
        for (int i = -10; i <= 10; i++) {
            gl.glVertex3f((float)(i * CELL_WIDTH), 0, (float)(-AXIS_LENGTH));
            gl.glVertex3f((float)(i * CELL_WIDTH), 0, (float)(AXIS_LENGTH));
        }

        for (int i = -10; i <= 10; i++) {
            gl.glVertex3f((float)(-AXIS_LENGTH), 0, (float)(i * CELL_WIDTH));
            gl.glVertex3f((float)(AXIS_LENGTH), 0, (float)(i * CELL_WIDTH));
        }
        gl.glEnd();
    }

    static class CommandLineProcessor {
        Options options
        int screenWidth = 200
        int screenHeight = 200
        String modelFileName
        String motionFileName
        String outputDir
        ArrayList<Integer> frames = new ArrayList<Integer>();

        public void run(String[] args) {
            initializeOptions();
            processCommandLineArguments(args);
        }

        void processCommandLineArguments(String[] args) {
            CommandLineParser parser = new PosixParser()
            try {
                CommandLine cmd = parser.parse(options, args)
                String[] parsedArgs = cmd.getArgs()

                if (parsedArgs.length < 3) {
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

                modelFileName = parsedArgs[0]
                motionFileName = parsedArgs[1]
                outputDir = parsedArgs[2]

                int current = 3
                while (current < parsedArgs.length) {
                    String[] comps = args[current].split("-")
                    if (comps.length == 1) {
                        frames.add(Integer.valueOf(comps[0]))
                    } else {
                        int first = Integer.valueOf(comps[0])
                        int last = Integer.valueOf(comps[1])
                        for (int i = first; i <= last; i++) {
                            frames.add(i)
                        }
                    }
                    current++
                }

            } catch (Exception e) {
                printErrorAndExit("Command line parsing error!", e)
            }
        }

        void displayHelp(Options options) {
            HelpFormatter formatter = new HelpFormatter()
            formatter.printHelp("java yumyai.poseest.mmd.motion.RenderAnimationFramesNoPhysics <model-name> <motion-name> <frame-interval-1> [frame-interval-2] ...", options)
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
