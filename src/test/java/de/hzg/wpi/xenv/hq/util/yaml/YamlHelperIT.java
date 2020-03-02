package de.hzg.wpi.xenv.hq.util.yaml;

import de.hzg.wpi.xenv.hq.manager.Manager;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
public class YamlHelperIT {
    public static final String PROFILES_ROOT = "config";

    @Test
    public void fromString() {
        Object result = YamlHelper.fromString("- name: Fieldset form example\n" +
                "  id: frmFieldset\n" +
                "  type: fieldset", Object.class);


    }

    @Test
    @Ignore
    public void test() throws IOException {
        Object result = YamlHelper.fromYamlFile(Paths.get("etc").resolve("PreExperimentDataCollector/meta.yaml"), Object.class);

        System.out.println(YamlHelper.toYamlString(result));
    }

    @Test
    public void test_manager() throws IOException {
        Manager result = YamlHelper.fromYamlFile(Paths.get(PROFILES_ROOT).resolve("manager.yml"), Manager.class);

        System.out.println(YamlHelper.toYamlString(result));
    }
}