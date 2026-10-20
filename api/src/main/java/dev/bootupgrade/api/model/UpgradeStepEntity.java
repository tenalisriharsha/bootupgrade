package dev.bootupgrade.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity for one step of an upgrade run. Mutable, with a no-arg
 * constructor, as required by JPA — the immutable {@code UpgradeStep}
 * record in {@code core} is mapped to/from this shape at the API boundary.
 */
@Entity
@Table(name = "upgrade_steps")
public class UpgradeStepEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private UpgradeRunEntity run;

    /** Preserves step order within a run; runs are not re-sortable by timestamp alone. */
    @Column(name = "sequence_index", nullable = false)
    private int sequenceIndex;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StepKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StepStatus status;

    @Lob
    @Column(nullable = false)
    private String description;

    @Lob
    private String diffSummary;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    protected UpgradeStepEntity() {
    }

    public UpgradeStepEntity(
            String id,
            int sequenceIndex,
            String name,
            StepKind kind,
            StepStatus status,
            String description,
            String diffSummary,
            Instant startedAt,
            Instant finishedAt) {
        this.id = id;
        this.sequenceIndex = sequenceIndex;
        this.name = name;
        this.kind = kind;
        this.status = status;
        this.description = description;
        this.diffSummary = diffSummary;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public String getId() {
        return id;
    }

    public UpgradeRunEntity getRun() {
        return run;
    }

    public void setRun(UpgradeRunEntity run) {
        this.run = run;
    }

    public int getSequenceIndex() {
        return sequenceIndex;
    }

    public String getName() {
        return name;
    }

    public StepKind getKind() {
        return kind;
    }

    public StepStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getDiffSummary() {
        return diffSummary;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
