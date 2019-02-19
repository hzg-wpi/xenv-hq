package de.hzg.wpi.xenv.hq.ant;

import org.junit.Test;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
public class AntTaskExecutorTest {
    @Test
    public void executeDummy() {
        AntProject project = new AntProject("ant/build.xml");

        AntTaskExecutor instance = new AntTaskExecutor("dummy", project);
        instance.run();
    }

    @Test
    public void executeFetchConfiguration() {
        AntProject project = new AntProject("ant/build.xml");

        AntTaskExecutor instance = new AntTaskExecutor("pull-configuration", project);
        instance.run();
    }

    @Test
    public void executeCommitConfiguration() {
        AntProject project = new AntProject("ant/build.xml");

        AntTaskExecutor instance = new AntTaskExecutor("commit-configuration", project);
        instance.run();
    }

    @Test
    public void executePushConfiguration() {
        AntProject project = new AntProject("ant/build.xml");

        AntTaskExecutor instance = new AntTaskExecutor("push-configuration", project);
        instance.run();
    }

}