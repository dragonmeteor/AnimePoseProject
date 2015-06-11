package yumyai.jogl;

import java.util.HashMap;
import javax.media.opengl.GL2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Program
{
    private static Program current = null;
    private static Logger logger = LoggerFactory.getLogger(Program.class);

    // ************* Static functions *************
    public static Boolean isAProgramInUse()
    {
        return current != null;
    }

    public static Program getCurrent()
    {
        return current;
    }

    public static void unuseProgram(GL2 gl)
    {
        gl.glUseProgram(0);
        current = null;
    }
    // ************* Private variables *************
    private int id;
    private VertexShader vertexShader;
    private FragmentShader fragmentShader;
    private GL2 gl;
    private HashMap<String, Uniform> uniforms;
    private HashMap<String, Attribute> attributes;
    private boolean disposed = false;

    // ************* Public interface *************
    public Program(GL2 gl, VertexShader vertexShader, FragmentShader fragmentShader)
    {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.gl = gl;

        this.id = gl.glCreateProgramObjectARB();

        buildProgram();

        initializeUniforms();
    }

    public Program(GL2 glContext, String vertexSrcFile, String fragmentSrcFile)
    {
        this.vertexShader = null;
        this.fragmentShader = null;
        this.gl = glContext;

        this.id = gl.glCreateProgram();

        this.vertexShader = new VertexShader(this.gl, vertexSrcFile);
        this.fragmentShader = new FragmentShader(this.gl, fragmentSrcFile);

        // Attach shaders and link the program (may throw exception)
        buildProgram();

        // Create a hash map from all the 'active' uniform variables
        initializeUniforms();

        initializeAttributes();
    }

    public int getId()
    {
        return this.id;
    }

    public Boolean isUsed()
    {
        return current == this;
    }

    public void use()
    {
        this.gl.glUseProgram(this.id);
        current = this;
    }

    public void unuse()
    {
        for (Attribute attrib : attributes.values())
        {
            if (attrib.isEnabled())
            {
                attrib.disable();
            }
        }
        unuseProgram(this.gl);
    }

    public HashMap<String, Uniform> getUniforms()
    {
        return this.uniforms;
    }       

    public Uniform getUniform(String name)
    {
        return uniforms.get(name);
    }
    
    public HashMap<String, Attribute> getAttributes()
    {
        return this.attributes;
    }
    
    public Attribute getAttribute(String name)
    {
        return attributes.get(name);
    }
    
    public boolean hasAttribute(String name)
    {
        return attributes.containsKey(name);
    }

    public void dispose()
    {
        if (!disposed)
        {
            vertexShader.dispose();
            fragmentShader.dispose();
            gl.glDeleteProgram(id);
            disposed = true;
        }
    }

    // ************* Protected functions *************
    protected void finalize()
    {
        if (!disposed)
        {
            dispose();
        }
    }

    protected void buildProgram()
    {
        // Attach the vertex shader
        this.gl.glAttachShader(this.id, this.vertexShader.GetId());

        // Attach the fragment shader
        this.gl.glAttachShader(this.id, this.fragmentShader.GetId());

        gl.glLinkProgram(this.id);

        // Check the linking status
        int[] linkCheck = new int[1];
        gl.glGetProgramiv(this.id,
                GL2.GL_OBJECT_LINK_STATUS_ARB, linkCheck, 0);

        if (linkCheck[0] == GL2.GL_FALSE)
        {
            throw new GlslException("Link error "
                    + Shader.getInfoLog(this.gl, this.id));
        }
    }

// ************* Private functions *************
    private void initializeUniforms()
    {
        this.uniforms = new HashMap<String, Uniform>();

        int[] uniformCount = new int[1];
        this.gl.glGetProgramiv(this.id, GL2.GL_ACTIVE_UNIFORMS, uniformCount, 0);

        //System.out.print("GLSL uniforms: ");
        for (int uniform_index = 0; uniform_index < uniformCount[0]; uniform_index++)
        {
            Uniform currUniform = new Uniform(this.gl, this, uniform_index);

            if (!currUniform.getName().startsWith("gl_"))
            {
                //System.out.print(currUniform.getName() + " ");
                this.uniforms.put(currUniform.getName(), currUniform);
            }
        }
        System.out.println();
    }

    private void initializeAttributes()
    {
        this.attributes = new HashMap<String, Attribute>();

        int[] attribCount = new int[1];
        this.gl.glGetProgramiv(this.id, GL2.GL_ACTIVE_ATTRIBUTES, attribCount, 0);
        
        for (int attribIndex = 0; attribIndex < attribCount[0]; attribIndex++)
        {
            Attribute currentAttrib = new Attribute(this.gl, this, attribIndex);
            if (!currentAttrib.getName().startsWith("gl_"))
            {
                this.attributes.put(currentAttrib.getName(), currentAttrib);
            }
        }        
    }
}
