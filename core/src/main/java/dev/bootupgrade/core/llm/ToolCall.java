package dev.bootupgrade.core.llm;

import java.util.Map;
import java.util.Objects;

/** A request from the LLM to invoke one allowed tool with the given arguments. */
public record ToolCall(String toolName, Map<String, String> arguments) {

    public ToolCall {
        Objects.requireNonNull(toolName, "toolName must not be null");
        if (toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be blank");
        }
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}
