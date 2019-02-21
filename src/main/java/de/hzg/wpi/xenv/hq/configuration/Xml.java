package de.hzg.wpi.xenv.hq.configuration;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Path;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
public interface Xml {
    default String toXmlString() throws Exception {
        Serializer serializer = new Persister();
        StringWriter result = new StringWriter();

        serializer.write(this, result);
        return result.toString();
    }

    default void toXml(Path path) throws Exception {
        Serializer serializer = new Persister();
        File result = new File(path.toAbsolutePath().toUri());

        serializer.write(this, result);
    }
}
