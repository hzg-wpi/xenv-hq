package de.hzg.wpi.xenv.hq.configuration.mongo;

import com.mongodb.client.MongoCollection;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelRoute;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 28.01.2020
 */
public class CamelDb extends Mongo<CamelRoute> {
    public static final String CAMEL_DB = "camel";
    public static final String ROUTES = "routes";


    public CamelDb() {
        super(CAMEL_DB, CamelRoute.class);
    }

    public MongoCollection<CamelRoute> getRoutes() {
        return super.getCollection(ROUTES);
    }
}
