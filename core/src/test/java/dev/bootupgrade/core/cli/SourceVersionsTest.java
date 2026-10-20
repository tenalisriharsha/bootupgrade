package dev.bootupgrade.core.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SourceVersionsTest {

    @TempDir
    Path tmp;

    @Test
    void readsJavaAndSpringBootVersionsFromThePom() throws IOException {
        Files.writeString(tmp.resolve("pom.xml"), """
                <project>
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.7.18</version>
                  </parent>
                  <properties><java.version>1.8</java.version></properties>
                </project>
                """);

        assertThat(SourceVersions.detect(tmp)).isEqualTo(new SourceVersions("1.8", "2.7.18"));
    }

    @Test
    void fallsBackToCompilerSourceWhenThereIsNoJavaVersionProperty() throws IOException {
        Files.writeString(tmp.resolve("pom.xml"),
                "<project><properties><maven.compiler.source>11</maven.compiler.source></properties></project>");

        assertThat(SourceVersions.detect(tmp)).isEqualTo(new SourceVersions("11", SourceVersions.UNKNOWN));
    }

    @Test
    void returnsUnknownWhenThereIsNoPom() {
        assertThat(SourceVersions.detect(tmp)).isEqualTo(new SourceVersions("unknown", "unknown"));
    }
}
