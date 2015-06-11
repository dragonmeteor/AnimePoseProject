package yumyai.gfx.ver01.mesh;

import yondoko.util.ObjectAllocator;
import yumyai.gfx.ver01.MaterialV1;
import yumyai.gfx.ver01.MeshV1;
import yumyai.gfx.ver01.RenderEngineV1;
import yumyai.gfx.ver01.material.MmdEdgeMaterialV1;
import yumyai.mmd.pmx.*;
import yumyai.mmd.pmx.morph.VertexMorph;

import javax.media.opengl.GL2;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PmxMeshV1 implements MeshV1 {
    private PmxModel model;
    private PmxPose pose;
    private final List<MaterialV1> materials = new ArrayList<MaterialV1>();
    private final List<Integer> materialTriangleCount = new ArrayList<Integer>();
    private FloatBuffer morphedPositions;
    public FloatBuffer positions;
    private FloatBuffer normals;
    protected boolean disposed;
    private Matrix4f[] boneXforms;
    private ArrayList<Integer> materialOrder = new ArrayList<Integer>();

    public PmxMeshV1(PmxModel model) {
        this.model = model;
        this.pose = new PmxPose(model);
        this.pose.clear();
        disposed = false;
        morphedPositions = FloatBuffer.allocate(3 * model.getVertexCount());
        positions = FloatBuffer.allocate(3 * model.getVertexCount());
        normals = FloatBuffer.allocate(3 * model.getVertexCount());
        boneXforms = new Matrix4f[model.getBoneCount()];
        for (int i = 0; i < model.getBoneCount(); i++) {
            boneXforms[i] = new Matrix4f();
            boneXforms[i].setIdentity();
        }
        for (int i=0; i < model.getMaterialCount(); i++) {
            materialOrder.add(i);
        }
        final PmxModel mm = model;
        Collections.sort(materialOrder, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                PmxMaterial m1 = mm.getMaterial(o1);
                PmxMaterial m2 = mm.getMaterial(o2);
                if (m1.diffuse.w > m2.diffuse.w) {
                    return -1;
                } else if (m1.diffuse.w == m1.diffuse.w) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
    }

    public void addMaterial(MaterialV1 material, int triangleCount) {
        materials.add(material);
        materialTriangleCount.add(triangleCount);
    }

    @Override
    public void draw(RenderEngineV1 engine) {
        for (int i = 0; i < materials.size(); i++) {
            drawWithMaterialIndex(engine, materialOrder.get(i));
        }
    }

    public void updateMeshWithPose() {
        ObjectAllocator allocator = ObjectAllocator.get();
        Point3f p = allocator.getPoint3f();
        Point3f P = allocator.getPoint3f();
        Vector3f n = allocator.getVector3f();
        Vector3f N = allocator.getVector3f();

        for (int i = 0; i < model.getVertexCount(); i++) {
            PmxVertex vertex = model.getVertex(i);
            morphedPositions.put(3 * i + 0, vertex.position.x);
            morphedPositions.put(3 * i + 1, vertex.position.y);
            morphedPositions.put(3 * i + 2, vertex.position.z);
        }
        for (int i = 0; i < model.getMorphCount(); i++) {
            float weight = pose.getMorphWeights(i);
            if (weight > 0) {
                PmxMorph morph = model.getMorph(i);
                if (morph instanceof VertexMorph) {
                    VertexMorph vertexMorph = (VertexMorph) morph;
                    for (VertexMorph.Offset offset : vertexMorph.offsets) {
                        int index = offset.vertexIndex;
                        float x = morphedPositions.get(3 * index + 0) + weight * offset.displacement.x;
                        float y = morphedPositions.get(3 * index + 1) + weight * offset.displacement.y;
                        float z = morphedPositions.get(3 * index + 2) + weight * offset.displacement.z;
                        morphedPositions.put(3 * index + 0, x);
                        morphedPositions.put(3 * index + 1, y);
                        morphedPositions.put(3 * index + 2, z);
                    }
                }
            }
        }

        for (int i = 0; i < model.getBoneCount(); i++) {
            pose.getBoneTransform(i, boneXforms[i]);
        }

        for (int i = 0; i < model.getVertexCount(); i++) {
            PmxVertex vertex = model.getVertex(i);
            P.set(0, 0, 0);
            N.set(0, 0, 0);
            int boneCount = 0;
            switch (vertex.boneDataType) {
                case 0:
                    boneCount = 1;
                    break;
                case 1:
                    boneCount = 2;
                    break;
                case 2:
                    boneCount = 4;
                    break;
                case 3:
                    boneCount = 2;
                    break;
            }
            for (int k = 0; k < boneCount; k++) {
                int boneIndex = vertex.boneIndices[k];
                if (boneIndex < 0)
                    continue;
                PmxBone bone = model.getBone(boneIndex);
                p.set(morphedPositions.get(3 * i + 0),
                        morphedPositions.get(3 * i + 1),
                        morphedPositions.get(3 * i + 2));
                p.sub(bone.position);
                Matrix4f xform = boneXforms[boneIndex];
                xform.transform(p);
                p.scale(vertex.boneWeights[k]);
                P.add(p);
                n.set(vertex.normal);
                xform.transform(n);
                n.scale(vertex.boneWeights[k]);
                N.add(n);
            }
            N.normalize();
            positions.put(3 * i + 0, P.x);
            positions.put(3 * i + 1, P.y);
            positions.put(3 * i + 2, P.z);
            normals.put(3 * i + 0, N.x);
            normals.put(3 * i + 1, N.y);
            normals.put(3 * i + 2, N.z);
        }

        allocator.put(P);
        allocator.put(p);
        allocator.put(N);
        allocator.put(n);
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
        material.use(engine);
        int triangleCount = materialTriangleCount.get(materialIndex);


        gl.glBegin(GL2.GL_TRIANGLES);
        for (int k = 0; k < triangleCount; k++) {

            int index = start + k;

            int v0 = model.getVertexIndex(3 * index);
            int v1 = model.getVertexIndex(3 * index + 1);
            int v2 = model.getVertexIndex(3 * index + 2);

            PmxVertex vv0 = model.getVertex(v0);
            PmxVertex vv1 = model.getVertex(v1);
            PmxVertex vv2 = model.getVertex(v2);

            gl.glTexCoord2f(vv0.texCoords.x, 1 - vv0.texCoords.y);
            gl.glNormal3f(normals.get(3 * v0), normals.get(3 * v0 + 1), normals.get(3 * v0 + 2));
            gl.glVertex3f(positions.get(3 * v0), positions.get(3 * v0 + 1), positions.get(3 * v0 + 2));

            gl.glTexCoord2f(vv1.texCoords.x, 1 - vv1.texCoords.y);
            gl.glNormal3f(normals.get(3 * v1), normals.get(3 * v1 + 1), normals.get(3 * v1 + 2));
            gl.glVertex3f(positions.get(3 * v1), positions.get(3 * v1 + 1), positions.get(3 * v1 + 2));

            gl.glTexCoord2f(vv2.texCoords.x, 1 - vv2.texCoords.y);
            gl.glNormal3f(normals.get(3 * v2), normals.get(3 * v2 + 1), normals.get(3 * v2 + 2));
            gl.glVertex3f(positions.get(3 * v2), positions.get(3 * v2 + 1), positions.get(3 * v2 + 2));
        }
        gl.glEnd();

        material.unuse(engine);

        PmxMaterial pmxMaterial = model.getMaterial(materialIndex);
        if ((pmxMaterial.renderFlag & 16) != 0) {
            gl.glEnable(GL2.GL_CULL_FACE);
            gl.glCullFace(GL2.GL_BACK);

            MmdEdgeMaterialV1 edgeMaterial = MmdEdgeMaterialV1.getInstance();
            edgeMaterial.use(engine);
            edgeMaterial.setEdgeColor(pmxMaterial.edgeColor);
            edgeMaterial.setEdgeSize(pmxMaterial.edgeSize);

            gl.glBegin(GL2.GL_TRIANGLES);
            for (int k = 0; k < triangleCount; k++) {

                int index = start + k;

                int v0 = model.getVertexIndex(3 * index);
                int v1 = model.getVertexIndex(3 * index + 1);
                int v2 = model.getVertexIndex(3 * index + 2);

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

    public PmxModel getModel() {
        return model;
    }

    public void setPmxPose(PmxPose pmxPose) {
        if (pmxPose == null) {
            this.pose.clear();
        } else {
            this.pose.clear();
            this.pose.copy(pmxPose);
        }
    }

    public PmxPose getPose() {
        return pose;
    }

    public int getMaterialCount() {
        return model.getMaterialCount();
    }
}
