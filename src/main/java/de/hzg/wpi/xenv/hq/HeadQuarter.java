package de.hzg.wpi.xenv.hq;

import com.google.common.base.Preconditions;
import de.hzg.wpi.xenv.hq.configuration.ConfigurationManager;
import de.hzg.wpi.xenv.hq.manager.XenvManager;
import fr.esrf.Tango.DevFailed;
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
import java.nio.file.StandardOpenOption;
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

    private final Logger logger = LoggerFactory.getLogger(HeadQuarter.class);

    @State
    private DeviceState state;
    @DeviceManagement
    public DeviceManager deviceManager;
    private List<XenvManager> xenvManagers;
    private List<ConfigurationManager> configurationManagers;

    public static void main(String[] args) throws IOException {
        setSystemProperties();

        ServerManager.getInstance().addClass("ConfigurationManager", ConfigurationManager.class);
        ServerManager.getInstance().addClass("XenvManager", XenvManager.class);
        ServerManager.getInstance().addClass("HeadQuarter", HeadQuarter.class);

        ServerManager.getInstance().start(args, HeadQuarter.class.getSimpleName());
    }

    @Init
    @StateMachine(endState = DeviceState.ON)
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
    @StateMachine(deniedStates = DeviceState.RUNNING)
    public void clearAll() {
        Arrays.stream(new String[]{"bin", "etc", "logs", "var"}).forEach(s -> {
            try {
                FileUtils.deleteDirectory(Paths.get(s).toFile());
            } catch (IOException e) {
                logger.error("Failed to stop StatusServer configuration");
                setState(DeviceState.FAULT);
            }
        });

    }

    @Command
    public void load(String profile) {
        xenvManagers.forEach(xenvManager -> {
            try {
                xenvManager.setProfile(profile);
                xenvManager.load();
            } catch (IOException e) {
                logger.error("XenvManager failed to load configuration");
                setState(DeviceState.FAULT);
            }
        });

        configurationManagers.forEach(configurationManager -> {
            try {
                configurationManager.setProfile(profile);
                configurationManager.load();
            } catch (Exception e) {
                logger.error("ConfigManager failed to load configuration");
                setState(DeviceState.FAULT);
            }
        });
        logger.trace("Done.");
    }

    @Attribute
    public String[] getXenvExecutables() {
        return XENV_EXECUTABLES;
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void startAll() {
        Arrays.stream(XENV_EXECUTABLES)
                .flatMap(s -> xenvManagers.stream().map(xenvManager -> (Runnable) () -> {
                    xenvManager.startServer(s);
                }))
                .forEach(Runnable::run);
    }

    @Command
    @StateMachine(endState = DeviceState.ON)
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
                .forEach(Runnable::run);
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void restartAll() {
        stopAll();

        updateStatusServerConfiguration();
        updateDataFormatServerConfiguration();
        updateCamelIntegrationConfiguration();
        updatePreExperimentDataCollectorConfiguration();

        startAll();
    }

    @Command
    @StateMachine(deniedStates = DeviceState.RUNNING)
    public void updateStatusServerConfiguration() {
        configurationManagers.forEach(configurationManager -> {
            try {
                Path conf = Files.createDirectories(Paths.get("etc/StatusServer"));
                Files.newOutputStream(
                        conf.resolve("0.xml"), StandardOpenOption.CREATE)
                        .write(configurationManager.getStatusServerXml().getBytes());
            } catch (Exception e) {
                logger.error("Failed to write StatusServer configuration");
                setState(DeviceState.FAULT);
            }

        });
    }

    @Command
    @StateMachine(deniedStates = DeviceState.RUNNING)
    public void updateDataFormatServerConfiguration() {
        configurationManagers.forEach(configurationManager -> {
            try {
                Path conf = Files.createDirectories(Paths.get("etc/DataFormatServer"));
                Files.newOutputStream(
                        conf.resolve(ServerManager.getInstance().getInstanceName() + ".nxdl.xml"), StandardOpenOption.CREATE)
                        .write(configurationManager.getNexusFile().getBytes());

                Files.newOutputStream(
                        conf.resolve("nxpath.mapping"), StandardOpenOption.CREATE)
                        .write(configurationManager.getNexusMapping().getBytes());
            } catch (Exception e) {
                logger.error("Failed to write DataFormatServer configuration");
                setState(DeviceState.FAULT);
            }

        });
    }

    @Command
    @StateMachine(deniedStates = DeviceState.RUNNING)
    public void updateCamelIntegrationConfiguration() {
        configurationManagers.forEach(configurationManager -> {
            try {
                Path conf = Files.createDirectories(Paths.get("etc/CamelIntegration"));
                Files.newOutputStream(
                        conf.resolve("routes.xml"), StandardOpenOption.CREATE)
                        .write(configurationManager.getCamelRoutes().getBytes());
            } catch (Exception e) {
                logger.error("Failed to write DataFormatServer configuration");
                setState(DeviceState.FAULT);
            }

        });
    }

    @Command
    @StateMachine(deniedStates = DeviceState.RUNNING)
    public void updatePreExperimentDataCollectorConfiguration() {
        configurationManagers.forEach(configurationManager -> {
            try {
                Path conf = Files.createDirectories(Paths.get("etc/PreExperimentDataCollector"));
                Files.newOutputStream(
                        conf.resolve("meta.yaml"), StandardOpenOption.CREATE)
                        .write(configurationManager.getPreExperimentDataCollectorYaml().getBytes());

                Files.newOutputStream(
                        conf.resolve("login.properties"), StandardOpenOption.CREATE)
                        .write(configurationManager.getPreExperimentDataCollectorLoginProperties().getBytes());
            } catch (Exception e) {
                logger.error("Failed to write DataFormatServer configuration");
                setState(DeviceState.FAULT);
            }

        });
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void restartStatusServer() {
        xenvManagers.forEach(xenvManager -> {
            try {
                xenvManager.stopServer("status_server");
            } catch (DevFailed devFailed) {
                DevFailedUtils.logDevFailed(devFailed, logger);
            } catch (NoSuchCommandException | TangoProxyException e) {
                logger.error("Failed to stop StatusServer configuration");
                setState(DeviceState.FAULT);
            }
        });

        updateStatusServerConfiguration();

        xenvManagers.forEach(xenvManager -> xenvManager.startServer("status_server"));
        logger.trace("Done.");
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void restartDataFormatServer() {
        xenvManagers.forEach(xenvManager -> {
            try {
                xenvManager.stopServer("data_format_server");
            } catch (DevFailed devFailed) {
                DevFailedUtils.logDevFailed(devFailed, logger);
            } catch (NoSuchCommandException | TangoProxyException e) {
                logger.error("Failed to stop DataFormatServer");
                setState(DeviceState.FAULT);
            }
        });

        updateDataFormatServerConfiguration();

        xenvManagers.forEach(xenvManager -> xenvManager.startServer("data_format_server"));
        logger.trace("Done.");
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void restartCamelIntegration() {
        xenvManagers.forEach(xenvManager -> {
            try {
                xenvManager.stopServer("camel_integration");
            } catch (DevFailed devFailed) {
                DevFailedUtils.logDevFailed(devFailed, logger);
            } catch (NoSuchCommandException | TangoProxyException e) {
                logger.error("Failed to stop DataFormatServer");
                setState(DeviceState.FAULT);
            }
        });

        updateCamelIntegrationConfiguration();

        xenvManagers.forEach(xenvManager -> xenvManager.startServer("camel_integration"));
        logger.trace("Done.");
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void restartPreExperimentDataCollector() {
        xenvManagers.forEach(xenvManager -> {
            try {
                xenvManager.stopServer("predator");
            } catch (DevFailed devFailed) {
                DevFailedUtils.logDevFailed(devFailed, logger);
                setState(DeviceState.FAULT);
            } catch (NoSuchCommandException | TangoProxyException e) {
                logger.error("Failed to stop DataFormatServer");
                setState(DeviceState.FAULT);
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
    }
}
