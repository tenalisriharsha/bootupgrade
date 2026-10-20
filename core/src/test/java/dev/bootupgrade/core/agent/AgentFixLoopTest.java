package dev.bootupgrade.core.agent;

import dev.bootupgrade.core.build.BuildResult;
import dev.bootupgrade.core.config.UpgradeConfig;
import dev.bootupgrade.core.llm.FakeLlmClient;
import dev.bootupgrade.core.llm.LlmResponse;
import dev.bootupgrade.core.llm.ToolCall;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentFixLoopTest {

    private static final BuildResult FAILING_BUILD =
            new BuildResult(false, 1, 1, 1, 0, 0, "Tests run: 1, Failures: 1, Errors: 0, Skipped: 0\nBUILD FAILURE");

    private static UpgradeConfig configFor(Path repoPath, int maxAgentIterations, Set<String> allowedTools) {
        return new UpgradeConfig(repoPath, 21, "3.3.5", maxAgentIterations, allowedTools);
    }

    /** A fake "run_build" tool returning a scripted sequence of results, one per call. */
    private static final class ScriptedBuildTool implements AgentTool {
        private final Deque<ToolResult> scriptedResults;

        ScriptedBuildTool(ToolResult... results) {
            this.scriptedResults = new ArrayDeque<>(List.of(results));
        }

        @Override
        public String name() {
            return "run_build";
        }

        @Override
        public String description() {
            return "scripted build tool for tests";
        }

        @Override
        public ToolResult execute(Path repoPath, Map<String, String> arguments) {
            return scriptedResults.pollFirst();
        }
    }

    @Test
    void fixesTheBuildAfterReadingAndWritingAFileThenRebuilding(@TempDir Path repoPath) throws IOException {
        Files.writeString(repoPath.resolve("Broken.java"), "class Broken { int x = ; }");

        FakeLlmClient llmClient = FakeLlmClient.scripted(
                LlmResponse.toolCall(new ToolCall("read_file", Map.of("path", "Broken.java"))),
                LlmResponse.toolCall(new ToolCall("write_file", Map.of("path", "Broken.java", "content", "class Broken { int x = 0; }"))),
                LlmResponse.toolCall(new ToolCall("run_build", Map.of())));
        AgentFixLoop loop = new AgentFixLoop(llmClient, List.of(
                new ReadFileTool(), new WriteFileTool(), new ScriptedBuildTool(ToolResult.ok("BUILD SUCCESS"))));

        AgentFixResult result = loop.run(
                repoPath, configFor(repoPath, 5, Set.of("read_file", "write_file", "run_build")), FAILING_BUILD);

        assertThat(result.fixed()).isTrue();
        assertThat(result.iterationsUsed()).isEqualTo(3);
        assertThat(result.toolCallLog()).hasSize(3);
        assertThat(Files.readString(repoPath.resolve("Broken.java"))).isEqualTo("class Broken { int x = 0; }");
        assertThat(llmClient.receivedRequests()).hasSize(3);
    }

    @Test
    void stopsImmediatelyWhenTheAgentRepliesWithPlainTextInsteadOfATool(@TempDir Path repoPath) {
        FakeLlmClient llmClient = FakeLlmClient.scripted(
                LlmResponse.text("I cannot fix this within the given tool budget."));
        AgentFixLoop loop = new AgentFixLoop(llmClient, List.of(new ReadFileTool(), new WriteFileTool()));

        AgentFixResult result = loop.run(
                repoPath, configFor(repoPath, 5, Set.of("read_file", "write_file")), FAILING_BUILD);

        assertThat(result.fixed()).isFalse();
        assertThat(result.iterationsUsed()).isEqualTo(1);
        assertThat(result.summary()).contains("I cannot fix this");
        assertThat(result.toolCallLog()).isEmpty();
    }

    @Test
    void reportsNotFixedWhenTheIterationCapIsReachedWithoutAGreenBuild(@TempDir Path repoPath) {
        FakeLlmClient llmClient = FakeLlmClient.scripted(
                LlmResponse.toolCall(new ToolCall("run_build", Map.of())),
                LlmResponse.toolCall(new ToolCall("run_build", Map.of())));
        AgentFixLoop loop = new AgentFixLoop(llmClient, List.of(
                new ScriptedBuildTool(ToolResult.failed("still red"), ToolResult.failed("still red"))));

        AgentFixResult result = loop.run(repoPath, configFor(repoPath, 2, Set.of("run_build")), FAILING_BUILD);

        assertThat(result.fixed()).isFalse();
        assertThat(result.iterationsUsed()).isEqualTo(2);
        assertThat(result.summary()).contains("did not reach a green build within 2 iteration(s)");
        assertThat(result.toolCallLog()).hasSize(2);
    }

    @Test
    void rejectsAToolCallThatIsNotInTheAllowedListButKeepsGoing(@TempDir Path repoPath) {
        FakeLlmClient llmClient = FakeLlmClient.scripted(
                LlmResponse.toolCall(new ToolCall("write_file", Map.of("path", "x", "content", "y"))),
                LlmResponse.toolCall(new ToolCall("run_build", Map.of())));
        AgentFixLoop loop = new AgentFixLoop(llmClient, List.of(
                new WriteFileTool(), new ScriptedBuildTool(ToolResult.ok("BUILD SUCCESS"))));

        // write_file is deliberately left out of the allow-list.
        AgentFixResult result = loop.run(repoPath, configFor(repoPath, 5, Set.of("run_build")), FAILING_BUILD);

        assertThat(result.fixed()).isTrue();
        assertThat(result.iterationsUsed()).isEqualTo(2);
        assertThat(result.toolCallLog().get(0)).contains("rejected");
    }

    @Test
    void onlyOffersAllowedToolsToTheLlm(@TempDir Path repoPath) {
        FakeLlmClient llmClient = FakeLlmClient.scripted(LlmResponse.toolCall(new ToolCall("run_build", Map.of())));
        AgentFixLoop loop = new AgentFixLoop(llmClient, List.of(
                new ReadFileTool(), new WriteFileTool(), new ScriptedBuildTool(ToolResult.ok("ok"))));

        loop.run(repoPath, configFor(repoPath, 5, Set.of("run_build")), FAILING_BUILD);

        assertThat(llmClient.receivedRequests()).hasSize(1);
        assertThat(llmClient.receivedRequests().get(0).allowedTools())
                .extracting(dev.bootupgrade.core.llm.ToolDefinition::name)
                .containsExactly("run_build");
    }

    @Test
    void doesNothingWhenMaxIterationsIsZero(@TempDir Path repoPath) {
        FakeLlmClient llmClient = new FakeLlmClient(List.of());
        AgentFixLoop loop = new AgentFixLoop(llmClient, List.of());

        AgentFixResult result = loop.run(repoPath, configFor(repoPath, 0, Set.of()), FAILING_BUILD);

        assertThat(result.fixed()).isFalse();
        assertThat(result.iterationsUsed()).isZero();
        assertThat(llmClient.receivedRequests()).isEmpty();
    }
}
