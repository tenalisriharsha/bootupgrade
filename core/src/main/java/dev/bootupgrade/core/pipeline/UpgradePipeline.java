package dev.bootupgrade.core.pipeline;

import dev.bootupgrade.core.build.BuildResult;
import dev.bootupgrade.core.build.MavenBuildRunner;
import dev.bootupgrade.core.config.UpgradeConfig;
import dev.bootupgrade.core.git.GitOperations;
import dev.bootupgrade.core.model.RunStatus;
import dev.bootupgrade.core.model.StepKind;
import dev.bootupgrade.core.model.StepStatus;
import dev.bootupgrade.core.model.UpgradeRun;
import dev.bootupgrade.core.model.UpgradeStep;
import dev.bootupgrade.core.recipes.RecipeResult;
import dev.bootupgrade.core.recipes.RecipeRunner;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Orchestrates one upgrade run: create a branch, run the deterministic
 * OpenRewrite recipes, commit whatever they changed, then check the build.
 *
 * <p>Phase 2 stops here — if the build is still red after recipes, the run
 * is reported as {@link RunStatus#PARTIAL} (or {@link RunStatus#FAILED} if
 * the recipes themselves failed) rather than silently claiming success. The
 * LLM agent fallback that picks up from there is wired in Phase 3.
 */
public final class UpgradePipeline {

    private final GitOperations gitOperations;
    private final RecipeRunner recipeRunner;
    private final MavenBuildRunner buildRunner;

    public UpgradePipeline(GitOperations gitOperations, RecipeRunner recipeRunner, MavenBuildRunner buildRunner) {
        this.gitOperations = Objects.requireNonNull(gitOperations, "gitOperations must not be null");
        this.recipeRunner = Objects.requireNonNull(recipeRunner, "recipeRunner must not be null");
        this.buildRunner = Objects.requireNonNull(buildRunner, "buildRunner must not be null");
    }

    /** Runs the recipe -> build-check pipeline against {@code config.repositoryPath()}. */
    public UpgradeRun run(UpgradeConfig config, String repositoryName, String sourceJavaVersion, String sourceSpringBootVersion) {
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(repositoryName, "repositoryName must not be null");
        Objects.requireNonNull(sourceJavaVersion, "sourceJavaVersion must not be null");
        Objects.requireNonNull(sourceSpringBootVersion, "sourceSpringBootVersion must not be null");

        String runId = UUID.randomUUID().toString();
        String branchName = "bootupgrade/" + repositoryName + "-" + runId.substring(0, 8);
        UpgradeRun run = UpgradeRun.start(
                runId,
                repositoryName,
                sourceJavaVersion,
                String.valueOf(config.targetJavaVersion()),
                sourceSpringBootVersion,
                config.targetSpringBootVersion(),
                branchName);
        run = run.withStatus(RunStatus.IN_PROGRESS);

        gitOperations.ensureInitialized(config.repositoryPath());
        gitOperations.createBranch(config.repositoryPath(), branchName);

        UpgradeStep recipeStep = applyRecipes(config, runId);
        run = run.withStep(recipeStep);

        UpgradeStep buildStep = checkBuild(config, runId);
        run = run.withStep(buildStep);

        if (recipeStep.status() == StepStatus.FAILED) {
            return run.withStatus(RunStatus.FAILED);
        }
        return run.withStatus(buildStep.status() == StepStatus.SUCCEEDED ? RunStatus.SUCCEEDED : RunStatus.PARTIAL);
    }

    private UpgradeStep applyRecipes(UpgradeConfig config, String runId) {
        UpgradeStep step = UpgradeStep.start(
                runId + "-recipes",
                "Apply OpenRewrite recipes",
                StepKind.RECIPE,
                "javax->jakarta rename and Spring Boot 2->3 config/property migration");
        RecipeResult result = recipeRunner.run(config.repositoryPath());
        if (!result.success()) {
            return step.markFailed("recipe run failed (exit " + result.exitCode() + ")");
        }
        List<String> changedFiles = gitOperations.uncommittedChanges(config.repositoryPath());
        if (changedFiles.isEmpty()) {
            return step.markSucceeded("no changes needed");
        }
        gitOperations.commitAll(config.repositoryPath(), "bootupgrade: apply OpenRewrite recipes");
        return step.markSucceeded(changedFiles.size() + " file(s) changed: " + String.join(", ", changedFiles));
    }

    private UpgradeStep checkBuild(UpgradeConfig config, String runId) {
        UpgradeStep step = UpgradeStep.start(
                runId + "-build", "Build check", StepKind.BUILD_CHECK, "mvn clean verify");
        BuildResult result = buildRunner.runVerify(config.repositoryPath());
        return result.success() ? step.markSucceeded(result.summary()) : step.markFailed(result.summary());
    }
}
