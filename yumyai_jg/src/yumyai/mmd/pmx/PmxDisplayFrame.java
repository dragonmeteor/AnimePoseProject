package yumyai.mmd.pmx;

import org.apache.commons.io.input.SwappedDataInputStream;
import yondoko.util.BinaryIo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class PmxDisplayFrame {
    public String japaneseName;
    public String englishName;
    public int flag;
    public final ArrayList<Member> members = new ArrayList<Member>();

    public void read(SwappedDataInputStream fin, Charset charset, int boneIndexSize, int morphIndexSize) throws IOException {
        japaneseName = BinaryIo.readVariableLengthString(fin, charset);
        englishName = BinaryIo.readVariableLengthString(fin, charset);
        flag = fin.readByte();
        int count = fin.readInt();
        for (int i = 0; i < count; i++) {
            Member member = new Member();
            member.read(fin, boneIndexSize, morphIndexSize);
            members.add(member);
        }
    }

    public static class Member {
        public int type;
        public int index;

        public void read(SwappedDataInputStream fin, int boneIndexSize, int morphIndexSize) throws IOException {
            type = fin.readByte();
            if (isBone()) {
                index = BinaryIo.readIntGivenSizeInBytes(fin, boneIndexSize, true);
            } else {
                index = BinaryIo.readIntGivenSizeInBytes(fin, morphIndexSize, true);
            }
        }

        public boolean isBone() {
            return (type & 0x01) == 0;
        }

        public boolean isMorph() {
            return (type & 0x01) != 0;
        }
    }
}
