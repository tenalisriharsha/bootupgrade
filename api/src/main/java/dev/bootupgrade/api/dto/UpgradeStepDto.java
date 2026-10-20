package dev.bootupgrade.api.dto;

import dev.bootupgrade.api.model.StepKind;
import dev.bootupgrade.api.model.StepStatus;
import dev.bootupgrade.api.model.UpgradeStepEntity;
import java.time.Instant;

/** Response shape for one step of an upgrade run. */
public record UpgradeStepDto(
        String id,
        String name,
        StepKind kind,
        StepStatus status,
        String description,
        String diffSummary,
        Instant startedAt,
        Instant finishedAt) {

    public static UpgradeStepDto from(UpgradeStepEntity entity) {
        return new UpgradeStepDto(
                entity.getId(),
                entity.getName(),
                entity.getKind(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getDiffSummary(),
                entity.getStartedAt(),
                entity.getFinishedAt());
    }
}
