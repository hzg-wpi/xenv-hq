package de.hzg.wpi.xenv.hq;

import com.google.common.base.Preconditions;
import de.hzg.wpi.xenv.hq.configuration.ConfigurationManager;
import de.hzg.wpi.xenv.hq.manager.XenvManager;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevVarLongStringArray;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.DeviceState;
import org.tango.client.ez.proxy.NoSuchCommandException;
import org.tango.client.ez.proxy.TangoProxyException;
import org.tango.server.ServerManager;
import org.tango.server.ServerManagerUtils;
import org.tango.server.annotation.*;
import org.tango.server.device.DeviceManager;
import org.tango.utils.DevFailedUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class HeadQuarter {
    public static final String PROFILES_ROOT = "configuration/profiles";
    public static final String[] XENV_EXECUTABLES = {"camel_integration", "data_format_server", "status_server", "predator"};
    public static final String XENV_HQ_TMP_DIR = "xenv.hq.tmp.dir";

    private final Logger logger = LoggerFactory.getLogger(HeadQuarter.class);

    @State(isPolled = true, pollingPeriod = 3000)
    private DeviceState state;
    @Status(isPolled = true, pollingPeriod = 3000)
    private String status;
    @DeviceManagement
    public DeviceManager deviceManager;
    public void setDeviceManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }
    private List<XenvManager> xenvManagers;
    private List<ConfigurationManager> configurationManagers;

    public static void main(String[] args) throws IOException {
        createTempDirectory();
        extractResources();
        setSystemProperties();

        ServerManager.getInstance().addClass("ConfigurationManager", ConfigurationManager.class);
        ServerManager.getInstance().addClass("XenvManager", XenvManager.class);
        ServerManager.getInstance().addClass("HeadQuarter", HeadQuarter.class);

        ServerManager.getInstance().start(args, HeadQuarter.class.getSimpleName());
    }

    private static void createTempDirectory() throws IOException {
        Path tmpDir = Files.createTempDirectory("hq_").toAbsolutePath();
        String result = tmpDir.toString();
        System.setProperty(XENV_HQ_TMP_DIR, result);
        FileUtils.forceDeleteOnExit(tmpDir.toFile());
    }

    private static void extractResources() throws IOException {
        Files.copy(
                HeadQuarter.class.getClassLoader().getResourceAsStream("ant/Executable_template"),
                Paths.get(System.getProperty(XENV_HQ_TMP_DIR)).resolve("Executable_template"),
                StandardCopyOption.REPLACE_EXISTING);

        Files.copy(
                HeadQuarter.class.getClassLoader().getResourceAsStream("ant/build.xml"),
                Paths.get(System.getProperty(XENV_HQ_TMP_DIR)).resolve("build.xml"),
                StandardCopyOption.REPLACE_EXISTING);
    }

    public static String getAntRoot() {
        return System.getProperty(XENV_HQ_TMP_DIR, "src/main/resources/ant");
    }

    @Init
    public void init() {
        xenvManagers = ServerManagerUtils.getBusinessObjects(ServerManager.getInstance().getInstanceName(), XenvManager.class);
        configurationManagers = ServerManagerUtils.getBusinessObjects(ServerManager.getInstance().getInstanceName(), ConfigurationManager.class);

        Preconditions.checkState(!xenvManagers.isEmpty());
        Preconditions.checkState(!configurationManagers.isEmpty());

        deviceManager.pushStateChangeEvent(DeviceState.STANDBY);
    }

    @Delete
    public void delete(){
        deviceManager.pushStateChangeEvent(DeviceState.OFF);
        deviceManager.pushStatusChangeEvent(DeviceState.OFF.name());
    }

    @Command
    public void clearAll() throws IOException {
        Arrays.stream(new String[]{"bin", "etc", "logs"}).forEach(s -> {
            try {
                FileUtils.deleteDirectory(Paths.get(s).toFile());
            } catch (IOException e) {
                logger.error("Failed to stop StatusServer configuration");
                deviceManager.pushStateChangeEvent(DeviceState.ALARM);
            }
        });

        Files.createDirectory(Paths.get("logs"));
    }

    @Command
    public void load(String profile) {
        logger.debug("Loading profile " + profile);
        xenvManagers.forEach(xenvManager -> {
            try {
                xenvManager.loadProfile(profile);
            } catch (Exception e) {
                logger.error("XenvManager failed to load configuration", e);
                deviceManager.pushStateChangeEvent(DeviceState.ALARM);
                xenvManager.setState(DeviceState.FAULT);
                xenvManager.setStatus("XenvManager failed to load configuration");
            }
        });

        if(getState() != DeviceState.ALARM) {
            deviceManager.pushStateChangeEvent(DeviceState.ON);
            deviceManager.pushStatusChangeEvent("Profile set to " + profile);
        }

        logger.trace("Done.");
    }

    @Attribute
    public String[] getXenvExecutables() {
        return XENV_EXECUTABLES;
    }

    @Command
    public void startAll() {
        Arrays.stream(XENV_EXECUTABLES)
                .flatMap(s -> xenvManagers.stream().map(xenvManager -> (Runnable) () -> {
                    xenvManager.startServer(s);
                }))
                .parallel()
                .forEach(Runnable::run);
    }

    @Command
    public void stopAll() {
        Arrays.stream(XENV_EXECUTABLES)
                .flatMap(s -> xenvManagers.stream().map(xenvManager -> (Runnable) () -> {
                    try {
                        xenvManager.stopServer(s);
                    } catch (DevFailed devFailed) {
                        DevFailedUtils.logDevFailed(devFailed, logger);
                    } catch (NoSuchCommandException | TangoProxyException e) {
                        logger.error(String.format("Failed to stop %s", s));
                    }
                }))
                .parallel()
                .forEach(Runnable::run);
    }


    @Command
    public void updateProfileCollections(DevVarLongStringArray collections) {
        configurationManagers.forEach(configurationManager -> {
            try {
                configurationManager.updateProfileCollections(collections);
            } catch (Exception e) {
                logger.error("Failed to update profile collections configuration");
                deviceManager.pushStateChangeEvent(DeviceState.ALARM);
            }

        });
    }

    //TODO move to waltz XenvHQ server side
    @Command
    public void updateAll() {
        configurationManagers.forEach(ConfigurationManager::writeStatusServerConfiguration);
        configurationManagers.forEach(ConfigurationManager::writeDataFormatServerConfiguration);
        configurationManagers.forEach(ConfigurationManager::writeCamelConfiguration);
        configurationManagers.forEach(ConfigurationManager::writePreExperimentDataCollectorConfiguration);
    }


    @Command
    public void restartAll() {
        stopAll();
        updateAll();
        startAll();
    }

    @Command
    public void restartStatusServer() {
        xenvManagers.forEach(xenvManager -> {
            try {
                xenvManager.stopServer("status_server");
            } catch (DevFailed devFailed) {
                DevFailedUtils.logDevFailed(devFailed, logger);
            } catch (NoSuchCommandException | TangoProxyException e) {
                logger.error("Failed to stop StatusServer configuration");
                deviceManager.pushStateChangeEvent(DeviceState.ALARM);
            }
        });

        configurationManagers.forEach(ConfigurationManager::writeStatusServerConfiguration);

        xenvManagers.forEach(xenvManager -> xenvManager.startServer("status_server"));
        logger.trace("Done.");
    }

    @Command
    public void restartDataFormatServer() {
        xenvManagers.forEach(xenvManager -> {
            try {
                xenvManager.stopServer("data_format_server");
            } catch (DevFailed devFailed) {
                DevFailedUtils.logDevFailed(devFailed, logger);
            } catch (NoSuchCommandException | TangoProxyException e) {
                logger.error("Failed to stop DataFormatServer");
                deviceManager.pushStateChangeEvent(DeviceState.ALARM);
            }
        });

        configurationManagers.forEach(ConfigurationManager::writeDataFormatServerConfiguration);

        xenvManagers.forEach(xenvManager -> xenvManager.startServer("data_format_server"));
        logger.trace("Done.");
    }

    //TODO move to waltz XenvHQ server side
    @Command
    public void restartCamelIntegration() {
        xenvManagers.forEach(xenvManager -> {
            try {
                xenvManager.stopServer("camel_integration");
            } catch (DevFailed devFailed) {
                DevFailedUtils.logDevFailed(devFailed, logger);
            } catch (NoSuchCommandException | TangoProxyException e) {
                logger.error("Failed to stop DataFormatServer");
                deviceManager.pushStateChangeEvent(DeviceState.ALARM);
            }
        });


        configurationManagers.forEach(ConfigurationManager::writeCamelConfiguration);

        xenvManagers.forEach(xenvManager -> xenvManager.startServer("camel_integration"));
        logger.trace("Done.");
    }

    @Command
    public void restartPreExperimentDataCollector() {
        xenvManagers.forEach(xenvManager -> {
            try {
                xenvManager.stopServer("predator");
            } catch (DevFailed devFailed) {
                DevFailedUtils.logDevFailed(devFailed, logger);
                setState(DeviceState.ALARM);
            } catch (NoSuchCommandException | TangoProxyException e) {
                logger.error("Failed to stop DataFormatServer");
                deviceManager.pushStateChangeEvent(DeviceState.ALARM);
            }
        });

        configurationManagers.forEach(ConfigurationManager::writePreExperimentDataCollectorConfiguration);

        xenvManagers.forEach(xenvManager -> xenvManager.startServer("predator"));
        logger.trace("Done.");
    }

    private static void setSystemProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(HeadQuarter.class.getResourceAsStream("/xenv.properties"));

        System.getProperties().putAll(properties);
    }

    public DeviceState getState() {
        return state;
    }

    public void setState(DeviceState state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = String.format("%d: %s",System.currentTimeMillis(),status);
    }
}
