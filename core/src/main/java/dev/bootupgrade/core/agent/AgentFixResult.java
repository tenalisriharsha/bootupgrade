package dev.bootupgrade.core.agent;

import java.util.List;
import java.util.Objects;

/** What an {@link AgentFixLoop} run produced: whether it reached a green build, and how. */
public record AgentFixResult(boolean fixed, int iterationsUsed, List<String> toolCallLog, String summary) {

    public AgentFixResult {
        Objects.requireNonNull(toolCallLog, "toolCallLog must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        toolCallLog = List.copyOf(toolCallLog);
    }

    public static AgentFixResult fixed(int iterationsUsed, List<String> toolCallLog) {
        return new AgentFixResult(true, iterationsUsed, toolCallLog,
                "build fixed after " + iterationsUsed + " agent iteration(s)");
    }

    public static AgentFixResult notFixed(int iterationsUsed, List<String> toolCallLog, String reason) {
        return new AgentFixResult(false, iterationsUsed, toolCallLog, reason);
    }
}
