package de.hzg.wpi.xenv.hq.configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;
import com.google.gson.Gson;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelManager;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelRoute;
import de.hzg.wpi.xenv.hq.configuration.collections.Collection;
import de.hzg.wpi.xenv.hq.configuration.collections.CollectionsManager;
import de.hzg.wpi.xenv.hq.configuration.collections.DataSource;
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
import java.util.stream.Collectors;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class ConfigurationManager {
    public static final List<String> VALID_DATA_SOURCE_TYPES = Arrays.asList("scalar", "spectrum", "log");
    public static final String CONFIGURATION_XML = "configuration.xml";
    public static final String TEMPLATE_NXDL_XML = "template.nxdl.xml";
    private final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    private String collection;


    @DeviceManagement
    private DeviceManager deviceManager;
    @State(isPolled = true, pollingPeriod = 3000)
    private volatile DeviceState state;
    @Status(isPolled = true, pollingPeriod = 3000)
    private volatile String status;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

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


    private CamelManager camelManager;
    private PredatorManager predatorManager;
    private CollectionsManager collectionsManager;

    private NexusXml getNexusFile() throws Exception {
        NexusXml nexusXml = XmlHelper.fromXml(
                Paths.get("config").resolve(TEMPLATE_NXDL_XML), NexusXml.class);
        FutureTask<NexusXml> task = new FutureTask<>(
                new NexusXmlGenerator(getSelectedDataSources(), nexusXml));
        task.run();

        return task.get();
    }

    //TODO move to waltz XenvHQ server side
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

    @Command
    public void writeDataFormatServerConfiguration() {
        try {
            Path conf = Files.createDirectories(Paths.get("etc/DataFormatServer"));
            Files.newOutputStream(
                    conf.resolve("default.nxdl.xml"))
                    .write(getNexusFileXml().getBytes());

            Files.newOutputStream(
                    conf.resolve("nxpath.mapping"))
                    .write(getNexusMapping().getBytes());
        } catch (Exception e) {
            logger.error("Failed to write DataFormatServer configuration");
            deviceManager.pushStateChangeEvent(DeviceState.ALARM);
        }
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

    @Command
    public void writeCamelConfiguration() {
        try {
            Path conf = Files.createDirectories(Paths.get("etc/CamelIntegration"));
            Files.newOutputStream(
                    conf.resolve("routes.xml"))
                    .write(getCamelRoutesXml().getBytes());
        } catch (Exception e) {
            logger.error("Failed to write DataFormatServer configuration");
            deviceManager.pushStateChangeEvent(DeviceState.ALARM);
        }
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

    @Command
    public void writeStatusServerConfiguration() {
        try {
            Path conf = Files.createDirectories(Paths.get("etc/StatusServer"));
            Files.newOutputStream(
                    conf.resolve("status_server.xml"))
                    .write(getStatusServerXml().getBytes());
        } catch (Exception e) {
            logger.error("Failed to write StatusServer configuration");
            deviceManager.pushStateChangeEvent(DeviceState.ALARM);
        }
    }

    private List<DataSource> getSelectedDataSources() {
        return collectionsManager.getSelectedDataSources().collect(Collectors.toList());
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

    @Command
    public void writePreExperimentDataCollectorConfiguration() {
        try {
            Path conf = Files.createDirectories(Paths.get("etc/PreExperimentDataCollector"));
            Files.newOutputStream(
                    conf.resolve("meta.yaml"))
                    .write(getPreExperimentDataCollectorYaml().getBytes());

            Files.newOutputStream(
                    conf.resolve("login.properties"))
                    .write(
                            String.join("\n", getPreExperimentDataCollectorLoginProperties()).getBytes());
        } catch (Exception e) {
            logger.error("Failed to write PreExperimentDataCollector configuration");
            deviceManager.pushStateChangeEvent(DeviceState.ALARM);
        }
    }

    public static void main(String[] args) {
        ServerManager.getInstance().start(args, ConfigurationManager.class);
    }

    @Attribute
    @AttributeProperties(format = "json")
    public String getDataSourceCollections() {
        return new Gson().toJson(
                collectionsManager.getDataSourceCollections().toArray());
    }

    @Command(inTypeDesc = "collectionId")
    public void deleteCollection(String collectionId) {
        collectionsManager.deleteDataSourceCollection(collectionId);
    }

    @Command(inTypeDesc = "[collectionId, sourceId]")
    public void cloneCollection(String[] args) {
        String targetId = args[0];
        String sourceId = args[1];
        collectionsManager.cloneDataSourceCollection(targetId, sourceId);
    }

    @Attribute
    public void setDataSourcesCollection(String collection) {
        this.collection = collection;
    }

    @Command
    public String getDataSources(String collection) {
        return new Gson().toJson(collectionsManager.getDataSources(collection).toArray());
    }

    @Init
    public void init() {
        camelManager = new CamelManager(new CamelDb());
        collectionsManager = new CollectionsManager(new CollectionsDb(), new DataSourceDb());
        predatorManager = new PredatorManager(new PredatorDb());

        deviceManager.pushStateChangeEvent(DeviceState.ON);
        deviceManager.pushStatusChangeEvent("ConfigurationManager has been initialized.");
    }

    @Delete
    public void delete() {
        camelManager.close();
        predatorManager.close();
        collectionsManager.close();

        deviceManager.pushStateChangeEvent(DeviceState.OFF);
        deviceManager.pushStatusChangeEvent(DeviceState.OFF.name());
    }

    @Command(inTypeDesc = "dataSource as JSON")
    public void insertDataSource(String arg) {
        Preconditions.checkState(collection != null, "Collection must be set prior this operation!");
        DataSource dataSource = new Gson().fromJson(arg,DataSource.class);

        collectionsManager.insertDataSource(collection, dataSource);


    }

    @Command(inTypeDesc = "dataSource as JSON")
    public void updateDataSource(String arg) {
        Preconditions.checkState(collection != null, "Collection must be set prior this operation!");
        DataSource dataSource = new Gson().fromJson(arg, DataSource.class);

        collectionsManager.updateDataSource(collection, dataSource);


    }

    @Command(inTypeDesc = "dataSource as JSON")
    public void deleteDataSource(String arg) {
        Preconditions.checkState(collection != null, "Collection must be set prior this operation!");
        DataSource dataSource = new Gson().fromJson(arg, DataSource.class);

        collectionsManager.deleteDataSource(collection, dataSource);
    }

    @Command
    public void selectCollections(DevVarLongStringArray collections) {
        Preconditions.checkArgument(collections.lvalue.length == collections.svalue.length);

        collectionsManager.setSelectedCollections(Streams.zip(
                Arrays.stream(collections.svalue),
                Arrays.stream(collections.lvalue).boxed(), Collection::new
        ));
    }
}
