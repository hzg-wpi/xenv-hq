package de.hzg.wpi.xenv.hq.configuration.status_server;

import de.hzg.wpi.xenv.hq.configuration.Configuration;
import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
public class StatusServerXmlGeneratorTest {

    @Test
    public void call() throws Exception {
        Configuration configuration = XmlHelper.fromString(
                "<Configuration profile='test'>\n" +
                        "    <dataSourceList>\n" +
                        "        <DataSource nxPath='/entry'\n" +
                        "                    type='scalar'\n" +
                        "                    src='test/xenv/predator/name'\n" +
                        "                    pollRate='0'\n" +
                        "            dataType='string'" +
                        "        />\n" +
                        "        <DataSource nxPath='/entry/hardware'\n" +
                        "                    type='log'\n" +
                        "                    src='tango://tango_host:10000/test/xenv/motor1/position'\n" +
                        "                    pollRate='200'\n" +
                        "            dataType='string'" +
                        "        />\n" +
                        "    </dataSourceList>\n" +
                        "</Configuration>", Configuration.class);

        StatusServerXmlGenerator instance = new StatusServerXmlGenerator(configuration);

        StatusServerXml result = instance.call();

        assertEquals("position", result.devices.get(0).attributes.get(0).name);
    }

    @Test
    public void call_multiple() throws Exception {
        Configuration configuration = XmlHelper.fromString(
                "<Configuration profile='test'>\n" +
                        "    <dataSourceList>\n" +
                        "        <DataSource nxPath='/entry'\n" +
                        "                    type='log'\n" +
                        "                    src='tine:///test/xenv/tine/name'\n" +
                        "                    pollRate='100'\n" +
                        "                    dataType='string'" +
                        "        />\n" +
                        "        <DataSource nxPath='/entry/hardware'\n" +
                        "                    type='log'\n" +
                        "                    src='tango://tango_host:10000/test/xenv/motor1/X'\n" +
                        "                    pollRate='200'\n" +
                        "            dataType='string'" +
                        "        />\n" +
                        "        <DataSource nxPath='/entry/hardware'\n" +
                        "                    type='log'\n" +
                        "                    src='tango://tango_host:10000/test/xenv/motor1/Y'\n" +
                        "                    pollRate='200'\n" +
                        "            dataType='string'" +
                        "        />\n" +
                        "    </dataSourceList>\n" +
                        "</Configuration>", Configuration.class);

        StatusServerXmlGenerator instance = new StatusServerXmlGenerator(configuration);

        StatusServerXml result = instance.call();


        assertEquals("X", result.devices.get(1).attributes.get(0).name);
        assertEquals("Y", result.devices.get(1).attributes.get(1).name);
    }
}