package de.hzg.wpi.xenv.hq.configuration;

import com.google.common.base.Preconditions;
import de.hzg.wpi.xenv.hq.ant.AntProject;
import de.hzg.wpi.xenv.hq.ant.AntTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.server.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class ConfigurationManager {
    private final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    AntProject antProject = new AntProject("ant/build.xml");

    Configuration configuration;

    @Attribute
    public String[] getProfiles() throws IOException {
        Preconditions.checkState(Files.exists(Paths.get("configuration")));

        return Files.list(Paths.get("configuration/profiles")).map(
                path -> path.getFileName().toString()
        ).toArray(String[]::new);
    }

    @Attribute
    public String getNexusFile() {
        return "";
    }

    @Attribute
    public String getNexusMapping() {
        return "";
    }

    @Attribute
    public String getStatusServerXml() {
        return "";
    }

    @Attribute
    public String getPreExperimentDataCollectorYaml() {
        return "";
    }

    @Attribute
    public String getDataSources() {
        return "";
    }

    @Init
    public void init() throws IOException {
        Files.deleteIfExists(Paths.get("configuration"));

        new AntTaskExecutor("pull-configuration", antProject);

        logger.info("ConfigurationManager has been initialized.");
    }

    @Delete
    public void delete() {
        new AntTaskExecutor("push-configuration", antProject);
    }

    @Command
    public void updateConfiguration() throws Exception {
        new AntTaskExecutor("update-configuration", antProject);
    }

    @Command(inTypeDesc = "Configuration profile to load")
    public void loadConfiguration(String profile) throws Exception {
        this.configuration = Configuration.fromXml(Paths.get("profiles").resolve(profile).resolve("configuration.xml"));
    }

    @Command(inTypeDesc = "username" +
            "nxPath" +
            "continuous" +
            "src")
    public void createDataSource(String[] params) {
        String nxPath = params[1];
        DataSource result = new DataSource(
                nxPath,
                Boolean.parseBoolean(params[2]),
                params[3]
        );

        boolean wasAdded = configuration.addDataSource(result);
        Preconditions.checkState(wasAdded, "DataSource with nxPath=" + nxPath + " already exists!");
    }

    @Command(inTypeDesc = "username" +
            "nxPath")
    public String removeDataSource(String[] params) {
        DataSource result = new DataSource(
                params[1],
                false, ""
        );

        configuration.removeDataSource(result);


        return "Done.";//TODO
    }

    @Command
    public String applyConfiguration() {
        return "Done.";//TODO
    }
}
