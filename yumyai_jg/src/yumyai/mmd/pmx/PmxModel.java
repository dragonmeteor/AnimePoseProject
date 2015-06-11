package yumyai.mmd.pmx;

import com.badlogic.gdx.physics.bullet.linearmath.int4;
import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;
import yondoko.util.PathUtil;
import yumyai.mmd.pmx.morph.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class PmxModel {
    private String directory;
    private float version;
    private Encoding encoding;
    private Charset charset;
    private int additionalTexCoordCount;
    private int vertexIndexSize;
    private int textureIndexSize;
    private int materialIndexSize;
    private int boneIndexSize;
    private int morphIndexSize;
    private int rigidBodyIndexSize;

    private String japaneseName;
    private String japaneseComment;
    private String englishName;
    private String englishComment;

    private ArrayList<PmxVertex> vertices = new ArrayList<PmxVertex>();
    private ArrayList<Integer> vertexIndices = new ArrayList<Integer>();
    private ArrayList<String> textureFileNames = new ArrayList<String>();
    private ArrayList<PmxMaterial> materials = new ArrayList<PmxMaterial>();
    private ArrayList<PmxBone> bones = new ArrayList<PmxBone>();
    private ArrayList<PmxBone> bonesByOrder = new ArrayList<PmxBone>();
    private ArrayList<PmxMorph> morphs = new ArrayList<PmxMorph>();
    private ArrayList<PmxDisplayFrame> displayFrames = new ArrayList<PmxDisplayFrame>();
    private ArrayList<PmxRigidBody> rigidBodies = new ArrayList<PmxRigidBody>();
    private ArrayList<PmxJoint> joints = new ArrayList<PmxJoint>();

    private HashMap<String, PmxBone> bonesByJapaneseName = new HashMap<String, PmxBone>();
    private HashMap<String, PmxMorph> morphsByjapaneseName = new HashMap<String, PmxMorph>();

    private ArrayList<Integer> boneOrder;

    private boolean[] vertexUsed;

    public PmxModel() {
        // NOP
    }

    public void read(SwappedDataInputStream fin) throws IOException {
        readHeader(fin);
        readVertices(fin);
        readVertexIndices(fin);
        readTextures(fin);
        readMaterials(fin);
        readBones(fin);
        readMorphs(fin);
        readDisplayFrames(fin);
        readRigidBodies(fin);
        readJoints(fin);
    }

    public void readHeader(SwappedDataInputStream fin) throws IOException {
        byte[] magicBuffer = new byte[4];
        fin.read(magicBuffer);
        String magic = new String(magicBuffer);
        if (!magic.equals("PMX "))
            throw new RuntimeException("not a PMX file");

        version = fin.readFloat();
        if (version != 2.00f) {
            throw new RuntimeException("versions other than 2.00 are not supported");
        }

        // Length of header
        if (fin.readByte() != 8) {
            throw new RuntimeException("invalid length of PMD header data");
        }

        int encodingByte = fin.readByte();
        if (encodingByte == 0) {
            encoding = Encoding.UTF16LE;
            charset = Charset.forName("UTF-16LE");
        } else {
            encoding = Encoding.UTF8;
            charset = Charset.forName("UTF-8");
        }

        additionalTexCoordCount = fin.readByte();

        vertexIndexSize = fin.readByte();
        textureIndexSize = fin.readByte();
        materialIndexSize = fin.readByte();
        boneIndexSize = fin.readByte();
        morphIndexSize = fin.readByte();
        rigidBodyIndexSize = fin.readByte();

        japaneseName = BinaryIo.readVariableLengthString(fin, charset);
        japaneseComment = BinaryIo.readVariableLengthString(fin, charset);
        englishName = BinaryIo.readVariableLengthString(fin, charset);
        englishComment = BinaryIo.readVariableLengthString(fin, charset);
    }

    public float getVersion() {
        return version;
    }

    public static PmxModel load(String fileName) throws IOException {
        PmxModel pmx = new PmxModel();

        File theFile = new File(fileName);
        File absoluteFile = theFile.getAbsoluteFile();
        pmx.directory = absoluteFile.getParent();

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
        pmx.read(fin);
        fin.close();

        return pmx;
    }

    private void readVertices(SwappedDataInputStream fin) throws IOException {
        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            PmxVertex vertex = new PmxVertex(additionalTexCoordCount);
            vertex.read(fin, boneIndexSize);
            vertices.add(vertex);
        }
    }

    private void readVertexIndices(SwappedDataInputStream fin) throws IOException {
        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            int index = BinaryIo.readIntGivenSizeInBytes(fin, vertexIndexSize, true);
            vertexIndices.add(index);
        }
        vertexUsed = new boolean[getVertexCount()];
        for (int i = 0; i < getVertexCount(); i++) {
            vertexUsed[i] = false;
        }
        for (int i : vertexIndices) {
            vertexUsed[i] = true;
        }
    }

    private void readTextures(SwappedDataInputStream fin) throws IOException {
        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            textureFileNames.add(BinaryIo.readVariableLengthString(fin, charset));
        }
    }

    private void readMaterials(SwappedDataInputStream fin) throws IOException {
        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            PmxMaterial material = new PmxMaterial();
            material.read(fin, charset, textureIndexSize);
            materials.add(material);
        }
    }

    private void readBones(SwappedDataInputStream fin) throws IOException {
        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            PmxBone bone = new PmxBone();
            bone.read(fin, charset, boneIndexSize);
            bone.boneIndex = i;
            bones.add(bone);
        }
        buildBonesByJapaneseName();
        computeBoneDisplacements();
        sortBones();
    }

    private void computeBoneDisplacements() {
        for (int i = 0; i < bones.size(); i++) {
            PmxBone bone = bones.get(i);
            if (bone.parentIndex < 0) {
                bone.displacementFromParent.set(bone.position);
            } else {
                PmxBone parent = bones.get(bone.parentIndex);
                bone.displacementFromParent.set(bone.position);
                bone.displacementFromParent.sub(parent.position);
            }
        }
    }

    private void buildBonesByJapaneseName() {
        for (PmxBone bone : bones) {
            bonesByJapaneseName.put(bone.japaneseName, bone);
        }
    }

    private void readMorphs(SwappedDataInputStream fin) throws IOException {
        int morphCount = fin.readInt();
        for (int i = 0; i < morphCount; i++) {
            String japaneseName = BinaryIo.readVariableLengthString(fin, charset);
            String englishName = BinaryIo.readVariableLengthString(fin, charset);
            int displayFrameId = fin.readByte();
            int morphType = fin.readByte();

            PmxMorph morph = null;
            switch(morphType) {
                case 0:
                    GroupMorph groupMorph = new GroupMorph();
                    groupMorph.readOffsets(fin, morphIndexSize);
                    morph = groupMorph;
                    break;
                case 1:
                    VertexMorph vertexMorph = new VertexMorph();
                    vertexMorph.readOffsets(fin, vertexIndexSize);
                    morph = vertexMorph;
                    break;
                case 2:
                    BoneMorph boneMorph = new BoneMorph();
                    boneMorph.readOffsets(fin, boneIndexSize);
                    morph = boneMorph;
                    break;
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    TexCoordMorph texCoordMorph = new TexCoordMorph(morphType - 3);
                    texCoordMorph.readOffsets(fin, vertexIndexSize);
                    morph = texCoordMorph;
                    break;
                case 8:
                    MaterialMorph materialMorph = new MaterialMorph();
                    materialMorph.readOffsets(fin, materialIndexSize);
                    morph = materialMorph;
                    break;
                default:
                    throw new RuntimeException("invalid morph type");
            }
            morph.japaneseName = japaneseName;
            morph.englishName = englishName;
            morph.displayFrameId = displayFrameId;
            morph.morphIndex = i;
            morphs.add(morph);
        }
        buildMorphsByJapaneseName();
    }

    private void buildMorphsByJapaneseName() {
        for (PmxMorph morph : morphs) {
            morphsByjapaneseName.put(morph.japaneseName, morph);
        }
    }

    private void readDisplayFrames(SwappedDataInputStream fin) throws IOException {
        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            PmxDisplayFrame frame = new PmxDisplayFrame();
            frame.read(fin, charset, boneIndexSize, morphIndexSize);
            displayFrames.add(frame);
        }
    }

    private void readRigidBodies(SwappedDataInputStream fin) throws IOException {
        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            PmxRigidBody rigidBody = new PmxRigidBody();
            rigidBody.read(fin, charset, boneIndexSize);
            rigidBodies.add(rigidBody);
        }
    }

    private void readJoints(SwappedDataInputStream fin) throws IOException {
        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            PmxJoint joint = new PmxJoint();
            joint.read(fin, charset, rigidBodyIndexSize);
            joints.add(joint);
        }
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public PmxVertex getVertex(int index) {
        return vertices.get(index);
    }

    public int getTriangleCount() {
        return vertexIndices.size() / 3;
    }

    public int getVertexIndex(int index) {
        return vertexIndices.get(index);
    }

    public int getTextureCount() {
        return textureFileNames.size();
    }

    public String getRelativeTextureFileName(int index) {
        return textureFileNames.get(index);
    }

    public String getAbsoluteTextureFileName(int index) {
        return PathUtil.getNormalizedAbsolutePath(directory + File.separator + getRelativeTextureFileName(index));
    }

    public Encoding getEncoding() {
        return encoding;
    }

    public int getAdditionalTexCoordCount() {
        return additionalTexCoordCount;
    }

    public String getJapaneseName() {
        return japaneseName;
    }

    public String getJapaneseComment() {
        return japaneseComment;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getEnglishComment() {
        return englishComment;
    }

    public int getMaterialCount() {
        return materials.size();
    }

    public PmxMaterial getMaterial(int index) {
        return materials.get(index);
    }

    public int getBoneCount() {
        return bones.size();
    }

    public PmxBone getBone(int index) {
        return bones.get(index);
    }

    public boolean hasBone(String name) {
        return bonesByJapaneseName.containsKey(name);
    }

    public PmxBone getBone(String name) {
        return bonesByJapaneseName.get(name);
    }

    public int getMorphCount() {
        return morphs.size();
    }

    public boolean hasMorph(String name) {
        return morphsByjapaneseName.containsKey(name);
    }

    public PmxMorph getMorph(int index) {
        return morphs.get(index);
    }

    public PmxMorph getMorph(String name) {
        return morphsByjapaneseName.get(name);
    }

    public int getDisplayFrameCount() {
        return displayFrames.size();
    }

    public PmxDisplayFrame getDisplayFrame(int i) {
        return displayFrames.get(i);
    }

    public int getRigidBodyCount() {
        return rigidBodies.size();
    }

    public PmxRigidBody getRigidBody(int index) {
        return rigidBodies.get(index);
    }

    public int getJointCount() {
        return joints.size();
    }

    public PmxJoint getJoint(int index) {
        return joints.get(index);
    }

    public static enum Encoding {
        UTF16LE,
        UTF8
    }

    private void sortBones() {
        boneOrder = new ArrayList<Integer>();
        for (int i = 0; i < getBoneCount(); i++) {
            boneOrder.add(i);
        }
        BoneComparator comparator = new BoneComparator();
        Collections.sort(boneOrder, comparator);
        for (int i = 0; i < getBoneCount(); i++) {
            PmxBone bone = getBone(boneOrder.get(i));
            bone.boneOrder = i;
            bonesByOrder.add(bone);
        }
    }

    public int getBoneOrder(int boneIndex) {
        return boneOrder.get(boneIndex);
    }

    public PmxBone getBoneByOrder(int order) {
        return bonesByOrder.get(order);
    }

    private class BoneComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer o1, Integer o2) {
            PmxBone b1 = bones.get(o1);
            PmxBone b2 = bones.get(o2);
            int boneCount = getBoneCount();
            int b1Score = 0;
            int b2Score = 0;
            if (b1.transformAfterPhysics()) {
                b1Score += boneCount*boneCount;
            }
            if (b2.transformAfterPhysics()) {
                b2Score += boneCount*boneCount;
            }
            b1Score += boneCount*b1.transformLevel;
            b2Score += boneCount*b2.transformLevel;
            b1Score += o1;
            b2Score += o2;
            return b1Score - b2Score;
        }
    }

    public boolean isVertexUsed(int index) {
        return vertexUsed[index];
    }
}
