package de.hzg.wpi.xenv.hq.configuration;

import org.junit.Ignore;
import org.junit.Test;

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

        instance.load("test");

        Configuration configuration = instance.configuration;

        assertNotNull(configuration);
        assertEquals(1, configuration.dataSourceList.size());
        assertEquals("/entry", configuration.dataSourceList.get(0).nxPath);
        assertEquals("scalar", configuration.dataSourceList.get(0).type);
        assertEquals("test/xenv/predator/name", configuration.dataSourceList.get(0).src);
    }
}