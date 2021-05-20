package de.hzg.wpi.xenv.hq;

import com.google.common.base.Preconditions;
import de.hzg.wpi.xenv.hq.configuration.ConfigurationManager;
import de.hzg.wpi.xenv.hq.manager.XenvManager;
import fr.esrf.Tango.DevVarLongStringArray;
import fr.esrf.TangoApi.ApiUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.DeviceState;
import org.tango.server.ServerManager;
import org.tango.server.ServerManagerUtils;
import org.tango.server.annotation.*;
import org.tango.server.device.DeviceManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class HeadQuarter {
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

    public static void main(String[] args) throws Exception {
        ApiUtil.set_db_obj(System.getProperty("TANGO_HOST", "localhost:10000"));
        XenvManager.createTempDirectory();
        XenvManager.extractResources();
        setSystemProperties();

        ServerManager.getInstance().addClass("ConfigurationManager", ConfigurationManager.class);
        ServerManager.getInstance().addClass("XenvManager", XenvManager.class);
        ServerManager.getInstance().addClass("HeadQuarter", HeadQuarter.class);

        ServerManager.getInstance().start(args, HeadQuarter.class.getSimpleName());
    }



    @Init
    public void init() {
        xenvManagers = ServerManagerUtils.getBusinessObjects(ServerManager.getInstance().getInstanceName(), XenvManager.class);
        configurationManagers = ServerManagerUtils.getBusinessObjects(ServerManager.getInstance().getInstanceName(), ConfigurationManager.class);

        Preconditions.checkState(!xenvManagers.isEmpty());
        Preconditions.checkState(!configurationManagers.isEmpty());

        deviceManager.pushStateChangeEvent(DeviceState.ON);
        deviceManager.pushStatusChangeEvent("HeadQuarter has been initialized.");
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
                logger.error("Failed to delete directory " + s, e);
                deviceManager.pushStateChangeEvent(DeviceState.ALARM);
                deviceManager.pushStatusChangeEvent("Failed to delete directory " + s);
            }
        });

        Files.createDirectory(Paths.get("logs"));
        deviceManager.pushStateChangeEvent(DeviceState.ON);
    }

    @Attribute
    public String[] getXenvExecutables() {
        return XENV_EXECUTABLES;
    }

    static <T> Consumer<T> throwingConsumerWrapper(
            ThrowingConsumer<T, Exception> throwingConsumer) {

        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    @Command
    public void startAll() {
        Arrays.stream(XENV_EXECUTABLES)
                .flatMap(s -> xenvManagers.stream().map(xenvManager -> (Callable<Void>) () -> {
                    xenvManager.startServer(s);
                    return null;
                }))
                .parallel()
                .forEach(throwingConsumerWrapper(Callable::call));
    }

    @Command
    public void stopAll() {
        Arrays.stream(XENV_EXECUTABLES)
                .flatMap(s -> xenvManagers.stream().map(xenvManager -> (Callable<Void>) () -> {
                        xenvManager.stopServer(s);
                    return null;
                }))
                .parallel()
                .forEach(throwingConsumerWrapper(Callable::call));
    }

    @Command
    public void restartStatusServer() {
        xenvManagers.forEach(throwingConsumerWrapper(xenvManager -> {
            xenvManager.stopServer("status_server");
        }));

        configurationManagers.forEach(ConfigurationManager::writeStatusServerConfiguration);

        xenvManagers.forEach(throwingConsumerWrapper(xenvManager -> xenvManager.startServer("status_server")));
        logger.trace("Done.");
    }


    @Command
    public void updateProfileCollections(DevVarLongStringArray collections) {
        configurationManagers.forEach(configurationManager -> {
            try {
                configurationManager.selectCollections(collections);
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
    public void restartDataFormatServer() {
        xenvManagers.forEach(throwingConsumerWrapper(xenvManager -> {
                xenvManager.stopServer("data_format_server");
        }));

        configurationManagers.forEach(ConfigurationManager::writeDataFormatServerConfiguration);

        xenvManagers.forEach(throwingConsumerWrapper(xenvManager -> xenvManager.startServer("data_format_server")));
        logger.trace("Done.");
    }

    //TODO move to waltz XenvHQ server side
    @Command
    public void restartCamelIntegration() {
        xenvManagers.forEach(throwingConsumerWrapper(xenvManager -> {
                xenvManager.stopServer("camel_integration");
        }));


        configurationManagers.forEach(ConfigurationManager::writeCamelConfiguration);

        xenvManagers.forEach(throwingConsumerWrapper(xenvManager -> xenvManager.startServer("camel_integration")));
        logger.trace("Done.");
    }

    @Command
    public void restartPreExperimentDataCollector() {
        xenvManagers.forEach(throwingConsumerWrapper(xenvManager -> {
                xenvManager.stopServer("predator");
        }));

        configurationManagers.forEach(ConfigurationManager::writePreExperimentDataCollectorConfiguration);

        xenvManagers.forEach(throwingConsumerWrapper(xenvManager -> xenvManager.startServer("predator")));
        logger.trace("Done.");
    }

    @FunctionalInterface
    public static interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
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
