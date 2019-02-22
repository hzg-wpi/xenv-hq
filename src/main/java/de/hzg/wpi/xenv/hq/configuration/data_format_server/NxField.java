package de.hzg.wpi.xenv.hq.configuration.data_format_server;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
@Root(name = "field")
public class NxField {
    @Attribute
    public String type;
    @Attribute
    public String name;

    @Element(required = false)
    public NxDimensions dimensions;

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

    public NxDimensions getDimensions() {
        return dimensions;
    }

    public void setDimensions(NxDimensions dimensions) {
        this.dimensions = dimensions;
    }
}
