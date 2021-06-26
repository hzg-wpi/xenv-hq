package de.hzg.wpi.xenv.hq.configuration.data_format_server;

import com.google.common.collect.Lists;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
public class NxPathParser {
    public String nxPath;

    public NxPathParser(URI nxPath) {
        this.nxPath = nxPath.getPath();
    }

    public JxPath toJXPath() {
        String[] split = nxPath.split("/");
        return new JxPath(Arrays.stream(split).skip(2).collect(Collectors.toList()));//2 - root and entry
    }

    public static class JxPath {
        List<String> parts;
        List<String> jxParts;

        public JxPath() {
            this.parts = Collections.emptyList();
            this.jxParts = Collections.singletonList("/");
        }

        public JxPath(List<String> parts) {
            this.parts = parts;
            this.jxParts = parts.stream()
                    .map(s -> "/groups[name='" + s + "']")
                    .collect(Collectors.toList());
        }

        public JxPath getJxParentPath() {
            if (parts.size() == 1) return null;
            else return new JxPath(parts.subList(0, parts.size() - 1));
        }

        public String getName() {
            return parts.get(parts.size() - 1);
        }


        @Override
        public String toString() {
            return String.join("", jxParts);
        }
    }
}
