package de.hzg.wpi.xenv.hq.configuration;

import com.google.common.base.Preconditions;
import de.hzg.wpi.xenv.hq.ant.AntProject;
import de.hzg.wpi.xenv.hq.ant.AntTaskExecutor;
import de.hzg.wpi.xenv.hq.configuration.mapping.MappingGenerationTask;
import de.hzg.wpi.xenv.hq.configuration.mapping.MappingGenerator;
import de.hzg.wpi.xenv.hq.configuration.nexus.NexusXml;
import de.hzg.wpi.xenv.hq.configuration.nexus.NexusXmlGenerationTask;
import de.hzg.wpi.xenv.hq.configuration.nexus.NexusXmlGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.server.annotation.*;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class ConfigurationManager {
    public static final String PROFILES_ROOT = "configuration/profiles";
    private final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    AntProject antProject = new AntProject("ant/build.xml");

    Configuration configuration;
    @Attribute(isMemorized = true)
    public String profile;
    @Attribute
    @AttributeProperties(format = "xml")
    public String nexusFile;

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

    public String getNexusFile() throws Exception {
        Preconditions.checkNotNull(configuration);

        return NexusXml.fromXml(
                Paths.get(PROFILES_ROOT).resolve(configuration.profile).resolve(configuration.profile + ".nxdl.xml"))
                .toXmlString();
    }

    public void setNexusFile(String nxFile) throws Exception {
        Preconditions.checkNotNull(configuration);

        NexusXml
                .fromString(nxFile)
                .toXml(
                        Paths.get(PROFILES_ROOT)
                                .resolve(configuration.profile)
                                .resolve(configuration.profile + ".nxdl.xml"));

        executorService.submit(new CommitConfigurationTask(System.getProperty("user.name", "unknown")));
    }


    @Attribute
    public String getNexusMapping() throws ExecutionException, InterruptedException, IOException {
        Preconditions.checkNotNull(configuration);

        StringWriter out = new StringWriter();

        MappingGenerationTask task = new MappingGenerationTask(new MappingGenerator(configuration));
        task.run();
        task.get().store(out, null);

        return out.toString();
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
    @AttributeProperties(format = "xml")
    public String getConfigurationXml() throws Exception {
        return configuration.toXmlString();
    }

    @Init
    public void init() throws Exception {
        if (Files.exists(Paths.get("configuration"))) {
            update();
        } else {
            new AntTaskExecutor("clone-configuration", antProject).run();
        }

        if (profile != null)
            load();


        logger.info("ConfigurationManager has been initialized.");
    }

    @Delete
    public void delete() {
        new AntTaskExecutor("push-configuration", antProject).run();
    }

    @Command
    public void update() {
        new AntTaskExecutor("pull-configuration", antProject).run();
        new AntTaskExecutor("update-configuration", antProject).run();
    }

    @Command
    public void load() throws Exception {
        Preconditions.checkNotNull(profile);
        this.configuration = Configuration.fromXml(Paths.get(PROFILES_ROOT).resolve(profile).resolve("configuration.xml"));
    }

    @Command(inTypeDesc = "username" +
            "nxPath" +
            "type[scalar|spectrum|log]" +
            "src" +
            "pollRate" +
            "dataType")
    public void createDataSource(String[] params) {
        String nxPath = params[1];
        DataSource result = new DataSource(
                nxPath,
                params[2],
                params[3],
                Integer.parseInt(params[4]),
                params[5]
        );

        boolean wasAdded = configuration.addDataSource(result);//TODO update
        Preconditions.checkState(wasAdded, "DataSource with nxPath=" + nxPath + " already exists!");

        executorService.submit(() -> apply(params[0]));
    }

    @Command(inTypeDesc = "username" +
            "nxPath")
    public void removeDataSource(String[] params) {
        DataSource result = new DataSource();
        result.nxPath = params[1];

        configuration.removeDataSource(result);

        executorService.submit(() -> apply(params[0]));
    }

    @Command
    public void apply(String username) {
        //TODO configuration -> NexusFile; mapping; StatusServerXml; PredatorYaml etc

        new UpdateNexusFileTask().run();

        new CommitConfigurationTask(username).run();
    }

    //TODO do not put data sources into nxdl.xml
    private class UpdateNexusFileTask implements Runnable {
        @Override
        public void run() {
            try {
                NexusXml nexusXml = NexusXml.fromXml(
                        Paths.get(PROFILES_ROOT).resolve(configuration.profile).resolve(configuration.profile + ".nxdl.xml"));
                NexusXmlGenerationTask task = new NexusXmlGenerationTask(
                        new NexusXmlGenerator(configuration, nexusXml));
                task.run();
                task.get()
                        .toXml(
                                Paths.get(PROFILES_ROOT).resolve(configuration.profile).resolve(configuration.profile + ".nxdl.xml"));
            } catch (Exception e) {
                logger.error("Failed to save NexusFile", e);
            }
        }
    }

    private class CommitConfigurationTask implements Runnable {
        String userName;

        public CommitConfigurationTask(String userName) {
            this.userName = userName;
        }

        public void run() {
            try {
                configuration.toXml(Paths.get(PROFILES_ROOT).resolve(configuration.profile).resolve("configuration.xml"));
                antProject.getProject().setUserProperty("user.name", userName);
                new AntTaskExecutor("commit-configuration", antProject).run();
                new AntTaskExecutor("push-configuration", antProject).run();
            } catch (Exception e) {
                logger.error("Failed to save configuration.xml");
                //TODO send event
            }
        }
    }
}
