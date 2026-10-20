package dev.bootupgrade.core.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WriteFileToolTest {

    private final WriteFileTool tool = new WriteFileTool();

    @Test
    void writesAndOverwritesAnExistingFile(@TempDir Path repoPath) throws IOException {
        Files.writeString(repoPath.resolve("Foo.java"), "old content");

        ToolResult result = tool.execute(repoPath, Map.of("path", "Foo.java", "content", "new content"));

        assertThat(result.success()).isTrue();
        assertThat(Files.readString(repoPath.resolve("Foo.java"))).isEqualTo("new content");
    }

    @Test
    void createsParentDirectoriesForANewFile(@TempDir Path repoPath) throws IOException {
        ToolResult result = tool.execute(
                repoPath, Map.of("path", "src/main/java/dev/example/Foo.java", "content", "package dev.example;"));

        assertThat(result.success()).isTrue();
        assertThat(Files.readString(repoPath.resolve("src/main/java/dev/example/Foo.java")))
                .isEqualTo("package dev.example;");
    }

    @Test
    void failsForAMissingPathArgument(@TempDir Path repoPath) {
        ToolResult result = tool.execute(repoPath, Map.of("content", "x"));

        assertThat(result.success()).isFalse();
        assertThat(result.output()).contains("path");
    }

    @Test
    void failsForAMissingContentArgument(@TempDir Path repoPath) {
        ToolResult result = tool.execute(repoPath, Map.of("path", "Foo.java"));

        assertThat(result.success()).isFalse();
        assertThat(result.output()).contains("content");
    }

    @Test
    void rejectsAPathThatEscapesTheRepoRoot(@TempDir Path repoPath) {
        ToolResult result = tool.execute(repoPath, Map.of("path", "../escaped.txt", "content", "x"));

        assertThat(result.success()).isFalse();
        assertThat(result.output()).contains("escapes the repository root");
    }
}
