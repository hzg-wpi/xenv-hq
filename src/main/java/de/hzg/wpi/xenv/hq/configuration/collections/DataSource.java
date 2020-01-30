package de.hzg.wpi.xenv.hq.configuration.collections;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import java.util.Objects;

/**
 * DataSources are distinguished by their nxPath
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Root
public class DataSource {
    public static final String DATASOURCE_SRC_EXTERNAL = "external:";

    @BsonId
    @BsonProperty("_id")
    @Attribute
    public long id;
    @Attribute
    public String nxPath;
    @Attribute
    public String type;
    @Attribute
    public String src;
    @Attribute
    public int pollRate;
    @Attribute
    public String dataType;

    public DataSource() {
    }

    public DataSource(long id, String nxPath, String type, String src, int pollRate, String dataType) {
        this.id = id;
        this.nxPath = nxPath;
        this.type = type;
        this.src = src;
        this.pollRate = pollRate;
        this.dataType = dataType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSource that = (DataSource) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
