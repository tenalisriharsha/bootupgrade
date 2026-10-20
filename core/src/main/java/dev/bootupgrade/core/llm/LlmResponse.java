package dev.bootupgrade.core.llm;

import java.util.Objects;

/**
 * What the LLM returned for one {@link LlmRequest}: either a plain text
 * answer, or a request to call one of the allowed tools.
 */
public sealed interface LlmResponse {

    record Text(String content) implements LlmResponse {
        public Text {
            Objects.requireNonNull(content, "content must not be null");
        }
    }

    record ToolInvocation(ToolCall call) implements LlmResponse {
        public ToolInvocation {
            Objects.requireNonNull(call, "call must not be null");
        }
    }

    static LlmResponse text(String content) {
        return new Text(content);
    }

    static LlmResponse toolCall(ToolCall call) {
        return new ToolInvocation(call);
    }
}
