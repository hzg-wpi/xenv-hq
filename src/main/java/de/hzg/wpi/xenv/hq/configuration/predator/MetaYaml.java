package de.hzg.wpi.xenv.hq.configuration.predator;

import com.google.common.base.Preconditions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
public class MetaYaml {

    public static String toYamlString(Object yaml) {
        StringWriter writer = new StringWriter();
        new Yaml().dump(yaml, writer);

        return writer.toString();
    }

    public static void toYaml(Object yaml, Path path) throws Exception {
        Yaml serializer = new Yaml();

        Writer writer = new OutputStreamWriter(Files.newOutputStream(path));

        serializer.dump(yaml, writer);
    }


    public static Object fromString(String yaml) {
        Yaml parser = new Yaml();

        return parser.load(new StringReader(yaml));
    }

    public static Object fromYamlFile(Path path) throws IOException {
        Preconditions.checkArgument(Files.exists(path));

        Yaml parser = new Yaml();

        return parser.load(Files.newBufferedReader(path));
    }

}
