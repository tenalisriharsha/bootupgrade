package dev.bootupgrade.api.dto;

/** Success-rate breakdown for a single repository across all of its runs. */
public record RepositorySuccessRateDto(
        String repositoryName,
        long totalRuns,
        long succeededRuns,
        long partialRuns,
        long failedRuns,
        double successRate) {
}
