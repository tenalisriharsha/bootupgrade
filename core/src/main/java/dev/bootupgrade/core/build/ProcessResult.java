package dev.bootupgrade.core.build;

import java.util.Objects;

/** The outcome of running one external process to completion (or timeout). */
public record ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut) {

    public ProcessResult {
        Objects.requireNonNull(stdout, "stdout must not be null");
        Objects.requireNonNull(stderr, "stderr must not be null");
    }

    public boolean succeeded() {
        return !timedOut && exitCode == 0;
    }
}
