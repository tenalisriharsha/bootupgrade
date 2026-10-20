package dev.bootupgrade.core.agent;

import dev.bootupgrade.core.build.BuildResult;
import dev.bootupgrade.core.build.MavenBuildRunner;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/** Runs {@code mvn clean verify} against the target repository and reports the outcome. Takes no arguments. */
public final class RunBuildTool implements AgentTool {

    /** Output beyond this many characters is dropped so one bad build can't blow out the LLM context. */
    private static final int MAX_OUTPUT_LENGTH = 4_000;

    private final MavenBuildRunner buildRunner;

    public RunBuildTool(MavenBuildRunner buildRunner) {
        this.buildRunner = Objects.requireNonNull(buildRunner, "buildRunner must not be null");
    }

    @Override
    public String name() {
        return "run_build";
    }

    @Override
    public String description() {
        return "Runs `mvn clean verify` against the repository and reports whether it passed. No arguments.";
    }

    @Override
    public ToolResult execute(Path repoPath, Map<String, String> arguments) {
        BuildResult result = buildRunner.runVerify(repoPath);
        String output = result.summary() + "\n" + truncate(result.output());
        return result.success() ? ToolResult.ok(output) : ToolResult.failed(output);
    }

    private static String truncate(String text) {
        if (text.length() <= MAX_OUTPUT_LENGTH) {
            return text;
        }
        return text.substring(text.length() - MAX_OUTPUT_LENGTH) + "\n... (truncated, showing the tail only)";
    }
}
