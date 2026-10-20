package dev.bootupgrade.core.build;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MavenBuildRunnerTest {

    @Test
    void buildsTheMvnCommandAndReportsSuccess(@TempDir Path repoPath) {
        FakeProcessRunner processRunner = new FakeProcessRunner(
                new ProcessResult(0, "BUILD SUCCESS", "", false));
        MavenBuildRunner runner = new MavenBuildRunner(processRunner, "mvn", Duration.ofMinutes(1));

        BuildResult result = runner.runVerify(repoPath);

        assertThat(result.success()).isTrue();
        assertThat(processRunner.lastCommand())
                .containsExactly("mvn", "-B", "-f", repoPath.toString(), "clean", "verify");
        assertThat(processRunner.lastWorkingDirectory()).isEqualTo(repoPath);
    }

    @Test
    void sumsSurefireSummaryLinesAcrossMultipleModulesWithoutDoubleCountingPerClassLines(@TempDir Path repoPath) {
        String output = """
                Running dev.bootupgrade.FooTest
                Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.1 s

                Results:

                Tests run: 2, Failures: 0, Errors: 0, Skipped: 0

                Running dev.bootupgrade.BarTest
                Tests run: 3, Failures: 1, Errors: 0, Skipped: 1, Time elapsed: 0.2 s

                Results:

                Tests run: 3, Failures: 1, Errors: 0, Skipped: 1
                """;
        FakeProcessRunner processRunner = new FakeProcessRunner(new ProcessResult(0, output, "", false));
        MavenBuildRunner runner = new MavenBuildRunner(processRunner);

        BuildResult result = runner.runVerify(repoPath);

        assertThat(result.testsRun()).isEqualTo(5);
        assertThat(result.testsFailed()).isEqualTo(1);
        assertThat(result.testsSkipped()).isEqualTo(1);
    }

    @Test
    void reportsFailureWhenTheProcessExitsNonZero(@TempDir Path repoPath) {
        FakeProcessRunner processRunner = new FakeProcessRunner(
                new ProcessResult(1, "BUILD FAILURE", "", false));
        MavenBuildRunner runner = new MavenBuildRunner(processRunner);

        BuildResult result = runner.runVerify(repoPath);

        assertThat(result.success()).isFalse();
        assertThat(result.exitCode()).isEqualTo(1);
    }

    @Test
    void reportsFailureWhenTheProcessTimesOutEvenIfExitCodeLooksZero(@TempDir Path repoPath) {
        FakeProcessRunner processRunner = new FakeProcessRunner(new ProcessResult(0, "", "", true));
        MavenBuildRunner runner = new MavenBuildRunner(processRunner);

        BuildResult result = runner.runVerify(repoPath);

        assertThat(result.success()).isFalse();
    }

    @Test
    void runWithCustomGoalsRejectsAnEmptyGoalList(@TempDir Path repoPath) {
        MavenBuildRunner runner = new MavenBuildRunner(new FakeProcessRunner(new ProcessResult(0, "", "", false)));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> runner.run(repoPath));
    }
}
