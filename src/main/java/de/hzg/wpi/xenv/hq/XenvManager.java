package de.hzg.wpi.xenv.hq;

import org.tango.server.annotation.Command;
import org.tango.server.annotation.Device;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Device
public class XenvManager {
    @Command
    public String startServer() {
        return "Done.";
    }
}
