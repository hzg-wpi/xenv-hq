package de.hzg.wpi.xenv.hq.configuration.mongo;

import org.bson.BsonDocument;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 29.01.2020
 */
public class PredatorDb extends Mongo<BsonDocument> {
    public PredatorDb() {
        super("predator", BsonDocument.class);
    }
}
