package dev.bootupgrade.api.model;

/** What kind of action an {@link UpgradeStepEntity} performed, mirroring core's StepKind. */
public enum StepKind {
    RECIPE,
    BUILD_CHECK,
    AGENT_FIX
}
