package dev.bootupgrade.core.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the current Java and Spring Boot versions out of a Maven project's {@code pom.xml}. */
public record SourceVersions(String java, String springBoot) {

    public static final String UNKNOWN = "unknown";

    private static final Pattern JAVA_VERSION = Pattern.compile("<(?:java\\.version|maven\\.compiler\\.release|maven\\.compiler\\.source)>\\s*([0-9.]+)\\s*<");
    private static final Pattern BOOT_PARENT = Pattern.compile(
            "<parent>\\s*<groupId>org\\.springframework\\.boot</groupId>\\s*<artifactId>spring-boot-starter-parent</artifactId>\\s*<version>\\s*([^<\\s]+)\\s*</version>");

    /** Detects the versions from {@code repo/pom.xml}; anything it cannot find is {@value #UNKNOWN}. */
    public static SourceVersions detect(Path repositoryPath) {
        Path pom = repositoryPath.resolve("pom.xml");
        if (!Files.isRegularFile(pom)) {
            return new SourceVersions(UNKNOWN, UNKNOWN);
        }
        String xml;
        try {
            xml = Files.readString(pom);
        } catch (IOException e) {
            return new SourceVersions(UNKNOWN, UNKNOWN);
        }
        return new SourceVersions(firstGroup(JAVA_VERSION, xml), firstGroup(BOOT_PARENT, xml));
    }

    private static String firstGroup(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : UNKNOWN;
    }
}
