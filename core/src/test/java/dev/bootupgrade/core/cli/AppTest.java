package dev.bootupgrade.core.cli;

import dev.bootupgrade.core.model.RunStatus;
import dev.bootupgrade.core.model.StepKind;
import dev.bootupgrade.core.model.UpgradeRun;
import dev.bootupgrade.core.model.UpgradeStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {

    @TempDir
    Path tmp;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);

    private String output() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private Path mavenProject(String javaVersion, String bootVersion) throws IOException {
        Path project = Files.createDirectory(tmp.resolve("demo-service"));
        Files.writeString(project.resolve("pom.xml"), """
                <project>
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>%s</version>
                  </parent>
                  <properties><java.version>%s</java.version></properties>
                </project>
                """.formatted(bootVersion, javaVersion));
        return project;
    }

    private static App.UpgradeExecutor returning(RunStatus status) {
        return (config, name, java, boot) -> {
            UpgradeStep recipes = UpgradeStep.start("s1", "Apply OpenRewrite recipes", StepKind.RECIPE, "recipes")
                    .markSucceeded("3 file(s) changed: pom.xml, A.java, B.java");
            return UpgradeRun.start("run-1", name, java, "21", boot, config.targetSpringBootVersion(), "bootupgrade/" + name + "-1")
                    .withStep(recipes)
                    .withStatus(status);
        };
    }

    @Test
    void printsUsageAndReturnsOneWhenNoArgsGiven() {
        int exitCode = App.run(new String[0], out);

        assertThat(exitCode).isEqualTo(1);
        assertThat(output()).contains(App.VERSION).contains("Usage: bootupgrade <path-to-maven-project>");
    }

    @Test
    void rejectsUnknownOptions() {
        int exitCode = App.run(new String[] {"--bogus"}, out);

        assertThat(exitCode).isEqualTo(1);
        assertThat(output()).contains("unexpected argument: --bogus");
    }

    @Test
    void rejectsAPathThatIsNotAMavenProject() {
        int exitCode = App.run(new String[] {tmp.toString()}, out, returning(RunStatus.SUCCEEDED));

        assertThat(exitCode).isEqualTo(1);
        assertThat(output()).contains("is not a Maven project");
    }

    @Test
    void reportsDetectedVersionsAndWritesTheChangelog() throws IOException {
        Path project = mavenProject("8", "2.7.18");
        Path outDir = tmp.resolve("out");

        int exitCode = App.run(new String[] {project.toString(), "--out", outDir.toString()}, out, returning(RunStatus.SUCCEEDED));

        assertThat(exitCode).isZero();
        assertThat(output())
                .contains("Java 8 -> 21")
                .contains("Spring Boot 2.7.18 -> 3.3.5")
                .contains("Apply OpenRewrite recipes")
                .contains("Result: SUCCEEDED");
        assertThat(outDir.resolve("bootupgrade-demo-service-changelog.md")).content().contains("# Upgrade Log: demo-service");
        assertThat(outDir.resolve("bootupgrade-demo-service-pr.md")).content().contains("Status: SUCCEEDED");
    }

    @Test
    void mapsRunStatusesToExitCodes() throws IOException {
        Path project = mavenProject("8", "2.7.18");
        String out1 = tmp.resolve("o").toString();

        assertThat(App.run(new String[] {project.toString(), "--out", out1}, out, returning(RunStatus.PARTIAL))).isEqualTo(2);
        assertThat(App.run(new String[] {project.toString(), "--out", out1}, out, returning(RunStatus.FAILED))).isEqualTo(3);
    }

    @Test
    void anUnreachableApiIsAWarningNotAFailure() throws IOException {
        Path project = mavenProject("8", "2.7.18");

        int exitCode = App.run(
                new String[] {project.toString(), "--out", tmp.resolve("o").toString(), "--report-to", "http://127.0.0.1:9"},
                out, returning(RunStatus.SUCCEEDED));

        assertThat(exitCode).isZero();
        assertThat(output()).contains("warning: could not report to http://127.0.0.1:9");
    }
}
