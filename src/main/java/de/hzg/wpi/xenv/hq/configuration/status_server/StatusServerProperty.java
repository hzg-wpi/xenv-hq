package de.hzg.wpi.xenv.hq.configuration.status_server;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
@Root(name = "property")
public class StatusServerProperty {
    @Attribute
    public String name;
    @Attribute
    public String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
