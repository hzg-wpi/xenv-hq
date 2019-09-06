package de.hzg.wpi.xenv.hq.configuration;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.io.Closeable;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 04.09.2019
 */
public class Mongo implements Closeable {
    public static final String XENV_HQ_DB = "xenv-hq-datasources";
    public final CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));
    private final String mongoHost = System.getProperty("mongodb.host", "localhost");
    private final MongoClient mongoClient;
    private final MongoDatabase mongoDb;


    public Mongo() {
        mongoClient = new MongoClient(mongoHost);
        mongoDb = mongoClient.getDatabase(XENV_HQ_DB).withCodecRegistry(pojoCodecRegistry);
    }

    public MongoDatabase getMongoDb() {
        return mongoDb;
    }

    public Iterable<String> getDataSourceCollections() {
        return mongoDb.listCollectionNames();
    }

    public MongoCollection<DataSource> getDataSources(String collection) {
        MongoCollection<DataSource> result = mongoDb.getCollection(collection, DataSource.class);
        result.withCodecRegistry(pojoCodecRegistry).createIndex(new Document("id", 1), new IndexOptions().unique(true));
        return result;
    }

    @Override
    public void close() {
        mongoClient.close();
    }
}
