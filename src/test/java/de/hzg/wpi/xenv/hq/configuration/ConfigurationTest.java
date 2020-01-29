package de.hzg.wpi.xenv.hq.configuration;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelRoute;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelRouteEndpoint;
import de.hzg.wpi.xenv.hq.configuration.mongo.CamelDb;
import de.hzg.wpi.xenv.hq.configuration.mongo.DataSourceDb;
import de.hzg.wpi.xenv.hq.configuration.mongo.Mongo;
import de.hzg.wpi.xenv.hq.configuration.mongo.PredatorDb;
import de.hzg.wpi.xenv.hq.profile.Profile;
import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import junit.framework.TestCase;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tango.server.device.DeviceManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
public class ConfigurationTest {
    Configuration instance;

    @Before
    public void before() throws Exception {
        instance = XmlHelper.fromString(
                "<Configuration profile='test'>\n" +
                        "    <collections/>\n" +
                        "    <dataSourceList>\n" +
                        "        <DataSource id='0'" +
                        "                    nxPath='/entry'\n" +
                        "                    type='scalar'\n" +
                        "                    src='test/xenv/predator/name'\n" +
                        "                    pollRate='0'\n" +
                        "            dataType='string'" +
                        "        />\n" +
                        "    </dataSourceList>\n" +
                        "</Configuration>", Configuration.class);
    }

    @Test
    public void create() {
        assertNotNull(instance);
        assertEquals(1, instance.dataSourceList.size());
        assertEquals("/entry", instance.dataSourceList.get(0).nxPath);
        assertEquals("scalar", instance.dataSourceList.get(0).type);
        assertEquals("test/xenv/predator/name", instance.dataSourceList.get(0).src);
        assertEquals("string", instance.dataSourceList.get(0).dataType);
    }

    @Test
    public void toXmlString() throws Exception {
        String result = XmlHelper.toXmlString(instance);

        System.out.println(result);
    }

    @Test
    public void addDataSource() {
//        instance.addOrReplaceDataSource(new DataSource(System.currentTimeMillis(), "/entry/hardware/motor", "log", "test/motor/0", 200, "float32"));
//
//        assertEquals(2, instance.dataSourceList.size());
//        assertEquals("/entry/hardware/motor", instance.dataSourceList.get(1).nxPath);
    }

    @Test
    public void replaceDataSource() {
//        instance.addOrReplaceDataSource(new DataSource(0L, "/entry", "log", "test/motor/0", 200, "float32"));
//
//        assertEquals(1, instance.dataSourceList.size());
//        assertEquals("test/motor/0", instance.dataSourceList.get(0).src);
    }

    @Test
    public void removeDataSource() {
//        instance.removeDataSource(new DataSource(0L, "/entry", "log", "test/motor/0", 200, "float32"));
//
//        assertTrue(instance.dataSourceList.isEmpty());
    }

    @Test
    @Ignore
    public void testMongo() {
        Mongo mongo = new DataSourceDb();

        MongoCollection<DataSource> dataSources = mongo.getMongoDb().getCollection("test", DataSource.class);

//        <dataSource id="1" nxPath="/entry/hardware/petra" type="log" src="tine:/PETRA/Idc/Buffer-0/I.SCH" pollRate="0" dataType="float64"/>
        dataSources.insertOne(
                new DataSource(1, "/entry/hardware/petra", "log", "tine:/PETRA/Idc/Buffer-0/X", 0, "float64")
        );

        dataSources = mongo.getMongoDb().getCollection("test1", DataSource.class);

//        <dataSource id="1" nxPath="/entry/hardware/petra" type="log" src="tine:/PETRA/Idc/Buffer-0/I.SCH" pollRate="0" dataType="float64"/>
        dataSources.insertOne(
                new DataSource(1, "/entry/hardware/petra", "log", "tine:/PETRA/Idc/Buffer-0/Y", 0, "float64")
        );

        mongo.close();
    }

    @Test
    @Ignore
    public void testCamel() {
        Mongo mongo = new CamelDb();

        MongoCollection<CamelRoute> routes = mongo.getMongoDb().getCollection("routes", CamelRoute.class);

        CamelRoute route = new CamelRoute();

        route.id = "test-route-0";

        route.from = new CamelRouteEndpoint();
        route.to = new CamelRouteEndpoint();

        route.from.uri = "tango://hzgpp07ctcon1:10000/p07/xenv/status_server?pipe=status_server_pipe&amp;poll=true";
        route.to.uri = "tango://hzgpp07ctcon1:10000/p07/xenv/data_format_server?pipe=pipe";

        routes.insertOne(
                route
        );

        mongo.close();
    }

