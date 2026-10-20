package dev.bootupgrade.core.build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Real {@link ProcessRunner}: spawns an actual OS process via
 * {@link ProcessBuilder}, draining stdout/stderr on separate threads so a
 * chatty child process (Maven) can never deadlock on a full pipe buffer.
 */
public final class DefaultProcessRunner implements ProcessRunner {

    @Override
    public ProcessResult run(List<String> command, Path workingDirectory, Duration timeout) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(workingDirectory, "workingDirectory must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }

        ProcessBuilder builder = new ProcessBuilder(command).directory(workingDirectory.toFile());
        ExecutorService drainPool = Executors.newFixedThreadPool(2);
        try {
            Process process = builder.start();
            Future<String> stdoutFuture = drainPool.submit(() -> drain(process.getInputStream()));
            Future<String> stderrFuture = drainPool.submit(() -> drain(process.getErrorStream()));

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ProcessResult(-1, safeGet(stdoutFuture), safeGet(stderrFuture), true);
            }
            return new ProcessResult(process.exitValue(), safeGet(stdoutFuture), safeGet(stderrFuture), false);
        } catch (IOException e) {
            throw new ProcessExecutionException("Failed to start process: " + command, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessExecutionException("Interrupted while waiting for process: " + command, e);
        } finally {
            drainPool.shutdownNow();
        }
    }

    private static String drain(InputStream stream) {
        try {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String safeGet(Future<String> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
            return "";
        }
    }
}
