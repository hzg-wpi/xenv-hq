package de.hzg.wpi.xenv.hq.ant;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/19/19
 */
public class AntTaskExecutor implements Runnable {
    private final String antTarget;
    private final AntProject project;

    public AntTaskExecutor(String antTarget, AntProject project) {
        this.antTarget = antTarget;
        this.project = project;
    }

    public void run() {
        project.getProject().executeTarget(antTarget);
    }


}
