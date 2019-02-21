package de.hzg.wpi.xenv.hq.configuration.status_server;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
@Root(name = "attribute")
public class StatusServerAttribute {
    @Attribute
    public String name;
    @Attribute
    public String alias;
    @Attribute
    public String method;
    @Attribute
    public String interpolation;
    @Attribute
    public int delay;
    @Attribute
    public String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getInterpolation() {
        return interpolation;
    }

    public void setInterpolation(String interpolation) {
        this.interpolation = interpolation;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
