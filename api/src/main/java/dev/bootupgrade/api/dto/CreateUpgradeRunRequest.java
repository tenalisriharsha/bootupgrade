package dev.bootupgrade.api.dto;

import dev.bootupgrade.api.model.RunStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request payload for reporting a completed (or in-progress) upgrade run
 * to the API, as the pipeline would at the end of a run.
 */
public record CreateUpgradeRunRequest(
        @NotBlank String repositoryName,
        @NotBlank String sourceJavaVersion,
        @NotBlank String targetJavaVersion,
        @NotBlank String sourceSpringBootVersion,
        @NotBlank String targetSpringBootVersion,
        @NotBlank String branchName,
        @NotNull RunStatus status,
        @Valid List<CreateUpgradeStepRequest> steps) {
}
