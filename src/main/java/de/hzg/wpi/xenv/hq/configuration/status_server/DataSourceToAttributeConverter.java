package de.hzg.wpi.xenv.hq.configuration.status_server;

import de.hzg.wpi.xenv.hq.configuration.DataSource;

import java.net.URI;
import java.util.concurrent.Callable;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
public class DataSourceToAttributeConverter implements Callable<StatusServerAttribute> {
    private final DataSource dataSource;

    public DataSourceToAttributeConverter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public StatusServerAttribute call() {
        StatusServerAttribute result = new StatusServerAttribute();

        StatusServerXmlGenerator.JxPath jxPath = new StatusServerXmlGenerator.JxPath(URI.create(dataSource.src));
        result.name = jxPath.getName();
        result.alias = jxPath.getName();
        result.method = "poll";
        result.delay = dataSource.pollRate;
        result.interpolation = "last";
        result.type = "change";

        return result;
    }
}
