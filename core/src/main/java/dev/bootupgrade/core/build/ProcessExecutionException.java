package dev.bootupgrade.core.build;

/** Thrown when a {@link ProcessRunner} fails to start or wait for a process. */
public final class ProcessExecutionException extends RuntimeException {

    public ProcessExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
