package de.hzg.wpi.xenv.hq.configuration;

import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Ignore
public class ConfigurationManagerIT {

    @Test
    public void loadConfiguration() throws Exception {
        ConfigurationManager instance = new ConfigurationManager();

        instance.loadProfile("test");

        Configuration configuration = instance.profile.configuration;

        assertNotNull(configuration);
        assertEquals(1, configuration.dataSourceList.size());
        assertEquals("/entry", configuration.dataSourceList.get(0).nxPath);
        assertEquals("scalar", configuration.dataSourceList.get(0).type);
        assertEquals("test/xenv/predator/name", configuration.dataSourceList.get(0).src);
    }


    @Test
    public void getNexusFile() throws Exception {
        ConfigurationManager instance = new ConfigurationManager();

        instance.loadProfile("test");

        String result = instance.getNexusFileXml();
        System.out.println(result);

    }

    @Test
    public void getNexusMapping() throws Exception {
        ConfigurationManager instance = new ConfigurationManager();

        instance.loadProfile("test");

        String result = instance.getNexusMapping();
        System.out.println(result);

        assertTrue(result.contains("tango\\://"));//escape ':'
    }




}