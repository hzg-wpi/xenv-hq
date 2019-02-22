package de.hzg.wpi.xenv.hq.configuration.camel;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
@Root(name = "route")
public class CamelRoute {
    @Attribute
    public String id;
    @Element
    public CamelRouteEndpoint from;
    @Element
    public CamelRouteEndpoint to;
}
