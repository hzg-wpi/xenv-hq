package de.hzg.wpi.xenv.hq.configuration.camel;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
@Root
public class CamelRouteEndpoint {
    @Attribute
    public String uri;
}
