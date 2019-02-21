package de.hzg.wpi.xenv.hq.configuration.status_server;

import com.google.common.collect.Lists;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
@Root(name = "device")
public class StatusServerDevice {
    @Attribute
    public String name;
    @Attribute
    public String url;
    @ElementList(required = false)
    public List<StatusServerAttribute> attributes = Lists.newArrayList();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<StatusServerAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<StatusServerAttribute> attributes) {
        this.attributes = attributes;
    }
}
