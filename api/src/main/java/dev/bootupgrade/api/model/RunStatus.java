package dev.bootupgrade.api.model;

/** Overall state of an {@link UpgradeRunEntity}, mirroring core's RunStatus. */
public enum RunStatus {
    PENDING,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED,
    PARTIAL
}
