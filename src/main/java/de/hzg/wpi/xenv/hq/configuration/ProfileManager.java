package de.hzg.wpi.xenv.hq.configuration;

import com.google.common.base.Preconditions;
import de.hzg.wpi.xenv.hq.HeadQuarter;
import de.hzg.wpi.xenv.hq.ant.AntProject;
import de.hzg.wpi.xenv.hq.ant.AntTaskExecutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static de.hzg.wpi.xenv.hq.HeadQuarter.PROFILES_ROOT;
import static de.hzg.wpi.xenv.hq.configuration.ConfigurationManager.CONFIGURATION_XML;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 3/14/19
 */
public class ProfileManager {
    public void createProfile(Profile profile, Configuration configuration) throws Exception {
        Preconditions.checkArgument(Files.notExists(profile.path), String.format("Profile %s already exists!", profile));

        Files.createDirectory(profile.path);


        executeAnt(profile, "copy-profile");

        if (configuration != null)
            configuration.toXml(profile.path.resolve(CONFIGURATION_XML));
        //TODO other config?

        executeAnt(profile, "add-profile");
    }

    public void deleteProfile(Profile profile) {
        Preconditions.checkArgument(Files.exists(profile.path), String.format("Profile %s must exists!", profile));


        executeAnt(profile, "remove-profile");
    }

    void executeAnt(ProfileManager.Profile profile, String s) {
        AntProject project = new AntProject(HeadQuarter.getAntRoot() + "/build.xml");

        setProfileProperties(profile, project);

        new AntTaskExecutor(s, project).run();
    }

    private void setProfileProperties(ProfileManager.Profile profile, AntProject project) {
        project.getProject().setBasedir(Paths.get(PROFILES_ROOT).getParent().toAbsolutePath().toString());
        project.getProject().setProperty("profile", profile.name);

        project.getProject().setProperty("tango_host", profile.tango_host);
        project.getProject().setProperty("instance_name", profile.instance_name);

        project.getProject().setProperty("tine_home", String.format("/home/%s/tine/database", System.getProperty("user.name")));
    }

    static class Profile {
        String name;
        String tango_host;
        String instance_name;
        Path path;

        public Profile(String name, String tango_host, String instance_name) {
            this.name = name;
            this.tango_host = tango_host;
            this.instance_name = instance_name;
            this.path = Paths.get(PROFILES_ROOT).resolve(name);
        }
    }
}
