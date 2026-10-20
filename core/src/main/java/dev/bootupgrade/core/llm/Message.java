package dev.bootupgrade.core.llm;

import java.util.Objects;

/** One turn in an {@link LlmRequest}'s conversation history. */
public record Message(Role role, String content) {

    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }

    public Message {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }

    public static Message system(String content) {
        return new Message(Role.SYSTEM, content);
    }

    public static Message user(String content) {
        return new Message(Role.USER, content);
    }

    public static Message assistant(String content) {
        return new Message(Role.ASSISTANT, content);
    }

    public static Message tool(String content) {
        return new Message(Role.TOOL, content);
    }
}
