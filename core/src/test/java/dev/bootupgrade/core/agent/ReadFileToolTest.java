package dev.bootupgrade.core.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReadFileToolTest {

    private final ReadFileTool tool = new ReadFileTool();

    @Test
    void readsAnExistingFileRelativeToTheRepoRoot(@TempDir Path repoPath) throws IOException {
        Files.writeString(repoPath.resolve("Foo.java"), "class Foo {}");

        ToolResult result = tool.execute(repoPath, Map.of("path", "Foo.java"));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).isEqualTo("class Foo {}");
    }

    @Test
    void readsAFileInANestedDirectory(@TempDir Path repoPath) throws IOException {
        Files.createDirectories(repoPath.resolve("src/main/java/dev/example"));
        Files.writeString(repoPath.resolve("src/main/java/dev/example/Foo.java"), "package dev.example;");

        ToolResult result = tool.execute(repoPath, Map.of("path", "src/main/java/dev/example/Foo.java"));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).isEqualTo("package dev.example;");
    }

    @Test
    void failsForAMissingFile(@TempDir Path repoPath) {
        ToolResult result = tool.execute(repoPath, Map.of("path", "DoesNotExist.java"));

        assertThat(result.success()).isFalse();
        assertThat(result.output()).contains("DoesNotExist.java");
    }

    @Test
    void failsForAMissingPathArgument(@TempDir Path repoPath) {
        ToolResult result = tool.execute(repoPath, Map.of());

        assertThat(result.success()).isFalse();
        assertThat(result.output()).contains("path");
    }

    @Test
    void rejectsAPathThatEscapesTheRepoRoot(@TempDir Path repoPath) throws IOException {
        Path outside = repoPath.getParent().resolve("secret.txt");
        Files.writeString(outside, "top secret");

        ToolResult result = tool.execute(repoPath, Map.of("path", "../secret.txt"));

        assertThat(result.success()).isFalse();
        assertThat(result.output()).contains("escapes the repository root");

        Files.deleteIfExists(outside);
    }
}
