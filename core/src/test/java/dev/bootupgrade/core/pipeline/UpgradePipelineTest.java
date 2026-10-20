package dev.bootupgrade.core.pipeline;

import dev.bootupgrade.core.build.MavenBuildRunner;
import dev.bootupgrade.core.build.ProcessResult;
import dev.bootupgrade.core.build.ProcessRunner;
import dev.bootupgrade.core.config.UpgradeConfig;
import dev.bootupgrade.core.git.GitOperations;
import dev.bootupgrade.core.model.RunStatus;
import dev.bootupgrade.core.model.StepKind;
import dev.bootupgrade.core.model.StepStatus;
import dev.bootupgrade.core.model.UpgradeRun;
import dev.bootupgrade.core.recipes.RecipeRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradePipelineTest {

    private static UpgradeConfig configFor(Path repoPath) {
        return new UpgradeConfig(repoPath, 21, "3.3.5", 5, Set.of());
    }

    private static ProcessRunner alwaysReturning(ProcessResult result) {
        return (command, workingDirectory, timeout) -> result;
    }

    @Test
    void aGreenBuildWithNoRecipeChangesSucceeds(@TempDir Path repoPath) throws IOException {
        Files.writeString(repoPath.resolve("pom.xml"), "<project/>");
        GitOperations git = new GitOperations();
        git.ensureInitialized(repoPath);

        RecipeRunner recipeRunner = new RecipeRunner(alwaysReturning(new ProcessResult(0, "no changes", "", false)));
        MavenBuildRunner buildRunner = new MavenBuildRunner(alwaysReturning(new ProcessResult(0, "BUILD SUCCESS", "", false)));
        UpgradePipeline pipeline = new UpgradePipeline(git, recipeRunner, buildRunner);

        UpgradeRun run = pipeline.run(configFor(repoPath), "my-service", "8", "2.7.18");

        assertThat(run.status()).isEqualTo(RunStatus.SUCCEEDED);
        assertThat(run.steps()).hasSize(2);
        assertThat(run.steps().get(0).kind()).isEqualTo(StepKind.RECIPE);
        assertThat(run.steps().get(0).status()).isEqualTo(StepStatus.SUCCEEDED);
        assertThat(run.steps().get(1).kind()).isEqualTo(StepKind.BUILD_CHECK);
        assertThat(run.steps().get(1).status()).isEqualTo(StepStatus.SUCCEEDED);
        assertThat(run.branchName()).startsWith("bootupgrade/my-service-");
        assertThat(run.targetJavaVersion()).isEqualTo("21");
    }

    @Test
    void recipeChangesAreCommittedAndReflectedInTheStepSummary(@TempDir Path repoPath) throws IOException {
        Files.writeString(repoPath.resolve("pom.xml"), "<project/>");
        GitOperations git = new GitOperations();
        git.ensureInitialized(repoPath);

        ProcessRunner recipeProcessRunner = (command, workingDirectory, timeout) -> {
            try {
                Files.writeString(repoPath.resolve("Migrated.java"), "// javax -> jakarta");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new ProcessResult(0, "1 file changed", "", false);
        };
        RecipeRunner recipeRunner = new RecipeRunner(recipeProcessRunner);
        MavenBuildRunner buildRunner = new MavenBuildRunner(alwaysReturning(new ProcessResult(0, "BUILD SUCCESS", "", false)));
        UpgradePipeline pipeline = new UpgradePipeline(git, recipeRunner, buildRunner);

        UpgradeRun run = pipeline.run(configFor(repoPath), "my-service", "8", "2.7.18");

        assertThat(run.steps().get(0).diffSummary()).contains("Migrated.java");
        assertThat(git.uncommittedChanges(repoPath)).isEmpty();
    }

    @Test
    void aStillRedBuildAfterSuccessfulRecipesIsReportedAsPartial(@TempDir Path repoPath) throws IOException {
        Files.writeString(repoPath.resolve("pom.xml"), "<project/>");
        GitOperations git = new GitOperations();
        git.ensureInitialized(repoPath);

        RecipeRunner recipeRunner = new RecipeRunner(alwaysReturning(new ProcessResult(0, "no changes", "", false)));
        MavenBuildRunner buildRunner = new MavenBuildRunner(alwaysReturning(new ProcessResult(1, "BUILD FAILURE", "", false)));
        UpgradePipeline pipeline = new UpgradePipeline(git, recipeRunner, buildRunner);

        UpgradeRun run = pipeline.run(configFor(repoPath), "my-service", "8", "2.7.18");

        assertThat(run.status()).isEqualTo(RunStatus.PARTIAL);
        assertThat(run.steps().get(1).status()).isEqualTo(StepStatus.FAILED);
    }

    @Test
    void aFailedRecipeRunIsReportedAsFailedRegardlessOfTheBuild(@TempDir Path repoPath) throws IOException {
        Files.writeString(repoPath.resolve("pom.xml"), "<project/>");
        GitOperations git = new GitOperations();
        git.ensureInitialized(repoPath);

        RecipeRunner recipeRunner = new RecipeRunner(alwaysReturning(new ProcessResult(1, "", "recipe boom", false)));
        MavenBuildRunner buildRunner = new MavenBuildRunner(alwaysReturning(new ProcessResult(0, "BUILD SUCCESS", "", false)));
        UpgradePipeline pipeline = new UpgradePipeline(git, recipeRunner, buildRunner);

        UpgradeRun run = pipeline.run(configFor(repoPath), "my-service", "8", "2.7.18");

        assertThat(run.status()).isEqualTo(RunStatus.FAILED);
        assertThat(run.steps().get(0).status()).isEqualTo(StepStatus.FAILED);
    }

    @Test
    void rejectsNullArguments(@TempDir Path repoPath) {
        GitOperations git = new GitOperations();
        RecipeRunner recipeRunner = new RecipeRunner(alwaysReturning(new ProcessResult(0, "", "", false)));
        MavenBuildRunner buildRunner = new MavenBuildRunner(alwaysReturning(new ProcessResult(0, "", "", false)));
        UpgradePipeline pipeline = new UpgradePipeline(git, recipeRunner, buildRunner);

        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> pipeline.run(null, "svc", "8", "2.7.18"));
    }
}
