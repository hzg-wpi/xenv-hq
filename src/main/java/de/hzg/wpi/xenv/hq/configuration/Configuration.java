package de.hzg.wpi.xenv.hq.configuration;

import de.hzg.wpi.xenv.hq.util.xml.Xml;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Root(strict = false)
public class Configuration implements Xml {
    @Attribute
    public String profile;

    @ElementList
    public List<DataSource> dataSourceList;

    public void addOrReplaceDataSource(DataSource result) {
        removeDataSource(result);
        dataSourceList.add(result);
    }

    public void removeDataSource(DataSource result) {
        dataSourceList.removeIf(dataSource -> dataSource.equals(result));
    }
}
