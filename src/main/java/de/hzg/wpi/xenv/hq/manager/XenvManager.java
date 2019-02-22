package de.hzg.wpi.xenv.hq.manager;

import com.google.common.base.Preconditions;
import de.hzg.wpi.xenv.hq.HeadQuarter;
import de.hzg.wpi.xenv.hq.ant.AntProject;
import de.hzg.wpi.xenv.hq.ant.AntTaskExecutor;
import de.hzg.wpi.xenv.hq.util.FilesHelper;
import de.hzg.wpi.xenv.hq.util.yaml.YamlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.DeviceState;
import org.tango.server.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static de.hzg.wpi.xenv.hq.HeadQuarter.PROFILES_ROOT;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class XenvManager {
    public static final String MANAGER_YML = "manager.yml";

    private final Logger logger = LoggerFactory.getLogger(XenvManager.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AntProject antProject = new AntProject("ant/build.xml");
    @Attribute(isMemorized = true)
    public String profile;
    private Configuration configuration;

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

        antProject.getProject().setProperty("tango_host", configuration.tango_host);
        antProject.getProject().setProperty("instance_name", configuration.instance_name);
        antProject.getProject().setProperty("tine_home", configuration.tine_home);
        antProject.getProject().setProperty("log_home", configuration.log_home);
        antProject.getProject().setProperty("log_level", configuration.log_level);
    }

    @Command(inTypeDesc = "status_server|data_format_server|camel_integration|predator")
    public String startServer(String executable) {
        Preconditions.checkNotNull(configuration, "load configuration first!");

        loadExecutable(executable);
        Runnable runnable = () -> {
            new AntTaskExecutor("prepare-executable", antProject).run();
            new AntTaskExecutor("fetch-executable-jar", antProject).run();
            new AntTaskExecutor("run-executable", antProject).run();

            //TODO send event
        };
        executorService.submit(runnable);

        return "Done.";
    }

    @Command(inTypeDesc = "status_server|data_format_server|camel_integration|predator")
    public String stopServer(String executable) {
        //TODO find pid
        //TODO kill server (use DServer?)

        return "Done.";
    }

    private void loadExecutable(String executable) {
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
    }

    private class CommitAndPushConfigurationTask implements Runnable {
        public void run() {
            try {
                YamlHelper.toYaml(configuration, Paths.get(HeadQuarter.PROFILES_ROOT).resolve(profile).resolve(MANAGER_YML));
                new AntTaskExecutor("commit-configuration", antProject).run();
                new AntTaskExecutor("push-configuration", antProject).run();
            } catch (Exception e) {
                logger.error("Failed to save configuration.xml");
                //TODO send event
            }
        }
    }
}
