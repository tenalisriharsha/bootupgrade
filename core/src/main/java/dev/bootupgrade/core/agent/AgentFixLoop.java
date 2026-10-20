package dev.bootupgrade.core.agent;

import dev.bootupgrade.core.build.BuildResult;
import dev.bootupgrade.core.config.UpgradeConfig;
import dev.bootupgrade.core.llm.LlmClient;
import dev.bootupgrade.core.llm.LlmRequest;
import dev.bootupgrade.core.llm.LlmResponse;
import dev.bootupgrade.core.llm.Message;
import dev.bootupgrade.core.llm.ToolCall;
import dev.bootupgrade.core.llm.ToolDefinition;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The Phase 3 fallback: when the deterministic recipes leave the build red, this loop hands the
 * failure to an {@link LlmClient} with a tight, explicit tool budget (read a file, write a file,
 * run the build — nothing else, per {@link UpgradeConfig#allowedToolNames()}) and lets it propose
 * and apply fixes, rebuilding after every change, until the build is green or
 * {@link UpgradeConfig#maxAgentIterations()} is reached.
 *
 * <p>The loop never pretends to succeed: if the iteration cap is hit, it returns a non-fixed
 * {@link AgentFixResult} naming exactly what was tried, instead of a false green.
 */
public final class AgentFixLoop {

    private static final String SYSTEM_PROMPT =
            "You are a tool-limited agent fixing a Spring Boot upgrade that deterministic recipes "
                    + "could not finish. You may only use the tools offered to you (reading a file, "
                    + "writing a file, or running the build). Inspect the failing file(s) with "
                    + "read_file, write a corrected version with write_file, then call run_build to "
                    + "check your fix. If you believe the build cannot be fixed within your tool "
                    + "budget, reply with plain text explaining why instead of calling a tool.";

    /** Argument values longer than this are shown truncated in the human-readable tool-call log. */
    private static final int LOG_ARGUMENT_PREVIEW_LENGTH = 60;

    /** The initial failing build's output is truncated to this many characters in the first prompt. */
    private static final int INITIAL_OUTPUT_PREVIEW_LENGTH = 6_000;

    private final LlmClient llmClient;
    private final Map<String, AgentTool> toolsByName;

    public AgentFixLoop(LlmClient llmClient, List<AgentTool> tools) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient must not be null");
        Objects.requireNonNull(tools, "tools must not be null");
        Map<String, AgentTool> byName = new LinkedHashMap<>();
        for (AgentTool tool : tools) {
            byName.put(tool.name(), tool);
        }
        this.toolsByName = Map.copyOf(byName);
    }

    /**
     * Runs the fix loop against {@code repoPath}, starting from {@code initialBuildResult} (the
     * build check that triggered the fallback). Respects {@code config}'s iteration cap and tool
     * allow-list.
     */
    public AgentFixResult run(Path repoPath, UpgradeConfig config, BuildResult initialBuildResult) {
        Objects.requireNonNull(repoPath, "repoPath must not be null");
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(initialBuildResult, "initialBuildResult must not be null");

        List<ToolDefinition> allowedDefinitions = toolsByName.values().stream()
                .filter(tool -> config.allowedToolNames().contains(tool.name()))
                .map(tool -> new ToolDefinition(tool.name(), tool.description()))
                .toList();

        List<Message> messages = new ArrayList<>();
        messages.add(Message.system(SYSTEM_PROMPT));
        messages.add(Message.user("The build is currently failing after the deterministic recipes ran:\n\n"
                + truncate(initialBuildResult.output(), INITIAL_OUTPUT_PREVIEW_LENGTH)));

        List<String> toolCallLog = new ArrayList<>();
        int maxIterations = config.maxAgentIterations();

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            LlmResponse response = llmClient.complete(LlmRequest.of(List.copyOf(messages), allowedDefinitions));

            if (response instanceof LlmResponse.Text text) {
                return AgentFixResult.notFixed(iteration, toolCallLog,
                        "agent stopped without a green build: " + text.content());
            }

            ToolCall call = ((LlmResponse.ToolInvocation) response).call();
            messages.add(Message.assistant("tool_call: " + describe(call)));

            AgentTool tool = toolsByName.get(call.toolName());
            if (tool == null || !config.allowedToolNames().contains(call.toolName())) {
                String error = "tool '" + call.toolName() + "' is not in the allowed tool list for this run";
                toolCallLog.add(describe(call) + " -> rejected: " + error);
                messages.add(Message.tool("ERROR: " + error));
                continue;
            }

            ToolResult result = tool.execute(repoPath, call.arguments());
            toolCallLog.add(describe(call) + " -> " + (result.success() ? "ok" : "failed"));
            messages.add(Message.tool(result.output()));

            if (call.toolName().equals("run_build") && result.success()) {
                return AgentFixResult.fixed(iteration, toolCallLog);
            }
        }

        return AgentFixResult.notFixed(maxIterations, toolCallLog,
                "agent did not reach a green build within " + maxIterations + " iteration(s)");
    }

    private static String describe(ToolCall call) {
        if (call.arguments().isEmpty()) {
            return call.toolName();
        }
        String args = call.arguments().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + truncate(entry.getValue(), LOG_ARGUMENT_PREVIEW_LENGTH))
                .collect(Collectors.joining(", "));
        return call.toolName() + "(" + args + ")";
    }

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... (truncated)";
    }
}
