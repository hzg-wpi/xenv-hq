package de.hzg.wpi.xenv.hq.manager;

import com.google.common.base.Preconditions;
import de.hzg.wpi.xenv.hq.HeadQuarter;
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
import java.util.concurrent.Executors;

import static de.hzg.wpi.xenv.hq.HeadQuarter.PROFILES_ROOT;
import static de.hzg.wpi.xenv.hq.HeadQuarter.XENV_HQ_TMP_DIR;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class XenvManager {
    public static final String MANAGER_YML = "manager.yml";

    @DeviceManagement
    private DeviceManager deviceManager;

    private final Logger logger = LoggerFactory.getLogger(XenvManager.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    @Attribute(isMemorized = true)
    public String profile;
    private Configuration configuration;

    public void setDeviceManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @Attribute
    public String[] getProfiles() throws IOException {
        Preconditions.checkState(Files.exists(Paths.get("configuration")));

        return Files.list(Paths.get(PROFILES_ROOT)).map(
                path -> path.getFileName().toString()
        ).toArray(String[]::new);
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    @Attribute
    public String getConfiguration() {
        return YamlHelper.toYamlString(configuration);
    }

    @Attribute
    public void setConfiguration(String yaml) {
        configuration = YamlHelper.fromString(yaml);

        executorService.submit(
                new CommitAndPushConfigurationTask());
    }

    @Init
    @StateMachine(endState = DeviceState.ON)
    public void init() throws IOException {
        FilesHelper.createIfNotExists("bin");
        FilesHelper.createIfNotExists("logs");
        FilesHelper.createIfNotExists("etc");
    }

    @Command
    public void load() throws IOException {
        Preconditions.checkNotNull(profile, "Set profile first!");

        configuration = YamlHelper.fromYamlFile(
                Paths.get(PROFILES_ROOT)
                        .resolve(profile)
                        .resolve(MANAGER_YML));


    }

    @Command(inTypeDesc = "status_server|data_format_server|camel_integration|predator")
    public String startServer(String executable) {
        Preconditions.checkNotNull(configuration, "load configuration first!");


        Runnable runnable = () -> {
            MDC.setContextMap(deviceManager.getDevice().getMdcContextMap());

            AntProject antProject = new AntProject(System.getProperty(XENV_HQ_TMP_DIR) + "ant/build.xml");

            populateAntProjectWithProperties(configuration, executable, antProject);

            new AntTaskExecutor("prepare-executable", antProject).run();
            new AntTaskExecutor("fetch-executable-jar", antProject).run();
            new AntTaskExecutor("run-executable", antProject).run();

            //TODO send event
        };
        executorService.execute(runnable);

        return "Done.";
    }

    @Command(inTypeDesc = "status_server|data_format_server|camel_integration|predator")
    public String stopServer(String executable) throws DevFailed, NoSuchCommandException, TangoProxyException {
        Preconditions.checkNotNull(configuration, "load configuration first!");

        AntProject antProject = new AntProject(System.getProperty(XENV_HQ_TMP_DIR) + "ant/build.xml");

        TangoServer tangoServer = populateAntProjectWithProperties(configuration, executable, antProject);

        String shortClassName = ClassUtils.getShortClassName(tangoServer.main_class);

        Path pidFile = Paths.get("bin")
                .resolve(
                        shortClassName + ".pid");

        if (!Files.exists(pidFile))
            tryToKillViaDServer(shortClassName);
        try {
            String pid = new String(
                    Files.readAllBytes(
                            pidFile));

            antProject.getProject().setProperty("pid", pid);
            new AntTaskExecutor("kill-executable", antProject).run();
        } catch (IOException | BuildException e) {
            logger.warn("Failed to kill executable by pid due to exception", e);
            tryToKillViaDServer(shortClassName);
        }

        return "Done.";
    }

    private void tryToKillViaDServer(String shortClassName) throws TangoProxyException, DevFailed, org.tango.client.ez.proxy.NoSuchCommandException {
        logger.info("Trying to kill via DServer");

        TangoProxy dserver = TangoProxies.newDeviceProxyWrapper(
                DeviceProxyFactory.get("dserver/" + shortClassName + "/" + configuration.instance_name, configuration.tango_host));

        dserver.executeCommand("Kill");
    }

    private TangoServer populateAntProjectWithProperties(Configuration configuration, String executable, AntProject antProject) {
        antProject.getProject().setProperty("tango_host", configuration.tango_host);
        antProject.getProject().setProperty("instance_name", configuration.instance_name);
        antProject.getProject().setProperty("tine_home", configuration.tine_home);
        antProject.getProject().setProperty("log_home", configuration.log_home);
        antProject.getProject().setProperty("log_level", configuration.log_level);
        antProject.getProject().setProperty("executable", executable);

        TangoServer tangoServer;
        switch (executable) {
            case "status_server":
                tangoServer = configuration.status_server;
                break;
            case "data_format_server":
                tangoServer = configuration.data_format_server;
                break;
            case "camel_integration":
                tangoServer = configuration.camel_integration;
                break;
            case "predator":
                tangoServer = configuration.predator;
                break;
            default:
                throw new IllegalArgumentException("Unknown executable: " + executable);
        }

        antProject.getProject().setProperty("server_name", tangoServer.server_name);
        antProject.getProject().setProperty("main_class", tangoServer.main_class);
        antProject.getProject().setProperty("version", tangoServer.version);
        antProject.getProject().setProperty("jmx_port", tangoServer.jmx_port);
        antProject.getProject().setProperty("url", tangoServer.url);

        return tangoServer;
    }

    private class CommitAndPushConfigurationTask implements Runnable {
        public void run() {
            MDC.setContextMap(deviceManager.getDevice().getMdcContextMap());
            try {
                YamlHelper.toYaml(configuration, Paths.get(HeadQuarter.PROFILES_ROOT).resolve(profile).resolve(MANAGER_YML));
                AntProject antProject = new AntProject(System.getProperty(XENV_HQ_TMP_DIR) + "ant/build.xml");
                new AntTaskExecutor("commit-configuration", antProject).run();
                new AntTaskExecutor("push-configuration", antProject).run();
            } catch (Exception e) {
                logger.error("Failed to save configuration.xml");
                //TODO send event
            }
        }
    }
}
