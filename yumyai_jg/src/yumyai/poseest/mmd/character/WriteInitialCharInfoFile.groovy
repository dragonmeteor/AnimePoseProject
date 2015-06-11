package yumyai.poseest.mmd.character

import org.apache.commons.io.FilenameUtils
import yondoko.struct.Aabb3f
import yumyai.mmd.pmd.PmdBone
import yumyai.mmd.pmd.PmdModel
import yumyai.mmd.pmd.PmdPose
import yumyai.mmd.pmx.PmxBone
import yumyai.mmd.pmx.PmxMaterial
import yumyai.mmd.pmx.PmxModel
import yumyai.mmd.pmx.PmxVertex
import yumyai.poseest.mmd.MmdCharInfo
import yumyai.poseest.mmd.PoseEstUtilGroovy
import yumyai.poseest.mmd.Settings

import javax.vecmath.Point3f
import javax.vecmath.Tuple3f

class WriteInitialCharInfoFile {
    public static void main(String[] args) {
        if (args.length < 2) {
            println("Usage: java yumyai.poseest.app.DetermineExtraJoints <list-file> <output-file>")
            System.exit(-1)
        }

        final ArrayList<String> modelFiles = PoseEstUtilGroovy.readFileUsageListAsArray(args[0])
        final ArrayList<MmdCharInfo> charInfo = MmdCharInfo.createExtraBoneInfoArray(modelFiles);
        //final HashMap<String, HashMap<String, Integer>> extraJointInfo = PoseEstUtilGroovy.readExtraJointInfo(args[1])
        
        int count = 0
        for (int k = 0; k < modelFiles.size(); k++) {
            String fileName = modelFiles[k]
            System.out.println("${k}: ${fileName}")

            ArrayList<String> boneNames = PrintPmdPmxBones.getBoneList(fileName)
            charInfo[k].boneNames.addAll(boneNames)

            String extension = FilenameUtils.getExtension(fileName).toLowerCase()
            def model = null
            if (extension.equals("pmd")) {
                model = PmdModel.load(fileName);
            } else if (extension.equals("pmx")) {
                model = PmxModel.load(fileName)
            }

            Aabb3f rightAabb = findAabbOfVerticesInfluencedByBone(model, "右足首")
            Point3f rightPoint = new Point3f();
            rightPoint.x = (rightAabb.pMin.x + rightAabb.pMax.x) / 2
            rightPoint.y = rightAabb.pMin.y
            rightPoint.z = rightAabb.pMin.z
            charInfo[k].extraBoneVertexIndex[Settings.EXTRA_TIPTOE_RIGHT_NAME] =
                    findVertexIndexClosestToPosition(model, rightPoint)

            Aabb3f leftAabb = findAabbOfVerticesInfluencedByBone(model, "左足首")
            Point3f leftPoint = new Point3f()
            leftPoint.x = (leftAabb.pMin.x + leftAabb.pMax.x) / 2
            leftPoint.y = leftAabb.pMin.y
            leftPoint.z = leftAabb.pMin.z
            charInfo[k].extraBoneVertexIndex[Settings.EXTRA_TIPTOE_LEFT_NAME] =
                    findVertexIndexClosestToPosition(model, leftPoint)

            Point3f headPosition = getBonePosition(model,"頭")
            Point3f middleEyePosition = findEyeMiddlePointPosition(model)
            Point3f p = new Point3f();
            Aabb3f relevantPointAabb = new Aabb3f();
            float lowestZ = Float.POSITIVE_INFINITY
            int lowestIndex = -1
            for (int i = 0; i < getVertexCount(model); i++) {
                if (!isVertexUsed(model, i))
                    continue
                if (!vertexInfluencedByBone(model, i, "頭", false))
                    continue
                getVertexPosition(model, i, p)
                if (Math.abs(p.x) > 1e-3 || p.z > 0 || p.y > middleEyePosition.y || p.y < headPosition.y)
                    continue
                relevantPointAabb.expandBy(p)
                if (p.z < lowestZ) {
                    lowestZ = p.z
                    lowestIndex = i
                }
            }
            charInfo[k].extraBoneVertexIndex[Settings.EXTRA_NOSE_TIP_NAME] = (int)lowestIndex

            if (lowestIndex < 0) {
                println("Cannot find the nose tip vertex according to the simple algorithm")
                continue
            }

            int faceMaterialIndex = findMaterialIndex(model, lowestIndex)
            boolean[] materialFlag = makeVertexMaterialFlag(model, faceMaterialIndex)
            Point3f refPoint = new Point3f(0, relevantPointAabb.pMax.y, relevantPointAabb.pMin.z);
            float closestDistance = Float.POSITIVE_INFINITY
            int closestIndex = -1
            for (int i = 0; i < getVertexCount(model); i++) {
                if (!materialFlag[i])
                    continue;
                getVertexPosition(model, i, p)
                if (Math.abs(p.x) > 1e-3 || p.z > 0 || p.y > middleEyePosition.y || p.y < headPosition.y)
                    continue
                float distance = Math.abs(p.y - refPoint.y)
                if (closestDistance > distance) {
                    closestDistance = distance
                    closestIndex = i
                }
            }
            charInfo[k].extraBoneVertexIndex[Settings.EXTRA_NOSE_ROOT_NAME] = closestIndex
        }

        MmdCharInfo.save(args[1], charInfo)
    }

