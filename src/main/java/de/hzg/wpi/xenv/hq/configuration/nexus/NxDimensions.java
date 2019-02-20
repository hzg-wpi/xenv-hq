package de.hzg.wpi.xenv.hq.configuration.nexus;

import com.google.common.collect.Lists;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
@Root(name = "dimensions")
public class NxDimensions {
    @Attribute
    public int rank;
    @ElementList(inline = true)
    public List<NxDimension> dimensions = Lists.newArrayList();

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public List<NxDimension> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<NxDimension> dimensions) {
        this.dimensions = dimensions;
    }
}
