package de.hzg.wpi.xenv.hq.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.listener.Log4jListener;

import java.io.File;

/**
 * Preconfigured Ant project to be used in {@link AntTaskExecutor}
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
public class AntProject {
    private final Project project;

    public AntProject(String file) {
        File buildFile = new File(file);

        Project p = new Project();
        p.setUserProperty("ant.file", buildFile.getAbsolutePath());
        p.setSystemProperties();
        p.init();

        ProjectHelper helper = ProjectHelper.getProjectHelper();
        p.addReference("ant.projectHelper", helper);
        helper.parse(p, buildFile);
//        DefaultLogger consoleLogger = new DefaultLogger();
//        consoleLogger.setErrorPrintStream(System.err);
//        consoleLogger.setOutputPrintStream(System.out);
//        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
//        p.addBuildListener(consoleLogger);

        Log4jListener log4jListener = new Log4jListener();
        p.addBuildListener(log4jListener);

        this.project = p;
    }

    public Project getProject() {
        return project;
    }
}
