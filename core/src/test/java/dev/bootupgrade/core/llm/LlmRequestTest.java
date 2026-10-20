package dev.bootupgrade.core.llm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmRequestTest {

    @Test
    void rejectsEmptyMessages() {
        assertThatThrownBy(() -> LlmRequest.of(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullMessages() {
        assertThatThrownBy(() -> LlmRequest.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void defaultsAllowedToolsToEmptyWhenOmitted() {
        LlmRequest request = LlmRequest.of(List.of(Message.user("hi")));

        assertThat(request.allowedTools()).isEmpty();
    }

    @Test
    void messagesAreImmutableRegardlessOfInputMutability() {
        List<Message> mutable = new ArrayList<>(List.of(Message.user("hi")));
        LlmRequest request = LlmRequest.of(mutable);
        mutable.add(Message.user("sneaky"));

        assertThat(request.messages()).hasSize(1);
        assertThatThrownBy(() -> request.messages().add(Message.user("blocked")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void carriesAllowedToolsWhenProvided() {
        ToolDefinition readFile = new ToolDefinition("read_file", "Reads a file's contents");
        LlmRequest request = LlmRequest.of(List.of(Message.system("sys"), Message.user("hi")), List.of(readFile));

        assertThat(request.allowedTools()).containsExactly(readFile);
    }
}
