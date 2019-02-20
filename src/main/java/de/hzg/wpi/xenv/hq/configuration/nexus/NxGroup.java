package de.hzg.wpi.xenv.hq.configuration.nexus;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
@Root(name = "group")
public class NxGroup {
    @Attribute
    public String type;
    @Attribute
    public String name;
    @Attribute(required = false)
    public String src;

    @ElementList(inline = true, required = false)
    public List<NxGroup> groups;
    @ElementList(inline = true, required = false)
    public List<NxField> fields;
    @ElementList(inline = true, required = false)
    public List<NxLink> links;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public List<NxGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<NxGroup> groups) {
        this.groups = groups;
    }

    public List<NxField> getFields() {
        return fields;
    }

    public void setFields(List<NxField> fields) {
        this.fields = fields;
    }

    public List<NxLink> getLinks() {
        return links;
    }

    public void setLinks(List<NxLink> links) {
        this.links = links;
    }
}

