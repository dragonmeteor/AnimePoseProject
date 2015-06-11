package yumyai.gfx.ver01.material;

import javax.media.opengl.GL2;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import yumyai.gfx.ver01.MaterialV1;
import yumyai.gfx.ver01.RenderEngineV1;
import yumyai.jogl.Program;

public class PhongMaterialV1 implements MaterialV1
{
    private static boolean programCreated = false;
    private static Program program;

    public static void createProgram(GL2 gl)
    {
        program = new Program(gl,
                "data/shaders/v1/layer01_phong/layer01_phong.vert",
                "data/shaders/v1/layer01_phong/layer01_phong.frag");
        programCreated = true;
    }

    public static void destroyProgram()
    {
        if (programCreated)
        {
            program.dispose();
            program = null;
            programCreated = false;
        }
    }    
    private Vector3f ambient = new Vector3f();
    private Vector4f diffuse = new Vector4f();
    private Vector3f specular = new Vector3f();
    private float shininess;

    @Override
    public void use(RenderEngineV1 renderEngine)
    {
        if (!programCreated)
        {
            createProgram(renderEngine.getGl());
        }

        renderEngine.pushBindingFrame();
        renderEngine.setVariable("sys_materialAmbient", getAmbient());
        renderEngine.setVariable("sys_materialDiffuse", getDiffuse());
        renderEngine.setVariable("sys_materialSpecular", getSpecular());
        renderEngine.setVariable("sys_materialShininess", getShininess());        

        renderEngine.use(program);
    }

    @Override
    public void unuse(RenderEngineV1 renderEngine)
    {        
        renderEngine.unuse(program);
        renderEngine.popBindingFrame();
    }

    public Vector3f getAmbient()
    {
        return ambient;
    }

    public void setAmbient(Vector3f ambient)
    {
        this.ambient.set(ambient);
    }

    public Vector4f getDiffuse()
    {
        return diffuse;
    }

    public void setDiffuse(Vector4f diffuse)
    {
        this.diffuse.set(diffuse);
    }

    public Vector3f getSpecular()
    {
        return specular;
    }

    public void setSpecular(Vector3f specular)
    {
        this.specular.set(specular);
    }

    public float getShininess()
    {
        return shininess;
    }

    public void setShininess(float shiniess)
    {
        this.shininess = shiniess;
    }    

    @Override
    public void dispose()
    {
        // NOP
    }
}
