package dev.bootupgrade.core.git;

/** Thrown when a {@link GitOperations} call fails for a reason outside caller control. */
public final class GitOperationException extends RuntimeException {

    public GitOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
