package de.hzg.wpi.xenv.hq.configuration.nexus;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import de.hzg.wpi.xenv.hq.configuration.Xml;
import org.apache.tools.ant.filters.StringInputStream;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
@Root(name = "definition")
public class NexusXml implements Xml {
    public static final String NX_COLLECTION = "NXcollection";
    public static final String NX_LOG = "NXlog";

    @ElementList(inline = true, required = false)
    public List<NxField> fields = Lists.newArrayList();
    @ElementList(inline = true, required = false)
    public List<NxGroup> groups = Lists.newArrayList();

    public static NexusXml fromXml(Path path) throws Exception {
        Preconditions.checkArgument(Files.exists(path), path.toAbsolutePath() + "does not exists!");

        Serializer serializer = new Persister();
        File source = new File(path.toAbsolutePath().toUri());

        NexusXml instance = serializer.read(NexusXml.class, source);

        return instance;
    }

    public static NexusXml fromString(String nxFile) throws Exception {
        Serializer serializer = new Persister();
        InputStream source = new StringInputStream(nxFile);

        NexusXml instance = serializer.read(NexusXml.class, source);

        return instance;
    }

    public String toXmlString() throws Exception {
        Serializer serializer = new Persister();
        StringWriter result = new StringWriter();

        serializer.write(this, result);
        return result.toString();
    }

    public List<NxField> getFields() {
        return fields;
    }

    public void setFields(List<NxField> fields) {
        this.fields = fields;
    }

    public List<NxGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<NxGroup> groups) {
        this.groups = groups;
    }

    public void toXml(Path path) throws Exception {
        Serializer serializer = new Persister();
        File result = new File(path.toAbsolutePath().toUri());

        serializer.write(this, result);
    }
}
