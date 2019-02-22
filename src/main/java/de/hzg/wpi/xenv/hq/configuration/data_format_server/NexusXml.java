package de.hzg.wpi.xenv.hq.configuration.data_format_server;

import com.google.common.collect.Lists;
import de.hzg.wpi.xenv.hq.util.xml.Xml;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

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
}
