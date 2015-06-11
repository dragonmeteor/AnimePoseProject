package yumyai.poseest.mmd.example

import com.jogamp.opengl.util.awt.Screenshot
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import yondoko.math.VectorMathUtil
import yondoko.struct.Aabb3f
import yondoko.util.ArgumentProcessor
import yondoko.util.ImageUtil
import yumyai.gfx.ver01.MeshV1
import yumyai.gfx.ver01.mesh.PmdMeshV1
import yumyai.gfx.ver01.mesh.PmdToMeshV1Converter
import yumyai.gfx.ver01.mesh.PmxMeshV1
import yumyai.gfx.ver01.mesh.PmxToMeshV1Converter
import yumyai.mmd.pmd.PmdAnimatedInstance
import yumyai.mmd.pmd.PmdModel
import yumyai.mmd.pmd.PmdPose
import yumyai.mmd.pmx.*
import yumyai.mmd.vmd.VmdMotion
import yumyai.mmd.vpd.VpdPose
import yumyai.poseest.mmd.MmdCharInfo
import yumyai.poseest.mmd.PoseEstUtil
import yumyai.poseest.mmd.Settings
import yumyai.poseest.ui.RenderingFrame

import javax.imageio.ImageIO
import javax.media.opengl.GL2
import javax.media.opengl.GLAutoDrawable
import javax.swing.*
import javax.vecmath.AxisAngle4f
import javax.vecmath.Color3f
import javax.vecmath.Matrix4f
import javax.vecmath.Point3f
import javax.vecmath.Point4f
import javax.vecmath.Quat4f
import javax.vecmath.Tuple3f
import javax.vecmath.Vector3f
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage

class GenTrainingExampleData extends RenderingFrame {
    public static final float MORPH_DISPLACEMENT_THRESHOLD = 0.05f
    Object mmbg
    Object settings
    ArrayList<MmdCharInfo> charInfo;
    String outputDir
    int currentIndex = 0
    int filesPerMmbg = 0
    int mmbgCount = 0
    String currentModelName = ""
    String currentMotionName = ""
    String currentBackgroundName = ""
    BufferedImage backgroundImage = null
    int currentFrame = -1
    int startInterval = 150
    Vector3f gravity = new Vector3f(0, -100, 0)

    def model
    def instance
    MmdCharInfo currentCharInfo = null
    VpdPose inVpdPose = new VpdPose()
    VpdPose restPose = new VpdPose()
    VpdPose interpPose = new VpdPose()
    def modelPose
    def mesh
    float minimumDistance = 0;

    Vector3f lightAmbient = new Vector3f(0.7f, 0.7f, 0.7f);
    Vector3f lightDiffuse = new Vector3f(0.5f, 0.5f, 0.5f);
    Vector3f lightSpecular = new Vector3f(1.0f, 1.0f, 1.0f)
    Vector3f lightDirection = new Vector3f(0, -1, -1)
    Matrix4f viewInverse = new Matrix4f()
    Aabb3f aabb = new Aabb3f()
    ArrayList<Aabb3f> charViewAabbs = new ArrayList<Aabb3f>();

    GenTrainingExampleData(String title, Object mmbg, Object settings,
                           ArrayList<MmdCharInfo> charInfo,
                           String outputDir) {
        super(title, settings["image_width"], settings["image_height"])
        this.mmbg = mmbg;
        this.settings = settings;
        this.charInfo = charInfo;
        this.outputDir = outputDir
        for (int i = 0; i < Settings.characterViewBones.size(); i++) {
            charViewAabbs.add(new Aabb3f());
        }
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            println("Usage: java yumyai.poseest.mmd.example.GenTrainingExampleData " +
                    "<mmbg-file> <setting-file> <model-file> <output-dir>")
            System.exit(-1)
        }

        ArgumentProcessor argProc = new ArgumentProcessor(args)
        String mmbgFileName = argProc.getString()
        String settingFileName = argProc.getString()
        String modelFileName = argProc.getString()
        String outputDir = argProc.getString()

