package dev.bootupgrade.api.dto;

import dev.bootupgrade.api.model.RunStatus;
import dev.bootupgrade.api.model.UpgradeRunEntity;
import java.time.Instant;
import java.util.List;

/** Response shape for a single run's detail page: metadata plus every step. */
public record UpgradeRunDetailDto(
        String id,
        String repositoryName,
        String sourceJavaVersion,
        String targetJavaVersion,
        String sourceSpringBootVersion,
        String targetSpringBootVersion,
        String branchName,
        RunStatus status,
        Instant createdAt,
        List<UpgradeStepDto> steps) {

    public static UpgradeRunDetailDto from(UpgradeRunEntity entity) {
        return new UpgradeRunDetailDto(
                entity.getId(),
                entity.getRepositoryName(),
                entity.getSourceJavaVersion(),
                entity.getTargetJavaVersion(),
                entity.getSourceSpringBootVersion(),
                entity.getTargetSpringBootVersion(),
                entity.getBranchName(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getSteps().stream().map(UpgradeStepDto::from).toList());
    }
}
