package yumyai.gfx.ver01.mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL2;
import yumyai.gfx.ver01.MaterialV1;
import yumyai.gfx.ver01.MeshV1;
import yumyai.gfx.ver01.RenderEngineV1;

public class StaticMeshV1 implements MeshV1
{
    protected FloatBuffer positions;
    protected FloatBuffer normals;
    protected FloatBuffer texCoords;
    protected IntBuffer triangles;
    protected final List<MaterialV1> materials = new ArrayList<MaterialV1>();
    protected final List<Integer> materialTriangleCount = new ArrayList<Integer>();
    
    protected boolean disposed;  
    
    public StaticMeshV1()
    {
        positions = null;
        normals = null;
        texCoords = null;
        triangles = null;
        disposed = false;
    }

    public FloatBuffer getPositions()
    {
        return positions;
    }

    public void setPositions(FloatBuffer positions)
    {
        this.positions = positions;
    }

    public FloatBuffer getNormals()
    {
        return normals;
    }

    public void setNormals(FloatBuffer normals)
    {
        this.normals = normals;
    }

    public FloatBuffer getTexCoords()
    {
        return texCoords;
    }

    public void setTexCoords(FloatBuffer texCoords)
    {
        this.texCoords = texCoords;
    }

    public IntBuffer getTriangles()
    {
        return triangles;
    }

    public void setTriangles(IntBuffer triangles)
    {
        this.triangles = triangles;
    }
    
    public void addMaterial(MaterialV1 material, int triangleCount)
    {
        materials.add(material);
        materialTriangleCount.add(triangleCount);
    }        
        
    @Override
    public void finalize() throws Throwable
    {        
        dispose();
        super.finalize();
    }
    
    @Override
    public void dispose()
    {
        if (!disposed)
        {            
            for (int i = 0; i < materials.size(); i++)
            {
                materials.get(i).dispose();                
            }
            materials.clear();
            disposed = true;
        }
    }
    
    public void drawWithMaterialIndex(RenderEngineV1 engine, int materialIndex)
    {
        GL2 gl = engine.getGl();
        
        if (materialIndex >= materials.size())
        {
            throw new RuntimeException("invalid material index: " + materialIndex);
        }        
        
        int start = 0;
        for (int i = 0; i < materialIndex; i++)
        {
            start += materialTriangleCount.get(i);
        }
        
        MaterialV1 material = materials.get(materialIndex);
        material.use(engine);            
        int triangleCount = materialTriangleCount.get(materialIndex);

        if (texCoords == null)
        {
            for (int k = 0; k < triangleCount; k++)
             {
                gl.glBegin(GL2.GL_TRIANGLES);

                int index = start + k;

                int v0 = triangles.get(3*index);
                int v1 = triangles.get(3*index+1);
                int v2 = triangles.get(3*index+2);

                gl.glNormal3f(normals.get(3*v0), normals.get(3*v0+1), normals.get(3*v0+2));
                gl.glVertex3f(positions.get(3*v0), positions.get(3*v0+1), positions.get(3*v0+2));

                gl.glNormal3f(normals.get(3*v1), normals.get(3*v1+1), normals.get(3*v1+2));
                gl.glVertex3f(positions.get(3*v1), positions.get(3*v1+1), positions.get(3*v1+2));

                gl.glNormal3f(normals.get(3*v2), normals.get(3*v2+1), normals.get(3*v2+2));
                gl.glVertex3f(positions.get(3*v2), positions.get(3*v2+1), positions.get(3*v2+2));

                gl.glEnd();
            }
        }
        else
        {
            for (int k = 0; k < triangleCount; k++)
             {
                gl.glBegin(GL2.GL_TRIANGLES);

                int index = start + k;

                int v0 = triangles.get(3*index);
                int v1 = triangles.get(3*index+1);
                int v2 = triangles.get(3*index+2);

                gl.glTexCoord2f(texCoords.get(2*v0), texCoords.get(2*v0+1));
                gl.glNormal3f(normals.get(3*v0), normals.get(3*v0+1), normals.get(3*v0+2));
                gl.glVertex3f(positions.get(3*v0), positions.get(3*v0+1), positions.get(3*v0+2));

                gl.glTexCoord2f(texCoords.get(2*v1), texCoords.get(2*v1+1));
                gl.glNormal3f(normals.get(3*v1), normals.get(3*v1+1), normals.get(3*v1+2));
                gl.glVertex3f(positions.get(3*v1), positions.get(3*v1+1), positions.get(3*v1+2));

                gl.glTexCoord2f(texCoords.get(2*v2), texCoords.get(2*v2+1));
                gl.glNormal3f(normals.get(3*v2), normals.get(3*v2+1), normals.get(3*v2+2));
                gl.glVertex3f(positions.get(3*v2), positions.get(3*v2+1), positions.get(3*v2+2));

                gl.glEnd();
            }
        }
                        
        material.unuse(engine);
    }

    @Override
    public void draw(RenderEngineV1 engine)
    {                
        for (int i = 0; i < materials.size(); i++)
        {
            drawWithMaterialIndex(engine, i);
        }
    }
    
    public int getMaterialCount()
    {
        return materials.size();
    }
}
