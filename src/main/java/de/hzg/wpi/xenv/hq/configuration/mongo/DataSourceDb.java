package de.hzg.wpi.xenv.hq.configuration.mongo;

import de.hzg.wpi.xenv.hq.configuration.DataSource;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 28.01.2020
 */
public class DataSourceDb extends Mongo<DataSource> {
    public static final String XENV_HQ_DB = "xenv-hq-datasources";

    public DataSourceDb() {
        super(XENV_HQ_DB, DataSource.class);
    }
}