    @Test
    @Ignore
    public void testPredator() throws Exception {
        Path path = Paths.get("configuration/profiles/test/PreExperimentDataCollector");//from target

        String predatorMeta = "meta.yaml";


        Mongo<BsonDocument> mongo = new PredatorDb();

        MongoCollection<BsonDocument> meta = mongo.getMongoDb().getCollection("meta", BsonDocument.class);


        String predatorYmlAsString = new String(Files.readAllBytes(path.resolve(predatorMeta)));

        meta.insertOne(
                new BsonDocument("_id", new BsonString("yml"))
                        .append("value", new BsonString(predatorYmlAsString))
        );


        MongoCollection<BsonDocument> login = mongo.getMongoDb().getCollection("login", BsonDocument.class);


        login.insertOne(
                new BsonDocument("_id", new BsonString("predator.tomcat.use.kerberos"))
                        .append("value", new BsonString("true"))
        );

        mongo.close();
    }

    @Test
    @Ignore
    public void testManualMergeOfCollections() {
        try (Mongo mongo = new DataSourceDb()) {

            MongoCollection<Document> test = mongo.getMongoDb().getCollection("test");

//        <dataSource id="1" nxPath="/entry/hardware/petra" type="log" src="tine:/PETRA/Idc/Buffer-0/I.SCH" pollRate="0" dataType="float64"/>
            test.insertOne(
                    new Document("value", 123)
            );

            MongoCollection<Document> test1 = mongo.getMongoDb().getCollection("test");

            test1 = mongo.getMongoDb().getCollection("test1");

//        <dataSource id="1" nxPath="/entry/hardware/petra" type="log" src="tine:/PETRA/Idc/Buffer-0/I.SCH" pollRate="0" dataType="float64"/>
            test1.insertOne(
                    new Document("value", 456)
            );

            List<Document> result = Lists.newArrayList("test", "test1").stream()
                    .map(collection -> mongo.getMongoDb().getCollection(collection))
                    .flatMap(mongoCollection -> StreamSupport.stream(mongoCollection.find().spliterator(), false))
                    .collect(Collectors.toList());

            TestCase.assertEquals(2, result.size());
            TestCase.assertEquals(123, result.get(0).get("value"));
            TestCase.assertEquals(456, result.get(1).get("value"));
        }
    }

    @Test
    @Ignore
    public void migrateToMondo() throws Exception {
        System.setProperty("mongodb.host", "hzgpp05xenv");

        Gson gson = new Gson();

        try (Mongo mongo = new DataSourceDb()) {
            ConfigurationManager manager = new ConfigurationManager();
            manager.setDeviceManager(mock(DeviceManager.class));

            Arrays.stream(manager.getProfiles())
                    .filter(s -> !s.equalsIgnoreCase("default"))
                    .map(s -> {
                        try {
                            manager.loadProfile(s);
                            return gson.fromJson(gson.toJson(manager.profile), Profile.class);
                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                            return null;
                        }
                    })
                    .filter(profile -> profile != null)
                    .forEach(profile -> {
                        mongo.getMongoDb().getCollection(profile.name, DataSource.class).insertMany(profile.getDataSources().stream().map(dataSource -> {
                            dataSource.id = System.nanoTime();
                            return dataSource;
                        }).collect(Collectors.toList()));
                    });
        }
    }

//    @Test
//    public void testReactiveMongo() throws Exception {
//        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClients.getDefaultCodecRegistry(),
//                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
//
//        MongoClient client = MongoClients.create();
//
//        MongoDatabase db = client.getDatabase("xenv-hq").withCodecRegistry(pojoCodecRegistry);
//
//        MongoCollection<Document> collection = db.getCollection("datasources");
//
//        TODO override _id
//        Observable.fromPublisher(collection.createIndex(
//                new Document("id",1))).blockingSubscribe();
//
//
//        Observable.fromPublisher(collection.insertOne(
//                new Document("id","first")
//                        .append("data", "data" /*new int[]{1,2,3}*/))).blockingSubscribe();
//
//        List<String> result =  Observable.fromPublisher(collection.find()).collect(ArrayList<String>::new,(list, document) -> list.add(document.toJson())).blockingGet();
//
//        System.out.println(result);
//
//    }
}
