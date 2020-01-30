package de.hzg.wpi.xenv.hq.configuration.data_format_server.mapping;

import de.hzg.wpi.xenv.hq.configuration.collections.DataSource;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
public class MappingGenerator implements Callable<Properties> {
    private final List<DataSource> dataSourceList;

    public MappingGenerator(List<DataSource> dataSourceList) {
        this.dataSourceList = dataSourceList;
    }

    @Override
    public Properties call() {
        Properties result = new Properties();

        dataSourceList.forEach(
                dataSource -> {
                    result.putAll(new DataSourceToNxMappingConverter(dataSource).call());
                }
        );


        return result;
    }
}
