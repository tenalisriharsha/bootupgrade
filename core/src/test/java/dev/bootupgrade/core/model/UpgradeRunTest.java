package dev.bootupgrade.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpgradeRunTest {

    private UpgradeRun newRun() {
        return UpgradeRun.start("run-1", "legacy-rest-service", "8", "21", "2.7.18", "3.3.5", "bootupgrade/run-1");
    }

    @Test
    void startCreatesPendingRunWithNoSteps() {
        UpgradeRun run = newRun();

        assertThat(run.status()).isEqualTo(RunStatus.PENDING);
        assertThat(run.steps()).isEmpty();
        assertThat(run.createdAt()).isNotNull();
    }

    @Test
    void withStepAppendsWithoutMutatingOriginal() {
        UpgradeRun run = newRun();
        UpgradeStep step = UpgradeStep.start("step-1", "javax to jakarta", StepKind.RECIPE, "migrate").markSucceeded("ok");

        UpgradeRun updated = run.withStep(step);

        assertThat(updated.steps()).containsExactly(step);
        assertThat(run.steps()).isEmpty();
    }

    @Test
    void withStatusReturnsNewInstanceWithUpdatedStatus() {
        UpgradeRun run = newRun();

        UpgradeRun inProgress = run.withStatus(RunStatus.IN_PROGRESS);

        assertThat(inProgress.status()).isEqualTo(RunStatus.IN_PROGRESS);
        assertThat(run.status()).isEqualTo(RunStatus.PENDING);
    }

    @Test
    void succeededAndFailedStepCountsReflectStepStatuses() {
        UpgradeStep succeeded1 = UpgradeStep.start("s1", "recipe", StepKind.RECIPE, "d").markSucceeded("ok");
        UpgradeStep succeeded2 = UpgradeStep.start("s2", "build", StepKind.BUILD_CHECK, "d").markSucceeded("ok");
        UpgradeStep failed = UpgradeStep.start("s3", "agent", StepKind.AGENT_FIX, "d").markFailed("nope");

        UpgradeRun run = newRun().withStep(succeeded1).withStep(succeeded2).withStep(failed);

        assertThat(run.succeededStepCount()).isEqualTo(2);
        assertThat(run.failedStepCount()).isEqualTo(1);
    }

    @Test
    void stepsListIsImmutable() {
        UpgradeRun run = newRun();
        assertThatThrownBy(() -> run.steps().add(
                UpgradeStep.start("s1", "recipe", StepKind.RECIPE, "d")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsBlankIdAndRepositoryName() {
        assertThatThrownBy(() -> UpgradeRun.start("", "repo", "8", "21", "2.7.18", "3.3.5", "branch"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UpgradeRun.start("id", "  ", "8", "21", "2.7.18", "3.3.5", "branch"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThatThrownBy(() -> UpgradeRun.start(null, "repo", "8", "21", "2.7.18", "3.3.5", "branch"))
                .isInstanceOf(NullPointerException.class);
    }
}
