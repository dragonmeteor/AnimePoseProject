package yumyai.gfx.ver01.mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL2;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import yumyai.gfx.ver01.MaterialV1;
import yumyai.gfx.ver01.MeshV1;
import yumyai.gfx.ver01.RenderEngineV1;
import yumyai.gfx.ver01.material.MmdEdgeMaterialV1;
import yumyai.gfx.ver01.material.MmdMaterialV1;
import yumyai.mmd.pmd.PmdMaterial;
import yumyai.mmd.pmd.PmdModel;
import yumyai.mmd.pmd.PmdMorph;
import yumyai.mmd.pmd.PmdPose;
import yondoko.util.ObjectAllocator;

public class PmdMeshV1 implements MeshV1 {
    private PmdModel model;
    private PmdPose pose;
    private final List<MaterialV1> materials = new ArrayList<MaterialV1>();
    private final List<Integer> materialTriangleCount = new ArrayList<Integer>();
    private Matrix4f[] boneXforms;
    private FloatBuffer morphedPositions;
    public FloatBuffer positions;
    private FloatBuffer normals;
    protected boolean disposed;

    public PmdMeshV1(PmdModel model) {
        this.model = model;
        this.pose = new PmdPose(model);
        this.pose.clear();

        morphedPositions = FloatBuffer.allocate(model.positions.capacity());
        positions = FloatBuffer.allocate(model.positions.capacity());
        normals = FloatBuffer.allocate(model.normals.capacity());

        boneXforms = new Matrix4f[model.bones.size()];
        for (int i = 0; i < boneXforms.length; i++) {
            boneXforms[i] = new Matrix4f();
        }

        updateMeshWithPose();
    }

    public void addMaterial(MaterialV1 material, int triangleCount) {
        materials.add(material);
        materialTriangleCount.add(triangleCount);
    }

    @Override
    public void draw(RenderEngineV1 engine) {
        for (int i = 0; i < materials.size(); i++) {
            drawWithMaterialIndex(engine, i);
        }
    }

    public void drawWithMaterialIndex(RenderEngineV1 engine, int materialIndex) {
        GL2 gl = engine.getGl();

        if (materialIndex >= materials.size()) {
            throw new RuntimeException("invalid material index: " + materialIndex);
        }

        int start = 0;
        for (int i = 0; i < materialIndex; i++) {
            start += materialTriangleCount.get(i);
        }

        MaterialV1 material = materials.get(materialIndex);
        PmdMaterial pmdMaterial = model.materials.get(materialIndex);

        int triangleCount = materialTriangleCount.get(materialIndex);
        FloatBuffer texCoords = model.texCoords;
        IntBuffer triangleVertexIndices = model.triangleVertexIndices;

        material.use(engine);

        if (texCoords == null) {
            gl.glBegin(GL2.GL_TRIANGLES);
            for (int k = 0; k < triangleCount; k++) {

                int index = start + k;

                int v0 = triangleVertexIndices.get(3 * index);
                int v1 = triangleVertexIndices.get(3 * index + 1);
                int v2 = triangleVertexIndices.get(3 * index + 2);

                gl.glNormal3f(normals.get(3 * v0), normals.get(3 * v0 + 1), normals.get(3 * v0 + 2));
                gl.glVertex3f(positions.get(3 * v0), positions.get(3 * v0 + 1), positions.get(3 * v0 + 2));

                gl.glNormal3f(normals.get(3 * v1), normals.get(3 * v1 + 1), normals.get(3 * v1 + 2));
                gl.glVertex3f(positions.get(3 * v1), positions.get(3 * v1 + 1), positions.get(3 * v1 + 2));

                gl.glNormal3f(normals.get(3 * v2), normals.get(3 * v2 + 1), normals.get(3 * v2 + 2));
                gl.glVertex3f(positions.get(3 * v2), positions.get(3 * v2 + 1), positions.get(3 * v2 + 2));
            }
            gl.glEnd();
        } else {
            gl.glBegin(GL2.GL_TRIANGLES);
            for (int k = 0; k < triangleCount; k++) {

                int index = start + k;

                int v0 = triangleVertexIndices.get(3 * index);
                int v1 = triangleVertexIndices.get(3 * index + 1);
                int v2 = triangleVertexIndices.get(3 * index + 2);

                gl.glTexCoord2f(texCoords.get(2 * v0), texCoords.get(2 * v0 + 1));
                gl.glNormal3f(normals.get(3 * v0), normals.get(3 * v0 + 1), normals.get(3 * v0 + 2));
                gl.glVertex3f(positions.get(3 * v0), positions.get(3 * v0 + 1), positions.get(3 * v0 + 2));

                gl.glTexCoord2f(texCoords.get(2 * v1), texCoords.get(2 * v1 + 1));
                gl.glNormal3f(normals.get(3 * v1), normals.get(3 * v1 + 1), normals.get(3 * v1 + 2));
                gl.glVertex3f(positions.get(3 * v1), positions.get(3 * v1 + 1), positions.get(3 * v1 + 2));

                gl.glTexCoord2f(texCoords.get(2 * v2), texCoords.get(2 * v2 + 1));
                gl.glNormal3f(normals.get(3 * v2), normals.get(3 * v2 + 1), normals.get(3 * v2 + 2));
                gl.glVertex3f(positions.get(3 * v2), positions.get(3 * v2 + 1), positions.get(3 * v2 + 2));
            }

            gl.glEnd();
        }

        if (pmdMaterial.edgeFlag != 0) {
            gl.glEnable(GL2.GL_CULL_FACE);
            gl.glCullFace(GL2.GL_BACK);

            MmdEdgeMaterialV1 edgeMaterial = MmdEdgeMaterialV1.getInstance();
            edgeMaterial.setEdgeColor(new Vector4f(0,0,0,1));
            edgeMaterial.setEdgeSize(1.0f);

            edgeMaterial.use(engine);

            gl.glBegin(GL2.GL_TRIANGLES);
            for (int k = 0; k < triangleCount; k++) {

                int index = start + k;

                int v0 = triangleVertexIndices.get(3 * index);
                int v1 = triangleVertexIndices.get(3 * index + 1);
                int v2 = triangleVertexIndices.get(3 * index + 2);

                gl.glNormal3f(normals.get(3 * v0), normals.get(3 * v0 + 1), normals.get(3 * v0 + 2));
                gl.glVertex3f(positions.get(3 * v0), positions.get(3 * v0 + 1), positions.get(3 * v0 + 2));

                gl.glNormal3f(normals.get(3 * v1), normals.get(3 * v1 + 1), normals.get(3 * v1 + 2));
                gl.glVertex3f(positions.get(3 * v1), positions.get(3 * v1 + 1), positions.get(3 * v1 + 2));

                gl.glNormal3f(normals.get(3 * v2), normals.get(3 * v2 + 1), normals.get(3 * v2 + 2));
                gl.glVertex3f(positions.get(3 * v2), positions.get(3 * v2 + 1), positions.get(3 * v2 + 2));
            }
            gl.glEnd();

            edgeMaterial.unuse(engine);

            gl.glDisable(GL2.GL_CULL_FACE);
        }

        material.unuse(engine);
    }

