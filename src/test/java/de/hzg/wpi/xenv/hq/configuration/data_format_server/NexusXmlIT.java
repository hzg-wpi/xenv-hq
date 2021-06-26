package de.hzg.wpi.xenv.hq.configuration.data_format_server;

import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import org.apache.commons.jxpath.JXPathContext;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
public class NexusXmlIT {

    @Test
    public void fromXml() throws Exception {
        NxGroup result = XmlHelper.fromXml(Paths.get("configuration/profiles/test/test.nxdl.xml"), NxGroup.class);

        NxGroup current = (NxGroup) JXPathContext.newContext(result).
                getValue("/groups[name='entry']/groups[name='hardware']/groups[name='beam_current']/groups[1]");

        assertEquals("current", current.name);
    }

    @Test
    public void fromXmlString() throws Exception {
        NxGroup result = XmlHelper.fromString(

                        "    <group type=\"NXentry\" name=\"entry\">\n" +
                        "        <group type=\"NXcollection\" name=\"hardware\">\n" +
                        "            <group type=\"NXcollection\" name=\"beam_current\" src=\"/PETRA/Idc/Buffer-0/I.SCH\">\n" +
                        "                <group type=\"NXlog\" name=\"current\">\n" +
                        "                    <field type=\"float64\" name=\"value\">\n" +
                        "                        <dimensions rank=\"1\">\n" +
                        "                            <dim index=\"0\" value=\"0\"/>\n" +
                        "                        </dimensions>\n" +
                        "                    </field>\n" +
                        "                    <field type=\"uint64\" name=\"time\">\n" +
                        "                        <dimensions rank=\"1\">\n" +
                        "                            <dim index=\"0\" value=\"0\"/>\n" +
                        "                        </dimensions>\n" +
                        "                    </field>\n" +
                        "                </group>\n" +
                        "            </group>\n" +
                        "        </group>\n" +
                        "    </group>", NxGroup.class
        );

        NxGroup current = (NxGroup) JXPathContext.newContext(result).
                getValue("/groups[name='entry']/groups[name='hardware']/groups[name='beam_current']/groups[1]");

        assertEquals("current", current.name);
    }
}