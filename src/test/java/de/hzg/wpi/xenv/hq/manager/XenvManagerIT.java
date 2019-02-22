package de.hzg.wpi.xenv.hq.manager;

import org.junit.Test;

import java.io.IOException;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
public class XenvManagerIT {

    @Test
    public void startServer() throws IOException {
        XenvManager instance = new XenvManager();
        instance.init();
        instance.profile = "test";
        instance.load();

        instance.startServer("status_server");
    }
}