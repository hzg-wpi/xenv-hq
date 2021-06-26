package de.hzg.wpi.xenv.hq.configuration.data_format_server;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
public class NxPathParserTest {

    @Test
    public void toJXPath() {
        NxPathParser instance = new NxPathParser(URI.create("/entry/hardware"));

        NxPathParser.JxPath result = instance.toJXPath();

        assertEquals("/groups[name='hardware']", result.toString());
    }
}