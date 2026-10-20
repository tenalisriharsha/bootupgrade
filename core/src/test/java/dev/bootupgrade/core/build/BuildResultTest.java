package dev.bootupgrade.core.build;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuildResultTest {

    @Test
    void computesTestsPassedFromTheOtherCounts() {
        BuildResult result = new BuildResult(true, 0, 10, 1, 2, 3, "output");

        assertThat(result.testsPassed()).isEqualTo(4);
    }

    @Test
    void reportsTestFailuresWhenEitherFailedOrErroredIsPositive() {
        assertThat(new BuildResult(false, 1, 5, 1, 0, 0, "").hasTestFailures()).isTrue();
        assertThat(new BuildResult(false, 1, 5, 0, 1, 0, "").hasTestFailures()).isTrue();
        assertThat(new BuildResult(true, 0, 5, 0, 0, 0, "").hasTestFailures()).isFalse();
    }

    @Test
    void summarizesASuccessfulBuild() {
        BuildResult result = new BuildResult(true, 0, 5, 0, 0, 0, "output");

        assertThat(result.summary()).isEqualTo("BUILD SUCCESS, 5 tests run, 0 failed, 0 errored");
    }

    @Test
    void summarizesAFailedBuildWithItsExitCode() {
        BuildResult result = new BuildResult(false, 1, 5, 2, 0, 0, "output");

        assertThat(result.summary()).isEqualTo("BUILD FAILURE (exit 1), 5 tests run, 2 failed, 0 errored");
    }

    @Test
    void rejectsANullOutput() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> new BuildResult(true, 0, 0, 0, 0, 0, null));
    }
}
