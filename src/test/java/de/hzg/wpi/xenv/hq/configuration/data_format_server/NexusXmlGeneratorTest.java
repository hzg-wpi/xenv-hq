package de.hzg.wpi.xenv.hq.configuration.data_format_server;

import com.google.common.collect.Lists;
import de.hzg.wpi.xenv.hq.configuration.Configuration;
import de.hzg.wpi.xenv.hq.configuration.collections.DataSource;
import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import org.apache.commons.jxpath.JXPathContext;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
public class NexusXmlGeneratorTest {

    NxGroup nexusXml;

    @Before
    public void before() throws Exception {
        nexusXml = XmlHelper.fromString(
                "<group type=\"NXentry\" name=\"entry\">\n" +
                "        <group type=\"NXcollection\" name=\"hardware\">\n" +
                "        </group>\n" +
                "    </group>", NxGroup.class);
    }

    @Test
    public void generateNxLog() throws Exception {
        Configuration configuration = new Configuration();
        configuration.dataSourceList = Lists.newArrayList(
                new DataSource(System.currentTimeMillis(), "/entry/hardware/motor1/Position", "log", "tango://...", 200, "uint16")
        );


        NexusXmlGenerator instance = new NexusXmlGenerator(configuration.dataSourceList, nexusXml);

        NxGroup nexusXml1 = instance.call();

        NxGroup result = (NxGroup) JXPathContext.newContext(nexusXml1).getValue("/groups[name='hardware']/groups[name='motor1']");

        assertNotNull(result);
        assertEquals("NXcollection", result.type);
        assertEquals("motor1", result.name);

        assertEquals("Position", result.groups.get(0).name);
    }

    @Test
    public void generateNxLog_nonExistingNxPath() throws Exception {
        Configuration configuration = new Configuration();
        configuration.dataSourceList = Lists.newArrayList(
                new DataSource(System.currentTimeMillis(), "/entry/software/motor1/Position", "log", "tango://...", 200, "uint16")
        );


        NexusXmlGenerator instance = new NexusXmlGenerator(configuration.dataSourceList, nexusXml);

        NxGroup nexusXml1 = instance.call();

        NxGroup result = (NxGroup) JXPathContext.newContext(nexusXml1).getValue("/groups[name='software']/groups[name='motor1']");

        assertNotNull(result);
        assertEquals("NXcollection", result.type);
        assertEquals("motor1", result.name);

        assertEquals("Position", result.groups.get(0).name);
    }

    @Test
    public void generateNxLog_multiple() throws Exception {
        Configuration configuration = new Configuration();
        configuration.dataSourceList = Lists.newArrayList(
                new DataSource(System.currentTimeMillis(), "/entry/hardware/motor1/X", "log", "tango://...", 200, "uint16"),
                new DataSource(System.currentTimeMillis(), "/entry/hardware/motor1/Y", "log", "tango://...", 200, "uint16")
        );


        NexusXmlGenerator instance = new NexusXmlGenerator(configuration.dataSourceList, nexusXml);

        NxGroup nexusXml1 = instance.call();

        NxGroup result = (NxGroup) JXPathContext.newContext(nexusXml1).getValue("/groups[name='hardware']/groups[name='motor1']");

        assertNotNull(result);
        assertEquals("NXcollection", result.type);
        assertEquals("motor1", result.name);

        assertEquals("X", result.groups.get(0).name);
        assertEquals("Y", result.groups.get(1).name);

        System.out.println(XmlHelper.toXmlString(nexusXml1));
    }

    @Test
    public void generateNxField() throws Exception {
        Configuration configuration = new Configuration();
        configuration.dataSourceList = Lists.newArrayList(
                new DataSource(System.currentTimeMillis(), "/entry/hardware/motor1/name", "scalar", "tango://...", 200, "string")
        );


        NexusXmlGenerator instance = new NexusXmlGenerator(configuration.dataSourceList, nexusXml);

        NxGroup nexusXml1 = instance.call();

        NxGroup result = (NxGroup) JXPathContext.newContext(nexusXml1).getValue("/groups[name='hardware']/groups[name='motor1']");

        assertNotNull(result);
        assertEquals("NXcollection", result.type);
        assertEquals("motor1", result.name);

        assertEquals("name", result.fields.get(0).name);
        assertEquals("string", result.fields.get(0).type);
    }

    @Test
    public void generateNxField_dimensions() throws Exception {
        Configuration configuration = new Configuration();
        configuration.dataSourceList = Lists.newArrayList(
                new DataSource(System.currentTimeMillis(), "/entry/hardware/motor1/Position", "spectrum", "tango://...", 200, "uint16")
        );


        NexusXmlGenerator instance = new NexusXmlGenerator(configuration.dataSourceList, nexusXml);

        NxGroup nexusXml1 = instance.call();

        NxGroup result = (NxGroup) JXPathContext.newContext(nexusXml1).getValue("/groups[name='hardware']/groups[name='motor1']");

        assertNotNull(result);
        assertEquals("NXcollection", result.type);
        assertEquals("motor1", result.name);

        assertEquals("Position", result.fields.get(0).name);
        assertEquals("uint16", result.fields.get(0).type);
        assertNotNull(result.fields.get(0).dimensions);
    }

    @Test
    public void generateNxField_replaceExisting() throws Exception {
        nexusXml = XmlHelper.fromString(
                "    <group type=\"NXentry\" name=\"entry\">\n" +
                "        <group type=\"NXcollection\" name=\"hardware\">\n" +
                "            <field type=\"string\" name=\"name\"/>\n" +
                "        </group>\n" +
                "    </group>", NxGroup.class);


        Configuration configuration = new Configuration();
        configuration.dataSourceList = Lists.newArrayList(
                new DataSource(System.currentTimeMillis(), "/entry/hardware/name", "scalar", "tango://...", 200, "uint16")
        );


        NexusXmlGenerator instance = new NexusXmlGenerator(configuration.dataSourceList, nexusXml);

        NxGroup nexusXml1 = instance.call();

        List<NxField> result = (List<NxField>) JXPathContext.newContext(nexusXml1).selectNodes("/groups[name='hardware']/fields[name='name']");

        assertNotNull(result);
        //TODO should we replace existing?
        assertEquals(2, result.size());
//        assertEquals("uint16", result.type);
    }
}