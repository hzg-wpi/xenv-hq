package de.hzg.wpi.xenv.hq.configuration.mongo;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.io.Closeable;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 04.09.2019
 */
public class Mongo<T> implements Closeable {
    private static final String MONGO_HOST = System.getProperty("mongodb.host", "localhost");


    private final CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    private final MongoClient mongoClient;
    private final MongoDatabase mongoDb;

    private final Class<T> clazz;


    public Mongo(String db, Class<T> clazz) {
        this.clazz = clazz;

        mongoClient = new MongoClient(MONGO_HOST);
        mongoDb = mongoClient.getDatabase(db).withCodecRegistry(pojoCodecRegistry);
    }

    public MongoDatabase getMongoDb() {
        return mongoDb;
    }

    public MongoCollection<T> getCollection(String collection) {
        MongoCollection<T> result = mongoDb.getCollection(collection, clazz);
        result.withCodecRegistry(pojoCodecRegistry);
        return result;
    }

    @Override
    public void close() {
        mongoClient.close();
    }
}
