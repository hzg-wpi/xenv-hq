package de.hzg.wpi.xenv.hq.ant;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AntTaskExecutorTest {
    //    @BeforeClass
    public static void beforeClass() throws IOException {
        Path configuration = Paths.get("configuration");
        if (Files.exists(configuration))
            Files.walk(configuration)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
    }

    @Test
    public void executeDummy() {
        AntProject project = new AntProject("src/main/resources/ant/build.xml");

        AntTaskExecutor instance = new AntTaskExecutor("dummy", project);
        instance.run();
    }

    @Test
    public void a_executeFetchConfiguration() {
        AntProject project = new AntProject("src/main/resources/ant/build.xml");

        AntTaskExecutor instance = new AntTaskExecutor("clone-configuration", project);
        instance.run();
    }

    @Test
    public void b_executeCommitConfiguration() {
        AntProject project = new AntProject("src/main/resources/ant/build.xml");

        AntTaskExecutor instance = new AntTaskExecutor("commit-configuration", project);
        instance.run();
    }

    @Test
    public void c_executePushConfiguration() {
        AntProject project = new AntProject("src/main/resources/ant/build.xml");

        AntTaskExecutor instance = new AntTaskExecutor("push-configuration", project);
        instance.run();
    }

}