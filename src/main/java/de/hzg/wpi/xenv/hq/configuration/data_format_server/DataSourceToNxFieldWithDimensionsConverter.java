package de.hzg.wpi.xenv.hq.configuration.data_format_server;

import com.google.common.collect.Lists;

import java.util.concurrent.Callable;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
public class DataSourceToNxFieldWithDimensionsConverter implements Callable<NxField> {
    private final DataSourceToNxFieldConverter helper;

    public DataSourceToNxFieldWithDimensionsConverter(String name, String type) {
        this.helper = new DataSourceToNxFieldConverter(name, type);
    }

    @Override
    public NxField call() {
        NxField value = this.helper.call();
        value.dimensions = new NxDimensions();
        value.dimensions.rank = 1;
        value.dimensions.dimensions = Lists.newArrayList();

        NxDimension dimension = new NxDimension();
        dimension.index = 0;
        dimension.value = 0;

        value.dimensions.dimensions.add(dimension);
        return value;
    }
}
