package dev.bootupgrade.core.model;

/** Outcome of a single {@link UpgradeStep}. */
public enum StepStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    SKIPPED
}
