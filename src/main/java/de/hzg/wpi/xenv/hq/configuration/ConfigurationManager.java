package de.hzg.wpi.xenv.hq.configuration;

import com.google.common.base.Preconditions;
import de.hzg.wpi.xenv.hq.ant.AntProject;
import de.hzg.wpi.xenv.hq.ant.AntTaskExecutor;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelRoutesXml;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.NexusXml;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.NexusXmlGenerator;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.mapping.MappingGenerator;
import de.hzg.wpi.xenv.hq.configuration.status_server.StatusServerXml;
import de.hzg.wpi.xenv.hq.configuration.status_server.StatusServerXmlGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.DeviceState;
import org.tango.server.annotation.*;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class ConfigurationManager {
    public static final String PROFILES_ROOT = "configuration/profiles";
    public static final List<String> VALID_DATA_SOURCE_TYPES = Arrays.asList("scalar", "spectrum", "log");
    public static final String DATA_FORMAT_SERVER = "DataFormatServer";
    public static final String CAMEL_INTEGRATION = "CamelIntegration";
    public static final String ROUTES_XML = "routes.xml";
    public static final String TEMPLATE_NXDL_XML = "template.nxdl.xml";
    private final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    AntProject antProject = new AntProject("ant/build.xml");

    Configuration configuration;
    @Attribute(isMemorized = true)
    public String profile;
    @Attribute
    @AttributeProperties(format = "xml")
    public String nexusFileTemplate;
    @Attribute
    @AttributeProperties(format = "xml")
    public String camelRoutes;

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
    @AttributeProperties(format = "xml")
    public String getNexusFile() throws Exception {
        Preconditions.checkNotNull(configuration);

        NexusXml nexusXml = XmlHelper.fromXml(
                Paths.get(PROFILES_ROOT).resolve(configuration.profile).resolve(DATA_FORMAT_SERVER).resolve("template.nxdl.xml"), NexusXml.class);
        FutureTask<NexusXml> task = new FutureTask<>(
                new NexusXmlGenerator(configuration, nexusXml));
        task.run();

        return task.get().toXmlString();
    }

    public String getNexusFileTemplate() throws Exception {
        Preconditions.checkNotNull(configuration);

        return XmlHelper.fromXml(
                Paths.get(PROFILES_ROOT)
                        .resolve(configuration.profile)
                        .resolve(DATA_FORMAT_SERVER)
                        .resolve(TEMPLATE_NXDL_XML), NexusXml.class)
                .toXmlString();
    }

    public void setNexusFileTemplate(String nxFile) throws Exception {
        Preconditions.checkNotNull(configuration);

        XmlHelper.fromString(nxFile, NexusXml.class)
                .toXml(
                        Paths.get(PROFILES_ROOT)
                                .resolve(configuration.profile)
                                .resolve(DATA_FORMAT_SERVER)
                                .resolve(TEMPLATE_NXDL_XML));

        executorService.submit(new CommitAndPushConfigurationTask(System.getProperty("user.name", "unknown")));
    }


    public String getCamelRoutes() throws Exception {
        Preconditions.checkNotNull(configuration);

        return XmlHelper.fromXml(
                Paths.get(PROFILES_ROOT)
                        .resolve(configuration.profile)
                        .resolve(CAMEL_INTEGRATION)
                        .resolve(ROUTES_XML), CamelRoutesXml.class)
                .toXmlString();
    }

    public void setCamelRoutes(String xml) throws Exception {
        Preconditions.checkNotNull(configuration);

        XmlHelper.fromString(xml, CamelRoutesXml.class)
                .toXml(
                        Paths.get(PROFILES_ROOT)
                                .resolve(configuration.profile)
                                .resolve(CAMEL_INTEGRATION)
                                .resolve(ROUTES_XML));

        executorService.submit(new CommitAndPushConfigurationTask(System.getProperty("user.name", "unknown")));
    }

    @Attribute
    public String getNexusMapping() throws ExecutionException, InterruptedException, IOException {
        Preconditions.checkNotNull(configuration);

        StringWriter out = new StringWriter();

        FutureTask<Properties> task = new FutureTask<>(new MappingGenerator(configuration));
        task.run();
        task.get().store(out, null);

        return out.toString();
    }

    @Attribute
    public String getStatusServerXml() throws Exception {
        Preconditions.checkNotNull(configuration);

        FutureTask<StatusServerXml> task = new FutureTask<>(new StatusServerXmlGenerator(configuration));
        task.run();
        return task.get().toXmlString();
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
    @StateMachine(endState = DeviceState.ON)
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
    @StateMachine(endState = DeviceState.OFF)
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
        this.configuration = XmlHelper.fromXml(Paths.get(PROFILES_ROOT).resolve(profile).resolve("configuration.xml"), Configuration.class);
    }

    @Command(inTypeDesc = "username" +
            "nxPath" +
            "type[scalar|spectrum|log]" +
            "url" +
            "pollRate" +
            "dataType")
    public void createDataSource(String[] params) {
        URI nxPath = URI.create(params[1]);
        Preconditions.checkArgument(VALID_DATA_SOURCE_TYPES.contains(params[2]));
        URI src = URI.create(params[3]);


        DataSource result = new DataSource(
                nxPath.toString(),
                params[2],
                src.toString(),
                Integer.parseInt(params[4]),
                params[5]
        );

        configuration.addOrReplaceDataSource(result);
    }

    @Command(inTypeDesc = "username" +
            "nxPath")
    public void removeDataSource(String[] params) {
        DataSource result = new DataSource();
        result.nxPath = params[1];

        configuration.removeDataSource(result);
    }

    @Command
    public void store(String username) {
        new CommitAndPushConfigurationTask(username).run();
    }

    private class CommitAndPushConfigurationTask implements Runnable {
        String userName;

        public CommitAndPushConfigurationTask(String userName) {
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
