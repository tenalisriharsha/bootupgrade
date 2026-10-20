package dev.bootupgrade.core.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Overwrites (or creates) a UTF-8 text file at a path relative to the target repository's root. */
public final class WriteFileTool implements AgentTool {

    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public String description() {
        return "Writes a UTF-8 text file, creating parent directories as needed. "
                + "Arguments: 'path' (relative to the repository root), 'content' (the full new file content).";
    }

    @Override
    public ToolResult execute(Path repoPath, Map<String, String> arguments) {
        String relativePath = arguments.get("path");
        String content = arguments.get("content");
        if (relativePath == null || relativePath.isBlank()) {
            return ToolResult.failed("ERROR: write_file requires a non-blank 'path' argument");
        }
        if (content == null) {
            return ToolResult.failed("ERROR: write_file requires a 'content' argument");
        }
        Path resolved;
        try {
            resolved = RepoPaths.resolveWithinRepo(repoPath, relativePath);
        } catch (IllegalArgumentException e) {
            return ToolResult.failed("ERROR: " + e.getMessage());
        }
        try {
            if (resolved.getParent() != null) {
                Files.createDirectories(resolved.getParent());
            }
            Files.writeString(resolved, content);
            return ToolResult.ok("wrote " + content.length() + " character(s) to '" + relativePath + "'");
        } catch (IOException e) {
            return ToolResult.failed("ERROR: could not write '" + relativePath + "': " + e.getMessage());
        }
    }
}
