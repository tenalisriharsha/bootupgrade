package dev.bootupgrade.core.build;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Scripted {@link ProcessRunner} test double: returns a fixed result and records what it was called with. */
public final class FakeProcessRunner implements ProcessRunner {

    private final ProcessResult scriptedResult;
    private List<String> lastCommand;
    private Path lastWorkingDirectory;
    private Duration lastTimeout;

    public FakeProcessRunner(ProcessResult scriptedResult) {
        this.scriptedResult = scriptedResult;
    }

    @Override
    public ProcessResult run(List<String> command, Path workingDirectory, Duration timeout) {
        this.lastCommand = List.copyOf(command);
        this.lastWorkingDirectory = workingDirectory;
        this.lastTimeout = timeout;
        return scriptedResult;
    }

    public List<String> lastCommand() {
        return lastCommand;
    }

    public Path lastWorkingDirectory() {
        return lastWorkingDirectory;
    }

    public Duration lastTimeout() {
        return lastTimeout;
    }
}
