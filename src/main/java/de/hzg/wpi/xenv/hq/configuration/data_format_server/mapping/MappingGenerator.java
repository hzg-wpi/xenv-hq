package de.hzg.wpi.xenv.hq.configuration.data_format_server.mapping;

import de.hzg.wpi.xenv.hq.configuration.Configuration;

import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
public class MappingGenerator implements Callable<Properties> {
    private final Configuration configuration;

    public MappingGenerator(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Properties call() {
        Properties result = new Properties();

        configuration.dataSourceList.forEach(
                dataSource -> {
                    result.putAll(new DataSourceToNxMappingConverter(dataSource).call());
                }
        );


        return result;
    }
}
