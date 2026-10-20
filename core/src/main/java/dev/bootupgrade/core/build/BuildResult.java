package dev.bootupgrade.core.build;

import java.util.Objects;

/**
 * The outcome of invoking Maven against a target repository: whether the
 * build succeeded, and the test counts parsed out of the Surefire summary
 * lines in its output (summed across every module, for a multi-module repo).
 */
public record BuildResult(
        boolean success,
        int exitCode,
        int testsRun,
        int testsFailed,
        int testsErrored,
        int testsSkipped,
        String output) {

    public BuildResult {
        Objects.requireNonNull(output, "output must not be null");
    }

    public int testsPassed() {
        return testsRun - testsFailed - testsErrored - testsSkipped;
    }

    public boolean hasTestFailures() {
        return testsFailed > 0 || testsErrored > 0;
    }

    /** One-line human summary, e.g. "BUILD SUCCESS, 12 tests run, 0 failed, 0 errored". */
    public String summary() {
        return (success ? "BUILD SUCCESS" : "BUILD FAILURE (exit " + exitCode + ")")
                + ", " + testsRun + " tests run, " + testsFailed + " failed, " + testsErrored + " errored";
    }
}
