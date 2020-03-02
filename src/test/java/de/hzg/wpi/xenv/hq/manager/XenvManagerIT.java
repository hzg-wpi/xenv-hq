package de.hzg.wpi.xenv.hq.manager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
public class XenvManagerIT {

    @Before
    public void before() throws IOException {
        XenvManager.createTempDirectory();
        XenvManager.extractResources();
    }

    @Test
    @Ignore
    public void startServer() throws Exception {
        XenvManager instance = new XenvManager();
        instance.init();

        instance.startServer("status_server");
    }

    @Test
    @Ignore
    public void stopServer() throws Exception {
        XenvManager instance = new XenvManager();
        instance.init();

        instance.stopServer("status_server");
    }
}