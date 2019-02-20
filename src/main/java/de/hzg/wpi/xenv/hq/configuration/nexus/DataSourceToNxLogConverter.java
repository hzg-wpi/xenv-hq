package de.hzg.wpi.xenv.hq.configuration.nexus;

import com.google.common.collect.Lists;

import java.util.concurrent.Callable;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
public class DataSourceToNxLogConverter implements Callable<NxGroup> {
    private final String name;
    private final String type;


    public DataSourceToNxLogConverter(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public NxGroup call() {
        NxGroup nxLog = new NxGroup();

        nxLog.name = name;
        nxLog.type = NexusXml.NX_LOG;

        NxField value = new DataSourceToNxFieldWithDimensionsConverter("value", type).call();
        NxField time = new DataSourceToNxFieldWithDimensionsConverter("time", "uint64").call();

        nxLog.fields = Lists.newArrayList();
        nxLog.fields.add(value);
        nxLog.fields.add(time);

        return nxLog;
    }
}
