package de.hzg.wpi.xenv.hq.configuration.data_format_server;

import java.util.concurrent.Callable;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
public class DataSourceToNxFieldConverter implements Callable<NxField> {
    private final String name;
    private final String type;

    public DataSourceToNxFieldConverter(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public NxField call() {
        NxField value = new NxField();
        value.name = name;
        value.type = type;
        return value;
    }
}
