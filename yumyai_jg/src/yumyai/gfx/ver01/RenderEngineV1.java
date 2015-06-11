package yumyai.gfx.ver01;

import java.util.Map;
import javax.media.opengl.GL2;
import javax.vecmath.*;

import yumyai.jogl.*;

public class RenderEngineV1 {
    protected final BindingStack bindingStack = new BindingStack();
    protected final TexturePool texturePool = new TexturePool();
    protected final GL2 gl;
    private boolean disposed = false;

    public RenderEngineV1(GL2 gl) {
        this.gl = gl;
    }

    public GL2 getGl() {
        return gl;
    }

    public void pushBindingFrame() {
        bindingStack.pushFrame();
    }

    public void popBindingFrame() {
        bindingStack.popFrame();
    }

    public void setVariable(String name, Object value) {
        bindingStack.set(name, value);
    }

    public Object getVariable(String name) {
        return bindingStack.get(name);
    }

    public void use(Program program) {
        program.use();

        int lastTextureUnit = 0;

        Map<String, Uniform> uniforms = program.getUniforms();
        for (Map.Entry<String, Uniform> entry : uniforms.entrySet()) {
            String uniformName = entry.getKey();
            Uniform uniform = entry.getValue();
            Object value = bindingStack.get(uniformName);
            if (value == null)
                continue;

            if (value instanceof Float) {
                uniform.set1Float((Float) value);
            } else if (value instanceof Tuple2f) {
                uniform.setTuple2((Tuple2f) value);
            } else if (value instanceof Tuple3f) {
                uniform.setTuple3((Tuple3f) value);
            } else if (value instanceof Tuple4f) {
                uniform.setTuple4((Tuple4f) value);
            } else if (value instanceof Integer) {
                uniform.set1Int((Integer) value);
            } else if (value instanceof Matrix4f) {
                uniform.setMatrix4((Matrix4f) value);
            } else if (value instanceof TextureTwoDim) {
                TextureTwoDim texture = (TextureTwoDim) value;
                TextureUnit textureUnit = TextureUnit.getTextureUnit(gl, lastTextureUnit);
                textureUnit.activate();

                texture.use();
                textureUnit.bindToUniform(uniform);

                lastTextureUnit++;
            } else if (value instanceof Texture1D) {
                Texture1D texture = (Texture1D)value;
                TextureUnit textureUnit = TextureUnit.getTextureUnit(gl, lastTextureUnit);
                textureUnit.activate();

                texture.use();
                textureUnit.bindToUniform(uniform);

                lastTextureUnit++;
            }
        }
    }

    public void unuse(Program program) {
        program.unuse();
    }

    public void unuseProgram() {
        Program.unuseProgram(gl);
    }

    public Texture getTexture(String fileName) {
        return texturePool.getTexture(gl, fileName);
    }

    public void garbageCollect() {
        texturePool.garbageCollect();
    }

    public void dispose() {
        if (!disposed) {
            texturePool.dispose();
            disposed = true;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    public TexturePool getTexturePool() {
        return texturePool;
    }
}
