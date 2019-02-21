package de.hzg.wpi.xenv.hq.configuration;

import com.google.common.base.Preconditions;
import org.apache.tools.ant.filters.StringInputStream;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
public class XmlHelper {
    private XmlHelper() {
    }

    public static <T> T fromString(String xml, Class<T> clazz) throws Exception {
        Serializer serializer = new Persister();
        InputStream source = new StringInputStream(xml);

        return serializer.read(clazz, source);
    }

    public static <T> T fromXml(Path path, Class<T> clazz) throws Exception {
        Preconditions.checkArgument(Files.exists(path), path.toAbsolutePath() + "does not exists!");

        Serializer serializer = new Persister();
        File source = new File(path.toAbsolutePath().toUri());

        return serializer.read(clazz, source);
    }
}
