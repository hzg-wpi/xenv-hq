package de.hzg.wpi.xenv.hq;

import de.hzg.wpi.xenv.hq.configuration.ConfigurationManager;
import de.hzg.wpi.xenv.hq.manager.XenvManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.server.ServerManager;
import org.tango.server.ServerManagerUtils;
import org.tango.server.annotation.Command;
import org.tango.server.annotation.Device;
import org.tango.server.annotation.DeviceManagement;
import org.tango.server.device.DeviceManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class HeadQuarter {
    public static final String PROFILES_ROOT = "configuration/profiles";

    private final Logger logger = LoggerFactory.getLogger(HeadQuarter.class);

    @DeviceManagement
    public DeviceManager deviceManager;

    public static void main(String[] args) throws IOException {
        setSystemProperties();

        ServerManager.getInstance().addClass("ConfigurationManager", ConfigurationManager.class);
        ServerManager.getInstance().addClass("XenvManager", XenvManager.class);
        ServerManager.getInstance().addClass("HeadQuarter", HeadQuarter.class);

        ServerManager.getInstance().start(args, HeadQuarter.class.getSimpleName());
    }

    public void setDeviceManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @Command
    public void restartStatusServer() {
        List<ConfigurationManager> configurationManagers = ServerManagerUtils.getBusinessObjects(ServerManager.getInstance().getInstanceName(), ConfigurationManager.class);

        configurationManagers.forEach(configurationManager -> {
            try {
                Files.newOutputStream(Paths.get("etc/StatusServer/" + ServerManager.getInstance().getInstanceName() + ".xml")).write(configurationManager.getStatusServerXml().getBytes());
            } catch (Exception e) {
                logger.error("Failed to write StatusServer configuration");
                //TODO state/status
            }

        });


        List<XenvManager> managers = ServerManagerUtils.getBusinessObjects(ServerManager.getInstance().getInstanceName(), XenvManager.class);

        managers.forEach(xenvManager -> xenvManager.startServer("status_server"));
        //TODO stop if is running
        //TODO update configuration
        //TODO start
        logger.trace("Done.");
    }

    private static void setSystemProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(HeadQuarter.class.getResourceAsStream("/xenv.properties"));

        System.getProperties().putAll(properties);
    }
}
