package yondoko.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class TextIo {
    public static String readFile(String filePath) {
        return readTextFile(filePath);
    }

    /**
     * Read a text file and return its content.
     *
     * @param filePath the path to the text file
     * @param charset the character set of the file
     * @return the content of the text file
     */
    public static String readTextFile(String filePath, Charset charset) {
        try {
            File f = new File(filePath);
            long fileSize = f.length();
            if (fileSize > Integer.MAX_VALUE) {
                throw new RuntimeException("file to large to load");
            }
            ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
            FileInputStream fileInputStream = new FileInputStream(f);
            FileChannel channel = fileInputStream.getChannel();
            channel.read(buffer);
            channel.close();
            fileInputStream.close();
            buffer.rewind();

            return new String(buffer.array(), charset);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read the content of a text file with UTF-8 encoding.
     * @param filePath
     * @return
     */
    public static String readTextFile(String filePath) {
        return readTextFile(filePath, Charset.forName("UTF-8"));
    }

    /**
     * Write the given string to a file with a specific encoding
     * @param filePath
     * @param content
     * @param charset
     */
    public static void writeTextFile(String filePath, String content, Charset charset) {
        try {
            FileOutputStream stream = new FileOutputStream(filePath);
            OutputStreamWriter writer = new OutputStreamWriter(stream, charset);
            writer.write(content);
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write the given string to the given file with UTF-8 encoding.
     * @param filePath
     * @param content
     */
    public static void writeTextFile(String filePath, String content) {
        writeTextFile(filePath, content, Charset.forName("UTF-8"));
    }
}
