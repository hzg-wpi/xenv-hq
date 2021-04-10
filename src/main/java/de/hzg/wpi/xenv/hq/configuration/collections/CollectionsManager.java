package de.hzg.wpi.xenv.hq.configuration.collections;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.mongodb.Block;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import de.hzg.wpi.xenv.hq.configuration.mongo.CollectionsDb;
import de.hzg.wpi.xenv.hq.configuration.mongo.DataSourceDb;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 30.01.2020
 */
public class CollectionsManager implements Closeable {
    private final CollectionsDb collectionsDb;
    private final DataSourceDb dataSourceDb;
    private final boolean nonParallelStream = false;

    public CollectionsManager(CollectionsDb collectionsDb, DataSourceDb dataSourceDb) {
        this.collectionsDb = collectionsDb;
        this.dataSourceDb = dataSourceDb;
    }

    public Stream<Document> getDataSourceCollections() {
        return StreamSupport
                .stream(dataSourceDb.getMongoDb().listCollections().spliterator(), nonParallelStream)
                .map(document -> new Document("id", document.get("name")).append("value", document.get("name")));
    }

    public void deleteDataSourceCollection(String collectionId) {
        dataSourceDb.getMongoDb().getCollection(collectionId).drop();
        collectionsDb.getCollections().deleteOne(new BsonDocument("_id", new BsonString(collectionId)));
    }

    public void cloneDataSourceCollection(String targetId, String sourceId) {
        Preconditions.checkArgument(!targetId.isEmpty());
        Preconditions.checkArgument(!sourceId.isEmpty());

        dataSourceDb.getMongoDb().getCollection(targetId);
        dataSourceDb.getMongoDb().getCollection(sourceId)
                .find()
                .forEach((Block<Document>) dataSourceDb.getMongoDb().getCollection(targetId)::insertOne);
    }

    public Stream<DataSource> getDataSources(String collection) {
        return StreamSupport
                .stream(dataSourceDb.getMongoDb().getCollection(collection, DataSource.class).find().spliterator(), nonParallelStream);
    }


    public Stream<DataSource> getSelectedDataSources() {
        //TODO improve e.g. link one db to another with filter?!
        return StreamSupport.stream(collectionsDb.getCollections().find().spliterator(), nonParallelStream)
                .filter(collection -> collection.value == 1)
                .map(collection -> dataSourceDb.getCollection(collection.id))
                .flatMap(dataSourceMongoCollection -> StreamSupport.stream(dataSourceMongoCollection.find().spliterator(), nonParallelStream));
    }


    @Override
    public void close() {
        collectionsDb.close();
        dataSourceDb.close();
    }

    public void insertDataSource(String collection, DataSource dataSource) {
        dataSourceDb.getMongoDb().getCollection(collection, DataSource.class)
                .insertOne(dataSource);
    }

    public void updateDataSource(String collection, DataSource dataSource) {
        dataSourceDb.getMongoDb().getCollection(collection, DataSource.class)
                .replaceOne(new Document("_id", dataSource.id), dataSource);
    }

    public void deleteDataSource(String collection, DataSource dataSource) {
        dataSourceDb.getMongoDb().getCollection(collection, DataSource.class)
                .deleteOne(new Document("_id", dataSource.id));
    }

    public Stream<Collection> getSelectedCollections() {
        return StreamSupport.stream(collectionsDb.getCollections().find().spliterator(), nonParallelStream);
    }

    public void setSelectedCollections(Stream<Collection> collections) {
        //TODO potentially may lead to falsy selected collections, if param collections is less than total number of collections
        List<WriteModel<Collection>> updates = new ArrayList<>(collections
                .map(collection -> new ReplaceOneModel<>(
                        new BsonDocument("_id", new BsonString(collection.id)),
                        collection,
                        new UpdateOptions().upsert(true)))
                .collect(Collectors.toCollection(Lists::newArrayList)));

        collectionsDb.getCollections().bulkWrite(updates);
    }
}
