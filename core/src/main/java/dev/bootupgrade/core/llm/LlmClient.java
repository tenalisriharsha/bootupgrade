package dev.bootupgrade.core.llm;

/**
 * Abstraction over the LLM used by the agent (Phase 3). Production code talks
 * to a real model through an implementation of this interface; tests and CI
 * use {@link FakeLlmClient}, a deterministic in-memory fake, so the upgrade
 * pipeline never needs network access or API keys to be tested.
 */
public interface LlmClient {

    /**
     * Completes one request. Implementations must be deterministic given the
     * same request where possible, and must never throw for a well-formed
     * request — failures are reported as an {@link LlmResponse.Text} the
     * caller can interpret, not as exceptions.
     */
    LlmResponse complete(LlmRequest request);
}
