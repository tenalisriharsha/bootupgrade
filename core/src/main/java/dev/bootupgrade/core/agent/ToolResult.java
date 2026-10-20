package dev.bootupgrade.core.agent;

import java.util.Objects;

/** The outcome of one {@link AgentTool} invocation: what to tell the LLM, and whether the call succeeded. */
public record ToolResult(String output, boolean success) {

    public ToolResult {
        Objects.requireNonNull(output, "output must not be null");
    }

    public static ToolResult ok(String output) {
        return new ToolResult(output, true);
    }

    public static ToolResult failed(String output) {
        return new ToolResult(output, false);
    }
}
