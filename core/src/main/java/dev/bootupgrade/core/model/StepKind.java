package dev.bootupgrade.core.model;

/** What kind of action an {@link UpgradeStep} performed. */
public enum StepKind {
    /** A deterministic OpenRewrite recipe run against the target repository. */
    RECIPE,
    /** A build and/or test invocation used to check the repository's state. */
    BUILD_CHECK,
    /** A patch proposed and applied by the tool-limited LLM agent. */
    AGENT_FIX
}
