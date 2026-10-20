package dev.bootupgrade.core.build;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Invokes {@code mvn clean verify} against a target repository and parses
 * the Surefire test-count summary out of its output. Used after every
 * upgrade step (recipe or agent fix) to check whether the repository is in
 * a better, equal, or worse state.
 */
public final class MavenBuildRunner {

    /** Matches a module's aggregated Surefire summary line, e.g. "Tests run: 5, Failures: 0, Errors: 0, Skipped: 0",
     * with or without the "[INFO] " prefix that Maven prints in batch mode.
     * Deliberately excludes the per-class line (which carries a trailing ", Time elapsed: ..." suffix) by
     * requiring the whole trimmed line to match, so module totals are never double-counted. */
    private static final Pattern SUREFIRE_SUMMARY = Pattern.compile(
            "(?:\\[INFO\\]\\s+)?Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)");

    private final ProcessRunner processRunner;
    private final String mvnExecutable;
    private final Duration timeout;

    public MavenBuildRunner(ProcessRunner processRunner, String mvnExecutable, Duration timeout) {
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner must not be null");
        this.mvnExecutable = Objects.requireNonNull(mvnExecutable, "mvnExecutable must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
    }

    public MavenBuildRunner(ProcessRunner processRunner) {
        this(processRunner, "mvn", Duration.ofMinutes(10));
    }

    /** Runs {@code mvn -B clean verify} against {@code repoPath} and parses the result. */
    public BuildResult runVerify(Path repoPath) {
        return run(repoPath, "clean", "verify");
    }

    /** Runs {@code mvn -B <goals>} against {@code repoPath} and parses the result. */
    public BuildResult run(Path repoPath, String... goals) {
        Objects.requireNonNull(repoPath, "repoPath must not be null");
        if (goals.length == 0) {
            throw new IllegalArgumentException("at least one goal is required");
        }
        List<String> command = new java.util.ArrayList<>();
        command.add(mvnExecutable);
        command.add("-B");
        command.add("-f");
        command.add(repoPath.toString());
        command.addAll(List.of(goals));

        ProcessResult result = processRunner.run(command, repoPath, timeout);
        return parse(result);
    }

    private static BuildResult parse(ProcessResult result) {
        int testsRun = 0;
        int testsFailed = 0;
        int testsErrored = 0;
        int testsSkipped = 0;
        for (String line : result.stdout().split("\\R")) {
            Matcher matcher = SUREFIRE_SUMMARY.matcher(line.strip());
            if (matcher.matches()) {
                testsRun += Integer.parseInt(matcher.group(1));
                testsFailed += Integer.parseInt(matcher.group(2));
                testsErrored += Integer.parseInt(matcher.group(3));
                testsSkipped += Integer.parseInt(matcher.group(4));
            }
        }
        boolean success = result.succeeded();
        return new BuildResult(success, result.exitCode(), testsRun, testsFailed, testsErrored, testsSkipped,
                result.stdout() + result.stderr());
    }
}
