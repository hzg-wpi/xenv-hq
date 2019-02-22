package de.hzg.wpi.xenv.hq.configuration.camel;

import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import org.junit.Test;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
public class CamelRoutesXmlTest {
    @Test
    public void create() throws Exception {
        CamelRoutesXml instance = XmlHelper.fromString("<routes xmlns=\"http://camel.apache.org/schema/spring\">\n" +
                "    <route id=\"status_server_0\">\n" +
                "        <from uri=\"tango:test/status_server/0?pipe=status_server_pipe&amp;poll=true\"/>\n" +
                "        <to uri=\"tango:test/dfs/0?pipe=pipe\"/>\n" +
                "    </route>\n" +
                "</routes>", CamelRoutesXml.class);


    }

}