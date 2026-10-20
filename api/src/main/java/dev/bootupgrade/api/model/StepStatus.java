package dev.bootupgrade.api.model;

/** Outcome of a single {@link UpgradeStepEntity}, mirroring core's StepStatus. */
public enum StepStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    SKIPPED
}
