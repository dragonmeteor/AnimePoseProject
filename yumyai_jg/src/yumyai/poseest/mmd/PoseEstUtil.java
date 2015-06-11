package yumyai.poseest.mmd;

import yondoko.math.VectorMathUtil;
import yondoko.struct.Aabb3f;
import yondoko.util.FloatUtil;
import yumyai.gfx.ver01.MeshV1;
import yumyai.gfx.ver01.mesh.PmdMeshV1;
import yumyai.gfx.ver01.mesh.PmxMeshV1;
import yumyai.jogl.ui.PerspectiveCamera;
import yumyai.mmd.pmd.PmdModel;
import yumyai.mmd.pmd.PmdMorph;
import yumyai.mmd.pmd.PmdPose;
import yumyai.mmd.pmx.*;
import yumyai.mmd.pmx.morph.VertexMorph;

import javax.media.opengl.GL2;
import javax.vecmath.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class PoseEstUtil {
    public static boolean influencedByBones(PmdModel model, int vertexIndex, Collection<String> bones) {
        int boneIndex0 = model.vertexBoneIndices.get(vertexIndex * 2 + 0);
        int boneIndex1 = model.vertexBoneIndices.get(vertexIndex * 2 + 1);
        float boneWeight0 = model.vertexBoneBlendWeights.get(vertexIndex * 2 + 0);
        float boneWeight1 = model.vertexBoneBlendWeights.get(vertexIndex * 2 + 1);
        boolean result = false;
        if (boneWeight0 != 0) {
            String boneName0 = model.bones.get(boneIndex0).japaneseName;
            result = result || bones.contains(boneName0);
        }
        if (boneWeight1 != 0) {
            String boneName1 = model.bones.get(boneIndex1).japaneseName;
            result = result || bones.contains(boneName1);
        }
        return result;
    }

    public static boolean isRelevantBone(PmxModel model, int boneIndex, Collection<String> bones) {
        if (boneIndex < 0)
            return false;
        PmxBone bone = model.getBone(boneIndex);
        if (bones.contains(bone.japaneseName))
            return true;
        else if (bone.copyRotation() || bone.copyTranslation()) {
            if (bone.copyParentBoneIndex < 0)
                return false;
            bone = model.getBone(bone.copyParentBoneIndex);
            return bones.contains(bone.japaneseName);
        } else {
            return false;
        }
        //return bones.contains(bone.japaneseName);
    }

    public static boolean influencedByBones(PmxModel model, int vertexIndex, Collection<String> bones) {
        PmxVertex vertex = model.getVertex(vertexIndex);
        boolean result = false;
        int boneCount = 1;
        switch (vertex.boneDataType) {
            case PmxVertex.BDEF1:
                boneCount = 1;
                break;
            case PmxVertex.BDEF2:
                boneCount = 2;
                break;
            case PmxVertex.BDEF4:
                boneCount = 4;
                break;
            case PmxVertex.SDEF:
                boneCount = 2;
                break;
        }
        for (int i = 0; i < boneCount; i++) {
            result = result || (isRelevantBone(model, vertex.boneIndices[i], bones) && vertex.boneWeights[i] > 0);
        }
        return result;
    }

    public static void includeVerticesInfluencedByBones(PmdMeshV1 mesh, Collection<String> bones, Aabb3f aabb) {
        PmdModel model = mesh.getModel();
        if (bones != null) {
            for (int i = 0; i < mesh.positions.capacity() / 3; i++) {
                if (!model.vertexUsed[i])
                    continue;
                if (!influencedByBones(model, i, bones))
                    continue;
                float x = mesh.positions.get(3 * i + 0);
                float y = mesh.positions.get(3 * i + 1);
                float z = mesh.positions.get(3 * i + 2);
                if (FloatUtil.isFinite(x) && FloatUtil.isFinite(y) && FloatUtil.isFinite(z)) {
                    aabb.expandBy(x, y, z);
                }
            }
        }
    }

    public static void includeVerticesInfluencedByMorphs(PmdMeshV1 mesh, Collection<String> morphs, Aabb3f aabb) {
        PmdModel model = mesh.getModel();
        if (morphs != null) {
            for (PmdMorph morph : model.morphs) {
                if (morphs.contains(morph.japaneseName)) {
                    for (int k = 0; k < morph.vertexIndices.length; k++) {
                        int v = model.morphs.get(0).vertexIndices[morph.vertexIndices[k]];
                        if (!model.vertexUsed[v])
                            continue;
                        if (Math.abs(morph.displacements[k].x) < Settings.MORPH_DISPLACEMENT_THRESHOLD &&
                                Math.abs(morph.displacements[k].y) < Settings.MORPH_DISPLACEMENT_THRESHOLD &&
                                Math.abs(morph.displacements[k].z) < Settings.MORPH_DISPLACEMENT_THRESHOLD)
                            continue;
                        float x = mesh.positions.get(3 * v + 0);
                        float y = mesh.positions.get(3 * v + 1);
                        float z = mesh.positions.get(3 * v + 2);
                        if (FloatUtil.isFinite(x) && FloatUtil.isFinite(y) && FloatUtil.isFinite(z)) {
                            aabb.expandBy(x, y, z);
                        }
                    }
                }
            }
        }
    }

    public static void includeBonePositions(PmdMeshV1 mesh, PmdPose pose, Collection<String> bones, Aabb3f aabb) {
        PmdModel model = mesh.getModel();
        Point3f bonePos = new Point3f();
        if (pose != null) {
            for (String boneName : bones) {
                int boneIndex = model.getBoneIndex(boneName);
                if (boneIndex >= 0)
                    continue;
                pose.getBoneWorldPosition(boneIndex, bonePos);
                float x = bonePos.x;
                float y = bonePos.y;
                float z = bonePos.z;
                if (FloatUtil.isFinite(x) && FloatUtil.isFinite(y) && FloatUtil.isFinite(z)) {
                    aabb.expandBy(x, y, z);
                }
            }
        }
    }

    public static void extendAabbInZDirection(PmdMeshV1 mesh, Aabb3f aabb) {
        PmdModel model = mesh.getModel();
        for (int i = 0; i < mesh.positions.capacity() / 3; i++) {
            if (!model.vertexUsed[i])
                continue;
            float x = mesh.positions.get(3 * i + 0);
            float y = mesh.positions.get(3 * i + 1);
            float z = mesh.positions.get(3 * i + 2);
            if (!(FloatUtil.isFinite(x) && FloatUtil.isFinite(y) && FloatUtil.isFinite(z)))
                continue;
            if (x >= aabb.pMin.x && x <= aabb.pMax.x && y >= aabb.pMin.y && y <= aabb.pMax.y) {
                aabb.expandBy(x, y, z);
            }
        }
    }

    public static void computeCharacterViewAabb(PmdMeshV1 mesh, PmdPose pose,
                                                Collection<String> bones,
                                                Collection<String> morphs,
                                                Aabb3f aabb) {
        aabb.reset();
        includeVerticesInfluencedByBones(mesh, bones, aabb);
        includeVerticesInfluencedByMorphs(mesh, morphs, aabb);
        includeBonePositions(mesh, pose, bones, aabb);
        extendAabbInZDirection(mesh, aabb);
    }

    public static void computeCharacterViewAabbs(PmdMeshV1 mesh, PmdPose pose, ArrayList<Aabb3f> charViewAabbs) {
        for (int j = 0; j < Settings.characterViewBones.size(); j++) {
            HashSet<String> bones = Settings.characterViewBones.get(j);
            HashSet<String> morphs = Settings.characterViewMorphs.get(j);
            Aabb3f aabb = charViewAabbs.get(j);
            computeCharacterViewAabb(mesh, pose, bones, morphs, aabb);
        }
    }

    public static void includeVerticesInfluencedByBones(PmxMeshV1 mesh, Collection<String> bones, Aabb3f aabb) {
        PmxModel model = mesh.getModel();
        if (bones != null) {
            for (int i = 0; i < model.getVertexCount(); i++) {
                if (!model.isVertexUsed(i))
                    continue;
                if (!influencedByBones(model, i, bones))
                    continue;
                float x = mesh.positions.get(3 * i + 0);
                float y = mesh.positions.get(3 * i + 1);
                float z = mesh.positions.get(3 * i + 2);
                if (FloatUtil.isFinite(x) && FloatUtil.isFinite(y) && FloatUtil.isFinite(z)) {
                    aabb.expandBy(x, y, z);
                }
            }
        }
    }

    public static void includeVerticesInfluencedByMorphs(PmxMeshV1 mesh, Collection<String> morphs, Aabb3f aabb) {
        PmxModel model = mesh.getModel();
        if (morphs != null) {
            for (int i = 0; i < model.getMorphCount(); i++) {
                PmxMorph morph = model.getMorph(i);
                if (morphs.contains(morph.japaneseName)) {
                    if (morph instanceof VertexMorph) {
                        VertexMorph vertexMorph = (VertexMorph) morph;
                        for (VertexMorph.Offset offset : vertexMorph.offsets) {
                            if (!model.isVertexUsed(offset.vertexIndex))
                                continue;
                            if (Math.abs(offset.displacement.x) < Settings.MORPH_DISPLACEMENT_THRESHOLD &&
                                    Math.abs(offset.displacement.y) < Settings.MORPH_DISPLACEMENT_THRESHOLD &&
                                    Math.abs(offset.displacement.z) < Settings.MORPH_DISPLACEMENT_THRESHOLD)
                                continue;
                            float x = mesh.positions.get(3 * offset.vertexIndex + 0);
                            float y = mesh.positions.get(3 * offset.vertexIndex + 1);
                            float z = mesh.positions.get(3 * offset.vertexIndex + 2);
                            if (FloatUtil.isFinite(x) && FloatUtil.isFinite(y) && FloatUtil.isFinite(z)) {
                                aabb.expandBy(x, y, z);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void includeBonePositions(PmxMeshV1 mesh, PmxPose pose, Collection<String> bones, Aabb3f aabb) {
        PmxModel model = mesh.getModel();
        Point3f bonePos = new Point3f();
        if (pose != null) {
            for (String boneName : bones) {
                if (!model.hasBone(boneName))
                    continue;
                int boneIndex = model.getBone(boneName).boneIndex;
                pose.getBoneWorldPosition(boneIndex, bonePos);
                float x = bonePos.x;
                float y = bonePos.y;
                float z = bonePos.z;
                if (FloatUtil.isFinite(x) && FloatUtil.isFinite(y) && FloatUtil.isFinite(z)) {
                    aabb.expandBy(x, y, z);
                }
            }
        }
    }

    public static void extendAabbInZDirection(PmxMeshV1 mesh, Aabb3f aabb) {
        PmxModel model = mesh.getModel();
        for (int i = 0; i < mesh.positions.capacity() / 3; i++) {
            if (!model.isVertexUsed(i))
                continue;
            float x = mesh.positions.get(3 * i + 0);
            float y = mesh.positions.get(3 * i + 1);
            float z = mesh.positions.get(3 * i + 2);
            if (!(FloatUtil.isFinite(x) && FloatUtil.isFinite(y) && FloatUtil.isFinite(z)))
                continue;
            if (x >= aabb.pMin.x && x <= aabb.pMax.x && y >= aabb.pMin.y && y <= aabb.pMax.y) {
                aabb.expandBy(x, y, z);
            }
        }
    }

    public static void computeCharacterViewAabb(PmxMeshV1 mesh, PmxPose pose,
                                                Collection<String> bones,
                                                Collection<String> morphs,
                                                Aabb3f aabb) {
        aabb.reset();
        includeVerticesInfluencedByBones(mesh, bones, aabb);
        includeVerticesInfluencedByMorphs(mesh, morphs, aabb);
        includeBonePositions(mesh, pose, bones, aabb);
        extendAabbInZDirection(mesh, aabb);
    }

    public static void computeCharacterViewAabbs(PmxMeshV1 mesh, PmxPose pose, ArrayList<Aabb3f> charViewAabbs) {
        for (int j = 0; j < Settings.characterViewBones.size(); j++) {
            HashSet<String> bones = Settings.characterViewBones.get(j);
            HashSet<String> morphs = Settings.characterViewMorphs.get(j);
            Aabb3f aabb = charViewAabbs.get(j);
            computeCharacterViewAabb(mesh, pose, bones, morphs, aabb);
        }
    }

    public static void makeCameraLookAtCenterFromPositiveX(Aabb3f aabb, PerspectiveCamera camera) {
        float middleX = (aabb.pMax.x + aabb.pMin.x) / 2;
        float middleY = (aabb.pMax.y + aabb.pMin.y) / 2;
        float middleZ = (aabb.pMax.z + aabb.pMin.z) / 2;
        float xCoord = aabb.pMax.x;
        float extent = Math.max(aabb.pMax.y - aabb.pMin.y, aabb.pMax.z - aabb.pMin.z) / 2 * Settings.DEFAULT_CAMERA_SCALE_FACTOR;
        float distance = (float) (extent / Math.tan(camera.fovy / 2 * Math.PI / 180));

        camera.eye.set(xCoord + distance, middleY, -middleZ);
        camera.target.set(xCoord, middleY, -middleZ);
        camera.up.set(0, 1, 0);
        camera.updateFrame();
    }

    public static void makeCameraLookAtCenterFromNegativeX(Aabb3f aabb, PerspectiveCamera camera) {
        float middleX = (aabb.pMax.x + aabb.pMin.x) / 2;
        float middleY = (aabb.pMax.y + aabb.pMin.y) / 2;
        float middleZ = (aabb.pMax.z + aabb.pMin.z) / 2;
        float xCoord = aabb.pMin.x;
        float extent = Math.max(aabb.pMax.y - aabb.pMin.y, aabb.pMax.z - aabb.pMin.z) / 2 * Settings.DEFAULT_CAMERA_SCALE_FACTOR;
        float distance = (float) (extent / Math.tan(camera.fovy / 2 * Math.PI / 180));

        camera.eye.set(xCoord - distance, middleY, -middleZ);
        camera.target.set(xCoord, middleY, -middleZ);
        camera.up.set(0, 1, 0);
        camera.updateFrame();
    }

    public static void makeCameraLookAtCenterFromPositiveZ(Aabb3f aabb, PerspectiveCamera camera) {
        float middleX = (aabb.pMax.x + aabb.pMin.x) / 2;
        float middleY = (aabb.pMax.y + aabb.pMin.y) / 2;
        float zCoord = -aabb.pMin.z;
        float extent = Math.max(aabb.pMax.y - aabb.pMin.y, aabb.pMax.x - aabb.pMin.x) / 2 * Settings.DEFAULT_CAMERA_SCALE_FACTOR;
        float distance = (float) (extent / Math.tan(camera.fovy / 2 * Math.PI / 180));

        camera.eye.set(middleX, middleY, (float) (zCoord + distance));
        camera.target.set(middleX, middleY, zCoord);
        camera.up.set(0, 1, 0);
        camera.updateFrame();
    }

    public static boolean debug = false;

    public static void setCameraToViewExtraBone(String boneName, PmdMeshV1 mesh, PerspectiveCamera camera) {
        if (boneName.equals("extra_noseTip")) {
            Collection<String> bones = Arrays.asList("頭", "首");
            Aabb3f aabb = new Aabb3f();
            includeVerticesInfluencedByBones(mesh, bones, aabb);
            makeCameraLookAtCenterFromPositiveX(aabb, camera);
        } else if (boneName.equals("extra_noseRoot")) {
            Collection<String> bones = Arrays.asList("頭", "首");
            Aabb3f aabb = new Aabb3f();
            includeVerticesInfluencedByBones(mesh, bones, aabb);
            makeCameraLookAtCenterFromPositiveZ(aabb, camera);
        } else if (boneName.equals("extra_tipToeRight")) {
            Collection<String> bones = Arrays.asList("右足首", "右つま先");
            Aabb3f aabb = new Aabb3f();
            includeVerticesInfluencedByBones(mesh, bones, aabb);
            makeCameraLookAtCenterFromNegativeX(aabb, camera);
        } else if (boneName.equals("extra_tipToeLeft")) {
            Collection<String> bones = Arrays.asList("左足首", "左つま先");
            Aabb3f aabb = new Aabb3f();
            includeVerticesInfluencedByBones(mesh, bones, aabb);
            makeCameraLookAtCenterFromPositiveX(aabb, camera);
        }
    }

    public static void setCameraToViewExtraBone(String boneName, PmxMeshV1 mesh, PerspectiveCamera camera) {
        if (boneName.equals("extra_noseTip")) {
            Collection<String> bones = Arrays.asList("頭", "首");
            Aabb3f aabb = new Aabb3f();
            includeVerticesInfluencedByBones(mesh, bones, aabb);
            makeCameraLookAtCenterFromPositiveX(aabb, camera);
        } else if (boneName.equals("extra_noseRoot")) {
            Collection<String> bones = Arrays.asList("頭", "首");
            Aabb3f aabb = new Aabb3f();
            includeVerticesInfluencedByBones(mesh, bones, aabb);
            makeCameraLookAtCenterFromPositiveZ(aabb, camera);
        } else if (boneName.equals("extra_tipToeRight")) {
            Collection<String> bones = Arrays.asList("右足首", "右つま先");
            Aabb3f aabb = new Aabb3f();
            includeVerticesInfluencedByBones(mesh, bones, aabb);
            makeCameraLookAtCenterFromNegativeX(aabb, camera);
        } else if (boneName.equals("extra_tipToeLeft")) {
            Collection<String> bones = Arrays.asList("左足首", "左つま先");
            Aabb3f aabb = new Aabb3f();
            includeVerticesInfluencedByBones(mesh, bones, aabb);
            makeCameraLookAtCenterFromPositiveX(aabb, camera);
        }
    }

    public static ArrayList<Aabb3f> createCharacterViewAabbs() {
        ArrayList<Aabb3f> result = new ArrayList<Aabb3f>();
        for (int i = 0; i < Settings.characterViewBones.size(); i++) {
            result.add(new Aabb3f());
        }
        return result;
    }

    public static void drawAllVertices(GL2 gl, PmdMeshV1 mesh) {
        gl.glBegin(GL2.GL_POINTS);
        for (int i = 0; i < mesh.positions.capacity() / 3; i++) {
            float x = mesh.positions.get(3 * i + 0);
            float y = mesh.positions.get(3 * i + 1);
            float z = mesh.positions.get(3 * i + 2);
            gl.glVertex3f(x, y, z);
        }
        gl.glEnd();
    }

    public static void drawAllVertices(GL2 gl, PmxMeshV1 mesh) {
        gl.glBegin(GL2.GL_POINTS);
        for (int i = 0; i < mesh.positions.capacity() / 3; i++) {
            float x = mesh.positions.get(3 * i + 0);
            float y = mesh.positions.get(3 * i + 1);
            float z = mesh.positions.get(3 * i + 2);
            gl.glVertex3f(x, y, z);
        }
        gl.glEnd();
    }

    public static int findClickedVertex(int ix, int iy, PerspectiveCamera camera, int screenWidth, int screenHeight,
                                        MeshV1 mesh, int materialIndex) {
        float threshold = 5.0f;
        float x = (ix - screenWidth / 2.0f) / (screenWidth / 2.0f);
        float y = ((screenHeight - iy -1) - screenHeight / 2.0f) / (screenHeight / 2.0f);
        Matrix4f viewMatrix = new Matrix4f();
        camera.getViewMatrixInverse(viewMatrix);
        Matrix4f projectionMatrix = VectorMathUtil.createProjectionMatrix(
                camera.fovy, camera.aspect, camera.near, camera.far);

        if (mesh instanceof PmdMeshV1) {
            PmdMeshV1 pmdMesh = (PmdMeshV1)mesh;
            int vertexCount = pmdMesh.positions.capacity() / 3;

            boolean[] materialFlags = new boolean[vertexCount];
            if (materialIndex == 0) {
                for (int i = 0; i < vertexCount; i++) {
                    materialFlags[i] = true;
                }
            } else {
                for (int i = 0; i < vertexCount; i++) {
                    materialFlags[i] = false;
                }
                int start = 0;
                for (int i = 0; i < materialIndex-1; i++) {
                    start += pmdMesh.getModel().materials.get(i).vertexCount;
                }
                int count = pmdMesh.getModel().materials.get(materialIndex-1).vertexCount;
                for (int i = start; i < start+count; i++) {
                    int index = pmdMesh.getModel().triangleVertexIndices.get(i);
                    materialFlags[index] = true;
                }
            }

            Point4f p4 = new Point4f();
            Point3f p3 = new Point3f();
            int closestIndex = -1;
            float closestDistance = Float.POSITIVE_INFINITY;
            for (int i = 0; i < vertexCount; i++) {
                if (!materialFlags[i])
                    continue;
                p3.x = p4.x = pmdMesh.positions.get(3*i+0);
                p3.y = p4.y = pmdMesh.positions.get(3*i+1);
                p3.z = p4.z = -pmdMesh.positions.get(3*i+2);
                p4.w = 1;
                viewMatrix.transform(p4);
                projectionMatrix.transform(p4);
                float px = p4.x / p4.w;
                float py = p4.y / p4.w;

                if (Math.abs(px-x) <= threshold / (screenWidth / 2.0f) &&
                        Math.abs(py-y) <= threshold / (screenHeight / 2.0f)) {
                    float distance = p3.distance(camera.eye);
                    if (closestDistance > distance) {
                        closestDistance = distance;
                        closestIndex = i;
                    }
                }
            }
            return closestIndex;
        } else if (mesh instanceof PmxMeshV1) {
            PmxMeshV1 pmxMesh = (PmxMeshV1)mesh;
            int vertexCount = pmxMesh.positions.capacity() / 3;

            boolean[] materialFlags = new boolean[vertexCount];
            if (materialIndex == 0) {
                for (int i = 0; i < vertexCount; i++) {
                    materialFlags[i] = true;
                }
            } else {
                for (int i = 0; i < vertexCount; i++) {
                    materialFlags[i] = false;
                }
                int start = 0;
                for (int i = 0; i < materialIndex-1; i++) {
                    start += pmxMesh.getModel().getMaterial(i).vertexCount;
                }
                int count = pmxMesh.getModel().getMaterial(materialIndex-1).vertexCount;
                for (int i = start; i < start+count; i++) {
                    int index = pmxMesh.getModel().getVertexIndex(i);
                    materialFlags[index] = true;
                }
            }

            Point4f p4 = new Point4f();
            Point3f p3 = new Point3f();
            int closestIndex = -1;
            float closestDistance = Float.POSITIVE_INFINITY;
            for (int i = 0; i < vertexCount; i++) {
                if (!materialFlags[i])
                    continue;
                p3.x = p4.x = pmxMesh.positions.get(3*i+0);
                p3.y = p4.y = pmxMesh.positions.get(3*i+1);
                p3.z = p4.z = -pmxMesh.positions.get(3*i+2);
                p4.w = 1;
                //System.out.println("p4 (raw) = " + p4);
                viewMatrix.transform(p4);
                //System.out.println("p4 (camera space) = " + p4);
                projectionMatrix.transform(p4);
                //System.out.println("p4 (projected) = " + p4);
                float px = p4.x / p4.w;
                float py = p4.y / p4.w;
                //System.out.println("px = " + px + ", py = " + py);

                if (Math.abs(px-x) <= threshold / (screenWidth / 2.0f) &&
                        Math.abs(py-y) <= threshold / (screenHeight / 2.0f)) {
                    float distance = p3.distance(camera.eye);
                    if (closestDistance > distance) {
                        closestDistance = distance;
                        closestIndex = i;
                    }
                }
            }
            return closestIndex;
        } else {
            return -1;
        }
    }

    public static void glVertex(GL2 gl, PmdMeshV1 mesh, int index) {
        float x = mesh.positions.get(3*index+0);
        float y = mesh.positions.get(3*index+1);
        float z = mesh.positions.get(3*index+2);
        gl.glVertex3f(x,y,z);
    }

    public static void drawTriangleEdges(GL2 gl, PmdModel model, PmdMeshV1 mesh, int index) {
        int v0 = model.triangleVertexIndices.get(3*index+0);
        int v1 = model.triangleVertexIndices.get(3*index+1);
        int v2 = model.triangleVertexIndices.get(3*index+2);

        glVertex(gl, mesh, v0);
        glVertex(gl, mesh, v1);
        glVertex(gl, mesh, v1);
        glVertex(gl, mesh, v2);
        glVertex(gl, mesh, v2);
        glVertex(gl, mesh, v0);
    }

    public static void glVertex(GL2 gl, PmxMeshV1 mesh, int index) {
        float x = mesh.positions.get(3*index+0);
        float y = mesh.positions.get(3*index+1);
        float z = mesh.positions.get(3*index+2);
        gl.glVertex3f(x,y,z);
    }

    public static void drawTriangleEdges(GL2 gl, PmxModel model, PmxMeshV1 mesh, int index) {
        int v0 = model.getVertexIndex(3 * index + 0);
        int v1 = model.getVertexIndex(3 * index + 1);
        int v2 = model.getVertexIndex(3 * index + 2);

        glVertex(gl, mesh, v0);
        glVertex(gl, mesh, v1);
        glVertex(gl, mesh, v1);
        glVertex(gl, mesh, v2);
        glVertex(gl, mesh, v2);
        glVertex(gl, mesh, v0);
    }


    public static void drawWireframe(GL2 gl, MeshV1 mesh, int materialIndex) {
        gl.glBegin(GL2.GL_LINES);
        if (mesh instanceof PmdMeshV1) {
            PmdMeshV1 pmdMesh = (PmdMeshV1)mesh;
            PmdModel model = pmdMesh.getModel();
            if (materialIndex == 0) {
                for (int i = 0; i < model.triangleVertexIndices.capacity() / 3; i++) {
                    drawTriangleEdges(gl, model, pmdMesh, i);
                }
            } else {
                int start = 0;
                for (int i = 0; i < materialIndex-1; i++) {
                    start += pmdMesh.getModel().materials.get(i).vertexCount;
                }
                int count = pmdMesh.getModel().materials.get(materialIndex-1).vertexCount;
                for (int i = start; i < start+count; i += 3) {
                    drawTriangleEdges(gl, model, pmdMesh, i/3);
                }
            }
        } else if (mesh instanceof PmxMeshV1) {
            PmxMeshV1 pmxMesh = (PmxMeshV1)mesh;
            PmxModel model = pmxMesh.getModel();
            if (materialIndex == 0) {
                for (int i = 0; i < model.getTriangleCount(); i++) {
                    drawTriangleEdges(gl, model, pmxMesh, i);
                }
            } else {
                int start = 0;
                for (int i = 0; i < materialIndex-1; i++) {
                    start += pmxMesh.getModel().getMaterial(i).vertexCount;
                }
                int count = pmxMesh.getModel().getMaterial(materialIndex-1).vertexCount;
                for (int i = start; i < start+count; i += 3) {
                    drawTriangleEdges(gl, model, pmxMesh, i/3);
                }
            }
        }
        gl.glEnd();
    }

    public static Aabb3f meshAabbAlongFrame(MeshV1 mesh, Vector3f frameX, Vector3f frameY, Vector3f frameZ, Aabb3f aabb) {
        Aabb3f output = new Aabb3f();
        Vector3f p = new Vector3f();
        if (mesh instanceof PmdMeshV1) {
            PmdMeshV1 pmdMesh = (PmdMeshV1)mesh;

            for (int i = 0; i < pmdMesh.positions.capacity()/3; i++) {
                float x = pmdMesh.positions.get(3 * i + 0);
                float y = pmdMesh.positions.get(3 * i + 1);
                float z = pmdMesh.positions.get(3 * i + 2);
                if (!(FloatUtil.isFinite(x) && FloatUtil.isFinite(y) && FloatUtil.isFinite(z)))
                    continue;
                p.set(x,y,z);
                if (!aabb.overlap(p))
                    continue;
                p.z *= -1;
                x = frameX.dot(p);
                y = frameY.dot(p);
                z = frameZ.dot(p);
                output.expandBy(x,y,z);
            }
            for (int i = 0; i < pmdMesh.positions.capacity()/3; i++) {
                float x = pmdMesh.positions.get(3 * i + 0);
                float y = pmdMesh.positions.get(3 * i + 1);
                float z = pmdMesh.positions.get(3 * i + 2);
                if (!(FloatUtil.isFinite(x) && FloatUtil.isFinite(y) && FloatUtil.isFinite(z)))
                    continue;
                p.set(x,y,-z);
                x = frameX.dot(p);
                y = frameY.dot(p);
                z = frameZ.dot(p);
                p.set(x,y,z);
                if (!output.overlap(p))
                    continue;
                if (x >= output.pMin.x && x <= output.pMax.x && y >= output.pMin.y && y <= output.pMax.y) {
                    output.expandBy(x, y, z);
                }
            }
        } else if (mesh instanceof PmxMeshV1) {
            PmxMeshV1 pmxMesh = (PmxMeshV1)mesh;

            for (int i = 0; i < pmxMesh.positions.capacity()/3; i++) {
                float x = pmxMesh.positions.get(3 * i + 0);
                float y = pmxMesh.positions.get(3 * i + 1);
                float z = pmxMesh.positions.get(3 * i + 2);
                if (!(FloatUtil.isFinite(x) && FloatUtil.isFinite(y) && FloatUtil.isFinite(z)))
                    continue;
                p.set(x,y,z);
                if (!aabb.overlap(p))
                    continue;
                p.z *= -1;
                x = frameX.dot(p);
                y = frameY.dot(p);
                z = frameZ.dot(p);
                output.expandBy(x,y,z);
            }
            for (int i = 0; i < pmxMesh.positions.capacity()/3; i++) {
                float x = pmxMesh.positions.get(3 * i + 0);
                float y = pmxMesh.positions.get(3 * i + 1);
                float z = pmxMesh.positions.get(3 * i + 2);
                if (!(FloatUtil.isFinite(x) && FloatUtil.isFinite(y) && FloatUtil.isFinite(z)))
                    continue;
                p.set(x,y,-z);
                x = frameX.dot(p);
                y = frameY.dot(p);
                z = frameZ.dot(p);
                p.set(x,y,z);
                if (!output.overlap(p))
                    continue;
                if (x >= output.pMin.x && x <= output.pMax.x && y >= output.pMin.y && y <= output.pMax.y) {
                    output.expandBy(x, y, z);
                }
            }
        }

        return output;
    }
}
