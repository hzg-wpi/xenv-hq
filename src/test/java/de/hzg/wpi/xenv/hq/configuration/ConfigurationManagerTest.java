package de.hzg.wpi.xenv.hq.configuration;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
public class ConfigurationManagerTest {

    @Test
    public void loadConfiguration() throws Exception {
        ConfigurationManager instance = new ConfigurationManager();

        instance.loadConfiguration("test");

        Configuration configuration = instance.configuration;

        assertNotNull(configuration);
        assertEquals(1, configuration.dataSourceList.size());
        assertEquals("/entry", configuration.dataSourceList.get(0).nxPath);
        assertFalse(configuration.dataSourceList.get(0).continuous);
        assertEquals("test/xenv/predator/name", configuration.dataSourceList.get(0).src);
    }
}