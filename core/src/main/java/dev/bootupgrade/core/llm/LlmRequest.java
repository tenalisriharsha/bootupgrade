package dev.bootupgrade.core.llm;

import java.util.List;
import java.util.Objects;

/**
 * A request to the LLM: the conversation so far, and the tools it is
 * currently allowed to call. The agent loop (Phase 3) is responsible for
 * keeping {@code allowedTools} to the tight budget the task requires.
 */
public record LlmRequest(List<Message> messages, List<ToolDefinition> allowedTools) {

    public LlmRequest {
        Objects.requireNonNull(messages, "messages must not be null");
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        messages = List.copyOf(messages);
        allowedTools = allowedTools == null ? List.of() : List.copyOf(allowedTools);
    }

    public static LlmRequest of(List<Message> messages) {
        return new LlmRequest(messages, List.of());
    }

    public static LlmRequest of(List<Message> messages, List<ToolDefinition> allowedTools) {
        return new LlmRequest(messages, allowedTools);
    }
}
