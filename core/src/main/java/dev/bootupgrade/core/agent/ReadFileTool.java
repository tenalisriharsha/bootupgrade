package dev.bootupgrade.core.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Reads a UTF-8 text file at a path relative to the target repository's root. */
public final class ReadFileTool implements AgentTool {

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String description() {
        return "Reads a UTF-8 text file. Argument: 'path' (relative to the repository root).";
    }

    @Override
    public ToolResult execute(Path repoPath, Map<String, String> arguments) {
        String relativePath = arguments.get("path");
        if (relativePath == null || relativePath.isBlank()) {
            return ToolResult.failed("ERROR: read_file requires a non-blank 'path' argument");
        }
        Path resolved;
        try {
            resolved = RepoPaths.resolveWithinRepo(repoPath, relativePath);
        } catch (IllegalArgumentException e) {
            return ToolResult.failed("ERROR: " + e.getMessage());
        }
        if (!Files.isRegularFile(resolved)) {
            return ToolResult.failed("ERROR: no such file '" + relativePath + "'");
        }
        try {
            return ToolResult.ok(Files.readString(resolved));
        } catch (IOException e) {
            return ToolResult.failed("ERROR: could not read '" + relativePath + "': " + e.getMessage());
        }
    }
}
