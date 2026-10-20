package dev.bootupgrade.core.model;

/** Overall state of an {@link UpgradeRun}. */
public enum RunStatus {
    PENDING,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED,
    PARTIAL
}
