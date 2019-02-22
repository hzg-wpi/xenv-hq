package de.hzg.wpi.xenv.hq.manager;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
public class Configuration {
    public String tango_host;
    public String instance_name;
    public String tine_home;
    public String log_home;
    public String log_level;

    public TangoServer status_server;
    public TangoServer data_format_server;
    public TangoServer camel_integration;
    public TangoServer predator;
}
