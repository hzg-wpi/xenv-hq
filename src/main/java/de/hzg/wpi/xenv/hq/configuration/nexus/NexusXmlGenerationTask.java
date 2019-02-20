package de.hzg.wpi.xenv.hq.configuration.nexus;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
public class NexusXmlGenerationTask extends FutureTask<NexusXml> {
    public NexusXmlGenerationTask(Callable<NexusXml> callable) {
        super(callable);
    }
}
