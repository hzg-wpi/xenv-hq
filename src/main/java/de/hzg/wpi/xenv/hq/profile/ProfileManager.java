package de.hzg.wpi.xenv.hq.profile;

import com.google.common.base.Preconditions;
import de.hzg.wpi.xenv.hq.HeadQuarter;
import de.hzg.wpi.xenv.hq.ant.AntProject;
import de.hzg.wpi.xenv.hq.ant.AntTaskExecutor;
import de.hzg.wpi.xenv.hq.configuration.Configuration;
import de.hzg.wpi.xenv.hq.manager.Manager;
import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import de.hzg.wpi.xenv.hq.util.yaml.YamlHelper;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Paths;

import static de.hzg.wpi.xenv.hq.HeadQuarter.PROFILES_ROOT;
import static de.hzg.wpi.xenv.hq.configuration.ConfigurationManager.CONFIGURATION_XML;
import static de.hzg.wpi.xenv.hq.manager.XenvManager.MANAGER_YML;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 3/14/19
 */
public class ProfileManager {
    public Profile loadProfile(String name) throws Exception {
        Preconditions.checkArgument(Files.exists(Paths.get(PROFILES_ROOT).resolve(name)));

        Configuration configuration = XmlHelper.fromXml(Paths.get(PROFILES_ROOT).resolve(name).resolve(CONFIGURATION_XML), Configuration.class);
        Manager manager = YamlHelper.fromYamlFile(Paths.get(PROFILES_ROOT).resolve(name).resolve(MANAGER_YML), Manager.class);

        Profile parent = null;
        if (configuration.parent != null)
            parent = loadProfile(configuration.parent);

        return new Profile(name, configuration, manager, parent);
    }

    public void createProfile(String name, String tango_host, String instance_name, @Nullable Profile parent) throws Exception {
        Preconditions.checkArgument(Files.notExists(Paths.get(PROFILES_ROOT).resolve(name)), String.format("Profile %s already exists!", name));

        Configuration configuration = new Configuration();
        if (parent != null)
            configuration.parent = parent.name;

        Manager manager = new Manager();
        manager.instance_name = instance_name;
        manager.tango_host = tango_host;

        Profile result = new Profile(name, configuration, manager, parent);

        Files.createDirectory(result.path);

        executeAnt(result, "copy-profile");

        //TODO move to ant?
        if (parent != null) {
            //TODO #6
            result.configuration.dataSourceList.addAll(parent.getDataSources());
            result.setPredatorYaml(parent.getPredatorYaml());
            result.setNexusFileTemplate(parent.getNexusTemplateXml());
            result.setCamelRoutes(parent.getCamelRoutesXml());
        }

        result.dumpConfiguration();

        executeAnt(result, "add-profile");
    }

    public void deleteProfile(String name) {
        Preconditions.checkArgument(Files.exists(Paths.get(PROFILES_ROOT).resolve(name)), String.format("Profile %s must exists!", name));

        Profile profile = new Profile(name, new Configuration(), new Manager(), null);

        executeAnt(profile, "remove-profile");
    }

    void executeAnt(Profile profile, String s) {
        AntProject project = new AntProject(HeadQuarter.getAntRoot() + "/build.xml");

        setProfileProperties(profile, project);

        new AntTaskExecutor(s, project).run();
    }

    private void setProfileProperties(Profile profile, AntProject project) {
        project.getProject().setBasedir(Paths.get(PROFILES_ROOT).getParent().toAbsolutePath().toString());
        project.getProject().setProperty("profile", profile.name);
        project.getProject().setProperty("parent", profile.configuration.parent);

        project.getProject().setProperty("tango_host", profile.manager.tango_host);
        project.getProject().setProperty("instance_name", profile.manager.instance_name);

        project.getProject().setProperty("tine_home", String.format("/home/%s/tine/database", System.getProperty("user.name")));
    }

}
