package dev.bootupgrade.core.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpgradeStepTest {

    @Test
    void startCreatesPendingStepWithStartedAtSet() {
        UpgradeStep step = UpgradeStep.start("step-1", "javax to jakarta", StepKind.RECIPE, "Migrate javax.* to jakarta.*");

        assertThat(step.id()).isEqualTo("step-1");
        assertThat(step.status()).isEqualTo(StepStatus.PENDING);
        assertThat(step.startedAt()).isNotNull();
        assertThat(step.finishedAt()).isNull();
        assertThat(step.isTerminal()).isFalse();
    }

    @Test
    void markSucceededSetsStatusAndFinishedAtAndDiffSummary() {
        UpgradeStep step = UpgradeStep.start("step-1", "javax to jakarta", StepKind.RECIPE, "Migrate javax.* to jakarta.*");

        UpgradeStep succeeded = step.markSucceeded("12 files changed");

        assertThat(succeeded.status()).isEqualTo(StepStatus.SUCCEEDED);
        assertThat(succeeded.diffSummary()).isEqualTo("12 files changed");
        assertThat(succeeded.finishedAt()).isNotNull();
        assertThat(succeeded.isTerminal()).isTrue();
        // original instance is untouched
        assertThat(step.status()).isEqualTo(StepStatus.PENDING);
    }

    @Test
    void markFailedSetsStatusAndFailureSummary() {
        UpgradeStep step = UpgradeStep.start("step-2", "build check", StepKind.BUILD_CHECK, "mvn verify");

        UpgradeStep failed = step.markFailed("2 tests failed");

        assertThat(failed.status()).isEqualTo(StepStatus.FAILED);
        assertThat(failed.diffSummary()).isEqualTo("2 tests failed");
        assertThat(failed.isTerminal()).isTrue();
    }

    @Test
    void markSkippedSetsStatusAndReason() {
        UpgradeStep step = UpgradeStep.start("step-3", "agent fix", StepKind.AGENT_FIX, "attempt automatic fix");

        UpgradeStep skipped = step.markSkipped("no agent configured");

        assertThat(skipped.status()).isEqualTo(StepStatus.SKIPPED);
        assertThat(skipped.diffSummary()).isEqualTo("no agent configured");
        assertThat(skipped.isTerminal()).isTrue();
    }

    @Test
    void rejectsBlankIdAndName() {
        assertThatThrownBy(() -> new UpgradeStep("", "name", StepKind.RECIPE, StepStatus.PENDING, "desc", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UpgradeStep("id", "  ", StepKind.RECIPE, StepStatus.PENDING, "desc", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFinishedAtBeforeStartedAt() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new UpgradeStep(
                "id", "name", StepKind.RECIPE, StepStatus.SUCCEEDED, "desc", null, now, now.minusSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThatThrownBy(() -> new UpgradeStep(null, "name", StepKind.RECIPE, StepStatus.PENDING, "desc", null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpgradeStep("id", "name", null, StepStatus.PENDING, "desc", null, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
