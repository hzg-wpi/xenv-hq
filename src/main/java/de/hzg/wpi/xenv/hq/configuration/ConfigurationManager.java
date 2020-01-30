package de.hzg.wpi.xenv.hq.configuration;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelManager;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelRoute;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.NexusXml;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.NexusXmlGenerator;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.mapping.MappingGenerator;
import de.hzg.wpi.xenv.hq.configuration.mongo.CamelDb;
import de.hzg.wpi.xenv.hq.configuration.mongo.CollectionsDb;
import de.hzg.wpi.xenv.hq.configuration.mongo.DataSourceDb;
import de.hzg.wpi.xenv.hq.configuration.mongo.PredatorDb;
import de.hzg.wpi.xenv.hq.configuration.predator.PredatorManager;
import de.hzg.wpi.xenv.hq.configuration.status_server.StatusServerXml;
import de.hzg.wpi.xenv.hq.configuration.status_server.StatusServerXmlGenerator;
import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import fr.esrf.Tango.DevVarLongStringArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
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
    public static final String TEMPLATE_NXDL_XML = "template.nxdl.xml";
    public static final String DATASOURCE_SRC_EXTERNAL = "external:";
    private final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    private final boolean nonParallelStream = false;
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

    @Attribute
    @AttributeProperties(format = "xml")
    public String nexusFileTemplate;

    public void setDeviceManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
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

    private CollectionsDb collectionsDb;

    private CamelManager camelManager;
    private PredatorManager predatorManager;

    private NexusXml getNexusFile() throws Exception {
        NexusXml nexusXml = XmlHelper.fromXml(
                Paths.get("config").resolve(TEMPLATE_NXDL_XML), NexusXml.class);
        FutureTask<NexusXml> task = new FutureTask<>(
                new NexusXmlGenerator(getSelectedDataSources(), nexusXml));
        task.run();

        return task.get();
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

    @Attribute
    @AttributeProperties(format = "xml")
    public String getNexusFileXml() throws Exception {
        return XmlHelper.toXmlString(getNexusFile());
    }

    @Attribute
    @AttributeProperties(format = "xml")
    public String getCamelRoutesXml() {
        return XmlHelper.toXmlString(camelManager.getCamelRoutesXml());
    }

    @Attribute
    public String[] getCamelRoutes() {
        return camelManager.getCamelRoutes()
                .toArray(String[]::new);
    }

    @Command
    public String getCamelRoute(String id) {
        return XmlHelper.toXmlString(camelManager.getCamelRoute(id));
    }

    @Command
    public void addOrReplaceCamelRoute(String camelRouteXml) throws Exception {
        CamelRoute update = XmlHelper.fromString(camelRouteXml, CamelRoute.class);

        camelManager.addOrReplaceCamelRoute(update);
    }

    @Command
    public void deleteCamelRoute(String id) {
        camelManager.deleteCamelRoute(id);
    }

    @Attribute
    public String getNexusMapping() throws ExecutionException, InterruptedException, IOException {
        StringWriter out = new StringWriter();

        FutureTask<Properties> task = new FutureTask<>(new MappingGenerator(getSelectedDataSources()));
        task.run();
        task.get().store(out, null);

        return out.toString();
    }

    @Attribute
    public String getStatusServerXml() throws Exception {
        FutureTask<StatusServerXml> task = new FutureTask<>(new StatusServerXmlGenerator(getSelectedDataSources()));
        task.run();
        return XmlHelper.toXmlString(task.get());
    }

    private List<DataSource> getSelectedDataSources() {
        return StreamSupport.stream(collectionsDb.getCollections().find().spliterator(), nonParallelStream)
                .filter(collection -> collection.value == 1)
                .map(collection -> dataSourcesDb.getCollection(collection.id))
                .flatMap(dataSourceMongoCollection -> StreamSupport.stream(dataSourceMongoCollection.find().spliterator(), nonParallelStream))
                .collect(Collectors.toList());
    }


    @Attribute
    @AttributeProperties(format = "yml")
    public String getPreExperimentDataCollectorYaml() {
        return predatorManager.getPreExperimentDataCollectorYaml();
    }

    @Attribute
    @AttributeProperties(format = "yml")
    public void setPreExperimentDataCollectorYaml(String yamlString) {
        predatorManager.setPreExperimentDataCollectorYaml(yamlString);
    }

    @Attribute
    public String[] getPreExperimentDataCollectorLoginProperties() {
        return predatorManager.getPreExperimentDataCollectorLoginProperties().toArray(String[]::new);
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
                .stream(dataSourcesDb.getMongoDb().getCollection(collection, DataSource.class).find().spliterator(), nonParallelStream)
                .toArray());
    }

    @Init
    public void init() {
        dataSourcesDb = new DataSourceDb();
        camelManager = new CamelManager(new CamelDb());
        collectionsDb = new CollectionsDb();
        predatorManager = new PredatorManager(new PredatorDb());

        deviceManager.pushStateChangeEvent(DeviceState.STANDBY);
        deviceManager.pushStatusChangeEvent("ConfigurationManager has been initialized.");
    }

    @Delete
    public void delete() {
        dataSourcesDb.close();
        camelManager.close();
        predatorManager.close();
        collectionsDb.close();

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
    public void updateProfileCollections(DevVarLongStringArray collections) {
        Preconditions.checkArgument(collections.lvalue.length == collections.svalue.length);

        for (int i = 0, size = collections.lvalue.length; i < size; ++i) {
            collectionsDb.getCollections()
                    .replaceOne(
                            new BsonDocument("_id", new BsonString(collections.svalue[i])),
                            new Collection(collections.svalue[i], collections.lvalue[i]), new UpdateOptions().upsert(true));
        }
    }
}
