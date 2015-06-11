package yondoko.util;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtil {
    public static String relativizeSecondToFirst(String firstDir, String secondFile) {
        Path firstPath = Paths.get(firstDir).toAbsolutePath();
        Path secondPath = Paths.get(secondFile).toAbsolutePath();
        Path result = firstPath.relativize(secondPath);
        return FilenameUtils.separatorsToUnix(result.toString());
    }

    public static String relativizeSecondToFirstDir(String firstFile, String secondFile) {
        Path firstPath = Paths.get(firstFile).toAbsolutePath();
        Path secondPath = Paths.get(secondFile).toAbsolutePath();
        Path result = firstPath.getParent().relativize(secondPath);
        return FilenameUtils.separatorsToUnix(result.toString());
    }

    public static String relativizeSecondToFirstDirWithDirString(String firstFile, String secondFile) {
        Path firstPath = Paths.get(firstFile).toAbsolutePath();
        Path secondPath = Paths.get(secondFile).toAbsolutePath();
        Path result = firstPath.getParent().relativize(secondPath);
        return "${DIR}/" + FilenameUtils.separatorsToUnix(result.toString());
    }

    public static String getNormalizedAbsolutePath(String path) {
        path = FilenameUtils.normalize(new File(path).getAbsolutePath(), true);
        return path;
    }
}
