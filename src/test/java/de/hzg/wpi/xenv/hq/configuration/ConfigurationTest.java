package de.hzg.wpi.xenv.hq.configuration;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
public class ConfigurationTest {
    Configuration instance;

    @Before
    public void before() throws Exception {
        instance = XmlHelper.fromString(
                "<Configuration profile='test'>\n" +
                        "    <dataSourceList>\n" +
                        "        <DataSource nxPath='/entry'\n" +
                        "                    type='scalar'\n" +
                        "                    src='test/xenv/predator/name'\n" +
                        "                    pollRate='0'\n" +
                        "            dataType='string'" +
                        "        />\n" +
                        "    </dataSourceList>\n" +
                        "</Configuration>", Configuration.class);
    }

    @Test
    public void create() {
        assertNotNull(instance);
        assertEquals(1, instance.dataSourceList.size());
        assertEquals("/entry", instance.dataSourceList.get(0).nxPath);
        assertEquals("scalar", instance.dataSourceList.get(0).type);
        assertEquals("test/xenv/predator/name", instance.dataSourceList.get(0).src);
        assertEquals("string", instance.dataSourceList.get(0).dataType);
    }

    @Test
    public void toXmlString() throws Exception {
        String result = instance.toXmlString();

        System.out.println(result);
    }
}