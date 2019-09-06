package de.hzg.wpi.xenv.hq.configuration;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import de.hzg.wpi.xenv.hq.HeadQuarter;
import de.hzg.wpi.xenv.hq.ant.AntProject;
import de.hzg.wpi.xenv.hq.ant.AntTaskExecutor;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelRoutesXml;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.NexusXml;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.NexusXmlGenerator;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.mapping.MappingGenerator;
import de.hzg.wpi.xenv.hq.configuration.status_server.StatusServerXml;
import de.hzg.wpi.xenv.hq.configuration.status_server.StatusServerXmlGenerator;
import de.hzg.wpi.xenv.hq.manager.Manager;
import de.hzg.wpi.xenv.hq.profile.Profile;
import de.hzg.wpi.xenv.hq.profile.ProfileManager;
import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import de.hzg.wpi.xenv.hq.util.yaml.YamlHelper;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.tango.DeviceState;
import org.tango.server.ChangeEventPusher;
import org.tango.server.ServerManager;
import org.tango.server.StateChangeEventPusher;
import org.tango.server.annotation.*;
import org.tango.server.device.DeviceManager;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.eq;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class ConfigurationManager {
    public static final List<String> VALID_DATA_SOURCE_TYPES = Arrays.asList("scalar", "spectrum", "log");
    public static final String CONFIGURATION_XML = "configuration.xml";
    public static final String DATASOURCE_SRC_EXTERNAL = "external:";
    private final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private final ProfileManager profileManager = new ProfileManager();

    private final JsonWriterSettings settings = JsonWriterSettings.builder()
            .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
            .build();
    private Mongo mongo;
    private MongoCollection<Document> dataSources;
    private String collection;


    @DeviceManagement
    private DeviceManager deviceManager;
    @State(isPolled = true)
    private volatile DeviceState state;
    @Status(isPolled = true)
    private volatile String status;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    Profile profile;
    @Attribute
    @AttributeProperties(format = "xml")
    public String nexusFileTemplate;
    @Attribute
    @AttributeProperties(format = "xml")
    public String camelRoutes;
    @Attribute
    @AttributeProperties(format = "yaml")
    public String preExperimentDataCollectorYaml;
    public static final Path CONFIGURATION_PATH = Paths.get("configuration");

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

    @Attribute
    @AttributeProperties(format = "json")
    public String getProfile() {
        return new Gson().toJson(profile);
    }

    public DeviceState getState() {
        return state;
    }

    public void setState(DeviceState state) {
        this.state = state;
        new StateChangeEventPusher(state, deviceManager).run();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        logger.debug(status);
        this.status = status;
        new ChangeEventPusher<>("Status", status, deviceManager).run();
    }

    private NexusXml getNexusFile() throws Exception {
        Preconditions.checkNotNull(profile);
        NexusXml nexusXml = profile.getNexusTemplateXml();
        FutureTask<NexusXml> task = new FutureTask<>(
                new NexusXmlGenerator(profile.getDataSources(), nexusXml));
        task.run();

        return task.get();
    }

    @Attribute
    @AttributeProperties(format = "xml")
    public String getNexusFileXml() throws Exception {
        return getNexusFile().toXmlString();
    }

    @Attribute
    @AttributeProperties(format = "webix/xml")
    public String getNexusFileWebixXml() throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(getClass().getResourceAsStream("/nexus-to-webix.xslt"));
        Transformer transformer = factory.newTransformer(xslt);

        Source xml = new StreamSource(new StringReader(getNexusFileXml()));

        StringWriter result = new StringWriter();

        transformer.transform(xml, new StreamResult(result));

        return result.toString();
    }

    public String getNexusFileTemplate() throws Exception {
        Preconditions.checkNotNull(profile);

        return profile.getNexusTemplateXml().toXmlString();
    }

    public void setNexusFileTemplate(String nxFile) throws Exception {
        Preconditions.checkNotNull(profile);

        profile.setNexusFileTemplate(
                XmlHelper.fromString(nxFile, NexusXml.class));

        commit(System.getProperty("user.name", "unknown"));
    }



    @Attribute
    @AttributeProperties(format = "yml")
    private String xenvManagerConfiguration;


    public String getCamelRoutes() throws Exception {
        Preconditions.checkNotNull(profile);

        return profile.getCamelRoutesXml()
                .toXmlString();
    }

    public void setCamelRoutes(String xml) throws Exception {
        Preconditions.checkNotNull(profile);

        profile.setCamelRoutes(XmlHelper.fromString(xml, CamelRoutesXml.class));

        commit(System.getProperty("user.name", "unknown"));
    }

    @Attribute
    public String getNexusMapping() throws ExecutionException, InterruptedException, IOException {
        Preconditions.checkNotNull(profile);

        StringWriter out = new StringWriter();

        FutureTask<Properties> task = new FutureTask<>(new MappingGenerator(profile.getDataSources()));
        task.run();
        task.get().store(out, null);

        return out.toString();
    }

    @Attribute
    public String getStatusServerXml() throws Exception {
        Preconditions.checkNotNull(profile);

        FutureTask<StatusServerXml> task = new FutureTask<>(new StatusServerXmlGenerator(profile.getDataSources()));
        task.run();
        return task.get().toXmlString();
    }

    public String getPreExperimentDataCollectorYaml() throws Exception {
        Preconditions.checkNotNull(profile);

        Object yaml = profile.getPredatorYaml();
        return YamlHelper.toYamlString(yaml);
    }

    public void setPreExperimentDataCollectorYaml(String yamlString) throws Exception {
        Preconditions.checkNotNull(profile);

        Object yaml = YamlHelper.fromString(yamlString, Object.class);
        profile.setPredatorYaml(yaml);

        commit(System.getProperty("user.name", "unknown"));
    }

    @Attribute
    @AttributeProperties(format = "xml")
    public String getConfigurationXml() throws Exception {
        Preconditions.checkNotNull(profile);

        return profile.getConfiguration().toXmlString();
    }

    public String getPreExperimentDataCollectorLoginProperties() throws IOException {
        Preconditions.checkNotNull(profile);

        return profile.getPredatorLoginProperties();
    }

    public String getXenvManagerConfiguration() throws IOException {
        Preconditions.checkNotNull(profile);

        return YamlHelper.toYamlString(profile.manager);
    }

    public void setXenvManagerConfiguration(String yaml) throws Exception {
        Preconditions.checkNotNull(profile);

        profile.setManager(YamlHelper.fromString(yaml, Manager.class));

        commit(System.getProperty("user.name", "unknown"));
    }

    public static void main(String[] args) {
        ServerManager.getInstance().start(args, ConfigurationManager.class);
    }

    @Attribute
    public String getDataSourceCollections() {
        return new Gson().toJson(StreamSupport
                .stream(mongo.getMongoDb().listCollections().spliterator(), false)
                .map(document -> new Document("id", document.get("name")))
                .toArray());
    }

    @Attribute
    public void setDataSourcesCollection(String collection) {
        this.collection = collection;
    }

    @Attribute
    public String getDataSources() {
        Preconditions.checkState(collection != null);
        ;

        return new Gson().toJson(StreamSupport
                .stream(mongo.getMongoDb().getCollection(collection, DataSource.class).find().spliterator(), false)
                .toArray());
    }

    @Command(name = "clone")
    public void cloneConfiguration() {
        AntProject antProject = new AntProject(HeadQuarter.getAntRoot() + "/build.xml");
        new AntTaskExecutor("clone-configuration", antProject).run();
    }

    @Init
    @StateMachine(endState = DeviceState.STANDBY)
    public void init() throws Exception {
        mongo = new Mongo();

        if (Files.exists(CONFIGURATION_PATH)) {
            update();
        } else {
            cloneConfiguration();
        }

        profile = null;

        setStatus("ConfigurationManager has been initialized.");
    }

    @Delete
    @StateMachine(endState = DeviceState.OFF)
    public void delete() {
        mongo.close();

        AntProject antProject = newAntProject();
        new AntTaskExecutor("push-configuration", antProject).run();
    }

    @Command(inTypeDesc = "[collection,dataSource as JSON]")
    public void addDataSource(String[] args) {
        String collection = args[0];
        Document dataSource = Document.parse(args[1]);

        mongo.getMongoDb().getCollection(collection)
                .insertOne(dataSource);
    }

    @Command(inTypeDesc = "[collection,dataSource as JSON]")
    public void updateDataSource(String[] args) {
        String collection = args[0];
        Document dataSource = Document.parse(args[1]);

        mongo.getMongoDb().getCollection(collection)
                .findOneAndUpdate(eq("_id", dataSource.get("_id")), dataSource);
    }

    @Command
    public void update() {
        executorService.submit(new PullAndUpdateConfigurationTask());
    }

    @Command
    public void loadProfile(String name) throws Exception {
        Preconditions.checkNotNull(name);
        this.profile = profileManager.loadProfile(name);

        setState(DeviceState.ON);
        setStatus("Profile set to " + name);
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
        String src = DATASOURCE_SRC_EXTERNAL.equalsIgnoreCase(params[3]) ? params[3] : URI.create(params[3]).toString();


        DataSource result = new DataSource(
                Long.parseLong(params[0]),
                nxPath.toString(),
                params[2],
                src,
                Integer.parseInt(params[4]),
                params[5]
        );

        profile.addDataSource(result);

        profile.dumpConfiguration();
    }

    @Command(inTypeDesc = "id")
    public void removeDataSource(long id) throws Exception {
        DataSource result = new DataSource();
        result.id = id;

        profile.removeDataSource(result);

        profile.dumpConfiguration();
    }

    @Command(inTypeDesc = "username")
    public void commit(String username) {
        //TODO #10
        AntProject antProject = newAntProject();
        antProject.getProject().setUserProperty("user.name", username);
        new AntTaskExecutor("commit-configuration", antProject).run();
    }

    @Command(inTypeDesc = "username")
    public void push() {
        executorService.submit(
                new PullAndUpdateConfigurationTask());
        executorService.submit(
                new PushConfigurationTask());
    }

    @Command(inTypeDesc = "profile|host|instance")
    public void createProfile(String[] profileData) throws Exception {
        Preconditions.checkArgument(profileData.length == 3);
        profileManager.createProfile(profileData[0], profileData[1], profileData[2], this.profile);
    }

    @Command(inTypeDesc = "profile")
    public void deleteProfile(String profile) {
        new ProfileManager().deleteProfile(profile);

        if (this.profile != null && profile.equalsIgnoreCase(this.profile.name)) {
            this.profile = null;
            setStatus("Profile has been set to null");
            setState(DeviceState.STANDBY);
        }
    }

    private AntProject newAntProject() {
        AntProject antProject = new AntProject(HeadQuarter.getAntRoot() + "/build.xml");
        antProject.getProject().setBasedir(CONFIGURATION_PATH.toAbsolutePath().toString());
        return antProject;
    }

    private class PullAndUpdateConfigurationTask implements Runnable {
        public void run() {
            MDC.setContextMap(deviceManager.getDevice().getMdcContextMap());
            try {
                AntProject antProject = newAntProject();
                new AntTaskExecutor("pull-configuration", antProject).run();
                new AntTaskExecutor("update-configuration", antProject).run();
            } catch (Exception e) {
                logger.error("Failed to pull configuration");
                setState(DeviceState.ALARM);
            }
        }
    }

    private class PushConfigurationTask implements Runnable {
        public void run() {
            MDC.setContextMap(deviceManager.getDevice().getMdcContextMap());
            try {
                AntProject antProject = newAntProject();
                new AntTaskExecutor("push-configuration", antProject).run();
            } catch (Exception e) {
                logger.error("Failed to push configuration");
                setState(DeviceState.ALARM);
            }
        }
    }

    @Command(inTypeDesc = "[collection,id]")
    public void deleteDataSource(String[] args) {
        String collection = args[0];
        String id = args[1];

        mongo.getMongoDb().getCollection(collection)
                .deleteOne(
                        eq("_id", id));
    }
}
