package de.hzg.wpi.xenv.hq.configuration.mapping;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/21/19
 */
public class MappingGenerationTask extends FutureTask<Properties> {
    public MappingGenerationTask(Callable<Properties> callable) {
        super(callable);
    }
}
