package de.hzg.wpi.xenv.hq.manager;

import com.google.common.util.concurrent.MoreExecutors;
import de.hzg.wpi.xenv.hq.ant.AntProject;
import de.hzg.wpi.xenv.hq.ant.AntTaskExecutor;
import de.hzg.wpi.xenv.hq.util.FilesHelper;
import de.hzg.wpi.xenv.hq.util.yaml.YamlHelper;
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.DeviceProxyFactory;
import org.apache.commons.lang3.ClassUtils;
import org.apache.tools.ant.BuildException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.tango.DeviceState;
import org.tango.client.ez.proxy.NoSuchCommandException;
import org.tango.client.ez.proxy.TangoProxies;
import org.tango.client.ez.proxy.TangoProxy;
import org.tango.client.ez.proxy.TangoProxyException;
import org.tango.server.annotation.*;
import org.tango.server.device.DeviceManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

import static de.hzg.wpi.xenv.hq.HeadQuarter.XENV_HQ_TMP_DIR;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class XenvManager {
    @DeviceManagement
    private DeviceManager deviceManager;

    @State(isPolled = true, pollingPeriod = 3000)
    private volatile DeviceState state;
    @Status(isPolled = true, pollingPeriod = 3000)
    private volatile String status;

    private final Logger logger = LoggerFactory.getLogger(XenvManager.class);
    private final ExecutorService executorService = MoreExecutors.newDirectExecutorService();
    private Manager configuration;

    private TangoServers servers;

    public void setDeviceManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @Attribute
    public String getTangoHost() {
        return configuration.tango_host;
    }

    @Attribute
    public String getInstanceName() {
        return configuration.instance_name;
    }

    @Init
    @StateMachine(endState = DeviceState.STANDBY)
    public void init() throws IOException {
        FilesHelper.createIfNotExists("bin");
        FilesHelper.createIfNotExists("logs");
        FilesHelper.createIfNotExists("etc");

        configuration = YamlHelper.fromYamlFile(
                Paths.get("config/manager.yml"),
                Manager.class
        );
        ;

        servers = YamlHelper.fromYamlFile(
                Paths.get("config/xenv-servers.yml"),
                TangoServers.class
        );
    }

    @Delete
    public void delete(){
        deviceManager.pushStateChangeEvent(DeviceState.OFF);
        deviceManager.pushStatusChangeEvent(DeviceState.OFF.name());
    }

    @Command(inTypeDesc = "status_server|data_format_server|camel_integration|predator")
    public void startServer(String executable) {
        Runnable runnable = () -> {
            MDC.setContextMap(deviceManager.getDevice().getMdcContextMap());

            AntProject antProject = new AntProject(System.getProperty(XENV_HQ_TMP_DIR) + "/build.xml");

            populateAntProjectWithProperties(configuration, executable, antProject);

            new AntTaskExecutor("prepare-executable", antProject).run();
            new AntTaskExecutor("fetch-executable-jar", antProject).run();
            new AntTaskExecutor("run-executable", antProject).run();

            deviceManager.pushStatusChangeEvent(String.format("Server %s has been launched.", executable));
        };
        executorService.execute(runnable);
    }

    @Command(inTypeDesc = "status_server|data_format_server|camel_integration|predator")
    public void stopServer(String executable) throws DevFailed, NoSuchCommandException, TangoProxyException {
        AntProject antProject = new AntProject(System.getProperty(XENV_HQ_TMP_DIR) + "/build.xml");

        TangoServer tangoServer = populateAntProjectWithProperties(configuration, executable, antProject);

        String shortClassName = ClassUtils.getShortClassName(tangoServer.main_class);

        Path pidFile = Paths.get("bin")
                .resolve(
                        shortClassName + ".pid");

        if (Files.exists(pidFile)) {
            try {
                String pid = new String(
                        Files.readAllBytes(
                                pidFile));

                antProject.getProject().setProperty("pid", pid);
                new AntTaskExecutor("kill-executable", antProject).run();
            } catch (IOException | BuildException e) {
                logger.warn("Failed to kill executable by pid due to exception", e);
                new AntTaskExecutor("force-kill-executable", antProject).run();
            }
        } else {
            logger.warn("{} file does not exists. Trying to kill {} via DServer",pidFile.toString(),executable);
            tryToKillViaDServer(shortClassName);
        }

        deviceManager.pushStatusChangeEvent(String.format("Server %s has been stopped", executable));
    }

    private void tryToKillViaDServer(String shortClassName) throws TangoProxyException, DevFailed, org.tango.client.ez.proxy.NoSuchCommandException {
        TangoProxy dserver = TangoProxies.newDeviceProxyWrapper(
                DeviceProxyFactory.get("dserver/" + shortClassName + "/" + configuration.instance_name, configuration.tango_host));

        dserver.executeCommand("Kill");
    }

    private TangoServer populateAntProjectWithProperties(Manager configuration, String executable, AntProject antProject) {
        antProject.getProject().setProperty("executable_template_dir", System.getProperty(XENV_HQ_TMP_DIR));
        antProject.getProject().setProperty("tango_host", configuration.tango_host);
        antProject.getProject().setProperty("instance_name", configuration.instance_name);
        antProject.getProject().setProperty("tine_home", configuration.tine_home);
        antProject.getProject().setProperty("log_home", configuration.log_home);
        antProject.getProject().setProperty("log_level", configuration.log_level);
        antProject.getProject().setProperty("xenv_root", System.getProperty("user.dir"));
        antProject.getProject().setProperty("executable", executable);

        TangoServer tangoServer;
        switch (executable) {
            case "status_server":
                tangoServer = servers.status_server;
                break;
            case "data_format_server":
                tangoServer = servers.data_format_server;
                break;
            case "camel_integration":
                tangoServer = servers.camel_integration;
                break;
            case "predator":
                tangoServer = servers.predator;
                break;
            default:
                throw new IllegalArgumentException("Unknown executable: " + executable);
        }

        antProject.getProject().setProperty("server_name", tangoServer.server_name);
        antProject.getProject().setProperty("main_class", tangoServer.main_class);
        antProject.getProject().setProperty("version", tangoServer.version);
        antProject.getProject().setProperty("jmx_port", tangoServer.jmx_port);
        antProject.getProject().setProperty("ram", tangoServer.ram);
        antProject.getProject().setProperty("url", tangoServer.url);

        return tangoServer;
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
