package de.hzg.wpi.xenv.hq.configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import de.hzg.wpi.xenv.hq.HeadQuarter;
import de.hzg.wpi.xenv.hq.ant.AntProject;
import de.hzg.wpi.xenv.hq.ant.AntTaskExecutor;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelRoute;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelRoutesXml;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.NexusXml;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.NexusXmlGenerator;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.mapping.MappingGenerator;
import de.hzg.wpi.xenv.hq.configuration.mongo.CamelDb;
import de.hzg.wpi.xenv.hq.configuration.mongo.DataSourceDb;
import de.hzg.wpi.xenv.hq.configuration.status_server.StatusServerXml;
import de.hzg.wpi.xenv.hq.configuration.status_server.StatusServerXmlGenerator;
import de.hzg.wpi.xenv.hq.manager.Manager;
import de.hzg.wpi.xenv.hq.profile.Profile;
import de.hzg.wpi.xenv.hq.profile.ProfileManager;
import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import de.hzg.wpi.xenv.hq.util.yaml.YamlHelper;
import fr.esrf.Tango.DevVarLongStringArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.tango.DeviceState;
import org.tango.server.ServerManager;
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
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    private final boolean parallel = false;
    private DataSourceDb dataSourcesDb;
    private MongoCollection<Document> dataSources;
    private String collection;


    @DeviceManagement
    private DeviceManager deviceManager;
    @State(isPolled = true, pollingPeriod = 3000)
    private volatile DeviceState state;
    @Status(isPolled = true, pollingPeriod = 3000)
    private volatile String status;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    Profile profile;
    @Attribute
    @AttributeProperties(format = "xml")
    public String nexusFileTemplate;

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
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = String.format("%d: %s", System.currentTimeMillis(), status);
    }

    private NexusXml getNexusFile() throws Exception {
        Preconditions.checkNotNull(profile);
        NexusXml nexusXml = profile.getNexusTemplateXml();
        FutureTask<NexusXml> task = new FutureTask<>(
                new NexusXmlGenerator(getDataSources(profile), nexusXml));
        task.run();

        return task.get();
    }

    private CamelDb camelDb;

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

    @Attribute
    @AttributeProperties(format = "xml")
    public String getNexusFileXml() throws Exception {
        return XmlHelper.toXmlString(getNexusFile());
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

    public String getNexusFileTemplate() throws Exception {
        Preconditions.checkNotNull(profile);

        return XmlHelper.toXmlString(profile.getNexusTemplateXml());
    }

    @Attribute
    @AttributeProperties(format = "xml")
    public String getCamelRoutesXml() {
        CamelRoutesXml result = new CamelRoutesXml();

        result.routes = StreamSupport.stream(camelDb.getRoutes()
                .find()
                .spliterator(), parallel)
                .collect(Collectors.toList());

        return XmlHelper.toXmlString(result);
    }

    @Attribute
    public String[] getCamelRoutes() {
        return StreamSupport.stream(camelDb.getRoutes()
                .find()
                .spliterator(), parallel)
                .map(camelRoute -> camelRoute.id)
                .toArray(String[]::new);
    }

    @Command
    public String getCamelRoute(String id) {
        return StreamSupport.stream(camelDb.getRoutes()
                .find(new BsonDocument("_id", new BsonString(id)))
                .spliterator(), parallel)
                .map(XmlHelper::toXmlString)
                .findFirst().orElseThrow(() -> new NoSuchElementException(id));
    }

    @Command
    public void addOrReplaceCamelRoute(String camelRouteXml) throws Exception {
        CamelRoute update = XmlHelper.fromString(camelRouteXml, CamelRoute.class);

        camelDb.getRoutes()
                .findOneAndReplace(new BsonDocument("_id", new BsonString(update.id)), update, new FindOneAndReplaceOptions().upsert(true));
    }

    @Command
    public void deleteCamelRoute(String id) {
        camelDb.getRoutes()
                .deleteOne(new BsonDocument("_id", new BsonString(id)));
    }

    @Attribute
    public String getNexusMapping() throws ExecutionException, InterruptedException, IOException {
        Preconditions.checkNotNull(profile);

        StringWriter out = new StringWriter();

        FutureTask<Properties> task = new FutureTask<>(new MappingGenerator(getDataSources(profile)));
        task.run();
        task.get().store(out, null);

        return out.toString();
    }

    @Attribute
    public String getStatusServerXml() throws Exception {
        Preconditions.checkNotNull(profile);

        FutureTask<StatusServerXml> task = new FutureTask<>(new StatusServerXmlGenerator(getDataSources(profile)));
        task.run();
        return XmlHelper.toXmlString(task.get());
    }

    private List<DataSource> getDataSources(Profile profile) {
        return profile.configuration.collections.stream()
                .filter(collection -> collection.value == 1)
                .map(collection -> dataSourcesDb.getCollection(collection.id))
                .flatMap(dataSourceMongoCollection -> StreamSupport.stream(dataSourceMongoCollection.find().spliterator(), parallel))
                .collect(Collectors.toList());
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

        return XmlHelper.toXmlString(profile.getConfiguration());
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
                .stream(dataSourcesDb.getMongoDb().listCollections().spliterator(), false)
                .map(document -> new Document("id", document.get("name")).append("value",document.get("name")))
                .toArray());
    }

    @Command(inTypeDesc = "collectionId")
    public void deleteCollection(String collectionId) {
        dataSourcesDb.getMongoDb().getCollection(collectionId).drop();
    }

    @Command(inTypeDesc = "[collectionId, sourceId]")
    public void cloneCollection(String[] args) {
        String targetId = args[0];
        String sourceId = args[1];
        Preconditions.checkArgument(!targetId.isEmpty());
        Preconditions.checkArgument(!sourceId.isEmpty());

        dataSourcesDb.getMongoDb().getCollection(targetId);
        dataSourcesDb.getMongoDb().getCollection(sourceId)
                .find()
                .forEach((Block<Document>) dataSourcesDb.getMongoDb().getCollection(targetId)::insertOne);
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
                .stream(dataSourcesDb.getMongoDb().getCollection(collection, DataSource.class).find().spliterator(), false)
                .toArray());
    }

    @Command(name = "clone")
    public void cloneConfiguration() {
        AntProject antProject = new AntProject(HeadQuarter.getAntRoot() + "/build.xml");
        new AntTaskExecutor("clone-configuration", antProject).run();
    }


    @Init
    public void init() throws Exception {
        dataSourcesDb = new DataSourceDb();
        camelDb = new CamelDb();

        if (Files.exists(CONFIGURATION_PATH)) {
            update();
        } else {
            cloneConfiguration();
        }

        profile = null;

        deviceManager.pushStateChangeEvent(DeviceState.STANDBY);
        deviceManager.pushStatusChangeEvent("ConfigurationManager has been initialized.");
    }

    @Delete
    public void delete() {
        dataSourcesDb.close();

        AntProject antProject = newAntProject();
        new AntTaskExecutor("push-configuration", antProject).run();
        deviceManager.pushStateChangeEvent(DeviceState.OFF);
        deviceManager.pushStatusChangeEvent(DeviceState.OFF.name());
    }

    @Command(inTypeDesc = "dataSource as JSON")
    public void insertDataSource(String arg) {
        Preconditions.checkState(collection != null, "Collection must be set prior this operation!");
        DataSource dataSource = new Gson().fromJson(arg,DataSource.class);

        dataSourcesDb.getMongoDb().getCollection(collection, DataSource.class)
                .insertOne(dataSource);
    }

    @Command(inTypeDesc = "dataSource as JSON")
    public void updateDataSource(String arg) {
        Preconditions.checkState(collection != null, "Collection must be set prior this operation!");
        DataSource dataSource = new Gson().fromJson(arg, DataSource.class);

        dataSourcesDb.getMongoDb().getCollection(collection, DataSource.class)
                .replaceOne(new Document("_id", dataSource.id), dataSource);
    }

    @Command(inTypeDesc = "dataSource as JSON")
    public void deleteDataSource(String arg) {
        Preconditions.checkState(collection != null, "Collection must be set prior this operation!");
        DataSource dataSource = new Gson().fromJson(arg, DataSource.class);

        dataSourcesDb.getMongoDb().getCollection(collection, DataSource.class)
                .deleteOne(new Document("_id", dataSource.id));
    }

    @Command
    public void update() {
        executorService.submit(new PullAndUpdateConfigurationTask());
    }

    @Command
    public void loadProfile(String name) throws Exception {
        Preconditions.checkNotNull(name);
        this.profile = profileManager.loadProfile(name);

        deviceManager.pushStateChangeEvent(DeviceState.ON);
        deviceManager.pushStatusChangeEvent("Profile set to " + name);
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

    @Command
    public void updateProfileCollections(DevVarLongStringArray collections) throws Exception {
        Preconditions.checkState(profile != null);
        ;
        Preconditions.checkArgument(collections.lvalue.length == collections.svalue.length);
        List<Collection> result = Lists.newArrayListWithCapacity(collections.lvalue.length);
        for (int i = 0, size = collections.lvalue.length; i < size; ++i) {
            result.add(new Collection(collections.svalue[i], collections.lvalue[i]));
        }

        profile.configuration.collections = result;

        profile.dumpConfiguration();
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
                deviceManager.pushStateChangeEvent(DeviceState.ALARM);
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
                deviceManager.pushStateChangeEvent(DeviceState.ALARM);
            }
        }
    }
}
