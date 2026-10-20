package dev.bootupgrade.core.llm;

import java.util.Objects;

/**
 * Describes one tool the agent is allowed to call. The actual tool allow-list
 * for a run comes from {@link dev.bootupgrade.core.config.UpgradeConfig} —
 * this type only carries enough information for the LLM to decide when to
 * call it.
 */
public record ToolDefinition(String name, String description) {

    public ToolDefinition {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
