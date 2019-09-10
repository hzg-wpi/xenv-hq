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
import org.tango.server.ChangeEventPusher;
import org.tango.server.ServerManager;
import org.tango.server.ServerManagerUtils;
import org.tango.server.StateChangeEventPusher;
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

    @State(isPolled = true)
    private DeviceState state;
    @Status(isPolled = true)
    private String status;
    @DeviceManagement
    public DeviceManager deviceManager;
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
        ServerManagerUtils.writePidFile(
                Paths.get(System.getProperty("xenv.hq.pidFile", System.getProperty("user.dir"))));
    }

    private static void createTempDirectory() throws IOException {
        Path tmpDir = Files.createTempDirectory("hq_").toAbsolutePath();
        String result = tmpDir.toString();
        System.setProperty(XENV_HQ_TMP_DIR, result);
        FileUtils.forceDeleteOnExit(tmpDir.toFile());
    }

    private static void extractResources() throws IOException {
        Files.copy(
                HeadQuarter.class.getClassLoader().getResourceAsStream("ant/build.xml"),
                Paths.get(System.getProperty(XENV_HQ_TMP_DIR)).resolve("build.xml"),
                StandardCopyOption.REPLACE_EXISTING);
    }

    public static String getAntRoot() {
        return System.getProperty(XENV_HQ_TMP_DIR, "src/main/resources/ant");
    }

    @Init
    @StateMachine(endState = DeviceState.STANDBY)
    public void init() {
        xenvManagers = ServerManagerUtils.getBusinessObjects(ServerManager.getInstance().getInstanceName(), XenvManager.class);
        configurationManagers = ServerManagerUtils.getBusinessObjects(ServerManager.getInstance().getInstanceName(), ConfigurationManager.class);

        Preconditions.checkState(!xenvManagers.isEmpty());
        Preconditions.checkState(!configurationManagers.isEmpty());
    }

    public void setDeviceManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @Command
    public void clearAll() throws IOException {
        Arrays.stream(new String[]{"bin", "etc", "logs"}).forEach(s -> {
            try {
                FileUtils.deleteDirectory(Paths.get(s).toFile());
            } catch (IOException e) {
                logger.error("Failed to stop StatusServer configuration");
                setState(DeviceState.ALARM);
            }
        });

        Files.createDirectory(Paths.get("logs"));
    }

    @Command
    public void load(String profile) {
        logger.trace("Loading profile " + profile);
        xenvManagers.forEach(xenvManager -> {
            try {
                xenvManager.loadProfile(profile);
            } catch (Exception e) {
                logger.error("XenvManager failed to load configuration", e);
                xenvManager.setState(DeviceState.FAULT);
                xenvManager.setStatus("XenvManager failed to load configuration");
            }
        });

        configurationManagers.forEach(configurationManager -> {
            try {
                configurationManager.loadProfile(profile);
            } catch (Exception e) {
                logger.error("ConfigManager failed to load configuration", e);
                configurationManager.setState(DeviceState.FAULT);
                configurationManager.setStatus("ConfigManager failed to load configuration");
            }
        });
        setState(DeviceState.ON);
        setStatus("Profile set to " + profile);

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
                setState(DeviceState.ALARM);
            }

        });
    }

    @Command
    public void updateAll() {
        updateStatusServerConfiguration();
        updateDataFormatServerConfiguration();
        updateCamelIntegrationConfiguration();
        updatePreExperimentDataCollectorConfiguration();
    }


    @Command
    public void restartAll() {
        stopAll();
        updateAll();
        startAll();
    }

    @Command
    public void updateStatusServerConfiguration() {
        configurationManagers.forEach(configurationManager -> {
            try {
                Path conf = Files.createDirectories(Paths.get("etc/StatusServer"));
                Files.newOutputStream(
                        conf.resolve("status_server.xml"))
                        .write(configurationManager.getStatusServerXml().getBytes());
            } catch (Exception e) {
                logger.error("Failed to write StatusServer configuration");
                setState(DeviceState.ALARM);
            }

        });
    }

    @Command
    public void updateDataFormatServerConfiguration() {
        configurationManagers.forEach(configurationManager -> {
            try {
                Path conf = Files.createDirectories(Paths.get("etc/DataFormatServer"));
                Files.newOutputStream(
                        conf.resolve("default.nxdl.xml"))
                        .write(configurationManager.getNexusFileXml().getBytes());

                Files.newOutputStream(
                        conf.resolve("nxpath.mapping"))
                        .write(configurationManager.getNexusMapping().getBytes());
            } catch (Exception e) {
                logger.error("Failed to write DataFormatServer configuration");
                setState(DeviceState.ALARM);
            }

        });
    }

    @Command
    public void updateCamelIntegrationConfiguration() {
        configurationManagers.forEach(configurationManager -> {
            try {
                Path conf = Files.createDirectories(Paths.get("etc/CamelIntegration"));
                Files.newOutputStream(
                        conf.resolve("routes.xml"))
                        .write(configurationManager.getCamelRoutes().getBytes());
            } catch (Exception e) {
                logger.error("Failed to write DataFormatServer configuration");
                setState(DeviceState.ALARM);
            }

        });
    }

    @Command
    public void updatePreExperimentDataCollectorConfiguration() {
        configurationManagers.forEach(configurationManager -> {
            try {
                Path conf = Files.createDirectories(Paths.get("etc/PreExperimentDataCollector"));
                Files.newOutputStream(
                        conf.resolve("meta.yaml"))
                        .write(configurationManager.getPreExperimentDataCollectorYaml().getBytes());

                Files.newOutputStream(
                        conf.resolve("login.properties"))
                        .write(configurationManager.getPreExperimentDataCollectorLoginProperties().getBytes());
            } catch (Exception e) {
                logger.error("Failed to write PreExperimentDataCollector configuration");
                setState(DeviceState.ALARM);
            }

        });
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
                setState(DeviceState.ALARM);
            }
        });

        updateStatusServerConfiguration();

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
                setState(DeviceState.ALARM);
            }
        });

        updateDataFormatServerConfiguration();

        xenvManagers.forEach(xenvManager -> xenvManager.startServer("data_format_server"));
        logger.trace("Done.");
    }

    @Command
    public void restartCamelIntegration() {
        xenvManagers.forEach(xenvManager -> {
            try {
                xenvManager.stopServer("camel_integration");
            } catch (DevFailed devFailed) {
                DevFailedUtils.logDevFailed(devFailed, logger);
            } catch (NoSuchCommandException | TangoProxyException e) {
                logger.error("Failed to stop DataFormatServer");
                setState(DeviceState.ALARM);
            }
        });

        updateCamelIntegrationConfiguration();

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
                setState(DeviceState.ALARM);
            }
        });

        updatePreExperimentDataCollectorConfiguration();

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
        new StateChangeEventPusher(state, deviceManager).run();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        new ChangeEventPusher<>("Status", status, deviceManager).run();
    }
}
