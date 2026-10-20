package dev.bootupgrade.core.llm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakeLlmClientTest {

    @Test
    void returnsScriptedResponsesInOrder() {
        FakeLlmClient client = FakeLlmClient.scripted(
                LlmResponse.text("first"),
                LlmResponse.text("second"));

        LlmRequest request = LlmRequest.of(List.of(Message.user("hello")));

        assertThat(client.complete(request)).isEqualTo(new LlmResponse.Text("first"));
        assertThat(client.complete(request)).isEqualTo(new LlmResponse.Text("second"));
    }

    @Test
    void recordsEveryRequestReceived() {
        FakeLlmClient client = FakeLlmClient.scripted(LlmResponse.text("ok"), LlmResponse.text("ok2"));
        LlmRequest first = LlmRequest.of(List.of(Message.user("one")));
        LlmRequest second = LlmRequest.of(List.of(Message.user("two")));

        client.complete(first);
        client.complete(second);

        assertThat(client.receivedRequests()).containsExactly(first, second);
    }

    @Test
    void canReturnToolCallResponses() {
        ToolCall call = new ToolCall("run_build", Map.of("goal", "verify"));
        FakeLlmClient client = FakeLlmClient.scripted(LlmResponse.toolCall(call));

        LlmResponse response = client.complete(LlmRequest.of(List.of(Message.user("fix it"))));

        assertThat(response).isInstanceOf(LlmResponse.ToolInvocation.class);
        assertThat(((LlmResponse.ToolInvocation) response).call()).isEqualTo(call);
    }

    @Test
    void throwsWhenScriptExhausted() {
        FakeLlmClient client = FakeLlmClient.scripted(LlmResponse.text("only one"));
        LlmRequest request = LlmRequest.of(List.of(Message.user("hi")));

        client.complete(request);

        assertThatThrownBy(() -> client.complete(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exhausted");
    }

    @Test
    void remainingScriptedResponsesDecreasesAsConsumed() {
        FakeLlmClient client = FakeLlmClient.scripted(LlmResponse.text("a"), LlmResponse.text("b"));
        assertThat(client.remainingScriptedResponses()).isEqualTo(2);

        client.complete(LlmRequest.of(List.of(Message.user("x"))));

        assertThat(client.remainingScriptedResponses()).isEqualTo(1);
    }
}
