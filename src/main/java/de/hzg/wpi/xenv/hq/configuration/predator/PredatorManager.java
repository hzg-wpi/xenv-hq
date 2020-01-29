package de.hzg.wpi.xenv.hq.configuration.predator;

import com.mongodb.client.model.FindOneAndReplaceOptions;
import de.hzg.wpi.xenv.hq.configuration.mongo.PredatorDb;
import org.bson.BsonDocument;
import org.bson.BsonString;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 29.01.2020
 */
public class PredatorManager {
    private final PredatorDb predatorDb;
    private final boolean nonParallelStream = false;

    public PredatorManager(PredatorDb predatorDb) {
        this.predatorDb = predatorDb;
    }

    public Stream<String> getPreExperimentDataCollectorYamls() {
        return StreamSupport.stream(
                predatorDb.getCollection("meta")
                        .find().spliterator(), nonParallelStream
        ).map(bsonDocument -> bsonDocument.get("_id").asString().getValue());
    }

    public String getPreExperimentDataCollectorYaml() {
        return StreamSupport.stream(
                predatorDb.getCollection("meta")
                        .find(new BsonDocument("_id", new BsonString("yml"))).spliterator(), nonParallelStream
        )
                .map(bsonDocument -> bsonDocument.get("value").asString().getValue())
                .findFirst().orElseThrow(() -> new NoSuchElementException());
    }

    public void setPreExperimentDataCollectorYaml(String yamlString) {
        predatorDb.getCollection("meta")
                .replaceOne(new BsonDocument("_id", new BsonString("yml")), new BsonDocument("value", new BsonString(yamlString)));
    }

    public Stream<String> getPreExperimentDataCollectorLoginProperties() {
        return StreamSupport.stream(
                predatorDb.getCollection("login")
                        .find().spliterator(), nonParallelStream
        ).map(bsonDocument -> new StringBuilder(bsonDocument.getString("_id").getValue()).append('=').append(bsonDocument.getString("value").getValue()).toString());
    }

    public void setPreExperimentDataCollectorLoginProperties(Map.Entry<String, String> property) {
        predatorDb.getCollection("login")
                .findOneAndReplace(new BsonDocument("_id", new BsonString(property.getKey())),
                        new BsonDocument("_id", new BsonString(property.getKey()))
                                .append("value", new BsonString(property.getValue())), new FindOneAndReplaceOptions().upsert(true));
    }
}