    static boolean[] makeVertexMaterialFlag(def model, int materialIndex) {
        if (model instanceof PmdModel) {
            PmdModel pmdModel = (PmdModel)model
            boolean[] result = new boolean[pmdModel.vertexCount]
            for (int i = 0; i < pmdModel.vertexCount; i++) {
                result[i] = false
            }
            int start = 0
            for (int i = 0; i < materialIndex; i++) {
                start += pmdModel.materials.get(i).vertexCount
            }
            int vertexCount = pmdModel.materials.get(materialIndex).vertexCount
            for (int i = start; i < start+vertexCount; i++) {
                int index = pmdModel.triangleVertexIndices.get(i)
                result[index] = true
            }
            return result
        } else if (model instanceof PmxModel) {
            PmxModel pmxModel = (PmxModel)model
            boolean[] result = new boolean[pmxModel.vertexCount]
            for (int i = 0; i < pmxModel.vertexCount; i++) {
                result[i] = false
            }
            int start = 0
            for (int i = 0; i < materialIndex; i++) {
                start += pmxModel.materials.get(i).vertexCount
            }
            int vertexCount = pmxModel.materials.get(materialIndex).vertexCount
            for (int i = start; i < start+vertexCount; i++) {
                int index = pmxModel.getVertexIndex(i)
                result[index] = true
            }
            return result
        }
    }

    static int findMaterialIndex(def model, int vertexIndex) {
        if (model instanceof PmdModel) {
            PmdModel pmdModel = (PmdModel)model
            int start = 0
            for (int i = 0; i < pmdModel.materials.size(); i++) {
                int vertexCount = pmdModel.materials.get(i).vertexCount
                for (int j = start; j < start + vertexCount; j++) {
                    int index = pmdModel.triangleVertexIndices.get(j)
                    if (index == vertexIndex)
                        return i
                }
                start += vertexCount
            }
        } else if (model instanceof PmxModel) {
            PmxModel pmxModel = (PmxModel)model
            int start = 0
            for (int i = 0; i < pmxModel.getMaterialCount(); i++) {
                PmxMaterial material = pmxModel.getMaterial(i)
                int vertexCount = material.vertexCount
                for (int j = start; j < start+vertexCount; j++) {
                    int index = pmxModel.getVertexIndex(j)
                    if (index == vertexIndex)
                        return i
                }
                start += vertexCount
            }
        }
        return -1
    }

    static boolean isVertexUsed(def model, int index) {
        if (model instanceof PmdModel) {
            return model.vertexUsed[index]
        } else if (model instanceof PmxModel) {
            return model.isVertexUsed(index)
        } else {
            return false
        }
    }

    static int getVertexCount(def model) {
        if (model instanceof PmdModel) {
            return model.positions.capacity() / 3
        } else if (model instanceof PmxModel) {
            return model.getVertexCount()
        }
    }

    static void getVertexPosition(def model, int index, Tuple3f p) {
        if (model instanceof PmdModel) {
            PmdModel pmdModel = (PmdModel)model
            p.x = pmdModel.positions.get(3*index+0)
            p.y = pmdModel.positions.get(3*index+1)
            p.z = pmdModel.positions.get(3*index+2)
        } else if (model instanceof PmxModel) {
            PmxModel pmxModel = (PmxModel)model
            p.set(pmxModel.getVertex(index).position)
        }
    }

    static Point3f getBonePosition(def model, String boneName) {
        Point3f result = new Point3f();
        if (model instanceof PmdModel) {
            PmdModel pmdModel = (PmdModel)model
            PmdPose pmdPose = new PmdPose(pmdModel)
            pmdPose.getBoneWorldPosition(pmdModel.getBoneIndex(boneName), result)
        } else if (model instanceof PmxModel) {
            PmxModel pmxModel = (PmxModel)model
            PmxBone headBone = pmxModel.getBone(boneName)
            result.set(headBone.position)
        }
        return result;
    }

    static Point3f findEyeMiddlePointPosition(def model) {
        Point3f result = new Point3f();
        Point3f leftEye = getBonePosition(model, "左目")
        Point3f rightEye = getBonePosition(model, "右目")
        result.scale(0.5f, leftEye)
        result.scaleAdd(0.5f, rightEye, result)
        return result;
    }

