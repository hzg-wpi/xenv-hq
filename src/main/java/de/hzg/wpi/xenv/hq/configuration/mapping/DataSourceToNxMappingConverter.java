package de.hzg.wpi.xenv.hq.configuration.mapping;

import de.hzg.wpi.xenv.hq.configuration.DataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
public class DataSourceToNxMappingConverter implements Callable<Map<String, String>> {
    private final DataSource dataSource;

    public DataSourceToNxMappingConverter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Map<String, String> call() {
        return new HashMap<String, String>() {{
            put(dataSource.src, dataSource.nxPath);
        }};
    }
}
