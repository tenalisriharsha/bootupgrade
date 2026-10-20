package dev.bootupgrade.api.dto;

import java.util.List;

/** Overall success-rate aggregation across every repository. */
public record SuccessRateSummaryDto(
        long totalRuns,
        long succeededRuns,
        long partialRuns,
        long failedRuns,
        double successRate,
        List<RepositorySuccessRateDto> byRepository) {
}
