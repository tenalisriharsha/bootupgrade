package dev.bootupgrade.api.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for one upgrade run. Mutable, with a no-arg constructor, as
 * required by JPA — the immutable {@code UpgradeRun} record in {@code core}
 * is mapped to/from this shape at the API boundary.
 */
@Entity
@Table(name = "upgrade_runs")
public class UpgradeRunEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String repositoryName;

    @Column(nullable = false)
    private String sourceJavaVersion;

    @Column(nullable = false)
    private String targetJavaVersion;

    @Column(nullable = false)
    private String sourceSpringBootVersion;

    @Column(nullable = false)
    private String targetSpringBootVersion;

    @Column(nullable = false)
    private String branchName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RunStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceIndex ASC")
    private List<UpgradeStepEntity> steps = new ArrayList<>();

    protected UpgradeRunEntity() {
    }

    public UpgradeRunEntity(
            String id,
            String repositoryName,
            String sourceJavaVersion,
            String targetJavaVersion,
            String sourceSpringBootVersion,
            String targetSpringBootVersion,
            String branchName,
            RunStatus status,
            Instant createdAt) {
        this.id = id;
        this.repositoryName = repositoryName;
        this.sourceJavaVersion = sourceJavaVersion;
        this.targetJavaVersion = targetJavaVersion;
        this.sourceSpringBootVersion = sourceSpringBootVersion;
        this.targetSpringBootVersion = targetSpringBootVersion;
        this.branchName = branchName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void addStep(UpgradeStepEntity step) {
        step.setRun(this);
        steps.add(step);
    }

    public String getId() {
        return id;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getSourceJavaVersion() {
        return sourceJavaVersion;
    }

    public String getTargetJavaVersion() {
        return targetJavaVersion;
    }

    public String getSourceSpringBootVersion() {
        return sourceSpringBootVersion;
    }

    public String getTargetSpringBootVersion() {
        return targetSpringBootVersion;
    }

    public String getBranchName() {
        return branchName;
    }

    public RunStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<UpgradeStepEntity> getSteps() {
        return steps;
    }

    public long succeededStepCount() {
        return steps.stream().filter(s -> s.getStatus() == StepStatus.SUCCEEDED).count();
    }

    public long failedStepCount() {
        return steps.stream().filter(s -> s.getStatus() == StepStatus.FAILED).count();
    }
}
