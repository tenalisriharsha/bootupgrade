package dev.bootupgrade.core.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {

    private String runAndCapture(String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        App.run(args, out);
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void printsUsageAndReturnsNonZeroWhenNoArgsGiven() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        int exitCode = App.run(new String[0], out);

        assertThat(exitCode).isEqualTo(1);
        assertThat(buffer.toString(StandardCharsets.UTF_8)).contains("Usage: bootupgrade <path-to-repository>");
    }

    @Test
    void printsBannerWithVersion() {
        String output = runAndCapture("/tmp/some-repo");

        assertThat(output).contains(App.VERSION);
    }

    @Test
    void printsResolvedConfigWhenRepositoryPathGiven() {
        String output = runAndCapture("/tmp/legacy-rest-service");

        assertThat(output)
                .contains("/tmp/legacy-rest-service")
                .contains("Target Java version: 21")
                .contains("Target Spring Boot version: 3.3.5")
                .contains("Max agent iterations: 5");
    }

    @Test
    void returnsZeroWhenRepositoryPathGiven() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        int exitCode = App.run(new String[] {"/tmp/some-repo"}, out);

        assertThat(exitCode).isZero();
    }
}
