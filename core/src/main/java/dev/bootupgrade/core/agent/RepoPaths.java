package dev.bootupgrade.core.agent;

import java.nio.file.Path;

/** Shared path-safety check for {@link ReadFileTool} and {@link WriteFileTool}. */
final class RepoPaths {

    private RepoPaths() {
    }

    /** Resolves {@code relativePath} against {@code repoPath}, rejecting anything that escapes it. */
    static Path resolveWithinRepo(Path repoPath, String relativePath) {
        Path normalizedRepo = repoPath.toAbsolutePath().normalize();
        Path resolved = normalizedRepo.resolve(relativePath).normalize();
        if (!resolved.startsWith(normalizedRepo)) {
            throw new IllegalArgumentException("path '" + relativePath + "' escapes the repository root");
        }
        return resolved;
    }
}
