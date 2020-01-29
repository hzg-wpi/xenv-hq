package de.hzg.wpi.xenv.hq.configuration.mongo;

import com.mongodb.client.MongoCollection;
import de.hzg.wpi.xenv.hq.configuration.Collection;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 29.01.2020
 */
public class CollectionsDb extends Mongo<Collection> {
    public CollectionsDb() {
        super("collections", Collection.class);
    }

    public MongoCollection<Collection> getCollections() {
        return super.getCollection("collections");
    }
}
