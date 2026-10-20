package dev.bootupgrade.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A single step in an upgrade run: one recipe application, build check, or
 * agent-proposed fix. Immutable — transitions (e.g. marking a step as
 * succeeded) return a new instance rather than mutating in place.
 */
public record UpgradeStep(
        String id,
        String name,
        StepKind kind,
        StepStatus status,
        String description,
        String diffSummary,
        Instant startedAt,
        Instant finishedAt) {

    public UpgradeStep {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(description, "description must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (finishedAt != null && startedAt != null && finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("finishedAt must not be before startedAt");
        }
    }

    /** Creates a new pending step, started now, with no diff summary yet. */
    public static UpgradeStep start(String id, String name, StepKind kind, String description) {
        return new UpgradeStep(id, name, kind, StepStatus.PENDING, description, null, Instant.now(), null);
    }

    public UpgradeStep markSucceeded(String diffSummary) {
        return new UpgradeStep(id, name, kind, StepStatus.SUCCEEDED, description, diffSummary, startedAt, Instant.now());
    }

    public UpgradeStep markFailed(String failureSummary) {
        return new UpgradeStep(id, name, kind, StepStatus.FAILED, description, failureSummary, startedAt, Instant.now());
    }

    public UpgradeStep markSkipped(String reason) {
        return new UpgradeStep(id, name, kind, StepStatus.SKIPPED, description, reason, startedAt, Instant.now());
    }

    public boolean isTerminal() {
        return status == StepStatus.SUCCEEDED || status == StepStatus.FAILED || status == StepStatus.SKIPPED;
    }
}
