package dev.bootupgrade.core.build;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultProcessRunnerTest {

    private final DefaultProcessRunner runner = new DefaultProcessRunner();

    @Test
    void capturesStdoutAndExitCodeOfASuccessfulProcess(@TempDir Path workingDirectory) {
        ProcessResult result = runner.run(
                List.of("sh", "-c", "echo hello-stdout; echo hello-stderr 1>&2"),
                workingDirectory,
                Duration.ofSeconds(10));

        assertThat(result.exitCode()).isZero();
        assertThat(result.succeeded()).isTrue();
        assertThat(result.timedOut()).isFalse();
        assertThat(result.stdout()).contains("hello-stdout");
        assertThat(result.stderr()).contains("hello-stderr");
    }

    @Test
    void reportsANonZeroExitCodeAsFailure(@TempDir Path workingDirectory) {
        ProcessResult result = runner.run(List.of("sh", "-c", "exit 3"), workingDirectory, Duration.ofSeconds(10));

        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void killsAProcessThatExceedsItsTimeout(@TempDir Path workingDirectory) {
        ProcessResult result = runner.run(List.of("sh", "-c", "sleep 5"), workingDirectory, Duration.ofMillis(200));

        assertThat(result.timedOut()).isTrue();
        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void rejectsAnEmptyCommand(@TempDir Path workingDirectory) {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> runner.run(List.of(), workingDirectory, Duration.ofSeconds(1)));
    }
}
