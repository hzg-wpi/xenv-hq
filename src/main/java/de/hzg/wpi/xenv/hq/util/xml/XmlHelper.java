package de.hzg.wpi.xenv.hq.util.xml;

import com.google.common.base.Preconditions;
import org.apache.tools.ant.filters.StringInputStream;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
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

    public static String toXmlString(Object o) {
        Serializer serializer = new Persister();
        StringWriter result = new StringWriter();

        try {
            serializer.write(o, result);
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void toXml(Object o, Path path) {
        Serializer serializer = new Persister();
        File result = path.toAbsolutePath().toFile();

        try {
            serializer.write(o, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
