package de.hzg.wpi.xenv.hq.configuration;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.tango.client.ez.proxy.TangoProxies;
import org.tango.client.ez.proxy.TangoProxy;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;

public class ConfigurationManagerIT {

    public static final String TEST_DEVICE = "tango://localhost:10000/dev/xenv/config";

    @Test
    @Ignore
    public void writeDataFormatServerConfiguration() throws Exception {
        TangoProxy config = TangoProxies.newDeviceProxyWrapper(TEST_DEVICE);

//        config.executeCommand("openFile", "/home/khokhria/Projects/jDFS/target/var/test.h5");

        config.writeAttribute("dataFormatServerConfigurationOutputDir", "target/DataFormatServer");

        config.executeCommand("writeDataFormatServerConfiguration");

        assertTrue(Files.exists(Paths.get("target/DataFormatServer/default.nxdl.xml")));
        assertTrue(Files.exists(Paths.get("target/DataFormatServer/nxpath.mapping")));
    }
}
