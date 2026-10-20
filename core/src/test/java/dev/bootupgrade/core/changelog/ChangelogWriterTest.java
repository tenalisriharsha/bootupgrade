package dev.bootupgrade.core.changelog;

import dev.bootupgrade.core.model.RunStatus;
import dev.bootupgrade.core.model.StepKind;
import dev.bootupgrade.core.model.UpgradeRun;
import dev.bootupgrade.core.model.UpgradeStep;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChangelogWriterTest {

    private UpgradeRun runWithSteps() {
        UpgradeRun run = UpgradeRun.start("run-1", "legacy-rest-service", "8", "21", "2.7.18", "3.3.5", "bootupgrade/run-1");
        UpgradeStep recipe = UpgradeStep.start("s1", "javax to jakarta", StepKind.RECIPE, "Migrate javax.* to jakarta.*")
                .markSucceeded("12 files changed");
        UpgradeStep build = UpgradeStep.start("s2", "build check", StepKind.BUILD_CHECK, "mvn verify")
                .markFailed("2 tests failed");
        return run.withStep(recipe).withStep(build).withStatus(RunStatus.PARTIAL);
    }

    @Test
    void entriesForProducesOneEntryPerStepInOrder() {
        var entries = ChangelogWriter.entriesFor(runWithSteps());

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).heading()).contains("SUCCEEDED").contains("RECIPE").contains("javax to jakarta");
        assertThat(entries.get(1).heading()).contains("FAILED").contains("BUILD_CHECK").contains("build check");
    }

    @Test
    void entryDetailIncludesDiffSummaryWhenPresent() {
        var entries = ChangelogWriter.entriesFor(runWithSteps());

        assertThat(entries.get(0).detail()).contains("Migrate javax.* to jakarta.*").contains("12 files changed");
    }

    @Test
    void renderChangelogIncludesHeaderMetadataAndSteps() {
        String changelog = ChangelogWriter.renderChangelog(runWithSteps());

        assertThat(changelog)
                .contains("# Upgrade Log: legacy-rest-service")
                .contains("8 -> 21")
                .contains("2.7.18 -> 3.3.5")
                .contains("bootupgrade/run-1")
                .contains("PARTIAL")
                .contains("javax to jakarta")
                .contains("build check");
    }

    @Test
    void renderChangelogHandlesRunWithNoSteps() {
        UpgradeRun run = UpgradeRun.start("run-2", "legacy-jpa-service", "8", "21", "2.7.18", "3.3.5", "bootupgrade/run-2");

        String changelog = ChangelogWriter.renderChangelog(run);

        assertThat(changelog).contains("No steps recorded yet");
    }

    @Test
    void renderPrDescriptionSummarizesStepCounts() {
        String prDescription = ChangelogWriter.renderPrDescription(runWithSteps());

        assertThat(prDescription)
                .contains("legacy-rest-service")
                .contains("1 succeeded")
                .contains("1 failed")
                .contains("2 total steps")
                .contains("### Changes");
    }

    @Test
    void renderChangelogCoversAnAgentFixStepAfterARedBuild() {
        UpgradeRun run = UpgradeRun.start("run-3", "legacy-security-service", "8", "21", "2.7.18", "3.0.13", "bootupgrade/run-3");
        UpgradeStep recipe = UpgradeStep.start("s1", "Apply OpenRewrite recipes", StepKind.RECIPE, "javax->jakarta")
                .markSucceeded("3 files changed");
        UpgradeStep build = UpgradeStep.start("s2", "Build check", StepKind.BUILD_CHECK, "mvn clean verify")
                .markFailed("BUILD FAILURE (exit 1), 1 tests run, 0 failed, 1 errored");
        UpgradeStep agentFix = UpgradeStep.start("s3", "LLM agent fix loop", StepKind.AGENT_FIX,
                        "tool-limited agent (read_file/write_file/run_build) attempting to fix the failing build")
                .markSucceeded("build fixed after 3 agent iteration(s) [read_file(path=SecurityConfig.java) -> ok; "
                        + "write_file(path=SecurityConfig.java) -> ok; run_build -> ok]");
        run = run.withStep(recipe).withStep(build).withStep(agentFix).withStatus(RunStatus.SUCCEEDED);

        String changelog = ChangelogWriter.renderChangelog(run);
        String prDescription = ChangelogWriter.renderPrDescription(run);

        assertThat(changelog)
                .contains("SUCCEEDED] AGENT_FIX — LLM agent fix loop")
                .contains("build fixed after 3 agent iteration(s)");
        assertThat(prDescription)
                .contains("2 succeeded")
                .contains("0 failed")
                .contains("3 total steps");
    }
}
