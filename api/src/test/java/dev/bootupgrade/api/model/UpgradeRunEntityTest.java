package dev.bootupgrade.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UpgradeRunEntityTest {

    @Test
    void countsStepsByStatus() {
        var run = new UpgradeRunEntity(
                "run-1", "legacy-rest-service", "8", "21", "2.7.18", "3.3.5",
                "bootupgrade/run-1", RunStatus.PARTIAL, Instant.now());

        run.addStep(new UpgradeStepEntity(
                "step-1", 0, "Apply OpenRewrite recipes", StepKind.RECIPE, StepStatus.SUCCEEDED,
                "desc", "diff", Instant.now(), Instant.now()));
        run.addStep(new UpgradeStepEntity(
                "step-2", 1, "Build check", StepKind.BUILD_CHECK, StepStatus.FAILED,
                "desc", "diff", Instant.now(), Instant.now()));
        run.addStep(new UpgradeStepEntity(
                "step-3", 2, "Agent fix", StepKind.AGENT_FIX, StepStatus.SUCCEEDED,
                "desc", "diff", Instant.now(), Instant.now()));

        assertThat(run.succeededStepCount()).isEqualTo(2);
        assertThat(run.failedStepCount()).isEqualTo(1);
        assertThat(run.getSteps()).hasSize(3);
        assertThat(run.getSteps().get(0).getRun()).isSameAs(run);
    }

    @Test
    void countsAreZeroWithNoSteps() {
        var run = new UpgradeRunEntity(
                "run-2", "legacy-jpa-service", "8", "21", "2.7.18", "3.3.5",
                "bootupgrade/run-2", RunStatus.PENDING, Instant.now());

        assertThat(run.succeededStepCount()).isZero();
        assertThat(run.failedStepCount()).isZero();
        assertThat(run.getSteps()).isEmpty();
    }
}
