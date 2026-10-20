package dev.bootupgrade.api.dto;

import dev.bootupgrade.api.model.StepKind;
import dev.bootupgrade.api.model.StepStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Request payload for one step of an upgrade run being reported to the API. */
public record CreateUpgradeStepRequest(
        @NotBlank String name,
        @NotNull StepKind kind,
        @NotNull StepStatus status,
        @NotBlank String description,
        String diffSummary,
        @NotNull Instant startedAt,
        Instant finishedAt) {
}
