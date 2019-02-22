package de.hzg.wpi.xenv.hq.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
public class FilesHelper {
    private FilesHelper() {
    }

    public static void createIfNotExists(String dir) throws IOException {
        Path etc = Paths.get(dir);
        if (java.nio.file.Files.notExists(etc)) {
            java.nio.file.Files.createDirectory(etc);
        }
    }
}
