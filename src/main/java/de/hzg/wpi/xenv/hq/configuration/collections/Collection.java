package de.hzg.wpi.xenv.hq.configuration.collections;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.09.2019
 */
@Root
public class Collection {
    @Attribute
    public String id;
    @Attribute
    public int value;

    public Collection() {
    }

    public Collection(String collection, int value) {
        this.id = collection;
        this.value = value;
    }
}
