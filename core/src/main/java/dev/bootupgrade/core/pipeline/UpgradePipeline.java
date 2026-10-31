package dev.bootupgrade.core.pipeline;

import dev.bootupgrade.core.agent.AgentFixLoop;
import dev.bootupgrade.core.agent.AgentFixResult;
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
 * <p>If the build is still red after recipes, the tool-limited LLM
 * {@link AgentFixLoop} (Phase 3) is given a chance to fix it, when one is
 * configured — otherwise, or if the agent cannot fix it either, the run is
 * reported as {@link RunStatus#PARTIAL} (or {@link RunStatus#FAILED} if the
 * recipes themselves failed) rather than silently claiming success.
 */
public final class UpgradePipeline {

    private final GitOperations gitOperations;
    private final RecipeRunner recipeRunner;
    private final MavenBuildRunner buildRunner;
    private final AgentFixLoop agentFixLoop;

    public UpgradePipeline(GitOperations gitOperations, RecipeRunner recipeRunner, MavenBuildRunner buildRunner) {
        this(gitOperations, recipeRunner, buildRunner, null);
    }

    /** Overload wiring in the Phase 3 agent fallback. {@code agentFixLoop} may be {@code null} to disable it. */
    public UpgradePipeline(
            GitOperations gitOperations,
            RecipeRunner recipeRunner,
            MavenBuildRunner buildRunner,
            AgentFixLoop agentFixLoop) {
        this.gitOperations = Objects.requireNonNull(gitOperations, "gitOperations must not be null");
        this.recipeRunner = Objects.requireNonNull(recipeRunner, "recipeRunner must not be null");
        this.buildRunner = Objects.requireNonNull(buildRunner, "buildRunner must not be null");
        this.agentFixLoop = agentFixLoop;
    }

    /** Runs the recipe -> build-check -> agent-fix pipeline against {@code config.repositoryPath()}. */
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
        if (recipeStep.status() == StepStatus.FAILED) {
            return run.withStatus(RunStatus.FAILED);
        }

        BuildResult buildResult = buildRunner.runVerify(config.repositoryPath());
        UpgradeStep buildStep = buildStepFor(runId, buildResult);
        run = run.withStep(buildStep);
        if (buildResult.success()) {
            return run.withStatus(RunStatus.SUCCEEDED);
        }

        if (agentFixLoop != null && config.maxAgentIterations() > 0) {
            UpgradeStep agentStep = runAgentFix(config, runId, buildResult);
            run = run.withStep(agentStep);
            return run.withStatus(agentStep.status() == StepStatus.SUCCEEDED ? RunStatus.SUCCEEDED : RunStatus.PARTIAL);
        }

        return run.withStatus(RunStatus.PARTIAL);
    }

    private UpgradeStep applyRecipes(UpgradeConfig config, String runId) {
        UpgradeStep step = UpgradeStep.start(
                runId + "-recipes",
                "Apply OpenRewrite recipes",
                StepKind.RECIPE,
                "javax->jakarta rename and Spring Boot 2->3 config/property migration");
        List<String> preExistingChanges = gitOperations.uncommittedChanges(config.repositoryPath());
        if (!preExistingChanges.isEmpty()) {
            return step.markFailed(config.repositoryPath() + " has uncommitted changes: "
                    + String.join(", ", preExistingChanges)
                    + "; commit or stash them before running bootupgrade");
        }
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

    private UpgradeStep buildStepFor(String runId, BuildResult result) {
        UpgradeStep step = UpgradeStep.start(
                runId + "-build", "Build check", StepKind.BUILD_CHECK, "mvn clean verify");
        return result.success() ? step.markSucceeded(result.summary()) : step.markFailed(result.summary());
    }

    private UpgradeStep runAgentFix(UpgradeConfig config, String runId, BuildResult initialBuildResult) {
        UpgradeStep step = UpgradeStep.start(
                runId + "-agent-fix",
                "LLM agent fix loop",
                StepKind.AGENT_FIX,
                "tool-limited agent (read_file/write_file/run_build) attempting to fix the failing build");

        AgentFixResult result = agentFixLoop.run(config.repositoryPath(), config, initialBuildResult);
        gitOperations.commitAll(
                config.repositoryPath(),
                "bootupgrade: agent fix attempt (" + (result.fixed() ? "fixed" : "unresolved") + ")");

        String toolLog = result.toolCallLog().isEmpty() ? "no tool calls" : String.join("; ", result.toolCallLog());
        String detail = result.summary() + " [" + toolLog + "]";
        return result.fixed() ? step.markSucceeded(detail) : step.markFailed(detail);
    }
}
