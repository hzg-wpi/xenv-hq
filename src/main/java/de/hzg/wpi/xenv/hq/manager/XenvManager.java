package de.hzg.wpi.xenv.hq.manager;

import com.google.common.base.Preconditions;
import de.hzg.wpi.xenv.hq.HeadQuarter;
import de.hzg.wpi.xenv.hq.ant.AntProject;
import de.hzg.wpi.xenv.hq.ant.AntTaskExecutor;
import de.hzg.wpi.xenv.hq.util.yaml.YamlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.server.annotation.Attribute;
import org.tango.server.annotation.Command;
import org.tango.server.annotation.Device;

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

    @Command
    public void load() throws IOException {
        Preconditions.checkNotNull(profile, "Set profile first!");

        configuration = YamlHelper.fromYamlFile(
                Paths.get(PROFILES_ROOT)
                        .resolve(profile)
                        .resolve(MANAGER_YML));
    }

    @Command
    public String startServer(String executable) {
        Preconditions.checkNotNull(configuration, "load configuration first!");

        //TODO prepare executable (ant copy)

        //TODO fetch .jar if does not exists
        //TODO start

        return "Done.";
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
