package dev.bootupgrade.api.dto;

import dev.bootupgrade.api.model.RunStatus;
import dev.bootupgrade.api.model.UpgradeRunEntity;
import java.time.Instant;

/** Response shape for one row of the runs list. */
public record UpgradeRunSummaryDto(
        String id,
        String repositoryName,
        String sourceJavaVersion,
        String targetJavaVersion,
        String sourceSpringBootVersion,
        String targetSpringBootVersion,
        RunStatus status,
        Instant createdAt,
        int totalSteps,
        long succeededStepCount,
        long failedStepCount) {

    public static UpgradeRunSummaryDto from(UpgradeRunEntity entity) {
        return new UpgradeRunSummaryDto(
                entity.getId(),
                entity.getRepositoryName(),
                entity.getSourceJavaVersion(),
                entity.getTargetJavaVersion(),
                entity.getSourceSpringBootVersion(),
                entity.getTargetSpringBootVersion(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getSteps().size(),
                entity.succeededStepCount(),
                entity.failedStepCount());
    }
}
