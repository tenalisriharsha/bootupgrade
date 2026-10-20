package dev.bootupgrade.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bootupgrade.api.dto.CreateUpgradeRunRequest;
import dev.bootupgrade.api.dto.CreateUpgradeStepRequest;
import dev.bootupgrade.api.model.RunStatus;
import dev.bootupgrade.api.model.StepKind;
import dev.bootupgrade.api.model.StepStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UpgradeRunServiceTest {

    @Autowired
    private UpgradeRunService service;

    @Test
    void createsAndRetrievesARun() {
        var request = new CreateUpgradeRunRequest(
                "legacy-rest-service", "8", "21", "2.7.18", "3.3.5", "bootupgrade/run-1",
                RunStatus.SUCCEEDED,
                List.of(
                        new CreateUpgradeStepRequest(
                                "Apply OpenRewrite recipes", StepKind.RECIPE, StepStatus.SUCCEEDED,
                                "Bump Boot 2->3, javax->jakarta", "12 files changed", Instant.now(), Instant.now()),
                        new CreateUpgradeStepRequest(
                                "Build check", StepKind.BUILD_CHECK, StepStatus.SUCCEEDED,
                                "mvn clean verify", null, Instant.now(), Instant.now())));

        var created = service.createRun(request);

        assertThat(created.id()).isNotBlank();
        assertThat(created.repositoryName()).isEqualTo("legacy-rest-service");
        assertThat(created.steps()).hasSize(2);
        assertThat(created.steps().get(0).name()).isEqualTo("Apply OpenRewrite recipes");

        var fetched = service.getRun(created.id());
        assertThat(fetched).isEqualTo(created);
    }

    @Test
    void throwsWhenRunNotFound() {
        assertThatThrownBy(() -> service.getRun("does-not-exist"))
                .isInstanceOf(UpgradeRunNotFoundException.class)
                .hasMessageContaining("does-not-exist");
    }

    @Test
    void listsRunsNewestFirst() throws InterruptedException {
        service.createRun(minimalRequest("repo-a", RunStatus.SUCCEEDED));
        Thread.sleep(5);
        service.createRun(minimalRequest("repo-b", RunStatus.FAILED));

        var runs = service.listRuns();

        assertThat(runs).hasSize(2);
        assertThat(runs.get(0).repositoryName()).isEqualTo("repo-b");
        assertThat(runs.get(1).repositoryName()).isEqualTo("repo-a");
    }

    @Test
    void aggregatesSuccessRateOverallAndPerRepository() {
        service.createRun(minimalRequest("repo-a", RunStatus.SUCCEEDED));
        service.createRun(minimalRequest("repo-a", RunStatus.FAILED));
        service.createRun(minimalRequest("repo-b", RunStatus.SUCCEEDED));

        var summary = service.successRate();

        assertThat(summary.totalRuns()).isEqualTo(3);
        assertThat(summary.succeededRuns()).isEqualTo(2);
        assertThat(summary.failedRuns()).isEqualTo(1);
        assertThat(summary.successRate()).isCloseTo(2.0 / 3, org.assertj.core.data.Offset.offset(0.0001));

        assertThat(summary.byRepository()).hasSize(2);
        var repoA = summary.byRepository().stream()
                .filter(r -> r.repositoryName().equals("repo-a"))
                .findFirst()
                .orElseThrow();
        assertThat(repoA.totalRuns()).isEqualTo(2);
        assertThat(repoA.succeededRuns()).isEqualTo(1);
        assertThat(repoA.successRate()).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.0001));

        var repoB = summary.byRepository().stream()
                .filter(r -> r.repositoryName().equals("repo-b"))
                .findFirst()
                .orElseThrow();
        assertThat(repoB.totalRuns()).isEqualTo(1);
        assertThat(repoB.successRate()).isEqualTo(1.0);
    }

    private static CreateUpgradeRunRequest minimalRequest(String repositoryName, RunStatus status) {
        return new CreateUpgradeRunRequest(
                repositoryName, "8", "21", "2.7.18", "3.3.5", "bootupgrade/" + repositoryName,
                status, List.of());
    }
}
