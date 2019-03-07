package de.hzg.wpi.xenv.hq.util.yaml;

import com.google.common.base.Preconditions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/22/19
 */
public class YamlHelper {
    private YamlHelper() {
    }

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


    public static <T> T fromString(String yaml, Class<T> clazz) {
        Yaml parser = new Yaml(new Constructor(clazz));

        return parser.<T>load(yaml);
    }

    public static <T> T fromYamlFile(Path path, Class<T> clazz) throws IOException {
        Preconditions.checkArgument(Files.exists(path));

        Yaml parser = new Yaml(new Constructor(clazz));

        return parser.load(Files.newBufferedReader(path));
    }

}
