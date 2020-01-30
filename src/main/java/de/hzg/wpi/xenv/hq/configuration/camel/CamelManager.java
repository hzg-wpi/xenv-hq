package de.hzg.wpi.xenv.hq.configuration.camel;

import com.mongodb.client.model.FindOneAndReplaceOptions;
import de.hzg.wpi.xenv.hq.configuration.mongo.CamelDb;
import org.bson.BsonDocument;
import org.bson.BsonString;

import java.io.Closeable;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 30.01.2020
 */
public class CamelManager implements Closeable {
    private final CamelDb camelDb;

    private final boolean nonParallelStream = false;

    public CamelManager(CamelDb camelDb) {
        this.camelDb = camelDb;
    }

    public Stream<String> getCamelRoutes() {
        return StreamSupport.stream(camelDb.getRoutes()
                .find()
                .spliterator(), nonParallelStream)
                .map(camelRoute -> camelRoute.id);
    }


    @Override
    public void close() {
        camelDb.close();
    }

    public CamelRoutesXml getCamelRoutesXml() {
        CamelRoutesXml result = new CamelRoutesXml();

        result.routes = StreamSupport.stream(
                camelDb.getRoutes().find().spliterator(), nonParallelStream)
                .collect(Collectors.toList());

        return result;
    }

    public CamelRoute getCamelRoute(String id) {
        return StreamSupport.stream(camelDb.getRoutes()
                .find(new BsonDocument("_id", new BsonString(id)))
                .spliterator(), nonParallelStream)
                .findFirst().orElseThrow(() -> new NoSuchElementException(id));
    }

    public void addOrReplaceCamelRoute(CamelRoute update) {
        camelDb.getRoutes()
                .findOneAndReplace(new BsonDocument("_id", new BsonString(update.id)), update, new FindOneAndReplaceOptions().upsert(true));
    }

    public void deleteCamelRoute(String id) {
        camelDb.getRoutes()
                .deleteOne(new BsonDocument("_id", new BsonString(id)));
    }
}
