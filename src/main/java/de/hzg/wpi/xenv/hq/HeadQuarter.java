package de.hzg.wpi.xenv.hq;

import de.hzg.wpi.xenv.hq.configuration.ConfigurationManager;
import org.tango.server.ServerManager;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
public class HeadQuarter {
    public static void main(String[] args) throws IOException {
        setSystemProperties();

        ServerManager.getInstance().addClass("ConfigurationManager", ConfigurationManager.class);
        ServerManager.getInstance().addClass("XenvManager", XenvManager.class);

        ServerManager.getInstance().start(args, HeadQuarter.class.getSimpleName());
    }

    private static void setSystemProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(HeadQuarter.class.getResourceAsStream("/xenv.properties"));

        System.getProperties().putAll(properties);
    }
}
