package de.hzg.wpi.xenv.hq.configuration;

import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
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
                        "        <DataSource id='0'" +
                        "                    nxPath='/entry'\n" +
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

    @Test
    public void addDataSource() {
        instance.addOrReplaceDataSource(new DataSource(System.currentTimeMillis(), "/entry/hardware/motor", "log", "test/motor/0", 200, "float32"));

        assertEquals(2, instance.dataSourceList.size());
        assertEquals("/entry/hardware/motor", instance.dataSourceList.get(1).nxPath);
    }

    @Test
    public void replaceDataSource() {
        instance.addOrReplaceDataSource(new DataSource(0L, "/entry", "log", "test/motor/0", 200, "float32"));

        assertEquals(1, instance.dataSourceList.size());
        assertEquals("test/motor/0", instance.dataSourceList.get(0).src);
    }

    @Test
    public void removeDataSource() {
        instance.removeDataSource(new DataSource(0L, "/entry", "log", "test/motor/0", 200, "float32"));

        assertTrue(instance.dataSourceList.isEmpty());
    }
}