package de.hzg.wpi.xenv.hq.configuration;

import org.junit.Before;
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
    Configuration instance;

    @Before
    public void before() throws Exception {
        Serializer serializer = new Persister();
        File source = new File("profiles/test/configuration.xml");

        instance = serializer.read(Configuration.class, source);
    }

    @Test
    public void create() {
        assertNotNull(instance);
        assertEquals(1, instance.dataSourceList.size());
        assertEquals("/entry", instance.dataSourceList.get(0).nxPath);
        assertFalse(instance.dataSourceList.get(0).continuous);
        assertEquals("test/xenv/predator/name", instance.dataSourceList.get(0).src);
    }

    @Test
    public void toXmlString() throws Exception {
        String result = instance.toXmlString();

        System.out.println(result);
    }
}