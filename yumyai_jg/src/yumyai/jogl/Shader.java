package yumyai.jogl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.media.opengl.GL2;

public abstract class Shader
{
    // ************* Static functions *************
    // Check whether the GLSL vertex and fragment shaders are supported
    public static Boolean checkGlslSupport(GL2 gl)
    {
        if (!gl.isExtensionAvailable("GL_ARB_vertex_shader")
                || !gl.isExtensionAvailable("GL_ARB_fragment_shader"))
        {

            System.err.println("GLSL is not supported!");
            return false;

        }
        else
        {
            System.out.println("GLSL is supported!");
            return true;
        }
    }

    public static String readFile(String filePath) throws GlslException
    {
        String content = null;

        try
        {
            File f = new File(filePath);
            FileReader fr = new FileReader(f);

            int size = (int) f.length();
            char buff[] = new char[size];
            int len = fr.read(buff);

            content = new String(buff, 0, len);

            fr.close();
        }
        catch (IOException e)
        {
            throw new GlslException(e.getMessage());
        }

        return content;
    }

    public static String getInfoLog(GL2 gl, int objectId)
    {
        int[] buf = new int[1];

        // Retrieve the log length
        gl.glGetObjectParameterivARB(objectId,
                GL2.GL_OBJECT_INFO_LOG_LENGTH_ARB, buf, 0);

        int logLength = buf[0];

        if (logLength <= 1)
        {
            return "";
        }
        else
        {
            // Retrieve the log message
            byte[] content = new byte[logLength + 1];
            gl.glGetInfoLogARB(objectId, logLength, buf, 0, content, 0);

            return new String(content);
        }
    }
    // ************* Private variables *************
    private final int type; // GL2.GL_FRAGMENT_SHADER or GL2.GL_VERTEX_SHADER
    private int id;
    private GL2 gl;
    private boolean disposed;

    // ************* Public interface *************
    public Shader(int shaderType, GL2 glContext,
            String srcFile) throws GlslException
    {
        this.type = shaderType;
        this.gl = glContext;

        this.id = this.gl.glCreateShaderObjectARB(this.type);

        String source = readFile(srcFile);

        setSource(source);

        if (!compile())
        {
            throw new GlslException("Compilation error "
                    + getInfoLog(this.gl, this.id));
        }
    }

    public int GetId()
    {
        return this.id;
    }

    public void dispose()
    {
        if (!disposed)
        {
            gl.glDeleteShader(this.id);
            disposed = true;
        }
    }

    // ************* Protected functions *************
    @Override
    protected void finalize()
    {
        dispose();        
    }

    private void setSource(String source)
    {
        // Attach the GLSL source code
        gl.glShaderSourceARB(this.id, 1,
                new String[]
                {
                    source
                },
                new int[]
                {
                    source.length()
                }, 0);
    }

    private Boolean compile()
    {
        // Try to compile the GLSL source code
        gl.glCompileShaderARB(this.id);

        // Check the compilation status
        int[] compileCheck = new int[1];
        this.gl.glGetObjectParameterivARB(this.id,
                GL2.GL_OBJECT_COMPILE_STATUS_ARB, compileCheck, 0);

        return compileCheck[0] == GL2.GL_TRUE;
    }
}
