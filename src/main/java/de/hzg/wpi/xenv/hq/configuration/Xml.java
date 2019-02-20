package de.hzg.wpi.xenv.hq.configuration;

import java.nio.file.Path;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
public interface Xml {
    String toXmlString() throws Exception;

    void toXml(Path path) throws Exception;
}