        FileUtils.forceMkdir(new File(outputDir))

        def slurper = new JsonSlurper();
        def mmbg = slurper.parse(new File(mmbgFileName))
        def settings = slurper.parse(new File(settingFileName))
        final ArrayList<MmdCharInfo> charInfo = MmdCharInfo.load(modelFileName)

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GenTrainingExampleData("Generate Training Example Data",
                        mmbg, settings, charInfo, outputDir).run()
            }
        });
    }

    public void run() {
        FileUtils.forceMkdir(new File(outputDir))

        if (settings["render_joint_pos"])
            filesPerMmbg = 3
        else
            filesPerMmbg = 2
        mmbgCount = mmbg.size()

        super.run()
    }

    String getFileNameByIndex(int index) {
        if (index % filesPerMmbg == filesPerMmbg - 1)
            return getDataFileNameByIndex(index)
        else
            return getImageFileNameByIndex(index)
    }

    String getImageFileNameByIndex(int index) {
        def fileIndex = mmbg[(int)(index /filesPerMmbg)]["index"]
        if (index % filesPerMmbg == 0)
            return String.format("%s/data_%08d.png", outputDir, (int) (fileIndex));
        else if (settings["render_joint_pos"] && (index % filesPerMmbg == 1))
            return String.format("%s/data_%08d_joints.png", outputDir, (int) (fileIndex));
    }

    String getDataFileNameByIndex(int index) {
        def fileIndex = mmbg[(int)(index / filesPerMmbg)]["index"]
        return String.format("%s/data_%08d_data.txt", outputDir, (int) (fileIndex));
    }

    boolean hasToDrawCharacter() {
        //return currentIndex % filesPerMmbg == 0;
        return (currentIndex % filesPerMmbg) != filesPerMmbg-1
    }

    boolean hasToDrawBoneLocations() {
        return settings["render_joint_pos"] && currentIndex % filesPerMmbg == 1;
    }

    boolean hasToOutputData() {
        return (currentIndex % filesPerMmbg) == filesPerMmbg-1
    }

    boolean modelHasBone(String boneName) {
        if (model instanceof PmdModel) {
            return model.getBoneIndex(boneName) != -1
        } else if (model instanceof PmxModel) {
            return model.hasBone(boneName)
        } else {
            return false
        }
    }

    int getBoneIndex(String boneName) {
        if (model instanceof PmdModel) {
            return model.getBoneIndex(boneName)
        } else if (model instanceof PmxModel) {
            return model.getBone(boneName).boneIndex
        } else {
            return -1
        }
    }

    void getBonePosition(String boneName, MmdCharInfo currentCharInfo, Point3f pos) {
        if (Settings.extraBoneNames.contains(boneName)) {
            int vertexIndex = currentCharInfo.extraBoneVertexIndex[boneName]
            if (mesh instanceof PmdMeshV1) {
                pos.x = mesh.positions.get(3*vertexIndex+0)
                pos.y = mesh.positions.get(3*vertexIndex+1)
                pos.z = mesh.positions.get(3*vertexIndex+2)
            } else if (mesh instanceof PmxMeshV1) {
                pos.x = mesh.positions.get(3*vertexIndex+0)
                pos.y = mesh.positions.get(3*vertexIndex+1)
                pos.z = mesh.positions.get(3*vertexIndex+2)
            }
        } else {
            int boneIndex = getBoneIndex(boneName)
            if (modelPose instanceof PmdPose) {
                modelPose.getBoneWorldPosition(boneIndex, pos)
            } else if (modelPose instanceof PmxPose) {
                modelPose.getBoneWorldPosition(boneIndex, pos)
            }
        }
    }

    @Override
    void init(GLAutoDrawable glAutoDrawable) {
        super.init(glAutoDrawable)
        camera.fovy = Settings.DEFAULT_FOVY
    }

    void nullifyPlaneMovement(VpdPose pose, String boneName) {
        Vector3f displacement = new Vector3f();
        Quat4f rotation = new Quat4f();
        if (pose.hasBone(boneName)) {
            pose.getBonePose(boneName, displacement, rotation)
            pose.setBoneDisplacement(boneName, 0, displacement.y, 0)
        }
    }

    void nullifyPlaneMovement(VpdPose pose) {
        nullifyPlaneMovement(pose, "å…¨ã¦ã®è¦ª")
    }


    Point3f bonePos = new Point3f();
    Point3f parentBonePos = new Point3f();
    Vector3f yAxis = new Vector3f(0,1,0);

    def mmbgConfig

    boolean newMotion = false;
    boolean newModel = false;
    boolean newFrame = false;
    boolean newBackground = false;

    void terminate() {
        renderEngine.dispose();
        System.exit(0)
    }

    @Override
    void draw(GLAutoDrawable glAutoDrawable) {
        if (currentIndex >= mmbgCount * filesPerMmbg) {
            terminate()
        }

        boolean exist = true
        while (currentIndex < mmbgCount * filesPerMmbg && exist) {
            String fileName = getFileNameByIndex(currentIndex);
            File file = new File(fileName)
            exist = file.exists()
            if (exist)
                currentIndex++
        }
        if (currentIndex >= mmbgCount * filesPerMmbg) {
            terminate()
        }


        mmbgConfig = mmbg[(int) (currentIndex / filesPerMmbg)]
        System.out.println(mmbgConfig["model"])
        System.out.println(mmbgConfig["motion"])
        System.out.println(mmbgConfig["frame"])
        System.out.println(mmbgConfig["background"])

        updateModel()
        updateMotion()
        updateBackground()

        if (newMotion || newModel || newFrame) {
            setupInstance()

            modelPose.clear();
            if (mesh instanceof PmdMeshV1) {
                mesh.setPmdPose(modelPose)
                mesh.updateMeshWithPose()
            } else if (mesh instanceof PmxMeshV1) {
                mesh.setPmxPose(modelPose)
                mesh.updateMeshWithPose()
            }
            computeAabb();
            //System.out.println("aabb = " + aabb);
            //minimumDistance = computeMinimumDistance();
            //System.out.println("minimumDistance = " + minimumDistance);

            simulatePhysics()

            if (mesh == null) {
                currentIndex++
                return
            }

            computeAabb()
        }

        setupCamera()

        final GL2 gl = glAutoDrawable.getGL().getGL2();
        gl.glEnable(GL2.GL_DEPTH_TEST)
        gl.glEnable(GL2.GL_BLEND);
        //gl.glDisable(GL2.GL_CULL_FACE)
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDepthFunc(GL2.GL_LEQUAL);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        camera.doProjection(glAutoDrawable);
        camera.doView(glAutoDrawable);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glPushMatrix();
        gl.glScaled(1, 1, -1);
        if (hasToDrawCharacter()) drawCharacter()
        if (hasToDrawBoneLocations()) drawBoneLocations(gl)
        gl.glPopMatrix();

        gl.glFlush()

        try {
            if (hasToDrawCharacter() || hasToDrawBoneLocations())
                takeScreenshot()
        } catch (Exception e) {
            e.printStackTrace()
        }

        if (hasToOutputData()) saveData()

        currentIndex++
    }

    private void updateModel() {
        newModel = false;
        if (!currentModelName.equals(mmbgConfig["model"])) {
            newModel = true
            String modelFileName = mmbgConfig["model"]
            String extension = FilenameUtils.getExtension(modelFileName).toLowerCase()
            if (extension.equals("pmd")) {
                model = PmdModel.load(modelFileName)
            } else {
                model = PmxModel.load(modelFileName)
            }
            currentModelName = mmbgConfig["model"]

            for (int i = 0; i < charInfo.size(); i++) {
                if (mmbgConfig["model"].equals(charInfo[i].fileName)) {
                    currentCharInfo = charInfo[i]
                    break
                }
            }
        }
    }

    private void updateMotion() {
        newFrame = false;
        if (currentFrame != mmbgConfig["frame"]) {
            newFrame = true
            if (mmbgConfig["frame"] == null) {
                currentFrame = 0;
            } else {
                currentFrame = mmbgConfig["frame"]
            }
        }

        newMotion = false;
        if (!currentMotionName.equals(mmbgConfig["motion"])) {
            newMotion = true;
            String motionFileName = mmbgConfig["motion"]
            String extension = FilenameUtils.getExtension(motionFileName).toLowerCase()
            if (extension.equals("vmd")) {
                VmdMotion vmdMotion = VmdMotion.load(motionFileName)
                inVpdPose.clear()
                vmdMotion.getPose(currentFrame, inVpdPose)
            } else {
                VpdPose vpdPose = VpdPose.load(motionFileName)
                inVpdPose = vpdPose
            }
            nullifyPlaneMovement(inVpdPose)
            currentMotionName = mmbgConfig["motion"]
        }
    }

    private void updateBackground() {
        newBackground = false;
        if (currentBackgroundName != mmbgConfig["background"]) {
            newBackground = true
            currentBackgroundName = mmbgConfig["background"]
        }
    }

    private void setupInstance() {
        if (model instanceof PmdModel) {
            instance = new PmdAnimatedInstance(model, true, true)
            instance.enablePhysics(true)
            instance.getPhysics().setGravity(gravity.x, gravity.y, gravity.z)
            modelPose = new PmdPose(model)
            mesh = PmdToMeshV1Converter.createMesh(model)
        } else if (model instanceof PmxModel) {
            instance = new PmxAnimatedInstance(model)
            instance.enablePhysics(true)
            instance.getPhysics().setGravity(gravity.x, gravity.y, gravity.z)
            modelPose = new PmxPose(model)
            mesh = PmxToMeshV1Converter.createMesh(model)
        }

        if (instance instanceof PmdAnimatedInstance) {
            modelPose.clear()
            instance.resetPhysics(modelPose)
        } else if (instance instanceof PmxAnimatedInstance) {
            modelPose.clear()
            instance.resetPhysics(modelPose)
        }
    }

    private float computeMinimumDistance() {
        Vector3f cameraFrameX = new Vector3f();
        Vector3f cameraFrameY = new Vector3f();
        Vector3f cameraFrameZ = new Vector3f();
        getCameraFrame(cameraFrameX, cameraFrameY, cameraFrameZ);

        Aabb3f viewAabb = PoseEstUtil.meshAabbAlongFrame((MeshV1)mesh, cameraFrameX, cameraFrameY, cameraFrameZ, aabb)
        //System.out.println("viewAabb = " + viewAabb);
        Point3f target = new Point3f((float)((viewAabb.pMin.x + viewAabb.pMax.x)/2.0f),
                (float)((viewAabb.pMin.y + viewAabb.pMax.y)/2.0f),
                viewAabb.pMax.z);
        float extent = Math.max(viewAabb.pMax.x - viewAabb.pMin.x, viewAabb.pMax.y - viewAabb.pMin.y) / 2.0f * Settings.DEFAULT_CAMERA_SCALE_FACTOR;
        //System.out.println("camera.fovy = " + camera.fovy);
        float distance = (float) (extent / Math.tan(camera.fovy / 2 * Math.PI / 180));
        return distance;
    }

    private void simulatePhysics() {
        long startTime = System.nanoTime()
        long origTime = startTime;
        long elaspedTime;
        for (int i = 0; i < startInterval; i++) {
            //System.out.println("i = " + i);
            float alpha = i * 1.0f / startInterval
            interpPose.clear()
            VpdPose.interpolate(restPose, inVpdPose, alpha, interpPose)
            //System.out.println("before sim");
            if (instance instanceof PmdAnimatedInstance) {
                instance.setVpdPose(interpPose)
                instance.update(i / 30.0)
            } else if (instance instanceof PmxAnimatedInstance) {
                //System.out.println("set pose");
                //System.out.println("blah")
                instance.setVpdPose(interpPose)
                //System.out.println("start simulate");
                instance.update(i / 30.0)
                //System.out.println("stop simulate");
            }
            //System.out.println("after sim");
            elaspedTime = System.nanoTime() - startTime;
            if (elaspedTime * 1e-9 > 1) {
                println("Simulation elasped time = " + (elaspedTime * 1e-9) + " seconds")
                startTime = elaspedTime
            }
        }
        elaspedTime = System.nanoTime() - origTime;
        if (elaspedTime * 1e-9 > 1) {
            println("Simulation elasped time = " + (elaspedTime * 1e-9) + " seconds")
        }

        if (instance instanceof PmdAnimatedInstance) {
            instance.getPmdPose(modelPose)
        } else if (instance instanceof PmxAnimatedInstance) {
            instance.getPmxPose(modelPose)
        }
        if (mesh instanceof PmdMeshV1) {
            mesh.setPmdPose(modelPose)
            mesh.updateMeshWithPose()
        } else if (mesh instanceof PmxMeshV1) {
            mesh.setPmxPose(modelPose)
            mesh.updateMeshWithPose()
        }
    }

    private void computeAabb() {
        aabb.reset()
        PoseEstUtil.computeCharacterViewAabbs(mesh, modelPose, charViewAabbs)

        float viewValue = mmbgConfig["view"]
        int viewIndex = 0
        for (int i = 0; i < settings["view_settings"].size(); i++) {
            if (viewValue < settings["view_settings"][i]) {
                viewIndex = i
                break
            }
        }
        if (viewIndex == 0)
            aabb.set(charViewAabbs.get(0))
        else if (viewIndex == 5)
            aabb.set(charViewAabbs.get(4))
        else {
            Aabb3f aabb0 = charViewAabbs.get(viewIndex-1)
            Aabb3f aabb1 = charViewAabbs.get(viewIndex)
            float alpha = (viewValue - settings["view_settings"][viewIndex-1]) /
                    (settings["view_settings"][viewIndex] - settings["view_settings"][viewIndex-1])
            aabb.pMin.scale((float)(1-alpha), aabb0.pMin)
            aabb.pMin.scaleAdd(alpha, aabb1.pMin, aabb.pMin)
            aabb.pMax.scale((float)(1-alpha), aabb0.pMax)
            aabb.pMax.scaleAdd(alpha, aabb1.pMax, aabb.pMax)
        }
    }

    private void getCameraFrame(Vector3f cameraFrameX, Vector3f cameraFrameY, Vector3f cameraFrameZ) {
        double U = settings["camera_theta_max"] * Math.PI / 180;
        double L = settings["camera_theta_min"] * Math.PI / 180;
        float cameraTheta = (float)(Math.acos(Math.cos(L) - mmbgConfig["camera_theta"]*(Math.cos(L) - Math.cos(U))));
        /*
        float cameraTheta = (float)((settings["camera_theta_min"] +
                mmbgConfig["camera_theta"]*(settings["camera_theta_max"] - settings["camera_theta_min"])) *
                Math.PI / 180)
                */
        float cameraPhi = (float)((settings["camera_phi_min"] +
                mmbgConfig["camera_phi"]*(settings["camera_phi_max"] - settings["camera_phi_min"])) *
                Math.PI / 180)
        cameraFrameZ.set((float)(Math.sin(cameraTheta)*Math.cos(cameraPhi)),
                (float)(Math.cos(cameraTheta)),
                (float)(Math.sin(cameraTheta)*Math.sin(cameraPhi)))
        cameraFrameY.set(0,1,0);
        cameraFrameX.set(0,0,0);
        VectorMathUtil.coordinateSystemGivenZandY(cameraFrameX, cameraFrameY, cameraFrameZ);

        float cameraRot = (float)((settings["camera_rotation_min"] + (mmbgConfig["camera_rotation"])*
                (settings["camera_rotation_max"] - settings["camera_rotation_min"])) * Math.PI / 180);
        System.out.println("cameraRot = " + cameraRot * 180 / Math.PI);
        Matrix4f N = new Matrix4f();
        N.setIdentity();
        N.setRotation(new AxisAngle4f(new Vector3f(0,0,1), cameraRot));

        Matrix4f M = new Matrix4f();
        M.setColumn(0, cameraFrameX.x, cameraFrameX.y, cameraFrameX.z, 0.0f);
        M.setColumn(1, cameraFrameY.x, cameraFrameY.y, cameraFrameY.z, 0.0f);
        M.setColumn(2, cameraFrameZ.x, cameraFrameZ.y, cameraFrameZ.z, 0.01);
        M.setColumn(3, 0.0f, 0.0f, 0.0f, 1.0f);

        M.mul(N);
        cameraFrameX.set(M.m00, M.m10, M.m20);
        cameraFrameY.set(M.m01, M.m11, M.m21);
        cameraFrameZ.set(M.m02, M.m12, M.m22);
    }

    private void setupCamera() {
        Vector3f cameraFrameX = new Vector3f();
        Vector3f cameraFrameY = new Vector3f();
        Vector3f cameraFrameZ = new Vector3f();
        getCameraFrame(cameraFrameX, cameraFrameY, cameraFrameZ);

        Aabb3f viewAabb = PoseEstUtil.meshAabbAlongFrame((MeshV1)mesh, cameraFrameX, cameraFrameY, cameraFrameZ, aabb)
        Point3f target = new Point3f((float)((viewAabb.pMin.x + viewAabb.pMax.x)/2.0f),
                (float)((viewAabb.pMin.y + viewAabb.pMax.y)/2.0f),
                viewAabb.pMax.z);
        float extent = Math.max(viewAabb.pMax.x - viewAabb.pMin.x, viewAabb.pMax.y - viewAabb.pMin.y) / 2.0f * Settings.DEFAULT_CAMERA_SCALE_FACTOR;
        float distance = (float) (extent / Math.tan(camera.fovy / 2 * Math.PI / 180));
        System.out.println("distance = " + distance);
        distance = Math.max(distance, minimumDistance);
        System.out.println("minimumDistance = " + minimumDistance);
        Point3f eye = new Point3f(target.x, target.y, (float)(target.z + distance));

        float windowHalfWidth = (float)(Math.tan(camera.fovy / 2 * Math.PI / 180) * distance);
        float diffX = windowHalfWidth - (viewAabb.pMax.x - viewAabb.pMin.x) / 2;
        float diffY = windowHalfWidth - (viewAabb.pMax.y - viewAabb.pMin.y) / 2;
        float minShiftX = diffX * (float)(settings["camera_shift_x_min"]);
        float maxShiftX = diffX * (float)(settings["camera_shift_x_max"]);
        float minShiftY = diffY * (float)(settings["camera_shift_y_min"]);
        float maxShiftY = diffY * (float)(settings["camera_shift_y_max"]);
        float shiftX = minShiftX + (maxShiftX - minShiftX) * (float)(mmbgConfig["camera_shift_x"]);
        float shiftY = minShiftY + (maxShiftY - minShiftY) * (float)(mmbgConfig["camera_shift_y"]);
        eye.x += shiftX;
        eye.y += shiftY;
        target.x += shiftX;
        target.y += shiftY;

        Matrix4f M = VectorMathUtil.matrixFromFrame(cameraFrameX, cameraFrameY, cameraFrameZ);
        M.transform(target);
        M.transform(eye)

        camera.eye.set(eye);
        camera.target.set(target);
        camera.up.set(cameraFrameY);
        camera.setAspect((float) (screenWidth * 1.0f / screenHeight))
        camera.updateFrameNotUsingVertical();
    }

    private void drawCharacter() {
        renderEngine.pushBindingFrame();
        renderEngine.setVariable("sys_eyePosition", camera.getEye());

        float ambient = 0.5f + 0.5f*mmbgConfig["light_fraction"]
        float diffuse = 1.2f - ambient;
        lightAmbient.set(ambient, ambient, ambient)
        lightDiffuse.set(diffuse, diffuse, diffuse)

        float lightTheta = (float)(3*Math.PI/4*mmbgConfig["light_theta"])
        float lightPhi = (float)(Math.PI / 6 + 4*Math.PI/6*mmbgConfig["light_phi"])
        lightDirection.y = (float)Math.cos(lightTheta)
        lightDirection.x = (float)(Math.sin(lightTheta) * Math.cos(lightPhi))
        lightDirection.z = (float)(Math.sin(lightTheta) * Math.sin(lightPhi))
        lightDirection.scale(-1)

        renderEngine.setVariable("sys_lightAmbient", lightAmbient);
        renderEngine.setVariable("sys_lightDiffuse", lightDiffuse);
        renderEngine.setVariable("sys_lightSpecular", lightSpecular);
        renderEngine.setVariable("sys_lightDirection", lightDirection);
        camera.getViewMatrix(viewInverse);
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

        renderEngine.popBindingFrame()
        renderEngine.garbageCollect()
    }

    private void drawBoneLocations(GL2 gl) {
        gl.glDisable(GL2.GL_DEPTH_TEST)
        gl.glBegin(GL2.GL_LINES);
        for (String boneName : Settings.displayBones) {
            String parentName = Settings.boneParentNames.get(boneName);
            if (parentName == null)
                continue
            getBonePosition(boneName, currentCharInfo, bonePos)
            Color3f boneColor = Settings.boneColors.get(boneName);
            gl.glColor3f(boneColor.x, boneColor.y, boneColor.z);
            gl.glVertex3f(bonePos.x, bonePos.y, bonePos.z)
            getBonePosition(parentName, currentCharInfo, parentBonePos)
            Color3f parentColor = Settings.boneColors.get(parentName)
            gl.glColor3f(parentColor.x, parentColor.y, parentColor.z)
            gl.glVertex3f(parentBonePos.x, parentBonePos.y, parentBonePos.z)
        }
        gl.glEnd();

        gl.glPointSize(5.0f)
        gl.glBegin(GL2.GL_POINTS)
        for (String boneName : Settings.displayBones) {
            getBonePosition(boneName, currentCharInfo, bonePos)
            Color3f color = Settings.boneColors.get(boneName);
            gl.glColor3f(color.x, color.y, color.z);
            gl.glVertex3f(bonePos.x, bonePos.y, bonePos.z)
        }
        gl.glEnd()
    }

    private void takeScreenshot() {
        BufferedImage screenShot = Screenshot.readToBufferedImage(0, 0, screenWidth, screenHeight, true);

        if (hasToDrawCharacter()) {
            if (newBackground) {
                BufferedImage rawBg = ImageIO.read(new File(mmbgConfig["background"]))
                int rawSize = Math.min(rawBg.getWidth(null), rawBg.getHeight(null));
                int size = (int)(rawSize*0.5f + rawSize*0.5f*mmbgConfig["background_size"])
                int x = (int)((rawBg.getWidth() - size)*mmbgConfig["background_x"])
                int y = (int)((rawBg.getHeight() - size)*mmbgConfig["background_y"])
                BufferedImage subBg = rawBg.getSubimage(x, y, size, size)
                Image scaledBg = subBg.getScaledInstance(screenWidth, screenHeight, Image.SCALE_SMOOTH)
                backgroundImage = ImageUtil.toBufferedImage(scaledBg)
            }
        }

        BufferedImage outputImage = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB)
        Graphics2D bGr = outputImage.createGraphics();
        bGr.drawImage(backgroundImage, 0, 0, null)
        bGr.drawImage(screenShot, 0, 0, null);
        bGr.dispose();

        BufferedImage outputImage2 = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
        bGr = outputImage2.createGraphics();
        bGr.drawImage(outputImage, 0, 0, null)
        bGr.dispose();

        String outputFileName = getFileNameByIndex(currentIndex)
        File file = new File(outputFileName)
        ImageIO.write(outputImage2, "png", file)
        System.out.println()
        System.out.println("Written ${outputFileName} ... ")
        System.out.println()
    }

    Point3f upperBodyPos = new Point3f();
    Point3f upperBodyCameraPos = new Point3f();
    Point4f p4 = new Point4f();
    Matrix4f viewMatrixInverse = new Matrix4f();

    def convertTuple3fToArray(Tuple3f v) {
        return [v.x, v.y, v.z]
    }

    private void saveData() {
        File dataFile = new File(getDataFileNameByIndex(currentIndex))
        if (dataFile.exists())
            return

        camera.getViewMatrixInverseNotUsingVertical(viewMatrixInverse)
        def projectionMatrix = VectorMathUtil.createProjectionMatrix(
                camera.fovy, camera.aspect, camera.near, camera.far);

        getBonePosition("上半身", currentCharInfo, upperBodyPos)
        upperBodyPos.z *= -1
        viewMatrixInverse.transform(upperBodyPos, upperBodyCameraPos)

        def outputData = [:]
        outputData["points_2d"] = [:]
        outputData["points_3d_world"] = [:]
        outputData["points_3d_camera"] = [:]

        outputData["camera_position"] = [
                camera.eye.x - upperBodyPos.x,
                camera.eye.y - upperBodyPos.y,
                camera.eye.z - upperBodyPos.z
        ]
        outputData["camera_x_axis"] = convertTuple3fToArray(camera.right)
        outputData["camera_y_axis"] = convertTuple3fToArray(camera.up)
        outputData["camera_z_axis"] = convertTuple3fToArray(camera.negGaze)

        for (String boneName : Settings.displayBones) {
            getBonePosition(boneName, currentCharInfo, bonePos)
            bonePos.z *= -1;
            String englishName = Settings.displayBoneEnglishNames.get(boneName)

            outputData["points_3d_world"][englishName] = [
                    bonePos.x - upperBodyPos.x,
                    bonePos.y - upperBodyPos.y,
                    bonePos.z - upperBodyPos.z
            ]
            viewMatrixInverse.transform(bonePos)
            outputData["points_3d_camera"][englishName] = [
                    bonePos.x - upperBodyCameraPos.x,
                    bonePos.y - upperBodyCameraPos.y,
                    bonePos.z - upperBodyCameraPos.z
            ]
            p4.set(bonePos.x, bonePos.y, bonePos.z, 1)
            projectionMatrix.transform(p4)
            double x = ((p4.x / p4.w) + 1) / 2 * screenWidth;
            double y = screenHeight - ((p4.y / p4.w) + 1) / 2 * screenHeight;
            outputData["points_2d"][englishName] = [x,y]
        }


        dataFile.withWriter("UTF-8") { fout ->
            String s = new JsonBuilder(outputData).toPrettyString()
            fout.write(s)
        }
    }

    private void drawSphere(GL2 gl) {
        gl.glBegin(GL2.GL_LINES);
        for (int i = 0; i < 8; i++) {
            float theta0 = (float) (Math.PI * i / 8);
            float theta1 = (float) (Math.PI * (i + 1) / 8);
            float y0 = (float) Math.cos(theta0);
            float y1 = (float) Math.cos(theta1);
            float yy0 = (float) Math.sin(theta0);
            float yy1 = (float) Math.sin(theta1);

            for (int j = 0; j < 16; j++) {
                float phi0 = (float) (2 * Math.PI * j / 16);
                float phi1 = (float) (2 * Math.PI * (j + 1) / 16);

                float x0 = (float) (Math.cos(phi0));
                float x1 = (float) (Math.cos(phi1));
                float z0 = (float) (Math.sin(phi0));
                float z1 = (float) (Math.sin(phi1));

                gl.glVertex3d(x0 * yy0, y0, z0 * yy0);
                gl.glVertex3d(x1 * yy0, y0, z1 * yy0);

                gl.glVertex3d(x1 * yy0, y0, z1 * yy0);
                gl.glVertex3d(x1 * yy1, y1, z1 * yy1);

                gl.glVertex3d(x1 * yy1, y1, z1 * yy1);
                gl.glVertex3d(x0 * yy1, y1, z0 * yy1);

                gl.glVertex3d(x0 * yy1, y1, z0 * yy1);
                gl.glVertex3d(x0 * yy0, y0, z0 * yy0);
            }
        }
        gl.glEnd();
    }

}
