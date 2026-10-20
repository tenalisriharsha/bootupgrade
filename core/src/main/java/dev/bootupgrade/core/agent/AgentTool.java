package dev.bootupgrade.core.agent;

import java.nio.file.Path;
import java.util.Map;

/**
 * One tool the {@link AgentFixLoop} is allowed to offer the LLM. The whole
 * point of Phase 3's tight tool budget is that this is the complete set of
 * things the agent can do to a target repository: read a file, write a
 * file, or run the build — nothing else.
 */
public interface AgentTool {

    /** The tool's name, as it appears in {@link dev.bootupgrade.core.config.UpgradeConfig#allowedToolNames()}. */
    String name();

    /** A short description handed to the LLM so it knows when and how to call this tool. */
    String description();

    /**
     * Executes the tool against {@code repoPath} with the given arguments. Must never throw —
     * failures (bad arguments, I/O errors, path traversal attempts) come back as a failed
     * {@link ToolResult} so the agent loop can feed the error back to the LLM instead of crashing.
     */
    ToolResult execute(Path repoPath, Map<String, String> arguments);
}
