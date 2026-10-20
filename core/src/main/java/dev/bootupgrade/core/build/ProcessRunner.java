package dev.bootupgrade.core.build;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Abstraction over running an external process (Maven, in practice). Lets
 * tests substitute a fake instead of spawning real child processes, while
 * {@link DefaultProcessRunner} does the real work in production.
 */
public interface ProcessRunner {

    /** Runs {@code command} with working directory {@code workingDirectory}, waiting up to {@code timeout}. */
    ProcessResult run(List<String> command, Path workingDirectory, Duration timeout);
}
