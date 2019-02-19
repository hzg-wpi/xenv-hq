package de.hzg.wpi.xenv.hq.configuration;

import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
public class ConfigurationTest {
    @Test
    public void create() throws Exception {
        Serializer serializer = new Persister();
        File source = new File("profiles/test/configuration.xml");

        Configuration instance = serializer.read(Configuration.class, source);

        assertNotNull(instance);
        assertEquals(1, instance.dataSourceList.size());
        assertEquals("/entry", instance.dataSourceList.get(0).nxPath);
        assertFalse(instance.dataSourceList.get(0).continuous);
        assertEquals("test/xenv/predator/name", instance.dataSourceList.get(0).src);
    }
}