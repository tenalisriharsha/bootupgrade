package dev.bootupgrade.core.agent;

import dev.bootupgrade.core.build.FakeProcessRunner;
import dev.bootupgrade.core.build.MavenBuildRunner;
import dev.bootupgrade.core.build.ProcessResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RunBuildToolTest {

    @Test
    void succeedsWhenTheBuildPasses(@TempDir Path repoPath) {
        FakeProcessRunner processRunner = new FakeProcessRunner(
                new ProcessResult(0, "Tests run: 3, Failures: 0, Errors: 0, Skipped: 0\nBUILD SUCCESS", "", false));
        RunBuildTool tool = new RunBuildTool(new MavenBuildRunner(processRunner));

        ToolResult result = tool.execute(repoPath, Map.of());

        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("BUILD SUCCESS").contains("3 tests run");
    }

    @Test
    void failsWhenTheBuildFails(@TempDir Path repoPath) {
        FakeProcessRunner processRunner = new FakeProcessRunner(new ProcessResult(1, "BUILD FAILURE", "", false));
        RunBuildTool tool = new RunBuildTool(new MavenBuildRunner(processRunner));

        ToolResult result = tool.execute(repoPath, Map.of());

        assertThat(result.success()).isFalse();
        assertThat(result.output()).contains("BUILD FAILURE");
    }

    @Test
    void truncatesVeryLongOutputToItsTail(@TempDir Path repoPath) {
        String hugeOutput = "x".repeat(10_000);
        FakeProcessRunner processRunner = new FakeProcessRunner(new ProcessResult(0, hugeOutput, "", false));
        RunBuildTool tool = new RunBuildTool(new MavenBuildRunner(processRunner));

        ToolResult result = tool.execute(repoPath, Map.of());

        assertThat(result.output()).contains("truncated");
        assertThat(result.output().length()).isLessThan(hugeOutput.length());
    }
}
