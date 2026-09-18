package dev.modelmaestro.orchestration;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "orchestration_runs")
public class OrchestrationRun {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String objective;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RunStatus status;

    @Column(name = "budget_micros", nullable = false)
    private long budgetMicros;

    // Nullable only for runs created before supervisor selection was introduced.
    @Column(name = "supervisor_config_id", updatable = false)
    private UUID supervisorConfigId;

    @Column(name = "spent_micros", nullable = false)
    private long spentMicros;

    @Column(name = "plan_content", columnDefinition = "text")
    private String planContent;

    @Column(name = "work_content", columnDefinition = "text")
    private String workContent;

    @Column(name = "final_result", columnDefinition = "text")
    private String finalResult;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrchestrationRun() {
        // Required by JPA.
    }

    public OrchestrationRun(String objective, long budgetMicros, UUID supervisorConfigId) {
        if (objective == null || objective.isBlank()) {
            throw new IllegalArgumentException("Objective must not be blank.");
        }
        if (budgetMicros <= 0) {
            throw new IllegalArgumentException("Budget must be greater than zero.");
        }
        if (supervisorConfigId == null) {
            throw new IllegalArgumentException("Supervisor configuration must not be null.");
        }
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.objective = objective.strip();
        this.status = RunStatus.CREATED;
        this.budgetMicros = budgetMicros;
        this.supervisorConfigId = supervisorConfigId;
        this.spentMicros = 0L;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void startPlanning() {
        transitionFrom(RunStatus.CREATED, RunStatus.PLANNING);
    }

    public void startRunning() {
        transitionFrom(RunStatus.PLANNING, RunStatus.RUNNING);
    }

    public void startReviewing() {
        transitionFrom(RunStatus.RUNNING, RunStatus.REVIEWING);
    }

    public void complete(
            String planContent,
            String workContent,
            String finalResult,
            long spentMicros) {
        if (status != RunStatus.REVIEWING) {
            throw invalidTransition(RunStatus.COMPLETED);
        }
        if (spentMicros < 0) {
            throw new IllegalArgumentException("Spent amount must not be negative.");
        }
        this.planContent = planContent;
        this.workContent = workContent;
        this.finalResult = finalResult;
        this.spentMicros = spentMicros;
        this.status = spentMicros > budgetMicros
                ? RunStatus.BUDGET_EXCEEDED
                : RunStatus.COMPLETED;
    }

    public void fail() {
        if (status != RunStatus.PLANNING
                && status != RunStatus.RUNNING
                && status != RunStatus.REVIEWING) {
            throw invalidTransition(RunStatus.FAILED);
        }
        this.status = RunStatus.FAILED;
    }

    private void transitionFrom(RunStatus expected, RunStatus next) {
        if (status != expected) {
            throw invalidTransition(next);
        }
        this.status = next;
    }

    private IllegalStateException invalidTransition(RunStatus next) {
        return new IllegalStateException(
                "Cannot transition run from %s to %s.".formatted(status, next));
    }

    @PreUpdate
    void updateTimestamp() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getObjective() {
        return objective;
    }

    public RunStatus getStatus() {
        return status;
    }

    public long getBudgetMicros() {
        return budgetMicros;
    }

    public UUID getSupervisorConfigId() {
        return supervisorConfigId;
    }

    public long getSpentMicros() {
        return spentMicros;
    }

    public String getPlanContent() {
        return planContent;
    }

    public String getWorkContent() {
        return workContent;
    }

    public String getFinalResult() {
        return finalResult;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
