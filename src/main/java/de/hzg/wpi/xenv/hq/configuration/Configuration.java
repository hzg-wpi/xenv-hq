package de.hzg.wpi.xenv.hq.configuration;

import com.google.common.base.Preconditions;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Root(strict = false)
public class Configuration implements Xml {
    @Attribute
    public String profile;

    @ElementList
    public List<DataSource> dataSourceList;

    public static Configuration fromXml(Path path) throws Exception {
        Preconditions.checkArgument(Files.exists(path), path.toAbsolutePath() + "does not exists!");

        Serializer serializer = new Persister();
        File source = new File(path.toAbsolutePath().toUri());

        Configuration instance = serializer.read(Configuration.class, source);

        return instance;
    }

    public boolean addDataSource(DataSource result) {
        if (dataSourceList.contains(result)) return false;
        dataSourceList.add(result);
        return true;
    }

    public void removeDataSource(DataSource result) {
        dataSourceList.remove(result);
    }

    public void toXml(Path path) throws Exception {
        Serializer serializer = new Persister();
        File result = new File(path.toAbsolutePath().toUri());

        serializer.write(this, result);
    }

    public String toXmlString() throws Exception {
        Serializer serializer = new Persister();
        StringWriter result = new StringWriter();

        serializer.write(this, result);
        return result.toString();
    }
}
