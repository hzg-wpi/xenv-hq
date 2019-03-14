package de.hzg.wpi.xenv.hq.configuration;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import de.hzg.wpi.xenv.hq.HeadQuarter;
import de.hzg.wpi.xenv.hq.ant.AntProject;
import de.hzg.wpi.xenv.hq.ant.AntTaskExecutor;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelRoutesXml;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.NexusXml;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.NexusXmlGenerator;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.mapping.MappingGenerator;
import de.hzg.wpi.xenv.hq.configuration.status_server.StatusServerXml;
import de.hzg.wpi.xenv.hq.configuration.status_server.StatusServerXmlGenerator;
import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import de.hzg.wpi.xenv.hq.util.yaml.YamlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.tango.DeviceState;
import org.tango.server.annotation.*;
import org.tango.server.device.DeviceManager;

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

import static de.hzg.wpi.xenv.hq.HeadQuarter.PROFILES_ROOT;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class ConfigurationManager {
    public static final List<String> VALID_DATA_SOURCE_TYPES = Arrays.asList("scalar", "spectrum", "log");
    public static final String DATA_FORMAT_SERVER = "DataFormatServer";
    public static final String CAMEL_INTEGRATION = "CamelIntegration";
    public static final String ROUTES_XML = "routes.xml";
    public static final String TEMPLATE_NXDL_XML = "template.nxdl.xml";
    public static final String PRE_EXPERIMENT_DATA_COLLECTOR = "PreExperimentDataCollector";
    public static final String META_YAML = "meta.yaml";
    public static final String CONFIGURATION_XML = "configuration.xml";
    public static final String LOGIN_PROPERTIES = "login.properties";
    private final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    @DeviceManagement
    private DeviceManager deviceManager;
    @State
    private volatile DeviceState state;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final AntProject antProject = new AntProject(HeadQuarter.getAntRoot() + "/build.xml");

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
    @AttributeProperties(format = "yaml")
    public String preExperimentDataCollectorYaml;

    @Attribute
    public String[] getProfiles() throws IOException {
        Preconditions.checkState(Files.exists(Paths.get("configuration")));

        return Files.list(Paths.get(HeadQuarter.PROFILES_ROOT)).map(
                path -> path.getFileName().toString()
        ).toArray(String[]::new);
    }

    public void setDeviceManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public DeviceState getState() {
        return state;
    }

    public void setState(DeviceState state) {
        this.state = state;
    }

    @Attribute
    @AttributeProperties(format = "xml")
    public String getNexusFile() throws Exception {
        Preconditions.checkNotNull(configuration);

        NexusXml nexusXml = XmlHelper.fromXml(
                Paths.get(HeadQuarter.PROFILES_ROOT).resolve(configuration.profile).resolve(DATA_FORMAT_SERVER).resolve("template.nxdl.xml"), NexusXml.class);
        FutureTask<NexusXml> task = new FutureTask<>(
                new NexusXmlGenerator(configuration, nexusXml));
        task.run();

        return task.get().toXmlString();
    }

    public String getNexusFileTemplate() throws Exception {
        Preconditions.checkNotNull(configuration);

        return XmlHelper.fromXml(
                Paths.get(HeadQuarter.PROFILES_ROOT)
                        .resolve(configuration.profile)
                        .resolve(DATA_FORMAT_SERVER)
                        .resolve(TEMPLATE_NXDL_XML), NexusXml.class)
                .toXmlString();
    }

    public void setNexusFileTemplate(String nxFile) throws Exception {
        Preconditions.checkNotNull(configuration);

        XmlHelper.fromString(nxFile, NexusXml.class)
                .toXml(
                        Paths.get(HeadQuarter.PROFILES_ROOT)
                                .resolve(configuration.profile)
                                .resolve(DATA_FORMAT_SERVER)
                                .resolve(TEMPLATE_NXDL_XML));

        executorService.submit(new CommitAndPushConfigurationTask(System.getProperty("user.name", "unknown")));
    }


    public String getCamelRoutes() throws Exception {
        Preconditions.checkNotNull(configuration);

        return XmlHelper.fromXml(
                Paths.get(HeadQuarter.PROFILES_ROOT)
                        .resolve(configuration.profile)
                        .resolve(CAMEL_INTEGRATION)
                        .resolve(ROUTES_XML), CamelRoutesXml.class)
                .toXmlString();
    }

    public void setCamelRoutes(String xml) throws Exception {
        Preconditions.checkNotNull(configuration);

        XmlHelper.fromString(xml, CamelRoutesXml.class)
                .toXml(
                        Paths.get(HeadQuarter.PROFILES_ROOT)
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

    public String getPreExperimentDataCollectorYaml() throws IOException {
        Object yaml = YamlHelper.fromYamlFile(
                Paths.get(HeadQuarter.PROFILES_ROOT)
                        .resolve(configuration.profile)
                        .resolve(PRE_EXPERIMENT_DATA_COLLECTOR)
                        .resolve(META_YAML), Object.class);
        return YamlHelper.toYamlString(yaml);
    }

    public void setPreExperimentDataCollectorYaml(String yamlString) throws Exception {
        Object yaml = YamlHelper.fromString(yamlString, Object.class);
        YamlHelper.toYaml(yaml, Paths.get(HeadQuarter.PROFILES_ROOT)
                .resolve(configuration.profile)
                .resolve(PRE_EXPERIMENT_DATA_COLLECTOR)
                .resolve(META_YAML));

        executorService.submit(new CommitAndPushConfigurationTask(System.getProperty("user.name", "unknown")));
    }

    public String getPreExperimentDataCollectorLoginProperties() throws IOException {
        return new String(
                Files.readAllBytes(
                        Paths.get(HeadQuarter.PROFILES_ROOT)
                                .resolve(configuration.profile)
                                .resolve(PRE_EXPERIMENT_DATA_COLLECTOR)
                                .resolve("login.properties")));
    }

    @Attribute
    @AttributeProperties(format = "xml")
    public String getConfigurationXml() throws Exception {
        Preconditions.checkNotNull(configuration);

        return configuration.toXmlString();
    }

    @Attribute
    public String[] getDataSources() throws Exception {
        Preconditions.checkNotNull(configuration);

        return configuration.dataSourceList.stream()
                .map(dataSource -> new Gson().toJson(dataSource)).toArray(String[]::new);
    }


    @Init
    @StateMachine(endState = DeviceState.STANDBY)
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
    @StateMachine(endState = DeviceState.ON)
    public void load() throws Exception {
        Preconditions.checkNotNull(profile);
        this.configuration = XmlHelper.fromXml(Paths.get(HeadQuarter.PROFILES_ROOT).resolve(profile).resolve(CONFIGURATION_XML), Configuration.class);
    }

    @Command(inTypeDesc =
            "id\n" +
                    "nxPath\n" +
                    "type[scalar|spectrum|log]\n" +
                    "url\n" +
                    "pollRate\n" +
                    "dataType\n")
    public void createDataSource(String[] params) throws Exception {
        URI nxPath = URI.create(params[1]);
        Preconditions.checkArgument(VALID_DATA_SOURCE_TYPES.contains(params[2]));
        URI src = URI.create(params[3]);


        DataSource result = new DataSource(
                Long.parseLong(params[0]),
                nxPath.toString(),
                params[2],
                src.toString(),
                Integer.parseInt(params[4]),
                params[5]
        );

        configuration.addOrReplaceDataSource(result);

        configuration.toXml(Paths.get(PROFILES_ROOT).resolve(profile).resolve(CONFIGURATION_XML));
    }

    @Command(inTypeDesc = "id")
    public void removeDataSource(long id) throws Exception {
        DataSource result = new DataSource();
        result.id = id;

        configuration.removeDataSource(result);

        configuration.toXml(Paths.get(PROFILES_ROOT).resolve(profile).resolve(CONFIGURATION_XML));
    }

    @Command(inTypeDesc = "username")
    public void store(String username) {
        executorService.submit(
                new CommitAndPushConfigurationTask(username));
    }

    @Command(inTypeDesc = "profile|host|instance")
    public void createProfile(String[] profile) throws Exception {
        ProfileManager.Profile profile1 = new ProfileManager.Profile(profile[0], profile[1], profile[2]);
        ProfileManager creator = new ProfileManager();
        creator.createProfile(profile1, configuration);
    }

    @Command(inTypeDesc = "profile")
    public void deleteProfile(String profile) {
        new ProfileManager().deleteProfile(new ProfileManager.Profile(profile, null, null));

        if (profile.equalsIgnoreCase(this.profile)) {
            this.profile = null;
            this.configuration = null;
            setState(DeviceState.STANDBY);
        }
    }



    private class CommitAndPushConfigurationTask implements Runnable {
        String userName;

        public CommitAndPushConfigurationTask(String userName) {
            this.userName = userName;
        }

        public void run() {
            MDC.setContextMap(deviceManager.getDevice().getMdcContextMap());
            try {
                antProject.getProject().setUserProperty("user.name", userName);
                new AntTaskExecutor("commit-configuration", antProject).run();
                new AntTaskExecutor("push-configuration", antProject).run();
            } catch (Exception e) {
                logger.error("Failed to save configuration.xml");
                setState(DeviceState.ALARM);
            }
        }
    }
}
