package de.hzg.wpi.xenv.hq.configuration;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Root(strict = false)
public class Configuration {
    @Attribute(required = false)
    public String parent;

    @ElementList
    public List<DataSource> dataSourceList = new ArrayList<>();

    @ElementList
    public List<Collection> collections = new ArrayList<>();
    ;

    public void addOrReplaceDataSource(DataSource result) {
        removeDataSource(result);
        dataSourceList.add(result);
    }

    public void removeDataSource(DataSource result) {
        dataSourceList.removeIf(dataSource -> dataSource.equals(result));
    }
}