    static Aabb3f findAabbOfVerticesInfluencedByBone(PmdModel model, String boneName, boolean recursive = true) {
        Aabb3f aabb = new Aabb3f();
        for (int i = 0; i < model.positions.capacity()/3; i++) {
            if (!model.vertexUsed[i])
                continue
            if (!vertexInfluencedByBone(model, i, boneName, recursive))
                continue
            float x = model.positions.get(3*i+0)
            float y = model.positions.get(3*i+1)
            float z = model.positions.get(3*i+2)
            aabb.expandBy(x,(float)Math.max(y,0),z)
        }
        return aabb;
    }

    static boolean vertexInfluencedByBone(PmdModel model, int i, String boneName, boolean recursive = true) {
        boolean result = true
        int bone0 = model.vertexBoneIndices.get(2*i+0)
        int bone1 = model.vertexBoneIndices.get(2*i+1)
        float weight0 = model.vertexBoneBlendWeights.get(2*i+0)
        float weight1 = model.vertexBoneBlendWeights.get(2*i+1)
        result = (weight0 > 0) && isRelevantBone(model, bone0, boneName, recursive) ||
                (weight1 > 0) && isRelevantBone(model, bone1, boneName, recursive)
        return result
    }

    static boolean isRelevantBone(PmdModel model, int boneIndex, String boneName, boolean recursive = true) {
        while (boneIndex >= 0) {
            PmdBone bone = model.bones.get(boneIndex)
            if (bone.japaneseName.equals(boneName))
                return true
            else
                boneIndex = bone.parentIndex
            if (!recursive)
                break
        }
        return false
    }

    static Aabb3f findAabbOfVerticesInfluencedByBone(PmxModel model, String boneName, boolean recursive = true) {
        Aabb3f aabb = new Aabb3f();
        for (int i = 0; i < model.getVertexCount(); i++) {
            if (!model.isVertexUsed(i))
                continue
            PmxVertex vertex = model.getVertex(i)
            if (!vertexInfluencedByBone(model, vertex, boneName, recursive))
                continue
            aabb.expandBy(vertex.position.x,(float)Math.max(vertex.position.y,0),vertex.position.z)
        }
        return aabb;
    }

    static boolean vertexInfluencedByBone(PmxModel model, int index, String boneName, boolean recursive = true) {
        return vertexInfluencedByBone(model, model.getVertex(index), boneName, true)
    }

    static boolean vertexInfluencedByBone(PmxModel model, PmxVertex vertex, String boneName, boolean recursive = true) {
        int boneCount = 1;
        switch(vertex.boneDataType) {
            case PmxVertex.BDEF1: boneCount = 1; break;
            case PmxVertex.BDEF2: boneCount = 2; break;
            case PmxVertex.BDEF4: boneCount = 4; break;
            case PmxVertex.SDEF: boneCount = 2; break;
        }
        for (int i = 0; i < boneCount; i++) {
            if (vertex.boneWeights[i] <= 0)
                continue;
            int boneIndex = vertex.boneIndices[i]
            if (!isRelevantBone(model, boneIndex, boneName, recursive))
                continue
            else
                return true;
        }
        return false;
    }

    static boolean isRelevantBone(PmxModel model, int boneIndex, String boneName, boolean recursive = true) {
        while (boneIndex >= 0) {
            PmxBone bone = model.getBone(boneIndex)
            if (bone.japaneseName.equals(boneName))
                return true
            if ((bone.copyRotation() || bone.copyTranslation()) && bone.copyParentBoneIndex >= 0) {
                PmxBone copyParentBone = model.getBone(bone.copyParentBoneIndex)
                if (copyParentBone.japaneseName.equals(boneName))
                    return true
            }
            boneIndex = bone.parentIndex
            if (!recursive)
                break
        }
        return false
    }

    static int findVertexIndexClosestToPosition(PmdModel model, Point3f pos) {
        Point3f p = new Point3f()
        float minDistance = Float.POSITIVE_INFINITY
        int minIndex = 0
        for (int i = 0; i < model.positions.capacity() / 3; i++) {
            if (!model.vertexUsed[i])
                continue;
            p.x = model.positions.get(3*i+0)
            p.y = model.positions.get(3*i+1)
            p.z = model.positions.get(3*i+2)
            float distance = pos.distance(p)
            if (minDistance > distance) {
                minDistance = distance
                minIndex = i
            }
        }
        return minIndex
    }

    static int findVertexIndexClosestToPosition(PmxModel model, Point3f pos) {
        Point3f p = new Point3f()
        float minDistance = Float.POSITIVE_INFINITY
        int minIndex = 0
        for (int i = 0; i < model.vertices.size(); i++) {
            if (!model.isVertexUsed(i))
                continue;
            PmxVertex vertex = model.vertices.get(i);
            float distance = vertex.position.distance(pos)
            if (minDistance > distance) {
                minDistance = distance
                minIndex = i
            }
        }
        return minIndex
    }
}
