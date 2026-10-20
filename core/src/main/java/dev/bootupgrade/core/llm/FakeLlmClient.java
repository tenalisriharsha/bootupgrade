package dev.bootupgrade.core.llm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic, in-memory {@link LlmClient} used in tests and CI. Returns a
 * fixed, scripted sequence of responses in order, and records every request
 * it received so tests can assert on what the pipeline sent the model.
 *
 * <p>Never makes a network call and needs no API key, so the full upgrade
 * pipeline can be exercised in tests without touching a real LLM.
 */
public final class FakeLlmClient implements LlmClient {

    private final Deque<LlmResponse> scriptedResponses;
    private final List<LlmRequest> receivedRequests = new ArrayList<>();

    public FakeLlmClient(List<LlmResponse> scriptedResponses) {
        Objects.requireNonNull(scriptedResponses, "scriptedResponses must not be null");
        this.scriptedResponses = new ArrayDeque<>(scriptedResponses);
    }

    public static FakeLlmClient scripted(LlmResponse... responses) {
        return new FakeLlmClient(List.of(responses));
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        receivedRequests.add(request);
        LlmResponse next = scriptedResponses.pollFirst();
        if (next == null) {
            throw new IllegalStateException(
                    "FakeLlmClient script exhausted after " + receivedRequests.size() + " request(s)");
        }
        return next;
    }

    /** All requests received so far, in order, for test assertions. */
    public List<LlmRequest> receivedRequests() {
        return List.copyOf(receivedRequests);
    }

    public int remainingScriptedResponses() {
        return scriptedResponses.size();
    }
}
