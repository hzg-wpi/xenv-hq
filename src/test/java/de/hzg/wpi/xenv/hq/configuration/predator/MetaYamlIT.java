package de.hzg.wpi.xenv.hq.configuration.predator;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static de.hzg.wpi.xenv.hq.configuration.ConfigurationManager.PROFILES_ROOT;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
public class MetaYamlIT {

    @Test
    public void fromString() {
        Object result = MetaYaml.fromString("- name: Fieldset form example\n" +
                "  id: frmFieldset\n" +
                "  type: fieldset");


    }

    @Test
    public void test() throws IOException {
        Object result = MetaYaml.fromYamlFile(Paths.get(PROFILES_ROOT).resolve("test/PreExperimentDataCollector/meta.yaml"));

        System.out.println(MetaYaml.toYamlString(result));
    }
}