package dev.bootupgrade.core.recipes;

import dev.bootupgrade.core.build.FakeProcessRunner;
import dev.bootupgrade.core.build.ProcessResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeRunnerTest {

    @Test
    void buildsTheRewritePluginCommandWithTheDefaultRecipes(@TempDir Path repoPath) {
        FakeProcessRunner processRunner = new FakeProcessRunner(new ProcessResult(0, "BUILD SUCCESS", "", false));
        RecipeRunner runner = new RecipeRunner(processRunner);

        RecipeResult result = runner.run(repoPath);

        assertThat(result.success()).isTrue();
        assertThat(result.appliedRecipes()).isEqualTo(RecipeRunner.DEFAULT_RECIPES);
        assertThat(processRunner.lastCommand()).containsExactly(
                "mvn", "-B", "-f", repoPath.toString(), "rewrite:run",
                "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_0");
    }

    @Test
    void joinsMultipleActiveRecipesWithCommas(@TempDir Path repoPath) {
        FakeProcessRunner processRunner = new FakeProcessRunner(new ProcessResult(0, "", "", false));
        RecipeRunner runner = new RecipeRunner(processRunner, "mvn", Duration.ofMinutes(1));

        runner.run(repoPath, List.of("recipe.One", "recipe.Two"));

        assertThat(processRunner.lastCommand()).contains("-Drewrite.activeRecipes=recipe.One,recipe.Two");
    }

    @Test
    void reportsFailureWhenThePluginExitsNonZero(@TempDir Path repoPath) {
        FakeProcessRunner processRunner = new FakeProcessRunner(new ProcessResult(1, "", "boom", false));
        RecipeRunner runner = new RecipeRunner(processRunner);

        RecipeResult result = runner.run(repoPath);

        assertThat(result.success()).isFalse();
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.output()).contains("boom");
    }

    @Test
    void rejectsAnEmptyRecipeList(@TempDir Path repoPath) {
        RecipeRunner runner = new RecipeRunner(new FakeProcessRunner(new ProcessResult(0, "", "", false)));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> runner.run(repoPath, List.of()));
    }
}
