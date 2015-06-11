package yumyai.mmd.pmd;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

public class PmdModel {
    public String japaneseName;
    public String englishName;
    public String japaneseComment;
    public String englishComment;
    public float version;
    public String directory;
    public FloatBuffer positions;
    public FloatBuffer normals;
    public FloatBuffer texCoords;
    public ShortBuffer vertexBoneIndices;
    public FloatBuffer vertexBoneBlendWeights;
    public IntBuffer triangleVertexIndices;
    public ByteBuffer edgeFlags;
    public List<PmdMaterial> materials = new ArrayList<PmdMaterial>();
    public List<PmdBone> bones = new ArrayList<PmdBone>();
    public List<PmdIkChain> ikChains = new ArrayList<PmdIkChain>();
    public List<PmdMorph> morphs = new ArrayList<PmdMorph>();
    public List<PmdRigidBody> rigidBodies = new ArrayList<PmdRigidBody>();
    public List<PmdJoint> joints = new ArrayList<PmdJoint>();
    public Map<String, Integer> boneIndexByName = new HashMap<String, Integer>();
    public Map<String, Integer> morphIndexByName = new HashMap<String, Integer>();
    public boolean hasEnglishNames;
    public boolean hasToon;
    public boolean hasPhysics;
    public List<Integer> morphIndices = new ArrayList<Integer>();
    public List<PmdBoneGroup> boneGroups = new ArrayList<PmdBoneGroup>();
    public List<String> toonFileNames = new ArrayList<String>();
    public boolean[] vertexUsed;

    public PmdModel() {
        // NOP
    }

    public int getBoneIndex(String name) {
        Integer index = boneIndexByName.get(name);
        if (index == null) {
            return -1;
        } else {
            return index;
        }
    }

