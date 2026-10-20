package dev.bootupgrade.core.config;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for one upgrade run: where the target repository is, what
 * versions it should end up on, and the guardrails for the LLM agent
 * fallback (Phase 3).
 */
public record UpgradeConfig(
        Path repositoryPath,
        int targetJavaVersion,
        String targetSpringBootVersion,
        int maxAgentIterations,
        Set<String> allowedToolNames) {

    public static final int MIN_SUPPORTED_JAVA_VERSION = 17;

    public UpgradeConfig {
        Objects.requireNonNull(repositoryPath, "repositoryPath must not be null");
        Objects.requireNonNull(targetSpringBootVersion, "targetSpringBootVersion must not be null");
        Objects.requireNonNull(allowedToolNames, "allowedToolNames must not be null");
        if (targetJavaVersion < MIN_SUPPORTED_JAVA_VERSION) {
            throw new IllegalArgumentException(
                    "targetJavaVersion must be >= " + MIN_SUPPORTED_JAVA_VERSION + ", got " + targetJavaVersion);
        }
        if (targetSpringBootVersion.isBlank()) {
            throw new IllegalArgumentException("targetSpringBootVersion must not be blank");
        }
        if (maxAgentIterations < 0) {
            throw new IllegalArgumentException(
                    "maxAgentIterations must not be negative, got " + maxAgentIterations);
        }
        allowedToolNames = Set.copyOf(allowedToolNames);
    }

    /** Java 21, Spring Boot 3.3.5, 5 agent iterations, the read/write/build-file tool trio. */
    public static UpgradeConfig defaultFor(Path repositoryPath) {
        return new UpgradeConfig(repositoryPath, 21, "3.3.5", 5, Set.of("read_file", "write_file", "run_build"));
    }
}
