package de.hzg.wpi.xenv.hq.configuration;

import com.mongodb.client.MongoCollection;
import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        String result = instance.toXmlString();

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
    public void testMongo() {
        Mongo mongo = new Mongo();

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
