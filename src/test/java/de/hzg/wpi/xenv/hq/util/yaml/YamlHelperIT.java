package de.hzg.wpi.xenv.hq.util.yaml;

import de.hzg.wpi.xenv.hq.manager.Configuration;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static de.hzg.wpi.xenv.hq.HeadQuarter.PROFILES_ROOT;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
public class YamlHelperIT {

    @Test
    public void fromString() {
        Object result = YamlHelper.fromString("- name: Fieldset form example\n" +
                "  id: frmFieldset\n" +
                "  type: fieldset");


    }

    @Test
    public void test() throws IOException {
        Object result = YamlHelper.fromYamlFile(Paths.get(PROFILES_ROOT).resolve("test/PreExperimentDataCollector/meta.yaml"));

        System.out.println(YamlHelper.toYamlString(result));
    }

    @Test
    public void test_manager() throws IOException {
        Configuration result = YamlHelper.fromYamlFile(Paths.get(PROFILES_ROOT).resolve("test/manager.yml"));

        System.out.println(YamlHelper.toYamlString(result));
    }
}