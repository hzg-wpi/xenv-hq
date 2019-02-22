package de.hzg.wpi.xenv.hq.configuration.status_server;

import de.hzg.wpi.xenv.hq.configuration.XmlHelper;
import org.junit.Test;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
public class StatusServerXmlTest {

    @Test
    public void create() throws Exception {
        StatusServerXml result = XmlHelper.fromString("<StatusServer use-aliases=\"true\">\n" +
                "    <properties>\n" +
                "        <property name=\"jacorb.poa.thread_pool_min\" value=\"1\"/>\n" +
                "        <property name=\"jacorb.poa.thread_pool_max\" value=\"10\"/>\n" +
                "    </properties>\n" +
                "    <!-- these attributes can be used for writing data directly to StatusServer -->\n" +
                "    <attributes>\n" +
                "    </attributes>\n" +
                "    <devices>\n" +
                "        <device name=\"Buffer-0\" url=\"tine:/PETRA/Idc/Buffer-0\">\n" +
                "            <attributes>\n" +
                "                <attribute name=\"I.SCH\" alias=\"BEAM_CURRENT\" method=\"event\" interpolation=\"last\" delay=\"0\" type=\"change\"/>\n" +
                "            </attributes>\n" +
                "        </device>\n" +
                "    </devices>\n" +
                "</StatusServer>", StatusServerXml.class);


    }
}