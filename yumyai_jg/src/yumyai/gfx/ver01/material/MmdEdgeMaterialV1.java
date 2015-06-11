package yumyai.gfx.ver01.material;

import yumyai.gfx.ver01.MaterialV1;
import yumyai.gfx.ver01.RenderEngineV1;
import yumyai.jogl.Program;
import yumyai.jogl.Texture2D;

import javax.media.opengl.GL2;
import javax.vecmath.Vector4f;

public class MmdEdgeMaterialV1 implements MaterialV1 {
    private static boolean programCreated = false;
    private static Program program;
    private final static MmdEdgeMaterialV1 instance = new MmdEdgeMaterialV1();

    public static void createProgram(GL2 gl) {
        program = new Program(gl,
                "data/shaders/v1/mmd_edge/mmd_edge.vert",
                "data/shaders/v1/mmd_edge/mmd_edge.frag");
        programCreated = true;
    }

    public static void destroyProgram() {
        if (programCreated) {
            program.dispose();
            program = null;
            programCreated = false;
        }
    }

    private Vector4f edgeColor = new Vector4f(0,0,0,1);
    private float edgeSize = 1;

    public Vector4f getEdgeColor() {
        return edgeColor;
    }

    public void setEdgeColor(Vector4f edgeColor) {
        this.edgeColor = edgeColor;
    }

    public float getEdgeSize() {
        return edgeSize;
    }

    public void setEdgeSize(float edgeSize) {
        this.edgeSize = edgeSize;
    }

    public static MmdEdgeMaterialV1 getInstance() {
        return instance;
    }

    @Override
    public void use(RenderEngineV1 renderEngine) {
        if (!programCreated) {
            createProgram(renderEngine.getGl());
        }

        GL2 gl = renderEngine.getGl();

        renderEngine.pushBindingFrame();
        renderEngine.setVariable("sys_edgeColor", getEdgeColor());
        renderEngine.setVariable("sys_edgeSize", getEdgeSize());

        renderEngine.use(program);
    }

    @Override
    public void unuse(RenderEngineV1 renderEngine) {
        renderEngine.unuse(program);
        renderEngine.popBindingFrame();
    }

    @Override
    public void dispose() {
        // NOP
    }

    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }
}
