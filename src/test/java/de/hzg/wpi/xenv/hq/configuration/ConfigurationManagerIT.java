package de.hzg.wpi.xenv.hq.configuration;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Paths;

import static de.hzg.wpi.xenv.hq.HeadQuarter.PROFILES_ROOT;
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

        instance.profile = "test";
        instance.load();

        Configuration configuration = instance.configuration;

        assertNotNull(configuration);
        assertEquals(1, configuration.dataSourceList.size());
        assertEquals("/entry", configuration.dataSourceList.get(0).nxPath);
        assertEquals("scalar", configuration.dataSourceList.get(0).type);
        assertEquals("test/xenv/predator/name", configuration.dataSourceList.get(0).src);
    }


    @Test
    public void getNexusFile() throws Exception {
        ConfigurationManager instance = new ConfigurationManager();

        instance.profile = "test";
        instance.load();

        String result = instance.getNexusFile();
        System.out.println(result);

    }

    @Test
    public void getNexusMapping() throws Exception {
        ConfigurationManager instance = new ConfigurationManager();

        instance.profile = "test";
        instance.load();

        String result = instance.getNexusMapping();
        System.out.println(result);

        assertTrue(result.contains("tango\\://"));//escape ':'
    }

    @Test
    public void testCopyDefaultProfile() throws Exception {
        String newTestProfile = "newTestProfile";

        ConfigurationManager instance = new ConfigurationManager();

        instance.profile = "test";
        instance.load();

        instance.executeAnt(newTestProfile, "copy-profile");
        FileUtils.forceDeleteOnExit(Paths.get(PROFILES_ROOT).resolve(newTestProfile).toFile());
    }

    @Test
    public void testCreateDeleteProfile() throws Exception {
        String newTestProfile = "newTestProfile";

        ConfigurationManager instance = new ConfigurationManager();

        instance.profile = "test";
        instance.load();

        instance.createProfile(newTestProfile);

        Thread.sleep(3000);

        instance.deleteProfile(newTestProfile);

        FileUtils.forceDeleteOnExit(Paths.get(PROFILES_ROOT).resolve(newTestProfile).toFile());
    }
}