    public int getMaterialCount() {
        return materials.size();
    }

    public void setPmdPose(PmdPose pmdPose) {
        if (pmdPose == null) {
            pose.clear();
        } else {
            if (model != pmdPose.getModel()) {
                throw new RuntimeException("given pose's model is not equal to the mesh's model");
            }
            pose.clear();
            pose.copy(pmdPose);
        }
    }

    public void getPmdPose(PmdPose pmdPose) {
        if (model != pmdPose.getModel()) {
            throw new RuntimeException("given pose's model is not equal to the mesh's model");
        }
        pmdPose.clear();
        pmdPose.copy(pose);
    }

    private void copyFloatBuffer(FloatBuffer source, FloatBuffer dest) {
        for (int i = 0; i < source.capacity(); i++) {
            dest.put(i, source.get(i));
        }
    }

    public void updateMeshWithPose() {
        copyFloatBuffer(model.positions, morphedPositions);
        for (int i = 1; i < model.morphs.size(); i++) {
            float weight = pose.getPoseMorphWeight(i);
            PmdMorph morph = model.morphs.get(i);

            for (int j = 0; j < morph.vertexIndices.length; j++) {
                int v = model.morphs.get(0).vertexIndices[morph.vertexIndices[j]];

                float ox = morphedPositions.get(3 * v + 0);
                float oy = morphedPositions.get(3 * v + 1);
                float oz = morphedPositions.get(3 * v + 2);

                morphedPositions.put(3 * v + 0, ox + weight * morph.displacements[j].x);
                morphedPositions.put(3 * v + 1, oy + weight * morph.displacements[j].y);
                morphedPositions.put(3 * v + 2, oz + weight * morph.displacements[j].z);
            }
        }

        for (int i = 0; i < model.bones.size(); i++) {
            pose.getVertexTransform(i, boneXforms[i]);
        }

        ObjectAllocator allcator = ObjectAllocator.get();
        Point3f p0 = allcator.getPoint3f();
        Point3f p1 = allcator.getPoint3f();
        Vector3f n0 = allcator.getVector3f();
        Vector3f n1 = allcator.getVector3f();

        for (int i = 0; i < model.positions.capacity() / 3; i++) {
            p0.set(morphedPositions.get(3 * i + 0), morphedPositions.get(3 * i + 1), morphedPositions.get(3 * i + 2));
            n0.set(model.normals.get(3 * i + 0), model.normals.get(3 * i + 1), model.normals.get(3 * i + 2));
            p1.set(morphedPositions.get(3 * i + 0), morphedPositions.get(3 * i + 1), morphedPositions.get(3 * i + 2));
            n1.set(model.normals.get(3 * i + 0), model.normals.get(3 * i + 1), model.normals.get(3 * i + 2));

            int boneIndex0 = model.vertexBoneIndices.get(2 * i + 0);
            int boneIndex1 = model.vertexBoneIndices.get(2 * i + 1);

            float weight0 = model.vertexBoneBlendWeights.get(2 * i + 0);
            float weight1 = model.vertexBoneBlendWeights.get(2 * i + 1);

            boneXforms[boneIndex0].transform(p0);
            boneXforms[boneIndex0].transform(n0);
            boneXforms[boneIndex1].transform(p1);
            boneXforms[boneIndex1].transform(n1);

            p0.scale(weight0);
            p1.scale(weight1);
            p0.add(p1);
            n0.scale(weight0);
            n1.scale(weight1);
            n0.add(n1);
            n0.normalize();

            positions.put(3 * i + 0, p0.x);
            positions.put(3 * i + 1, p0.y);
            positions.put(3 * i + 2, p0.z);
            normals.put(3 * i + 0, n0.x);
            normals.put(3 * i + 1, n0.y);
            normals.put(3 * i + 2, n0.z);
        }

        allcator.put(n1);
        allcator.put(n0);
        allcator.put(p1);
        allcator.put(p0);
    }

    @Override
    public void dispose() {
        if (!disposed) {
            for (int i = 0; i < materials.size(); i++) {
                materials.get(i).dispose();
            }
            materials.clear();
            disposed = true;
        }
    }

    public PmdModel getModel() {
        return model;
    }
}
