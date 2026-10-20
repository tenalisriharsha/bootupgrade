package dev.bootupgrade.core.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One end-to-end attempt to upgrade a repository from a source Java/Spring
 * Boot version to a target version. Immutable — {@link #withStep} and
 * {@link #withStatus} return a new instance with the change applied.
 */
public record UpgradeRun(
        String id,
        String repositoryName,
        String sourceJavaVersion,
        String targetJavaVersion,
        String sourceSpringBootVersion,
        String targetSpringBootVersion,
        String branchName,
        RunStatus status,
        List<UpgradeStep> steps,
        Instant createdAt) {

    public UpgradeRun {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(repositoryName, "repositoryName must not be null");
        Objects.requireNonNull(sourceJavaVersion, "sourceJavaVersion must not be null");
        Objects.requireNonNull(targetJavaVersion, "targetJavaVersion must not be null");
        Objects.requireNonNull(sourceSpringBootVersion, "sourceSpringBootVersion must not be null");
        Objects.requireNonNull(targetSpringBootVersion, "targetSpringBootVersion must not be null");
        Objects.requireNonNull(branchName, "branchName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (repositoryName.isBlank()) {
            throw new IllegalArgumentException("repositoryName must not be blank");
        }
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    /** Creates a fresh, pending run with no steps yet. */
    public static UpgradeRun start(
            String id,
            String repositoryName,
            String sourceJavaVersion,
            String targetJavaVersion,
            String sourceSpringBootVersion,
            String targetSpringBootVersion,
            String branchName) {
        return new UpgradeRun(
                id,
                repositoryName,
                sourceJavaVersion,
                targetJavaVersion,
                sourceSpringBootVersion,
                targetSpringBootVersion,
                branchName,
                RunStatus.PENDING,
                List.of(),
                Instant.now());
    }

    public UpgradeRun withStep(UpgradeStep step) {
        Objects.requireNonNull(step, "step must not be null");
        var updatedSteps = new java.util.ArrayList<>(steps);
        updatedSteps.add(step);
        return new UpgradeRun(
                id, repositoryName, sourceJavaVersion, targetJavaVersion,
                sourceSpringBootVersion, targetSpringBootVersion, branchName,
                status, updatedSteps, createdAt);
    }

    public UpgradeRun withStatus(RunStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        return new UpgradeRun(
                id, repositoryName, sourceJavaVersion, targetJavaVersion,
                sourceSpringBootVersion, targetSpringBootVersion, branchName,
                newStatus, steps, createdAt);
    }

    public long succeededStepCount() {
        return steps.stream().filter(s -> s.status() == StepStatus.SUCCEEDED).count();
    }

    public long failedStepCount() {
        return steps.stream().filter(s -> s.status() == StepStatus.FAILED).count();
    }
}
