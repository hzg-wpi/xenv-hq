package de.hzg.wpi.xenv.hq.profile;

import de.hzg.wpi.xenv.hq.configuration.Configuration;
import de.hzg.wpi.xenv.hq.configuration.DataSource;
import de.hzg.wpi.xenv.hq.configuration.camel.CamelRoutesXml;
import de.hzg.wpi.xenv.hq.configuration.data_format_server.NexusXml;
import de.hzg.wpi.xenv.hq.manager.Manager;
import de.hzg.wpi.xenv.hq.util.xml.XmlHelper;
import de.hzg.wpi.xenv.hq.util.yaml.YamlHelper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static de.hzg.wpi.xenv.hq.HeadQuarter.PROFILES_ROOT;
import static de.hzg.wpi.xenv.hq.configuration.ConfigurationManager.CONFIGURATION_XML;
import static de.hzg.wpi.xenv.hq.manager.XenvManager.MANAGER_YML;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 4/11/19
 */
public class Profile {
    public static final String TEMPLATE_NXDL_XML = "template.nxdl.xml";
    public static final String LOGIN_PROPERTIES = "login.properties";
    public static final String DATA_FORMAT_SERVER = "DataFormatServer";
    public static final String CAMEL_INTEGRATION = "CamelIntegration";
    public static final String ROUTES_XML = "routes.xml";
    public static final String PRE_EXPERIMENT_DATA_COLLECTOR = "PreExperimentDataCollector";
    public static final String META_YAML = "meta.yaml";
    public final Profile parent;
    public final String name;
    public transient final Path path;
    public final Configuration configuration;
    public /*final*/ Manager manager;

    public Profile(String name, Configuration configuration, Manager manager, @Nullable Profile parent) {
        this.name = name;
        this.configuration = configuration;
        this.manager = manager;
        this.parent = parent;
        this.path = Paths.get(PROFILES_ROOT).resolve(name);
    }

    public List<DataSource> getDataSources() {
        return new ArrayList<DataSource>() {{
//            if(Profile.this.parent != null)
//                addAll(Profile.this.parent.getDataSources());
            addAll(Profile.this.configuration.dataSourceList);
        }};
    }

    public void addDataSource(DataSource dataSource) {
        this.configuration.addOrReplaceDataSource(dataSource);
        //TODO update parent #6
    }

    public void removeDataSource(DataSource result) {
        configuration.removeDataSource(result);
        //TODO update parent #6
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public NexusXml getNexusTemplateXml() throws Exception {
        return XmlHelper.fromXml(
                path.resolve(DATA_FORMAT_SERVER).resolve(TEMPLATE_NXDL_XML), NexusXml.class);
    }

    public void setNexusFileTemplate(NexusXml nxFile) throws Exception {
        nxFile.toXml(
                path
                        .resolve(DATA_FORMAT_SERVER)
                        .resolve(TEMPLATE_NXDL_XML));
    }

    public Object getPredatorYaml() throws Exception {
        return YamlHelper.fromYamlFile(
                path
                        .resolve(PRE_EXPERIMENT_DATA_COLLECTOR)
                        .resolve(META_YAML), Object.class);
    }

    public void setPredatorYaml(Object yaml) throws Exception {
        YamlHelper.toYaml(yaml,
                path
                        .resolve(PRE_EXPERIMENT_DATA_COLLECTOR)
                        .resolve(META_YAML));
    }

    public CamelRoutesXml getCamelRoutesXml() throws Exception {
        return XmlHelper.fromXml(
                path
                        .resolve(CAMEL_INTEGRATION)
                        .resolve(ROUTES_XML), CamelRoutesXml.class);
    }

    public void setCamelRoutes(CamelRoutesXml xml) throws Exception {
        xml.toXml(
                path
                        .resolve(CAMEL_INTEGRATION)
                        .resolve(ROUTES_XML));
    }

    public String getPredatorLoginProperties() throws IOException {
        return new String(
                Files.readAllBytes(
                        path
                                .resolve(PRE_EXPERIMENT_DATA_COLLECTOR)
                                .resolve(LOGIN_PROPERTIES)));
    }

    public void setManager(Manager manager) throws Exception {
        this.manager = manager;
        YamlHelper.toYaml(
                manager,
                Paths.get(PROFILES_ROOT).resolve(name).resolve(MANAGER_YML));
    }

    public void dumpConfiguration() throws Exception {
        configuration.toXml(Paths.get(PROFILES_ROOT).resolve(name).resolve(CONFIGURATION_XML));
        if (parent != null) {
            parent.dumpConfiguration();
        }
    }
}
