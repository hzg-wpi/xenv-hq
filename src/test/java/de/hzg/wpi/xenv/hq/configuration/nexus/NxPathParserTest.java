package de.hzg.wpi.xenv.hq.configuration.nexus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
public class NxPathParserTest {

    @Test
    public void toJXPath() {
        NxPathParser instance = new NxPathParser("/entry/hardware");

        NxPathParser.JxPath result = instance.toJXPath();

        assertEquals("/groups[name='entry']/groups[name='hardware']", result);
    }
}