    public int getBoneIndexByEnglishNameBruteForce(String name) {
        for (int i = 0; i < bones.size(); i++) {
            PmdBone bone = bones.get(i);
            if (bone.englishName.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public int getBoneIndexByJapaneseNameBruteForce(String name) {
        for (int i = 0; i < bones.size(); i++) {
            PmdBone bone = bones.get(i);
            if (bone.japaneseName.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public int getMorphIndex(String name) {
        Integer index = morphIndexByName.get(name);
        if (index == null) {
            return -1;
        } else {
            return index;
        }
    }

    public static PmdModel load(String fileName, String encoding) throws IOException {
        PmdModel pmd = new PmdModel();

        File theFile = new File(fileName);
        File absoluteFile = theFile.getAbsoluteFile();
        pmd.directory = absoluteFile.getParent();

        long fileSize = theFile.length();
        if (fileSize > Integer.MAX_VALUE) {
            throw new RuntimeException("file to large to load");
        }
        ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
        FileInputStream fileInputStream = new FileInputStream(theFile);
        FileChannel channel = fileInputStream.getChannel();
        channel.read(buffer);
        channel.close();
        fileInputStream.close();
        buffer.rewind();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.array());

        SwappedDataInputStream fin = new SwappedDataInputStream(inputStream);
        pmd.read(fin);
        fin.close();

        return pmd;
    }

    public static PmdModel load(String fileName) throws IOException {
        return load(fileName, "Shift-JIS");
    }

    private void read(SwappedDataInputStream fin) throws IOException {
        readHeader(fin);
        readVertices(fin);
        readTriangleVertexIndices(fin);
        readMaterials(fin);
        readBones(fin);
        readIkChains(fin);
        readMorphs(fin);
        readMorphIndices(fin);
        readBoneGroups(fin);
        readEnglishNames(fin);
        readToonFileNames(fin);
        readRigidBodies(fin);
        readJoints(fin);
    }

    public void save(String fileName) throws IOException {
        File file = new File(fileName);
        File directory = file.getParentFile();

        FileOutputStream outStream = new FileOutputStream(fileName);
        DataOutputStream fout = new DataOutputStream(outStream);

        writeHeader(fout);
        writeVertices(fout);
        writeTriangleVertexIndices(fout);
        writeMaterials(fout, directory);
        writeBones(fout);
        writeIkChains(fout);
        writeMorphs(fout);
        writeMorphIndices(fout);
        writeBoneGroups(fout);
        writeEnglishNames(fout);
        writeToonFileNames(fout, directory);
        writeRigidBodies(fout);
        writeJoints(fout);

        fout.close();
        outStream.close();
    }

    private void readHeader(SwappedDataInputStream fin) throws IOException {
        String magic = BinaryIo.readShiftJisString(fin, 3);
        //System.out.println("magic = " + magic);
        if (!magic.equals("Pmd")) {
            throw new RuntimeException("magic is wrong!");
        }
        version = fin.readFloat();
        japaneseName = BinaryIo.readShiftJisString(fin, 20);
        japaneseComment = BinaryIo.readShiftJisString(fin, 256);
        englishName = "";
        englishComment = "";
    }

    private void writeHeader(DataOutputStream fout) throws IOException {
        fout.write('P');
        fout.write('m');
        fout.write('d');
        BinaryIo.writeLittleEndianFloat(fout, version);
        BinaryIo.writeByteString(fout, japaneseName.getBytes("Shift-JIS"), 20);
        BinaryIo.writeByteString(fout, japaneseComment.getBytes("Shift-JIS"), 256);
    }

    public int getVertexCount() {
        return positions.capacity() / 3;
    }

    private void readVertices(SwappedDataInputStream fin) throws IOException {
        int vertexCount = fin.readInt();

        positions = FloatBuffer.allocate(vertexCount * 3);
        normals = FloatBuffer.allocate(vertexCount * 3);
        texCoords = FloatBuffer.allocate(vertexCount * 2);
        vertexBoneIndices = ShortBuffer.allocate(vertexCount * 2);
        vertexBoneBlendWeights = FloatBuffer.allocate(vertexCount * 2);
        edgeFlags = ByteBuffer.allocate(vertexCount);

        for (int i = 0; i < vertexCount; i++) {
            positions.put(3 * i + 0, fin.readFloat());
            positions.put(3 * i + 1, fin.readFloat());
            positions.put(3 * i + 2, fin.readFloat());

            normals.put(3 * i + 0, fin.readFloat());
            normals.put(3 * i + 1, fin.readFloat());
            normals.put(3 * i + 2, fin.readFloat());

            texCoords.put(2 * i + 0, fin.readFloat());
            texCoords.put(2 * i + 1, 1-fin.readFloat());

            vertexBoneIndices.put(2 * i + 0, fin.readShort());
            vertexBoneIndices.put(2 * i + 1, fin.readShort());

            byte weight = fin.readByte();

            vertexBoneBlendWeights.put(2 * i + 0, weight / 100.0f);
            vertexBoneBlendWeights.put(2 * i + 1, 1.0f - weight / 100.0f);

            edgeFlags.put(fin.readByte());
        }
        positions.rewind();
        normals.rewind();
        texCoords.rewind();
        vertexBoneIndices.rewind();
        vertexBoneBlendWeights.rewind();
    }

    private void writeVertices(DataOutputStream fout) throws IOException {
        int vertexCount = positions.capacity() / 3;
        BinaryIo.writeLittleEndianInt(fout, vertexCount);

        for (int i = 0; i < vertexCount; i++) {
            BinaryIo.writeLittleEndianFloat(fout, positions.get(3 * i + 0));
            BinaryIo.writeLittleEndianFloat(fout, positions.get(3 * i + 1));
            BinaryIo.writeLittleEndianFloat(fout, positions.get(3 * i + 2));

            BinaryIo.writeLittleEndianFloat(fout, normals.get(3 * i + 0));
            BinaryIo.writeLittleEndianFloat(fout, normals.get(3 * i + 1));
            BinaryIo.writeLittleEndianFloat(fout, normals.get(3 * i + 2));

            BinaryIo.writeLittleEndianFloat(fout, texCoords.get(2 * i + 0));
            BinaryIo.writeLittleEndianFloat(fout, 1 - texCoords.get(2 * i + 1));

            BinaryIo.writeLittleEndianShort(fout, vertexBoneIndices.get(2 * i + 0));
            BinaryIo.writeLittleEndianShort(fout, vertexBoneIndices.get(2 * i + 1));

            byte weight = (byte) (vertexBoneBlendWeights.get(2 * i + 0) * 100.0f);
            fout.write(weight);
            fout.write(edgeFlags.get(i));
        }
    }

    private void readTriangleVertexIndices(SwappedDataInputStream fin) throws IOException {
        int vertexIndexCount = fin.readInt();
        //System.out.println("vertexIndexCount = " + vertexIndexCount);
        vertexUsed = new boolean[getVertexCount()];
        for (int i = 0; i < getVertexCount(); i++) {
            vertexUsed[i] = false;
        }
        triangleVertexIndices = IntBuffer.allocate(vertexIndexCount);
        for (int i = 0; i < vertexIndexCount; i++) {
            int index = fin.readUnsignedShort();
            triangleVertexIndices.put(index);
            vertexUsed[index] = true;
        }
        triangleVertexIndices.rewind();
    }

    private void writeTriangleVertexIndices(DataOutputStream fout) throws IOException {
        int vertexIndexCount = triangleVertexIndices.capacity();
        BinaryIo.writeLittleEndianInt(fout, vertexIndexCount);
        for (int i = 0; i < vertexIndexCount; i++) {
            BinaryIo.writeLittleEndianShort(fout, (short) triangleVertexIndices.get(i));
        }
    }

    private void readMaterials(SwappedDataInputStream fin) throws IOException {
        int materialCount = fin.readInt();
        //System.out.println("materialCount = " + materialCount);
        int vertexStart = 0;
        for (int i = 0; i < materialCount; i++) {
            PmdMaterial material = new PmdMaterial();
            material.read(fin, directory);
            material.vertexStart = vertexStart;
            vertexStart += materialCount;
            materials.add(material);
        }
    }

    private void writeMaterials(DataOutputStream fout, File outDir) throws IOException {
        int materialCount = materials.size();
        BinaryIo.writeLittleEndianInt(fout, materialCount);
        for (int i = 0; i < materialCount; i++) {
            PmdMaterial material = materials.get(i);
            material.write(fout, outDir);
        }
    }

    private void readBones(SwappedDataInputStream fin) throws IOException {
        int boneCount = fin.readUnsignedShort();
        for (int i = 0; i < boneCount; i++) {
            PmdBone bone = new PmdBone();
            bone.read(fin);
            bones.add(bone);
            boneIndexByName.put(bone.japaneseName, i);
        }
        for (int i = 0; i < boneCount; i++) {
            if (bones.get(i).parentIndex >= 0) {
                bones.get(i).parent = bones.get(bones.get(i).parentIndex);
            } else {
                bones.get(i).parent = null;
            }
        }
    }

    private void writeBones(DataOutputStream fout) throws IOException {
        int boneCount = bones.size();
        BinaryIo.writeLittleEndianShort(fout, (short) boneCount);
        for (int i = 0; i < boneCount; i++) {
            PmdBone bone = bones.get(i);
            bone.write(fout);
        }
    }

    private void readIkChains(SwappedDataInputStream fin) throws IOException {
        int ikChainCount = fin.readUnsignedShort();
        for (int i = 0; i < ikChainCount; i++) {
            PmdIkChain ikChain = new PmdIkChain();
            ikChain.read(fin);
            ikChains.add(ikChain);

            /*
             if (bones.get(ikChain.boneIndex).japaneseName.equals("右足首") ||
             bones.get(ikChain.boneIndex).japaneseName.equals("左足首"))
             {
             ikChain.isLeg = true;
             }
             else
             {
             ikChain.isLeg = false;
             }

             ikChain.isLeftLeg = bones.get(ikChain.boneIndex).japaneseName.equals("左足首");
             ikChain.isRightLeg = bones.get(ikChain.boneIndex).japaneseName.equals("右足首");
             */

            if (bones.get(ikChain.boneIndex).japaneseName.equals("右足ＩＫ")
                    || bones.get(ikChain.boneIndex).japaneseName.equals("左足ＩＫ")) {
                if (ikChain.chainBoneIndices.length == 2) {
                    ikChain.isLeg = true;
                } else {
                    ikChain.isLeg = false;
                }
            } else {
                ikChain.isLeg = false;
            }

            if (ikChain.isLeg) {
                ikChain.isLeftLeg = bones.get(ikChain.boneIndex).japaneseName.equals("左足ＩＫ");
                ikChain.isRightLeg = bones.get(ikChain.boneIndex).japaneseName.equals("右足ＩＫ");
            }
        }
    }

    private void writeIkChains(DataOutputStream fout) throws IOException {
        int ikChainCount = ikChains.size();
        BinaryIo.writeLittleEndianShort(fout, (short) ikChainCount);
        for (int i = 0; i < ikChainCount; i++) {
            PmdIkChain ikChain = ikChains.get(i);
            ikChain.write(fout);
        }
    }

    private void readMorphs(SwappedDataInputStream fin) throws IOException {
        int morphCount = fin.readUnsignedShort();
        for (int i = 0; i < morphCount; i++) {
            PmdMorph morph = new PmdMorph();
            morph.read(fin);
            morphs.add(morph);
            morphIndexByName.put(morph.japaneseName, i);
        }
    }

    private void writeMorphs(DataOutputStream fout) throws IOException {
        int morphCount = morphs.size();
        BinaryIo.writeLittleEndianShort(fout, (short) morphCount);
        for (int i = 0; i < morphCount; i++) {
            PmdMorph morph = morphs.get(i);
            morph.write(fout);
        }
    }

    private void readMorphIndices(SwappedDataInputStream fin) throws IOException {
        int morphIndexCount = fin.readByte();
        for (int i = 0; i < morphIndexCount; i++) {
            morphIndices.add(fin.readUnsignedShort());
        }
    }

    private void writeMorphIndices(DataOutputStream fout) throws IOException {
        fout.write(0);
    }

    private void readBoneGroups(SwappedDataInputStream fin) throws IOException {
        int boneGroupCount = fin.readByte();
        for (int i = 0; i < boneGroupCount; i++) {
            PmdBoneGroup boneGroup = new PmdBoneGroup();
            boneGroup.englishName = boneGroup.japaneseName = BinaryIo.readShiftJisString(fin, 50);
            boneGroups.add(boneGroup);
        }

        List<List<Integer>> boneIndices = new ArrayList<List<Integer>>(boneGroupCount);
        for (int i = 0; i < boneGroupCount; i++) {
            boneIndices.add(new ArrayList<Integer>());
        }

        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            int boneIndex = fin.readUnsignedShort();
            int groupIndex = fin.readByte();
            boneIndices.get(groupIndex - 1).add(boneIndex);
        }
        for (int i = 0; i < boneGroupCount; i++) {
            boneGroups.get(i).boneIndices = new int[boneIndices.get(i).size()];
            for (int j = 0; j < boneIndices.get(i).size(); j++) {
                boneGroups.get(i).boneIndices[j] = boneIndices.get(i).get(j);
            }
        }
    }

    private void writeBoneGroups(DataOutputStream fout) throws IOException {
        int boneGroupCount = boneGroups.size();
        fout.write(boneGroupCount);
        for (int i = 0; i < boneGroupCount; i++) {
            PmdBoneGroup boneGroup = boneGroups.get(i);
            byte[] japaneseNameArray = boneGroup.japaneseName.getBytes("Shift-JIS");
            BinaryIo.writeByteString(fout, japaneseNameArray, 50);
        }

        int count = 0;
        for (int i = 0; i < boneGroups.size(); i++) {
            count += boneGroups.get(i).boneIndices.length;
        }
        BinaryIo.writeLittleEndianInt(fout, count);
        for (int i = 0; i < boneGroups.size(); i++) {
            PmdBoneGroup group = boneGroups.get(i);
            for (int j = 0; j < group.boneIndices.length; j++) {
                BinaryIo.writeLittleEndianShort(fout, (short) group.boneIndices[j]);
                fout.write(i + 1);
            }
        }
    }

    private void readEnglishNames(SwappedDataInputStream fin) throws IOException {
        int temp = fin.read();
        if (temp == -1) {
            hasEnglishNames = false;
        } else if (temp == 0) {
            hasEnglishNames = false;
        } else {
            hasEnglishNames = true;
            englishName = BinaryIo.readShiftJisString(fin, 20);
            englishComment = BinaryIo.readShiftJisString(fin, 256);
            for (int i = 0; i < bones.size(); i++) {
                bones.get(i).englishName = BinaryIo.readShiftJisString(fin, 20);
            }
            morphs.get(0).englishName = "base";
            for (int i = 0; i < morphs.size() - 1; i++) {
                morphs.get(i + 1).englishName = BinaryIo.readShiftJisString(fin, 20);
            }
            for (int i = 0; i < boneGroups.size(); i++) {
                boneGroups.get(i).englishName = BinaryIo.readShiftJisString(fin, 50);
            }
        }
    }

    private void writeEnglishNames(DataOutputStream fout) throws IOException {
        if (!hasEnglishNames) {
            fout.write(0);
        } else {
            fout.write(1);
            BinaryIo.writeShiftJISString(fout, englishName, 20);
            BinaryIo.writeShiftJISString(fout, englishComment, 256);
            for (int i = 0; i < bones.size(); i++) {
                BinaryIo.writeShiftJISString(fout, bones.get(i).englishName, 20);
            }
            for (int i = 0; i < morphs.size() - 1; i++) {
                BinaryIo.writeShiftJISString(fout, morphs.get(i + 1).englishName, 20);
            }
            for (int i = 0; i < boneGroups.size(); i++) {
                BinaryIo.writeShiftJISString(fout, boneGroups.get(i).englishName, 50);
            }
        }
    }

    private void readToonFileNames(SwappedDataInputStream fin) throws IOException {
        try {
            for (int i = 0; i < 10; i++) {
                String toonFileName = BinaryIo.readShiftJisString(fin, 100);
                File toonFile = new File(directory + File.separator + toonFileName);
                if (!toonFile.exists()) {
                    toonFile = new File("data/textures/mmd_toons/" + toonFileName);
                }
                toonFileName = toonFile.getAbsolutePath();
                toonFileNames.add(toonFileName);
            }
            hasToon = true;
        } catch (EOFException e) {
            hasToon = false;
            toonFileNames.clear();
        }
        if (!hasToon) {
            for (int i = 0; i < 10; i++) {
                File toonFile = new File("data/textures/mmd_toons/toon" + String.format("%02d.png",i+1));
                String toonFileName = toonFile.getAbsolutePath();
                toonFileNames.add(toonFileName);
            }
            hasToon = true;
        }
    }

    private void writeToonFileNames(DataOutputStream fout, File outDir) throws IOException {
        for (int i = 0; i < 10; i++) {
            String toonFileName = toonFileNames.get(i);
            byte[] toonFileByteArray = toonFileName.getBytes("Shift-JIS");
            BinaryIo.writeByteString(fout, toonFileByteArray, 100);

            File toonFile = new File(directory + "/" + toonFileName);
            if (toonFile.exists()) {
                File destFile = new File(outDir.getAbsoluteFile() + "/" + toonFileName);
                FileUtils.copyFile(toonFile, destFile);
            }
        }
    }

    private void readRigidBodies(SwappedDataInputStream fin) throws IOException {
        try {
            int rigidBodyCount = fin.readInt();
            for (int i = 0; i < rigidBodyCount; i++) {
                PmdRigidBody rigidBody = new PmdRigidBody();
                rigidBody.read(fin);
                rigidBodies.add(rigidBody);
                if (rigidBody.type != PmdRigidBodyType.FollowBone) {
                    if (rigidBody.boneIndex >= 0) {
                        bones.get(rigidBody.boneIndex).controlledByPhysics = true;
                    }
                }
            }
            hasPhysics = true;
        } catch (EOFException e) {
            hasPhysics = false;
            rigidBodies.clear();
        }
    }

    private void writeRigidBodies(DataOutputStream fout) throws IOException {
        int rigidBodyCount = rigidBodies.size();
        BinaryIo.writeLittleEndianInt(fout, rigidBodyCount);
        for (int i = 0; i < rigidBodyCount; i++) {
            PmdRigidBody rigidBody = rigidBodies.get(i);
            rigidBody.write(fout);
        }
    }

    private void readJoints(SwappedDataInputStream fin) throws IOException {
        if (hasPhysics) {
            int jointCount = fin.readInt();
            for (int i = 0; i < jointCount; i++) {
                PmdJoint joint = new PmdJoint();
                joint.read(fin);
                joints.add(joint);
            }
        }
    }

    private void writeJoints(DataOutputStream fout) throws IOException {
        if (hasPhysics) {
            int jointCount = joints.size();
            BinaryIo.writeLittleEndianInt(fout, jointCount);
            for (int i = 0; i < jointCount; i++) {
                PmdJoint joint = joints.get(i);
                joint.write(fout);
            }
        }
    }
}
