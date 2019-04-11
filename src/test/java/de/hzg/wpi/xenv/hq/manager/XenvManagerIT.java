package de.hzg.wpi.xenv.hq.manager;

import org.junit.Test;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
public class XenvManagerIT {

    @Test
    public void startServer() throws Exception {
        XenvManager instance = new XenvManager();
        instance.init();
        instance.loadProfile("test");

        instance.startServer("status_server");
    }
}