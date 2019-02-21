package de.hzg.wpi.xenv.hq.configuration.status_server;

import com.google.common.collect.Iterables;
import de.hzg.wpi.xenv.hq.configuration.Configuration;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
public class StatusServerXmlGenerator implements Callable<StatusServerXml> {
    private final Configuration configuration;

    public StatusServerXmlGenerator(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public StatusServerXml call() throws Exception {
        StatusServerXml result = new StatusServerXml();

        JXPathContext jxPathContext = JXPathContext.newContext(result);
        configuration.dataSourceList.stream()
                .filter(dataSource -> dataSource.type.equalsIgnoreCase("log"))//StatusServer processes only log
                .forEach(dataSource -> {
                    JxPath jxPath = new JxPath(URI.create(dataSource.src));

                    StatusServerDevice device = getDevice(jxPath.getJxParentPath(), jxPathContext);

                    device.attributes.add(
                            new DataSourceToAttributeConverter(dataSource).call());
                });

        return result;
    }

    private StatusServerDevice getDevice(JxPath jxPath, JXPathContext jxPathContext) {
        try {
            return (StatusServerDevice) jxPathContext.getValue("/devices[url='" + jxPath.src.toString() + "']");
        } catch (JXPathNotFoundException e) {
            StatusServerDevice device = new StatusServerDevice();

            device.name = jxPath.getName();
            device.url = jxPath.src.toString();

            ((StatusServerXml) jxPathContext.getContextBean()).devices.add(device);

            return device;
        }
    }

    static class JxPath {
        URI src;
        List<String> parts;

        public JxPath(URI src) {
            this.src = src;
            this.parts = Arrays.asList(src.getPath().split("/"));
        }

        public JxPath getJxParentPath() {
            return new JxPath(src.resolve("."));
        }

        public String getName() {
            return Iterables.getLast(parts);
        }
    }
}
