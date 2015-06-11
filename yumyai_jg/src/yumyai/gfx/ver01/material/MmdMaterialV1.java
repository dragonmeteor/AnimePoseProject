package yumyai.gfx.ver01.material;

import yumyai.gfx.ver01.MaterialV1;
import yumyai.gfx.ver01.RenderEngineV1;
import yumyai.jogl.Program;
import yumyai.jogl.Texture2D;

import javax.media.opengl.GL2;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

public class MmdMaterialV1 implements MaterialV1 {
    private static boolean programCreated = false;
    private static Program program;

    public static void createProgram(GL2 gl) {
        program = new Program(gl,
                "data/shaders/v1/mmd_material/mmd_material.vert",
                "data/shaders/v1/mmd_material/mmd_material.frag");
        programCreated = true;
    }

    public static void destroyProgram() {
        if (programCreated) {
            program.dispose();
            program = null;
            programCreated = false;
        }
    }

    private Vector3f ambient = new Vector3f();
    private Vector4f diffuse = new Vector4f();
    private Vector3f specular = new Vector3f();
    private float shininess;

    private boolean useTexture;
    private String textureFileName;
    private boolean useToon;
    private String toonTextureFileName;
    private boolean useSphereMap;
    private String sphereMapTextureFileName;
    private int sphereMapMode;
    private boolean hasEdge;

    @Override
    public void use(RenderEngineV1 renderEngine) {
        if (!programCreated) {
            createProgram(renderEngine.getGl());
        }

        GL2 gl = renderEngine.getGl();
        //gl.glEnable(GL2.GL_BLEND);
        //gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        renderEngine.pushBindingFrame();
        renderEngine.setVariable("sys_materialAmbient", getAmbient());
        renderEngine.setVariable("sys_materialDiffuse", getDiffuse());
        renderEngine.setVariable("sys_materialSpecular", getSpecular());
        renderEngine.setVariable("sys_materialShininess", getShininess());

        renderEngine.setVariable("sys_useTexture", isUseTexture() ? 1 : 0);
        if (isUseTexture()) {
            Texture2D texture = (Texture2D) renderEngine.getTexture(textureFileName);
            renderEngine.setVariable("sys_materialTexture", texture);
        } else {
            renderEngine.setVariable("sys_materialTexture", null);
        }


        renderEngine.setVariable("sys_useToon", isUseToon() ? 1 : 0);
        if (isUseToon()) {
            Texture2D toonTexture = (Texture2D) renderEngine.getTexture(toonTextureFileName);
            toonTexture.wrapS = GL2.GL_CLAMP;
            toonTexture.wrapT = GL2.GL_CLAMP;
            toonTexture.wrapR = GL2.GL_CLAMP;
            toonTexture.minFilter = GL2.GL_NEAREST;
            toonTexture.magFilter = GL2.GL_NEAREST;
            renderEngine.setVariable("sys_toonTexture", toonTexture);
        } else {
            renderEngine.setVariable("sys_toonTexture", null);
        }

        renderEngine.setVariable("sys_useSphereMap", isUseSphereMap() ? 1 : 0);
        if (isUseSphereMap()) {
            renderEngine.setVariable("sys_sphereMapMode", sphereMapMode);
            Texture2D sphereMapTexture = (Texture2D) renderEngine.getTexture(sphereMapTextureFileName);
            sphereMapTexture.wrapS = GL2.GL_CLAMP;
            sphereMapTexture.wrapT = GL2.GL_CLAMP;
            sphereMapTexture.wrapR = GL2.GL_CLAMP;
            sphereMapTexture.minFilter = GL2.GL_NEAREST;
            sphereMapTexture.magFilter = GL2.GL_NEAREST;
            renderEngine.setVariable("sys_sphereMapTexture", sphereMapTexture);
        } else {
            renderEngine.setVariable("sys_sphereMapTexture", null);
            renderEngine.setVariable("sys_sphereMapMode", sphereMapMode);
        }

        renderEngine.use(program);
    }

    @Override
    public void unuse(RenderEngineV1 renderEngine) {
        renderEngine.unuse(program);
        if (isUseTexture())
        {
            Texture2D texture = (Texture2D)renderEngine.getTexture(textureFileName);
            texture.unuse();
        }
        if (isUseToon()) {
            Texture2D toonTexture = (Texture2D) renderEngine.getTexture(toonTextureFileName);
            toonTexture.unuse();
        }
        if (isUseSphereMap()) {
            Texture2D sphereMapTexture = (Texture2D) renderEngine.getTexture(sphereMapTextureFileName);
            sphereMapTexture.unuse();
        }
        renderEngine.popBindingFrame();
        GL2 gl = renderEngine.getGl();
    }

    public Vector3f getAmbient() {
        return ambient;
    }

    public void setAmbient(Vector3f ambient) {
        this.ambient.set(ambient);
    }

    public Vector4f getDiffuse() {
        return diffuse;
    }

    public void setDiffuse(Vector4f diffuse) {
        this.diffuse.set(diffuse);
    }

    public Vector3f getSpecular() {
        return specular;
    }

    public void setSpecular(Vector3f specular) {
        this.specular.set(specular);
    }

    public float getShininess() {
        return shininess;
    }

    public void setShininess(float shiniess) {
        this.shininess = shiniess;
    }

    public String getTextureFileName() {
        return textureFileName;
    }

    public void setTextureFileName(String textureFileName) {
        this.textureFileName = textureFileName;
    }

    public boolean isUseToon() {
        return useToon;
    }

    public void setUseToon(boolean useToon) {
        this.useToon = useToon;
    }

    public boolean isUseTexture() {
        return useTexture;
    }

    public void setUseTexture(boolean useTexture) {
        this.useTexture = useTexture;
    }

    public String getToonTextureFileName() {
        return toonTextureFileName;
    }

    public void setToonTextureFileName(String toonTextureFileName) {
        this.toonTextureFileName = toonTextureFileName;
    }

    public boolean isUseSphereMap() {
        return useSphereMap;
    }

    public void setUseSphereMap(boolean useSphereMap) {
        this.useSphereMap = useSphereMap;
    }

    public String getSphereMapTextureFileName() {
        return sphereMapTextureFileName;
    }

    public void setSphereMapTextureFileName(String sphereMapTextureFileName) {
        this.sphereMapTextureFileName = sphereMapTextureFileName;
    }

    public int getSphereMapMode() {
        return sphereMapMode;
    }

    public void setSphereMapMode(int sphereMapMode) {
        this.sphereMapMode = sphereMapMode;
    }

    public boolean isHasEdge() {
        return hasEdge;
    }

    public void setHasEdge(boolean hasEdge) {
        this.hasEdge = hasEdge;
    }

    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    @Override
    public void dispose() {
        // NOP
    }
}
