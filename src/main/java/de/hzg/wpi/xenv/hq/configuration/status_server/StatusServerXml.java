package de.hzg.wpi.xenv.hq.configuration.status_server;

import com.google.common.collect.Lists;
import de.hzg.wpi.xenv.hq.configuration.Xml;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
@Root(name = "StatusServer")
public class StatusServerXml implements Xml {
    @Attribute(name = "use-aliases")
    public boolean useAliases;
    @ElementList(required = false)
    public List<StatusServerAttribute> attributes = Lists.newArrayList();
    @ElementList(required = false)
    public List<StatusServerDevice> devices = Lists.newArrayList();
    @ElementList(required = false)
    public List<StatusServerProperty> properties = Lists.newArrayList();


    public boolean isUseAliases() {
        return useAliases;
    }

    public void setUseAliases(boolean useAliases) {
        this.useAliases = useAliases;
    }

    public List<StatusServerAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<StatusServerAttribute> attributes) {
        this.attributes = attributes;
    }

    public List<StatusServerDevice> getDevices() {
        return devices;
    }

    public void setDevices(List<StatusServerDevice> devices) {
        this.devices = devices;
    }

    public List<StatusServerProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<StatusServerProperty> properties) {
        this.properties = properties;
    }
}
