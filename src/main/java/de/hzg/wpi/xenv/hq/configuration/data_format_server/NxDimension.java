package de.hzg.wpi.xenv.hq.configuration.data_format_server;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
@Root(name = "dim")
public class NxDimension {
    @Attribute
    public int index;
    @Attribute
    public int value;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
