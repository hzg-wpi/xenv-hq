package de.hzg.wpi.xenv.hq.configuration.camel;

import de.hzg.wpi.xenv.hq.util.xml.Xml;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
@Root(name = "routes")
public class CamelRoutesXml implements Xml {
    @ElementList(inline = true)
    public List<CamelRoute> routes;
}
