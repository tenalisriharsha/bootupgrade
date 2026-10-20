package dev.bootupgrade.api.service;

import dev.bootupgrade.api.dto.CreateUpgradeRunRequest;
import dev.bootupgrade.api.dto.CreateUpgradeStepRequest;
import dev.bootupgrade.api.dto.RepositorySuccessRateDto;
import dev.bootupgrade.api.dto.SuccessRateSummaryDto;
import dev.bootupgrade.api.dto.UpgradeRunDetailDto;
import dev.bootupgrade.api.dto.UpgradeRunSummaryDto;
import dev.bootupgrade.api.model.RunStatus;
import dev.bootupgrade.api.model.UpgradeRunEntity;
import dev.bootupgrade.api.model.UpgradeStepEntity;
import dev.bootupgrade.api.repository.UpgradeRunRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for recording and querying upgrade runs. Owns id
 * generation, request-to-entity mapping, and the success-rate aggregation —
 * the controller stays a thin translation of HTTP to these calls.
 */
@Service
@Transactional
public class UpgradeRunService {

    private final UpgradeRunRepository runRepository;

    public UpgradeRunService(UpgradeRunRepository runRepository) {
        this.runRepository = runRepository;
    }

    public UpgradeRunDetailDto createRun(CreateUpgradeRunRequest request) {
        var run = new UpgradeRunEntity(
                UUID.randomUUID().toString(),
                request.repositoryName(),
                request.sourceJavaVersion(),
                request.targetJavaVersion(),
                request.sourceSpringBootVersion(),
                request.targetSpringBootVersion(),
                request.branchName(),
                request.status(),
                Instant.now());

        List<CreateUpgradeStepRequest> steps = request.steps() == null ? List.of() : request.steps();
        for (int i = 0; i < steps.size(); i++) {
            run.addStep(toStepEntity(steps.get(i), i));
        }

        var saved = runRepository.save(run);
        return UpgradeRunDetailDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<UpgradeRunSummaryDto> listRuns() {
        return runRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(UpgradeRunSummaryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UpgradeRunDetailDto getRun(String runId) {
        var run = runRepository.findById(runId)
                .orElseThrow(() -> new UpgradeRunNotFoundException(runId));
        return UpgradeRunDetailDto.from(run);
    }

    @Transactional(readOnly = true)
    public SuccessRateSummaryDto successRate() {
        List<UpgradeRunEntity> allRuns = runRepository.findAll();

        Map<String, List<UpgradeRunEntity>> byRepository = allRuns.stream()
                .collect(Collectors.groupingBy(UpgradeRunEntity::getRepositoryName));

        List<RepositorySuccessRateDto> perRepository = byRepository.entrySet().stream()
                .map(entry -> toRepositoryRate(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(RepositorySuccessRateDto::repositoryName))
                .toList();

        long total = allRuns.size();
        long succeeded = countByStatus(allRuns, RunStatus.SUCCEEDED);
        long partial = countByStatus(allRuns, RunStatus.PARTIAL);
        long failed = countByStatus(allRuns, RunStatus.FAILED);

        return new SuccessRateSummaryDto(total, succeeded, partial, failed, rate(succeeded, total), perRepository);
    }

    private RepositorySuccessRateDto toRepositoryRate(String repositoryName, List<UpgradeRunEntity> runs) {
        long total = runs.size();
        long succeeded = countByStatus(runs, RunStatus.SUCCEEDED);
        long partial = countByStatus(runs, RunStatus.PARTIAL);
        long failed = countByStatus(runs, RunStatus.FAILED);
        return new RepositorySuccessRateDto(repositoryName, total, succeeded, partial, failed, rate(succeeded, total));
    }

    private static long countByStatus(List<UpgradeRunEntity> runs, RunStatus status) {
        return runs.stream().filter(r -> r.getStatus() == status).count();
    }

    private static double rate(long succeeded, long total) {
        return total == 0 ? 0.0 : (double) succeeded / total;
    }

    private static UpgradeStepEntity toStepEntity(CreateUpgradeStepRequest request, int sequenceIndex) {
        return new UpgradeStepEntity(
                UUID.randomUUID().toString(),
                sequenceIndex,
                request.name(),
                request.kind(),
                request.status(),
                request.description(),
                request.diffSummary(),
                request.startedAt(),
                request.finishedAt());
    }
}
